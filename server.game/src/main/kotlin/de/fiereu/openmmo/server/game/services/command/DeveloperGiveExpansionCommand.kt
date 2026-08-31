package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.net.game.packets.PokedexSpeciesUnlockPacket
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class DeveloperGiveExpansionCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val expansion: ExpansionSpeciesRegistry,
    private val pokemonFactory: WildMonFactory,
    private val storyPlayer: StoryPlayerService,
) : ChatCommand {
  override val name = "givemon"
  override val aliases = listOf("devgivemon")
  override val usage = "/givemon <species> [level] [shiny] - partial names list matches"
  override val description = "adds a Pokemon from the Expansion catalog to your party"
  override val permission = CharacterPermissions.DEVELOPER

  init {
    if (tools.enabled) {
      val report = expansion.report()
      log.info {
        "[ExpansionCatalog] baseSpecies=${report.baseSpecies} forms=${report.forms} " +
            "records=${report.totalSpeciesAndForms} highestExpansionId=${report.highestOriginalId} " +
            "clientMapped=${report.knownClientMappings} " +
            "clientUnmapped=${report.requiringClientMapping} missing=${report.missingRequiredData}"
      }
    }
  }

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val speciesArgument = ctx.args.getOrNull(0)
    val levelArgument = ctx.args.getOrNull(1)
    val shinyArgument = ctx.args.getOrNull(2)
    // Saying which argument was wrong, rather than only reprinting the usage line, so a rejected
    // command is diagnosable from the chat reply instead of silently looking like nothing happened.
    if (speciesArgument == null) {
      ctx.reply(usage)
      return
    }
    // Many species exist only as named forms: there is no SPECIES_XERNEAS, only XERNEAS_NEUTRAL
    // and XERNEAS_ACTIVE, so a bare name falls back to the base form of a prefix match.
    val entry = expansion.get(speciesArgument) ?: resolveForm(speciesArgument, ctx) ?: return
    val level = if (levelArgument == null) DEFAULT_LEVEL else levelArgument.toIntOrNull()
    if (level == null || level !in 1..100) {
      ctx.reply("Level must be a number from 1 to 100, not \"${levelArgument.orEmpty()}\".")
      return
    }
    if (shinyArgument != null && shinyArgument != "shiny") {
      ctx.reply("The third argument may only be the word shiny.")
      return
    }

    val created = pokemonFactory.create(entry.serverId, level, BattleRng())
    if (created == null) {
      val reason = "Expansion data cannot yet map its type, ability, egg group, or growth rate"
      log.error {
        "[ExpansionClient] REJECTED ${entry.stableId} serverId=${entry.serverId} reason=$reason"
      }
      ctx.reply("Could not create ${entry.symbol}: $reason.")
      return
    }

    val wireId = entry.clientWireId
    if (wireId == null) {
      val reason = "no verified PokeMMO species mapping"
      log.warn {
        "[ExpansionClient] BLOCKED ${entry.stableId} expansionId=${entry.originalId} " +
            "serverId=${entry.serverId} wireId=NONE stage=party-serialization reason=$reason"
      }
      ctx.reply(
          "Server creation succeeded for ${entry.displayName} (Expansion ${entry.originalId}, " +
              "server ${entry.serverId}), but it was not added: $reason.")
      return
    }

    // Species the client does not already own only exist if the generated content is installed.
    if (entry.isNewToClient && !tools.expansionClientContent) {
      val reason = "generated Expansion client content is not enabled"
      log.warn { "[ExpansionClient] BLOCKED ${entry.stableId} wireId=$wireId reason=$reason" }
      ctx.reply("${entry.displayName} was not added: $reason.")
      return
    }
    if (!entry.clientContentCompatible) {
      val reason =
          if (entry.usesClientUnsupportedType) "the current client cannot represent its type"
          else "required client sprite/icon sources are incomplete"
      log.warn { "[ExpansionClient] BLOCKED ${entry.stableId} wireId=$wireId reason=$reason" }
      ctx.reply("${entry.displayName} was not added: $reason.")
      return
    }

    val moveIds = created.moves.map { it.id.toInt() }.filter { it != 0 }
    val isShiny = shinyArgument == "shiny"
    val given =
        storyPlayer.givePokemon(
            ctx.session, ctx.state, entry.serverId, level, moveIds, isShiny = isShiny)
    if (given == null) {
      ctx.reply("Could not add ${entry.displayName}; make sure your party has an open slot.")
      return
    }
    // Without this the species stays a silhouette in the Pokedex even while it sits in the party.
    ctx.session.send(PokedexSpeciesUnlockPacket(wireId.toShort()))
    log.info {
      "[ExpansionClient] SENT ${entry.stableId} expansionId=${entry.originalId} " +
          "serverId=${entry.serverId} wireId=$wireId stage=party"
    }
    ctx.reply(
        "Added ${if (isShiny) "shiny " else ""}${entry.displayName} at level $level " +
            "(client species $wireId).")
  }

  /**
   * Resolves a partial, misspelled, or form-bearing name, or answers with suggestions.
   *
   * The chat line only reaches the server on enter, so "suggestions while typing" means: type any
   * fragment and send it, and the matches come back as a list to pick from - `alolan` lists every
   * Alolan form under its exact usable name. Matching prefers a whole-name prefix, then any
   * substring, then close misspellings; a lone match at the strongest tier is given directly
   * instead of being echoed back, with the base form preferred so "xerneas" is not a battle form.
   */
  private suspend fun resolveForm(argument: String, ctx: CommandContext): ExpansionSpeciesDef? {
    val query = regionAliases(normalize(argument.removePrefix("SPECIES_")))
    if (query.isEmpty()) {
      ctx.reply(usage)
      return null
    }
    val keyed = expansion.all().map { it.symbol.removePrefix("SPECIES_").lowercase() to it }
    val prefix = keyed.filter { (key, _) -> normalize(key).startsWith(query) }
    val substring = keyed.filter { (key, _) -> query in normalize(key) }
    val fuzzy =
        keyed.filter { (key, entry) ->
          editDistance(normalize(entry.displayName), query) <= if (query.length > 4) 2 else 1
        }
    val matches = (prefix.ifEmpty { substring }.ifEmpty { fuzzy }).distinctBy { it.second.stableId }
    if (matches.isEmpty()) {
      log.info { "[ExpansionClient] UNKNOWN species argument \"$argument\"" }
      ctx.reply("Nothing matches \"$argument\". Try any part of the name, like sand or alolan.")
      return null
    }
    // "sandshrew" should give Sandshrew, not ask to choose between it and its Alolan form; a base
    // form whose whole name matches the query wins outright.
    matches
        .singleOrNull { (key, entry) -> !entry.isForm && normalize(key) == query }
        ?.let {
          return it.second
        }
    matches.singleOrNull()?.let {
      return it.second
    }
    val shown = matches.take(MAX_SUGGESTIONS).joinToString(", ") { (key, _) -> key }
    val more = matches.size - MAX_SUGGESTIONS
    ctx.reply(
        "Matches for \"$argument\": $shown" +
            (if (more > 0) " and $more more (narrow it down)" else "") +
            ". Send /givemon with one of these names.")
    return null
  }

  private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

  /**
   * The Expansion names regional forms by the region, not the adjective: it is VULPIX_ALOLA, never
   * VULPIX_ALOLAN. People type the adjective, so it is folded down to what the symbols use.
   */
  private fun regionAliases(query: String): String =
      query
          .replace("alolan", "alola")
          .replace("galarian", "galar")
          .replace("hisuian", "hisui")
          .replace("paldean", "paldea")

  /** Plain Levenshtein, small strings only, for catching misspellings like "sandshrw". */
  private fun editDistance(left: String, right: String): Int {
    if (left == right) return 0
    var previous = IntArray(right.length + 1) { it }
    for (i in 1..left.length) {
      val current = IntArray(right.length + 1)
      current[0] = i
      for (j in 1..right.length) {
        val substitution = previous[j - 1] + if (left[i - 1] == right[j - 1]) 0 else 1
        current[j] = minOf(previous[j] + 1, current[j - 1] + 1, substitution)
      }
      previous = current
    }
    return previous[right.length]
  }

  private companion object {
    const val DEFAULT_LEVEL = 50
    const val MAX_SUGGESTIONS = 20
  }
}
