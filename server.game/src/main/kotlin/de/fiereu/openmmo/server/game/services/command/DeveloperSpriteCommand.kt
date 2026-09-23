package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.NpcService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Draws one throwaway npc with a given sprite id on the caller's own tile, so what the client shows
 * for an id is seen rather than inferred from its loaders. `/devsprite 162` takes the id from the
 * map's region's ROM sprite set; `/devsprite 162 10` from the client's own set. Each call replaces
 * the previous probe; walking off the map drops it.
 */
@Singleton
class DeveloperSpriteCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val npcService: NpcService,
) : ChatCommand {
  override val name = "devsprite"
  override val usage = "/devsprite <graphicsId> [spriteRegion]"
  override val description = "spawns a test npc with a sprite id on your tile (spriteRegion 10 = the client's own set)"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val graphicsId = ctx.args.getOrNull(0)?.toIntOrNull()
    if (graphicsId == null) {
      ctx.reply("Usage: $usage")
      return
    }
    val state = ctx.state
    val spriteRegion = ctx.args.getOrNull(1)?.toIntOrNull() ?: state.regionId
    npcService.spawnProbe(ctx.session, state.regionId, state.bankId, state.mapId, state.x.toInt(), state.y.toInt(), spriteRegion, graphicsId)
    ctx.reply("Probe: sprite set $spriteRegion, graphics $graphicsId at (${state.x}, ${state.y}).")
  }
}
