package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

/**
 * The map-directory tour: automated warping with a human naming what renders.
 *
 * `/warp tour [bank] [map]` warps to the first id and captures the player's plain chat: typing what
 * the location banner says binds that name to the current ids, saves it, and immediately warps to
 * the next map id. "skip" advances without naming, "back" steps backwards, "nextbank" jumps to the
 * next bank, "stop" ends the tour. Every name lands in two places at once: the bookmark file, so
 * `/tp <name>` works from that moment on, and `map-directory.txt`, the table the decomp warp
 * generator will eventually consume.
 */
@Singleton
class MapTourService
@Inject
constructor(
    private val warps: WarpService,
    private val scope: CoroutineScope,
) {

  private val directory = File("map-directory.txt")
  private val bookmarks = File("tp-bookmarks.txt")

  /** Where the auto-tour currently stands, for the screenshot capture to label its images. */
  private val positionFile = File("tour-position.txt")

  @Volatile private var autoJob: Job? = null

  fun start(ctx: SessionContext, charId: Long, region: Int, bank: Int, map: Int) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    state.touring = true
    goTo(ctx, charId, region, bank, map)
    ctx.send(
        notice(
            "Tour started at $region:$bank:$map. Type the location name you see " +
                "(plain chat), or: skip, back, nextbank, stop."))
  }

  /**
   * The hands-off variant: warps to the next map id on a timer and writes each position to
   * [positionFile] so a screen-capture loop can label its images. Nobody needs to know or type any
   * names - the captured renders get identified afterwards. Chat "stop" (or /warp tour stop) ends
   * it; the last few ids per bank are usually void maps, "nextbank" in chat jumps onward.
   */
  fun startAuto(
      ctx: SessionContext,
      charId: Long,
      region: Int,
      bank: Int,
      map: Int,
      intervalSeconds: Int,
  ) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    state.touring = true
    autoJob?.cancel()
    autoJob =
        scope.launch {
          // The NDS map space is a single header index split into bytes: bank is the low byte,
          // map the high one - decoded from the client's own out-of-bounds errors, whose indexes
          // rose by 256 per map step against an array of the ROM's header count. Iterating the
          // index visits every real map exactly once and nothing invalid.
          var index = (map shl 8) or (bank and 0xFF)
          val limit = HEADER_COUNTS.getOrDefault(region, 512)
          while (state.touring && index < limit) {
            goTo(ctx, charId, region, index and 0xFF, index shr 8)
            positionFile.writeText("$region;${index and 0xFF};${index shr 8}\n")
            delay(intervalSeconds * 1000L)
            index++
          }
          state.touring = false
          positionFile.writeText("stopped\n")
        }
    ctx.send(
        notice(
            "Auto-tour started at $region:$bank:$map, one map every " +
                "$intervalSeconds seconds. Type stop to end, nextbank to jump banks."))
  }

  /** Handles one captured chat line while touring; true when it was consumed by the tour. */
  fun onChat(ctx: SessionContext, charId: Long, message: String): Boolean {
    val state = ctx.attributes[PLAYER_STATE] ?: return false
    if (!state.touring) return false
    val region = state.regionId
    val bank = state.bankId
    val map = state.mapId
    when (val text = message.trim().lowercase()) {
      "stop" -> {
        state.touring = false
        autoJob?.cancel()
        autoJob = null
        ctx.send(notice("Tour ended at $region:$bank:$map. Directory saved."))
      }
      "skip" -> goToIndex(ctx, charId, region, headerIndex(bank, map) + 1)
      "back" -> goToIndex(ctx, charId, region, headerIndex(bank, map) - 1)
      "nextbank" -> goToIndex(ctx, charId, region, headerIndex(bank, map) + 16)
      "" -> ctx.send(notice("Type the banner name, or skip / back / nextbank / stop."))
      else -> {
        record(region, bank, map, message.trim())
        ctx.send(notice("$region:$bank:$map = \"${message.trim()}\" saved; next map."))
        goTo(ctx, charId, region, bank, map + 1)
        return true.also { log.info { "[Tour] $region:$bank:$map named '$text'" } }
      }
    }
    return true
  }

  private fun goTo(ctx: SessionContext, charId: Long, region: Int, bank: Int, map: Int) {
    warps.executeRawWarp(ctx, charId, region, bank, map, 10, 10)
  }

  private fun goToIndex(ctx: SessionContext, charId: Long, region: Int, index: Int) {
    val safe = index.coerceIn(0, HEADER_COUNTS.getOrDefault(region, 512) - 1)
    goTo(ctx, charId, region, safe and 0xFF, safe shr 8)
  }

  private fun headerIndex(bank: Int, map: Int): Int = (map shl 8) or (bank and 0xFF)

  private companion object {
    /** Map header counts per region ROM: White 436, Platinum 593, HeartGold 540. */
    val HEADER_COUNTS = mapOf(2 to 436, 3 to 593, 4 to 540)
  }

  private fun record(region: Int, bank: Int, map: Int, name: String) {
    val clean = name.replace(';', ',')
    directory.appendText("$region;$bank;$map;$clean\n")
    // Also a bookmark, replacing any earlier binding of the same name, so /tp works right away.
    val kept =
        (if (bookmarks.isFile) bookmarks.readLines() else emptyList()).filterNot {
          it.substringBefore(';').equals(clean, ignoreCase = true)
        }
    bookmarks.writeText((kept + "$clean;$region;$bank;$map;10;10").joinToString("\n") + "\n")
  }
}
