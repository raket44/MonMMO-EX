package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
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
 * The region-link ferry captain's talk (retail strings 16780100..16780108: "Ahoy there! Where
 * would you like to disembark?", the four-badge rule, "Please board the ferry and wait for
 * departure.", "Come back when you are ready."). A region the character has never been to is
 * entered at its new-game start, so its story begins; one already started lands at its harbour.
 * The lines are drawn through the ROM's bare "{STR_VAR_1}" text with the sentence buffered in,
 * so no client-string dialog path is needed.
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
    val badges = badgeCount(stored, current)
    if (badges < BADGES_NEEDED) {
      say(ctx, current, "Sorry, the ferry service isn't\navailable at this time.\nYou need at least $BADGES_NEEDED badges\nto travel to a new region.")
      return
    }
    say(ctx, current, "Ahoy there!\nWhere would you like to disembark?")
    for (dest in DESTINATIONS) {
      if (dest == current) continue
      val started = hasStarted(stored, dest)
      val where = if (started) harbourName(dest) else startName(dest)
      if (!ask(ctx, current, "Sail to ${dest.displayName.uppercase()}?\n($where)")) continue
      say(ctx, current, "Please board the ferry and\nwait for departure.")
      ctx.closeMessage()
      log.info { "Ferry: char=$charId ${current.name} -> ${dest.name} (${if (started) "harbour" else "start"})" }
      if (started) toHarbour(ctx, charId, dest) else toStart(ctx, stored, dest)
      return
    }
    say(ctx, current, "Come back when you are ready.")
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

  /** A region whose story has begun has flags under its prefix; DS regions never do. */
  private fun hasStarted(stored: StoredCharacter, region: Region): Boolean =
      stored.storyFlags.any { it.startsWith("${region.name.lowercase()}/") }

  private fun startName(r: Region) =
      when (r) {
        Region.KANTO -> "PALLET TOWN"
        Region.HOENN -> "LITTLEROOT TOWN"
        Region.UNOVA -> "NUVEMA TOWN"
        Region.SINNOH -> "TWINLEAF TOWN"
        Region.JOHTO -> "NEW BARK TOWN"
      }

  private fun harbourName(r: Region) =
      when (r) {
        Region.KANTO -> "VERMILION CITY"
        Region.HOENN -> "SLATEPORT CITY"
        Region.UNOVA -> "CASTELIA CITY"
        Region.SINNOH -> "CANALAVE CITY"
        Region.JOHTO -> "OLIVINE CITY"
      }

  /** The ROM text that is only "{STR_VAR_1}", so [text] buffered into STR_VAR_1 is the whole box. */
  private fun line(current: Region): DialogLine =
      object : DialogLine {
        override val textId = if (current == Region.HOENN) HOENN_STR_VAR_LINE else KANTO_STR_VAR_LINE
      }

  private suspend fun say(ctx: ScriptContext, current: Region, text: String) {
    ctx.bufferText(1, text)
    ctx.say(line(current))
  }

  private suspend fun ask(ctx: ScriptContext, current: Region, text: String): Boolean {
    ctx.bufferText(1, text)
    return ctx.askYesNo(line(current))
  }

  private companion object {
    const val BADGES_NEEDED = 4
    /** SevenIsland_House_Room1_Text_StrVar1_1 / SootopolisCity_MysteryEventsHouse_1F_Text_StrVar1Tie. */
    const val KANTO_STR_VAR_LINE = 4313204
    const val HOENN_STR_VAR_LINE = 274649488
    val DESTINATIONS = listOf(Region.HOENN, Region.UNOVA, Region.SINNOH, Region.JOHTO, Region.KANTO)
    /** DS ROM headers (bank = low byte, map = high byte) of the start towns and harbours. */
    val NDS_STARTS = mapOf(Region.UNOVA to (133 to 1), Region.SINNOH to (155 to 1), Region.JOHTO to (60 to 0))
    val NDS_HARBOURS = mapOf(Region.UNOVA to (28 to 0), Region.SINNOH to (33 to 0), Region.JOHTO to (77 to 0))
  }
}
