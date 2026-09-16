package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flips a party monster's rarity flags (shiny / alpha / secret) to look at the client's renders:
 * the record flag bits (icon outline, party sprite scale, dex marks) and the follower flag byte
 * (0x80 large). A follower already set is refreshed on the next map load or re-selection.
 */
@Singleton
class DeveloperRarityCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val characters: CharacterStore,
) : ChatCommand {
  override val name = "rarity"
  override val usage = "/rarity <party slot 1-6> [shiny] [alpha] [secret] | plain"
  override val description = "sets a party monster's shiny / alpha / secret flags"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val slot = ctx.args.getOrNull(0)?.toIntOrNull()
    if (slot == null || slot !in 1..6) {
      ctx.reply("Usage: $usage")
      return
    }
    val charId = ctx.state.characterId ?: return
    val party = characters.getCharacter(charId)?.pokemon ?: return
    val mon = party.getOrNull(slot - 1)
    if (mon == null) {
      ctx.reply("No monster in party slot $slot.")
      return
    }
    val flags = ctx.args.drop(1).map { it.lowercase() }
    val updated =
        mon.copy(
            isShiny = "shiny" in flags,
            isAlpha = "alpha" in flags,
            isSecret = "secret" in flags,
        )
    characters.updatePokemon(charId, updated)
    val refreshed = characters.getCharacter(charId)?.pokemon ?: return
    ctx.session.send(
        PokemonContainerPacket(
            container = PokemonContainer.PARTY,
            hasChange = true,
            delete = false,
            pokemon = refreshed,
        ))
    ctx.reply(
        "Slot $slot is now ${listOfNotNull("shiny".takeIf { updated.isShiny }, "alpha".takeIf { updated.isAlpha }, "secret".takeIf { updated.isSecret }).ifEmpty { listOf("plain") }.joinToString(" ")}." +
            " Re-select it as follower (or change maps) to refresh the overworld sprite.")
  }
}
