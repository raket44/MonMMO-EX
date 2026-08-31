package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.MapTourService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The map-directory tour. `/tour auto 100 1` warps through raw map ids on a timer while the capture
 * script screenshots each render for identification; `/tour 100 1` is the manual variant where
 * plain chat names the current map. "/warp ..." never worked for this - the client swallows
 * anything starting with /w as its own whisper command and the server never hears it.
 */
@Singleton
class DeveloperTourCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val tour: MapTourService,
) : ChatCommand {
  override val name = "tour"
  override val usage = "/tour auto [bank] [map] [seconds] | /tour [bank] [map] | /tour stop"
  override val description = "auto-warps through map ids to build the map directory"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val state = ctx.state
    if (ctx.args.firstOrNull() == "stop") {
      state.touring = false
      ctx.reply("Tour stopped.")
      return
    }
    val auto = ctx.args.firstOrNull() == "auto"
    val numbers = ctx.args.drop(if (auto) 1 else 0).mapNotNull { it.toIntOrNull() }
    // Region first when three or more numbers are given; otherwise the player's current region,
    // which must already be an NDS one - touring Kanto's ids sent the player into the void.
    val region = if (numbers.size >= 3) numbers[0] else state.regionId
    if (region !in 2..4) {
      ctx.reply(
          "Tours cover the NDS regions: /tour auto 2 100 1 for Unova, 3 for Sinnoh, 4 for Johto.")
      return
    }
    val offset = if (numbers.size >= 3) 1 else 0
    val bank = numbers.getOrNull(offset) ?: state.bankId
    val map = numbers.getOrNull(1 + offset) ?: 1
    val interval = numbers.getOrNull(2 + offset) ?: 5
    if (auto) tour.startAuto(ctx.session, ctx.characterId, region, bank, map, interval)
    else tour.start(ctx.session, ctx.characterId, region, bank, map)
  }
}
