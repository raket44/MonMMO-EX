package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.net.game.packets.ServerMessageArg
import de.fiereu.openmmo.net.game.packets.ServerMessagePacket
import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.server.game.services.command.NdsMapSpawns
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.NewGameStarts
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The region-link ferry captain's talk, in the client's own strings (16780100..16780108) with its
 * own region list (registry set 10 + region): four badges here, pick a region, confirm, sail. A
 * region with no badge yet is entered at its new-game start, so its story begins; one with a
 * badge lands at its harbour (next to that region's captain, or the DS town's entry tile).
 */
@Singleton
class FerryTravel
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val placements: FerryPlacements,
    private val warps: WarpService,
    private val ndsWarps: NdsWarps,
) {

  fun script(stored: StoredCharacter, here: FerryPlacements.Placement): Script = Script { ctx -> talk(ctx, stored, here) }

  private suspend fun talk(ctx: ScriptContext, stored: StoredCharacter, here: FerryPlacements.Placement) {
    val charId = stored.info.id
    val current = Region.byId(here.regionId) ?: return
    if (badgeCount(stored, current) < BADGES_NEEDED) {
      // "Sorry, the ferry service isn't available at this time. You need at least {00} badges..."
      ctx.session.send(
          ServerMessagePacket(
              NEED_BADGES,
              listOf(ServerMessageArg(0, RAW_STRING, false, 0, null, null, BADGES_NEEDED.toString(), null)),
              showOnMap = true,
              mode = null))
      return
    }
    // "Ahoy there! Where would you like to disembark?" with the client's own region list under
    // it: registry set 10 + current region = the other regions in the client's ROM order
    // (f/xq1.CH0: Kanto, Johto, Hoenn, Sinnoh, Unova) followed by Cancel (f/Lx.R40).
    val choices = MENU_ORDER.filter { it != current }
    val pick = ctx.builtinMenu(line(WHERE_TO), REGION_MENU_SET_BASE + here.regionId)
    val dest = choices.getOrNull(pick - 1)
    if (dest == null) {
      ctx.say(line(COME_BACK))
      return
    }
    // "Please board the ferry and wait for departure." as the yes/no.
    if (!ctx.askYesNo(line(BOARD))) {
      ctx.say(line(COME_BACK))
      return
    }
    ctx.closeMessage()
    val started = badgeCount(stored, dest) > 0
    log.info { "Ferry: char=$charId ${current.name} -> ${dest.name} (${if (started) "harbour" else "start"})" }
    if (started) toHarbour(ctx, charId, dest) else toStart(ctx, stored, dest)
  }

  private suspend fun toHarbour(ctx: ScriptContext, charId: Long, dest: Region) {
    val id = dest.wireValue.toInt()
    placements.harbour(id)?.let { (map, p) ->
      val (x, y) = placements.landing(map, p)
      ctx.warp(id, map.bankId.toInt(), map.mapId.toInt(), x, y, Direction.DOWN)
      return
    }
    // DS regions: the harbour town's own ROM entry tile.
    val (bank, mapId) = NDS_HARBOURS[dest] ?: return
    rawArrival(ctx, charId, id, bank, mapId)
  }

  private suspend fun toStart(ctx: ScriptContext, stored: StoredCharacter, dest: Region) {
    val charId = stored.info.id
    val id = dest.wireValue.toInt()
    if (dest == Region.KANTO || dest == Region.HOENN) {
      // The region's new game: its opening flags and vars, then the same first map a fresh
      // character of that region gets (Emerald's moving truck, FireRed's bedroom).
      val start = NewGameStarts.forRegion(dest, female = ctx.playerGender() != 0)
      start.storyFlags.forEach { characterStore.setStoryFlag(charId, it) }
      start.storyVars.forEach { (k, v) -> characterStore.setStoryVar(charId, k, v) }
      start.dynamicWarp?.let { dw ->
        ctx.setDynamicWarp(dw.regionId.toInt(), dw.bankId.toInt(), dw.mapId.toInt(), dw.x.toInt(), dw.y.toInt(), dw.facing)
      }
      characterStore.flushCharacterAsync(charId)
      ctx.warp(id, start.bankId.toInt(), start.mapId.toInt(), start.x.toInt(), start.y.toInt(), Direction.DOWN)
      return
    }
    val (bank, mapId) = NDS_STARTS[dest] ?: return
    rawArrival(ctx, charId, id, bank, mapId)
  }

  /** A DS map the server does not host: land on the ROM header's entry tile, on its rail if any. */
  private fun rawArrival(ctx: ScriptContext, charId: Long, regionId: Int, bank: Int, map: Int) {
    val (x, y) = NdsMapSpawns.at(regionId, bank, map) ?: (10 to 10)
    val rail = ndsWarps.nearestRailLine(regionId, bank, map, x, y)
    warps.executeRawWarp(ctx.session, charId, regionId, bank, map, x, y, rail)
  }

  private fun badgeCount(stored: StoredCharacter, region: Region): Int {
    val prefix = "${region.name.lowercase()}/FLAG_BADGE0"
    return (1..8).count { stored.storyFlags.contains("${prefix}${it}_GET") }
  }

  /** Client string ids are valid dialog text ids (one registry with the ROM texts, f/nV0.Id1). */
  private fun line(stringId: Int): DialogLine = object : DialogLine { override val textId = stringId }

  private companion object {
    const val BADGES_NEEDED = 4
    /** strings_en.xml "REGION LINK FERRY" block. */
    const val WHERE_TO = 16780100
    const val BOARD = 16780103
    const val COME_BACK = 16780104
    const val NEED_BADGES = 16780108
    const val RAW_STRING = 5
    /** f/Lx.R40: category-10 sets 10..14 list the other regions for current region 0..4. */
    const val REGION_MENU_SET_BASE = 10
    /** f/xq1.CH0, the order the client lists regions in. */
    val MENU_ORDER = listOf(Region.KANTO, Region.JOHTO, Region.HOENN, Region.SINNOH, Region.UNOVA)
    /** DS ROM headers (bank = low byte, map = high byte) of the start towns and harbours. */
    val NDS_STARTS = mapOf(Region.UNOVA to (133 to 1), Region.SINNOH to (155 to 1), Region.JOHTO to (60 to 0))
    val NDS_HARBOURS = mapOf(Region.UNOVA to (28 to 0), Region.SINNOH to (33 to 0), Region.JOHTO to (77 to 0))
  }
}
