package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.net.game.packets.WorldClock

/**
 * Berry farming, the retail way: the client owns the UI (its Plant Seeds window, the plant sprite
 * 10/1062 with a frame per growth stage, the water droplets) and the server owns every plot's
 * state per character, in real hours. Hoenn and Unova share this; the ROM's own soil spots (the
 * OBJ_EVENT_GFX_BERRY_TREE objects on Routes 102-130) are the Hoenn plots.
 *
 * The rules come from retail's berry table (`monmmo/berries.csv`: seed flavour degrees, grow /
 * water / wither hours, yields - see reference/berries/README.md) and the PokeMMO wiki: a plot
 * takes 1-3 seeds whose flavour degrees add up to a berry's; thirst is 0-5 droplets; dry (0)
 * lowers the yield and kills after a while, watering at 5 floods (yield down) and watering a
 * flooded plant kills it; a ripe berry not picked within the wither time dies. The interval
 * model below (3 droplets at planting, -1 every `otherWaterHours` after the first
 * `firstWaterHours`, +2 per watering) is fitted to the guide's "water within 7 h, then again
 * before 12 h" for a 16 h berry; every constant here is one place to tune.
 *
 * Persistence is the character's story vars under `berry/<region>/<bank>/<map>/<idx>/...`, so a
 * plot is per character (retail: each player farms their own plants).
 */
object BerryPlots {
  /** OBJ_EVENT_GFX_BERRY_TREE in the GBA map data - the soil spots. */
  const val BERRY_TREE_GFX = 60

  /** Seed items: 1030 Plain Spicy .. 1039 Very Sour (the client's own ids). */
  const val FIRST_SEED_ITEM = 1030
  const val LAST_SEED_ITEM = 1039
  /** The client's berry items are the Gen 5 indexes in the 5000 band (ItemRegistry). */
  const val BERRY_WIRE_BASE = 5000

  /** Watering tools by region: Hoenn's Wailmer Pail, Unova's Sprayduck (retail: one per region). */
  // Client ids: the Hoenn table entry (1000 + GBA 268) and the Gen 5-band Sprayduck.
  val WATERING_TOOL = mapOf(1 to 1268, 2 to 5448)

  const val DROPLETS_AT_PLANTING = 3
  const val DROPLETS_PER_WATERING = 2
  const val MAX_DROPLETS = 5
  /** Hours a plant survives fully dry before it dies. */
  const val DRY_DEATH_HOURS = 8.0
  /** Yield lost by a flooding. */
  const val FLOOD_PENALTY = 1

  /** Client plant stages (m07.gs0: 0/1 seed frame 0, 2 sprout, 3 sapling, 4 flowering, 5 ripe, 6 dead, 7 empty soil). */
  const val STAGE_SEED = 1
  const val STAGE_SPROUT = 2
  const val STAGE_SAPLING = 3
  const val STAGE_FLOWERING = 4
  const val STAGE_RIPE = 5
  const val STAGE_DEAD = 6
  const val STAGE_EMPTY = 7

  data class Berry(
      val gen5Index: Int,
      val degrees: List<Int>,
      val growHours: Double,
      val firstWaterHours: Double,
      val otherWaterHours: Double,
      val reduceHours: Double,
      val witherHours: Double,
      val minYield: Int,
      val maxYield: Int,
  ) {
    val wireItemId: Int
      get() = BERRY_WIRE_BASE + gen5Index
  }

  val berries: List<Berry> by lazy {
    val text = checkNotNull(BerryPlots::class.java.getResourceAsStream("/monmmo/berries.csv")) { "monmmo/berries.csv missing" }
    text.bufferedReader().readLines().drop(1).filter { it.isNotBlank() }.map { line ->
      val c = line.split(';')
      Berry(
          gen5Index = c[0].toInt(),
          degrees = c.subList(1, 6).map(String::toInt),
          growHours = c[6].toDouble(),
          firstWaterHours = c[7].toDouble(),
          otherWaterHours = c[8].toDouble(),
          reduceHours = c[9].toDouble(),
          witherHours = c[10].toDouble(),
          minYield = c[11].toInt(),
          maxYield = c[12].toInt())
    }
  }

  /** A seed item's flavour degrees: Plain = 1 of its flavour, Very = 2. */
  fun seedDegrees(seedItem: Int): List<Int>? {
    if (seedItem !in FIRST_SEED_ITEM..LAST_SEED_ITEM) return null
    val n = seedItem - FIRST_SEED_ITEM
    val flavour = n % 5
    val degree = if (n >= 5) 2 else 1
    return List(5) { if (it == flavour) degree else 0 }
  }

  /** The berry a seed combination grows, or null when "that seed combination would not grow anything". */
  fun berryFor(seeds: List<Int>): Berry? {
    if (seeds.isEmpty() || seeds.size > 3) return null
    val total = IntArray(5)
    for (seed in seeds) {
      val d = seedDegrees(seed) ?: return null
      for (i in 0 until 5) total[i] += d[i]
    }
    return berries.firstOrNull { b -> (0 until 5).all { b.degrees[it] == total[it] } }
  }

  // --- persisted plot state (story vars) ---------------------------------------------------

  fun key(regionId: Int, bankId: Int, mapId: Int, idx: Int, field: String) = "berry/$regionId/$bankId/$mapId/$idx/$field"

  class Plot(private val vars: Map<String, Int>, private val prefix: String) {
    /** Seeds packed one per byte (item - 1029, so 1..10; 0 = none); 0 = nothing planted. */
    val seedsPacked: Int get() = vars["$prefix/seeds"] ?: 0
    val plantedMinute: Int get() = vars["$prefix/planted"] ?: 0
    val droplets: Int get() = vars["$prefix/water"] ?: 0
    val wateredMinute: Int get() = vars["$prefix/waterAt"] ?: 0
    val dryMinutes: Int get() = vars["$prefix/dry"] ?: 0
    val floods: Int get() = vars["$prefix/floods"] ?: 0
    val dead: Int get() = vars["$prefix/dead"] ?: 0

    val seeds: List<Int>
      get() = (0 until 3).mapNotNull { i -> ((seedsPacked shr (8 * i)) and 0xFF).takeIf { it != 0 }?.let { it + FIRST_SEED_ITEM - 1 } }
    val planted: Boolean get() = seedsPacked != 0
    val berry: Berry? get() = berryFor(seeds)
  }

  fun packSeeds(seeds: List<Int>): Int =
      seeds.take(3).foldIndexed(0) { i, acc, item -> acc or ((item - FIRST_SEED_ITEM + 1) shl (8 * i)) }

  fun nowMinute(): Int = (WorldClock.nowSecond() / 60).toInt()

  /** Death reasons, in the client's own words (strings 16780314/16780319/16780322). */
  enum class Death { DRY, FLOODED, UNPICKED }

  data class View(
      val stage: Int,
      val droplets: Int,
      val berry: Berry?,
      val death: Death?,
      /** Hours the plant has spent fully dry, for the yield penalty. */
      val dryHours: Double,
      val ripeHours: Double,
  )

  /**
   * What the plot looks like right now: growth from the planting time, thirst from the last
   * watering, deaths from dryness / flooding / neglect. Pure, so the spawn packet and the dialog
   * agree. Death is not persisted here; the dialog's "clean up the plot?" clears the plot.
   */
  fun view(plot: Plot, nowMinute: Int = nowMinute()): View {
    if (!plot.planted) return View(STAGE_EMPTY, 0, null, null, 0.0, 0.0)
    val berry = plot.berry ?: return View(STAGE_EMPTY, 0, null, null, 0.0, 0.0)
    if (plot.dead != 0) return View(STAGE_DEAD, 0, berry, Death.entries[(plot.dead - 1).coerceIn(0, 2)], 0.0, 0.0)
    val hours = (nowMinute - plot.plantedMinute) / 60.0
    val sinceWater = (nowMinute - plot.wateredMinute) / 60.0
    // Droplets: the first one lasts firstWaterHours, every later one otherWaterHours.
    val intervals =
        if (sinceWater < berry.firstWaterHours) 0
        else 1 + ((sinceWater - berry.firstWaterHours) / berry.otherWaterHours).toInt()
    val droplets = (plot.droplets - intervals).coerceIn(0, MAX_DROPLETS)
    val dryStartHours = berry.firstWaterHours + (plot.droplets - 1).coerceAtLeast(0) * berry.otherWaterHours
    val dryHoursNow = if (plot.droplets == 0) sinceWater else (sinceWater - dryStartHours).coerceAtLeast(0.0)
    val dryHours = plot.dryMinutes / 60.0 + dryHoursNow
    val stage =
        when {
          hours >= berry.growHours -> STAGE_RIPE
          hours >= berry.growHours * 0.75 -> STAGE_FLOWERING
          hours >= berry.growHours * 0.5 -> STAGE_SAPLING
          hours >= berry.growHours * 0.25 -> STAGE_SPROUT
          else -> STAGE_SEED
        }
    val ripeHours = (hours - berry.growHours).coerceAtLeast(0.0)
    val death =
        when {
          dryHoursNow >= DRY_DEATH_HOURS -> Death.DRY
          stage == STAGE_RIPE && ripeHours >= berry.witherHours -> Death.UNPICKED
          else -> null
        }
    return View(if (death != null) STAGE_DEAD else stage, droplets, berry, death, dryHours, ripeHours)
  }

  /**
   * The u16 the client's plant entity reads (f/m07.eL0: stage = bits 8-11, droplets = bits 12-14;
   * the low byte is unused by the renderer). Sent in the spawn packet's second u16 for a berry
   * tree object. HYPOTHESIS from the renderer's bit math, to be confirmed on device.
   */
  fun packedState(view: View): Int = ((view.droplets and 0x7) shl 12) or ((view.stage and 0xF) shl 8)

  /** Harvest count: the retail range, minus a berry per reduceHours spent dry and per flooding, never below one. */
  fun yield(view: View, plot: Plot, random: kotlin.random.Random = kotlin.random.Random.Default): Int {
    val berry = view.berry ?: return 0
    val base = random.nextInt(berry.minYield, berry.maxYield + 1)
    val penalty = (view.dryHours / berry.reduceHours).toInt() + plot.floods * FLOOD_PENALTY
    return (base - penalty).coerceAtLeast(1)
  }
}
