package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.DexProgressService
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Frees a party slot.
 *
 * There is no way to remove a party monster otherwise, which makes the party a one-way door: six
 * gifts in and nothing further can be tested. That is a developer problem rather than a game one,
 * so it lives behind the developer permission with the rest of the tools.
 */
@Singleton
class DeveloperReleaseCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val characters: CharacterStore,
    private val battles: BattleService,
    private val dexProgress: DexProgressService,
) : ChatCommand {
  override val name = "r"
  override val aliases = listOf("release", "devrelease")
  override val usage = "/r <slot>"
  override val description = "removes a Pokemon from your party by slot number, counting from 1"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    if (battles.inBattle(ctx.characterId)) {
      ctx.reply("Finish the battle first.")
      return
    }
    val stored = characters.getCharacter(ctx.characterId)
    if (stored == null) {
      ctx.reply("No character is loaded.")
      return
    }
    val party = stored.pokemon.toList()
    if (party.isEmpty()) {
      ctx.reply("Your party is empty.")
      return
    }
    // Slots are one-based in the reply because that is how the party reads on screen.
    val slot = ctx.args.getOrNull(0)?.toIntOrNull()
    if (slot == null || slot !in 1..party.size) {
      ctx.reply(
          "Slot must be a number from 1 to ${party.size}. Your party: " +
              party.mapIndexed { index, mon -> "${index + 1}=${mon.dexId}" }.joinToString(", "))
      return
    }
    val target = party[slot - 1]
    if (!characters.removePokemon(ctx.characterId, target.id)) {
      ctx.reply("Could not release the Pokemon in slot $slot; the party was left as it was.")
      return
    }
    // Only tell the client once the database no longer has it.
    val remaining = characters.getCharacter(ctx.characterId)?.pokemon ?: emptyList()
    ctx.session.send(
        PokemonContainerPacket(
            container = PokemonContainer.PARTY,
            hasChange = true,
            delete = false,
            pokemon = remaining,
        ))
    dexProgress.refresh(ctx.session, ctx.characterId)
    log.info {
      "[DevRelease] character=${ctx.characterId} released monster=${target.id} " +
          "dexId=${target.dexId} slot=$slot remaining=${remaining.size}"
    }
    ctx.reply("Released the Pokemon in slot $slot. ${remaining.size} left in your party.")
  }
}
