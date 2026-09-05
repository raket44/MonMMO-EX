package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.server.game.services.EncounterService
import javax.inject.Inject
import javax.inject.Singleton

/** Calls a horde out of the ground the player stands on, the way Sweet Scent does. */
@Singleton
class HordeCommand @Inject constructor(private val encounters: EncounterService, private val maps: MapManager) :
    ChatCommand {
  override val name = "horde"
  override val usage = "/horde [3|5]"
  override val description = "starts a horde battle from the current tile's horde table"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val size = ctx.args.getOrNull(0)?.toIntOrNull() ?: 3
    val state = ctx.state
    val map = maps.getMap(state.regionId, state.bankId, state.mapId)
    val problem = encounters.startHorde(ctx.session, ctx.characterId, state, map, size)
    if (problem != null) ctx.reply(problem)
  }
}
