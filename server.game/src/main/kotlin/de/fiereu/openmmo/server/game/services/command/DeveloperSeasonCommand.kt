package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.Season
import de.fiereu.openmmo.net.game.packets.SeasonPacket
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.MapLoadService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Forces the season the server hands out (s2c 0xB9) and reloads the caller's map so the client
 * rebuilds its tilesets under it. The client's own `/seasonoverride` sets the same byte, but only
 * refreshes the DS map models: GBA tilesets take their season when they are built (f/YL1, summer is
 * the base, the other three are applied by f/w1.zt), so a season test on a Kanto map needs the map
 * loaded again under the new value. The override is global and lasts until `calendar` or a restart.
 */
@Singleton
class DeveloperSeasonCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val mapManager: MapManager,
    private val mapLoadService: MapLoadService,
) : ChatCommand {
  override val name = "devseason"
  override val usage = "/devseason <spring|summer|autumn|winter|0-3|calendar>"
  override val description = "forces the season every client is sent, and reloads your map under it"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val word = ctx.args.firstOrNull()?.lowercase()
    if (word == null) {
      ctx.reply(usage)
      ctx.reply("Now: ${Season.current()}${Season.override?.let { " (forced)" } ?: " (calendar)"}")
      return
    }
    val chosen: Season? =
        when (word) {
          "calendar", "auto", "off", "-1" -> null
          else ->
              word.toIntOrNull()?.let { id -> Season.entries.firstOrNull { it.id == id && it != Season.NONE } }
                  ?: Season.entries.firstOrNull { it.name.equals(word, ignoreCase = true) && it != Season.NONE }
                  ?: run {
                    ctx.reply(usage)
                    return
                  }
        }
    Season.override = chosen
    val season = Season.current()
    ctx.session.send(SeasonPacket(season))
    val info = ctx.character.info
    val map = mapManager.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    if (map != null) {
      mapLoadService.resetClientCache(ctx.session, map)
      ctx.session.send(mapManager.createLoadMapPacket(map, reloadPlayer = true, deleteCache = true))
      mapLoadService.preloadConnectedMaps(ctx.session, map)
    }
    ctx.reply(
        if (chosen == null) "Season back on the calendar: $season. Map reloaded."
        else "Season forced to $season for everyone. Map reloaded; others see it on their next map load.")
  }
}
