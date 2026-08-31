package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The universal warp-trigger rulebook: one engine, one editable list per region. Every warp fixture
 * type gets a FIRE mode, a required press direction and an arrival behavior:
 * - fire STAND: fires only from a press while STANDING on the tile. This is the rule for every
 *   walkable fixture (mats, stairs, ladders): the step onto the tile is a plain step the client
 *   animates freely, and the follow-up press into the fixture is a second packet that fires in sync
 *   with the client's own warp animation. (Firing on the step-toward packet cut the animation
 *   short - the "instant warp" glitch.)
 * - fire STEP: fires when stepping TOWARD the tile with the required press - for impassable
 *   fixtures (true doors) the player can never stand on; the client animates off the same input.
 * - fire CONTACT: fires on stepping onto the tile from any direction (panels, escalators,
 *   drop-ladders), held back on arrival boxes by the caller's guard.
 * - arrival REST keeps the player on the landing tile; STEP walks them out (Gen 4 only - GBA
 *   arrivals keep their own settled logic).
 *
 * `warp-rules.txt` (working directory, hot-reloaded) holds `region;type;fire;press;arrival` rows -
 * type is the Gen 4 permission byte (decimal) for regions 3/4 and the TileBehavior NAME for GBA
 * regions. File rows override the built-in defaults, so any single fixture type can be retuned per
 * region without a recompile.
 */
@Singleton
class WarpRules @Inject constructor() {

  enum class Fire {
    STAND,
    STEP,
    CONTACT
  }

  enum class Arrival {
    REST,
    STEP
  }

  data class Rule(val fire: Fire, val press: Direction?, val arrival: Arrival)

  private val file = File("warp-rules.txt")
  @Volatile private var stamp = -1L
  @Volatile private var rules: Map<String, Rule> = emptyMap()
  @Volatile private var tileRules: Map<String, Rule> = emptyMap()

  fun forType(region: Int, type: Int): Rule? = lookup("$region;$type")

  fun forName(region: Int, name: String): Rule? = lookup("$region;$name")

  /**
   * A single tile's own rule (row shape `region;bank;map;x;y;fire;press;arrival`) - the "give this
   * one warp its own rules if needed" lever. Checked before the type rule.
   */
  fun forTile(region: Int, bank: Int, map: Int, x: Int, y: Int): Rule? {
    refresh()
    return tileRules["$region;$bank;$map;$x;$y"]
  }

  private fun lookup(key: String): Rule? {
    refresh()
    return rules[key]
  }

  private fun refresh() {
    val now = if (file.isFile) file.lastModified() else 0L
    if (now == stamp) return
    stamp = now
    val merged = HashMap(DEFAULTS)
    val tiles = HashMap<String, Rule>()
    if (file.isFile) {
      var count = 0
      file.readLines().forEach { raw ->
        val line = raw.substringBefore('#').trim()
        if (line.isEmpty()) return@forEach
        val p = line.split(';').map { it.trim() }
        // Type rows have 5 fields, per-tile rows 8; the last three are always fire/press/arrival.
        if (p.size != 5 && p.size != 8) return@forEach
        val fire =
            runCatching { Fire.valueOf(p[p.size - 3].uppercase()) }.getOrNull() ?: return@forEach
        val press =
            p[p.size - 2]
                .takeIf { it != "-" }
                ?.let { runCatching { Direction.valueOf(it.uppercase()) }.getOrNull() }
        val arrival =
            runCatching { Arrival.valueOf(p[p.size - 1].uppercase()) }.getOrNull() ?: return@forEach
        val rule = Rule(fire, press, arrival)
        if (p.size == 5) merged["${p[0]};${p[1]}"] = rule
        else tiles["${p[0]};${p[1]};${p[2]};${p[3]};${p[4]}"] = rule
        count++
      }
      log.info { "WarpRules: $count rows loaded from ${file.name} (${tiles.size} per-tile)" }
    }
    rules = merged
    tileRules = tiles
  }

  companion object {
    private fun gen4(): Map<String, Rule> {
      val rows = HashMap<String, Rule>()
      // Types named by the pokeheartgold/pokeplatinum decomps; identical numbering.
      val table =
          listOf(
              // type, fire, press, arrival
              Triple(0x3C, Fire.STAND, Direction.UP) to Arrival.REST, // LADDER_NORTH
              Triple(0x3D, Fire.STAND, Direction.DOWN) to Arrival.REST, // LADDER_SOUTH
              Triple(0x3E, Fire.CONTACT, null) to Arrival.REST, // LADDER_DOWN
              Triple(0x5E, Fire.STAND, Direction.RIGHT) to Arrival.REST, // WARP_STAIRS_EAST
              Triple(0x5F, Fire.STAND, Direction.LEFT) to Arrival.REST, // WARP_STAIRS_WEST
              Triple(0x62, Fire.STAND, Direction.RIGHT) to Arrival.REST, // WARP_ENTRANCE_EAST
              Triple(0x63, Fire.STAND, Direction.LEFT) to Arrival.REST, // WARP_ENTRANCE_WEST
              Triple(0x64, Fire.STAND, Direction.UP) to Arrival.REST, // WARP_ENTRANCE_NORTH
              Triple(0x65, Fire.STAND, Direction.DOWN) to Arrival.REST, // WARP_ENTRANCE_SOUTH
              Triple(0x67, Fire.CONTACT, null) to Arrival.REST, // WARP_PANEL
              Triple(0x69, Fire.STEP, Direction.UP) to Arrival.STEP, // DOOR
              Triple(0x6A, Fire.CONTACT, null) to Arrival.STEP, // ESCALATOR_FLIP_FACE
              Triple(0x6B, Fire.CONTACT, null) to Arrival.STEP, // ESCALATOR
              Triple(0x6C, Fire.STEP, Direction.RIGHT) to Arrival.STEP, // WARP_EAST
              Triple(0x6D, Fire.STEP, Direction.LEFT) to Arrival.STEP, // WARP_WEST
              Triple(0x6E, Fire.STEP, Direction.UP) to Arrival.STEP, // WARP_NORTH
              Triple(0x6F, Fire.STEP, Direction.DOWN) to Arrival.STEP, // WARP_SOUTH
          )
      for (region in listOf(3, 4)) {
        for ((head, arrival) in table) {
          rows["$region;${head.first}"] = Rule(head.second, head.third, arrival)
        }
      }
      return rows
    }

    private fun gba(): Map<String, Rule> {
      val rows = HashMap<String, Rule>()
      // Arrivals: only doors walk out. The GBA client plays the stair walk-off itself, so a
      // server step there doubles it (play-verified); ladders, pads and arrow mats rest.
      val table =
          listOf(
              Triple("DOOR", Fire.STEP, Direction.UP) to Arrival.STEP,
              Triple("NON_ANIMATED_DOOR", Fire.CONTACT, null) to Arrival.STEP,
              Triple("LADDER", Fire.CONTACT, null) to Arrival.REST,
              Triple("STAIR_WARP_EAST", Fire.STAND, Direction.RIGHT) to Arrival.REST,
              Triple("STAIR_WARP_WEST", Fire.STAND, Direction.LEFT) to Arrival.REST,
              Triple("NORTH_ARROW_WARP", Fire.STAND, Direction.UP) to Arrival.REST,
              Triple("SOUTH_ARROW_WARP", Fire.STAND, Direction.DOWN) to Arrival.REST,
              Triple("EAST_ARROW_WARP", Fire.STAND, Direction.RIGHT) to Arrival.REST,
              Triple("WEST_ARROW_WARP", Fire.STAND, Direction.LEFT) to Arrival.REST,
          )
      for (region in listOf(0, 1)) {
        for ((head, arrival) in table) {
          rows["$region;${head.first}"] = Rule(head.second, head.third, arrival)
        }
      }
      return rows
    }

    val DEFAULTS: Map<String, Rule> = gen4() + gba()
  }
}
