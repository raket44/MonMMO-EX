package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.items.ItemDef
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.CosmeticsRegistry
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Puts any registered item in the bag - retail items and the imported catalogue alike, since the
 * registry now carries both. Same name matching as /givemon: any fragment lists what it matches, a
 * lone match is granted directly, and misspellings within a couple of letters still land.
 */
@Singleton
class DeveloperGiveItemCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val items: ItemRegistry,
    private val storyPlayer: StoryPlayerService,
) : ChatCommand {
  override val name = "giveitem"
  override val aliases = listOf("gi")
  override val usage = "/giveitem <item> [quantity] - partial names list matches"
  override val description = "adds an item to your bag by name"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    // The quantity is the last argument when it parses as a number, so multi-word names need no
    // quoting: "/giveitem ice stone 3" is the Ice Stone three times.
    val quantity = ctx.args.lastOrNull()?.toIntOrNull()
    val nameArgs = if (quantity == null) ctx.args else ctx.args.dropLast(1)
    val query = normalize(nameArgs.joinToString(""))
    if (query.isEmpty()) {
      ctx.reply(usage)
      return
    }
    if (quantity != null && quantity !in 1..999) {
      ctx.reply("Quantity must be from 1 to 999.")
      return
    }
    val amount = quantity ?: 1
    // Cosmetics live only in the client-generated catalog (CosmeticsRegistry), not the server
    // ItemRegistry - match them by pak name or raw item id before the normal name search.
    val cosmetic =
        CosmeticsRegistry.itemBacked().firstOrNull { normalize(it.name) == query }
            ?: query.toIntOrNull()?.let { CosmeticsRegistry.byItemId(it) }
    if (cosmetic != null) {
      if (!storyPlayer.giveItemById(ctx.session, ctx.state, cosmetic.itemId, amount)) {
        ctx.reply("Could not add ${cosmetic.name} to the bag.")
        return
      }
      log.info {
        "[DevGiveItem] character=${ctx.characterId} cosmetic=${cosmetic.name} " +
            "id=${cosmetic.itemId} quantity=$amount"
      }
      ctx.reply(
          "Added $amount x ${cosmetic.name} (${cosmetic.slot} cosmetic, item ${cosmetic.itemId})." +
              " Relog to unlock it in the customization menu.")
      return
    }
    val item = resolve(query, ctx) ?: return
    if (!storyPlayer.giveItem(ctx.session, ctx.state, item, amount)) {
      ctx.reply("Could not add ${item.name} to the bag.")
      return
    }
    log.info {
      "[DevGiveItem] character=${ctx.characterId} item=${item.name} " +
          "id=${items.idOf(item)} quantity=$amount"
    }
    ctx.reply("Added $amount x ${item.name} (item ${items.idOf(item)}).")
  }

  private fun resolve(query: String, ctx: CommandContext): ItemDef? {
    val keyed = items.all().map { normalize(it.name) to it }
    keyed
        .firstOrNull { (key, _) -> key == query }
        ?.let {
          return it.second
        }
    val prefix = keyed.filter { (key, _) -> key.startsWith(query) }
    val substring = keyed.filter { (key, _) -> query in key }
    val fuzzy =
        keyed.filter { (key, _) -> editDistance(key, query) <= if (query.length > 4) 2 else 1 }
    val matches = (prefix.ifEmpty { substring }.ifEmpty { fuzzy }).distinctBy { it.second }
    if (matches.isEmpty()) {
      ctx.reply("No item matches \"$query\". Try any part of the name, like stone or berry.")
      return null
    }
    matches.singleOrNull()?.let {
      return it.second
    }
    val shown = matches.take(MAX_SUGGESTIONS).joinToString(", ") { (_, item) -> item.name }
    val more = matches.size - MAX_SUGGESTIONS
    ctx.reply(
        "Matching items: $shown" +
            (if (more > 0) " and $more more (narrow it down)" else "") +
            ". Send /giveitem with one of these.")
    return null
  }

  private fun normalize(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

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
    const val MAX_SUGGESTIONS = 20
  }
}
