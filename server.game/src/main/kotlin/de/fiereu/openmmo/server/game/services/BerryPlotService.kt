package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.maps.NpcDef
import de.fiereu.openmmo.net.game.packets.dialog.DialogMessageArg
import de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The berry plot interaction, spoken entirely in the client's own strings (16780300-16780375, the
 * loamy-soil block): look at the plot, plant seeds through the client's Plant Seeds window, water
 * it with the region's tool, pick the berries, clean up a dead plant. State and rules live in
 * [BerryPlots]; the plot object is the ROM's own soil spot, re-sent after every change so the
 * client redraws the plant frame and droplets.
 */
@Singleton
class BerryPlotService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val dialog: DialogService,
    private val npcService: Provider<NpcService>,
) {

  fun isPlot(npc: NpcDef): Boolean = npc.graphicsId == BerryPlots.BERRY_TREE_GFX

  fun script(stored: StoredCharacter, regionId: Int, bankId: Int, mapId: Int, npc: NpcDef): Script =
      Script { ctx -> interact(ctx, stored, regionId, bankId, mapId, npc) }

  private fun plot(stored: StoredCharacter, regionId: Int, bankId: Int, mapId: Int, idx: Int) =
      BerryPlots.Plot(stored.storyVars, "berry/$regionId/$bankId/$mapId/$idx")

  /** The packed plant state for the spawn packet of a soil spot, or null when this is not one. */
  fun spawnState(stored: StoredCharacter?, regionId: Int, bankId: Int, mapId: Int, npc: NpcDef): Int? {
    if (!isPlot(npc)) return null
    val vars = stored?.storyVars ?: return BerryPlots.packedState(BerryPlots.View(BerryPlots.STAGE_EMPTY, 0, null, null, 0.0, 0.0))
    return BerryPlots.packedState(BerryPlots.view(BerryPlots.Plot(vars, "berry/$regionId/$bankId/$mapId/${npc.entityIdx}")))
  }

  private suspend fun interact(ctx: ScriptContext, stored: StoredCharacter, regionId: Int, bankId: Int, mapId: Int, npc: NpcDef) {
    val charId = stored.info.id
    val idx = npc.entityIdx
    val plot = plot(stored, regionId, bankId, mapId, idx)
    val view = BerryPlots.view(plot)
    val tool = BerryPlots.WATERING_TOOL[regionId]
    val hasTool = tool != null && (stored.items[tool] ?: 0) > 0
    val playerName = stored.info.name

    when {
      view.death != null -> {
        val text =
            when (view.death) {
              BerryPlots.Death.DRY -> DEAD_DRY
              BerryPlots.Death.FLOODED -> DEAD_FLOODED
              BerryPlots.Death.UNPICKED -> DEAD_UNPICKED
            }
        if (ctx.askYesNo(line(text))) {
          clear(charId, regionId, bankId, mapId, idx)
          refresh(ctx, regionId, bankId, mapId, idx)
        }
      }
      view.stage == BerryPlots.STAGE_EMPTY -> {
        // "It's soft fertile soil. Would you like to plant seeds?"
        if (!ctx.askYesNo(line(SOFT_SOIL))) return
        if (!hasTool) {
          // "A {STRING_240268} is needed to plant seeds." - the client fills the tool name itself.
          ctx.say(line(NEEDS_TOOL))
          return
        }
        plant(ctx, stored, regionId, bankId, mapId, idx, playerName)
      }
      view.stage == BerryPlots.STAGE_RIPE -> {
        val berry = checkNotNull(view.berry)
        val count = BerryPlots.yield(view, plot)
        ctx.setMessageArg(0, itemArg(0, berry.wireItemId))
        ctx.setMessageArg(1, numberArg(1, count))
        // "There are {01} {00}!\n\nWould you like to pick the {00}?"
        if (ctx.askYesNo(line(RIPE_PICK))) {
          val item = ctx.resolveItemWire(berry.wireItemId)
          if (item == null) {
            log.warn { "Berry wire ${berry.wireItemId} (gen5 ${berry.gen5Index}) has no item" }
            return
          }
          if (ctx.giveItem(item, count)) {
            ctx.setMessageArg(0, textArg(0, playerName))
            ctx.setMessageArg(1, numberArg(1, count))
            ctx.setMessageArg(2, itemArg(2, berry.wireItemId))
            ctx.say(line(PICKED)) // "{00} picked {01} {02}."
            clear(charId, regionId, bankId, mapId, idx)
            refresh(ctx, regionId, bankId, mapId, idx)
          }
        } else {
          ctx.say(line(NOT_PICKED)) // "You did not pick the {00}."
        }
      }
      else -> {
        val berry = checkNotNull(view.berry)
        // Stage line, then the moisture line, then the offer to water.
        when (view.stage) {
          BerryPlots.STAGE_SEED -> ctx.say(line(SEEDS_PLANTED))
          BerryPlots.STAGE_SPROUT -> ctx.say(line(SPROUTED))
          BerryPlots.STAGE_SAPLING -> {
            ctx.setMessageArg(0, itemArg(0, berry.wireItemId))
            ctx.say(line(GROWING_TALLER)) // "The {00} plant has started to grow taller."
          }
          else -> {
            ctx.setMessageArg(0, itemArg(0, berry.wireItemId))
            ctx.setMessageArg(1, textArg(1, adverb(view)))
            ctx.say(line(GROWING_HOW)) // "The {00} plant is growing {01}."
          }
        }
        ctx.say(line(MOISTURE[view.droplets.coerceIn(0, 5)]))
        val plantName = if (view.stage == BerryPlots.STAGE_SEED) "seeds" else "sapling"
        ctx.setMessageArg(0, textArg(0, plantName))
        // "Would you like to water the {00}?"
        if (!ctx.askYesNo(line(WATER_ASK))) return
        if (!hasTool) {
          ctx.say(line(NEEDS_TOOL))
          return
        }
        water(ctx, charId, regionId, bankId, mapId, idx, plot, view, playerName, plantName)
      }
    }
  }

  private suspend fun plant(ctx: ScriptContext, stored: StoredCharacter, regionId: Int, bankId: Int, mapId: Int, idx: Int, playerName: String) {
    val charId = stored.info.id
    val picked = dialog.plantSeeds(ctx.session, ctx.state, PLANT_FIRST, ctx.entityId) ?: return
    val seeds = picked.filter { it in BerryPlots.FIRST_SEED_ITEM..BerryPlots.LAST_SEED_ITEM }
    if (seeds.isEmpty()) return
    val fresh = characterStore.getCharacter(charId) ?: return
    val needed = seeds.groupingBy { it }.eachCount()
    if (needed.any { (item, n) -> (fresh.items[item] ?: 0) < n }) {
      ctx.say(line(NOT_ENOUGH_SEEDS))
      return
    }
    if (BerryPlots.berryFor(seeds) == null) {
      ctx.say(line(BAD_COMBINATION))
      return
    }
    for ((item, n) in needed) {
      val def = ctx.resolveItemWire(item) ?: continue
      ctx.takeItem(def, n)
    }
    val now = BerryPlots.nowMinute()
    val prefix = "berry/$regionId/$bankId/$mapId/$idx"
    characterStore.setStoryVar(charId, "$prefix/seeds", BerryPlots.packSeeds(seeds))
    characterStore.setStoryVar(charId, "$prefix/planted", now)
    characterStore.setStoryVar(charId, "$prefix/water", BerryPlots.DROPLETS_AT_PLANTING)
    characterStore.setStoryVar(charId, "$prefix/waterAt", now)
    characterStore.setStoryVar(charId, "$prefix/dry", 0)
    characterStore.setStoryVar(charId, "$prefix/floods", 0)
    characterStore.setStoryVar(charId, "$prefix/dead", 0)
    ctx.setMessageArg(1, textArg(1, playerName))
    ctx.say(line(PLANTED)) // "{01} planted the seeds in the loamy soil."
    refresh(ctx, regionId, bankId, mapId, idx)
  }

  private suspend fun water(
      ctx: ScriptContext,
      charId: Long,
      regionId: Int,
      bankId: Int,
      mapId: Int,
      idx: Int,
      plot: BerryPlots.Plot,
      view: BerryPlots.View,
      playerName: String,
      plantName: String,
  ) {
    val prefix = "berry/$regionId/$bankId/$mapId/$idx"
    val now = BerryPlots.nowMinute()
    // Dry time so far is banked before the counter restarts.
    characterStore.setStoryVar(charId, "$prefix/dry", (view.dryHours * 60).toInt())
    if (view.droplets >= BerryPlots.MAX_DROPLETS) {
      // Watering a flooded plant kills it; flooding a full one costs yield.
      if (plot.floods > 0) {
        characterStore.setStoryVar(charId, "$prefix/dead", BerryPlots.Death.FLOODED.ordinal + 1)
        refresh(ctx, regionId, bankId, mapId, idx)
        ctx.say(line(NOW_FLOODED))
        return
      }
      characterStore.setStoryVar(charId, "$prefix/floods", plot.floods + 1)
      characterStore.setStoryVar(charId, "$prefix/water", BerryPlots.MAX_DROPLETS)
      characterStore.setStoryVar(charId, "$prefix/waterAt", now)
      ctx.setMessageArg(0, textArg(0, playerName))
      ctx.setMessageArg(1, textArg(1, plantName))
      ctx.say(line(WATERED))
      ctx.say(line(NOW_FLOODED))
      refresh(ctx, regionId, bankId, mapId, idx)
      return
    }
    val droplets = (view.droplets + BerryPlots.DROPLETS_PER_WATERING).coerceAtMost(BerryPlots.MAX_DROPLETS)
    characterStore.setStoryVar(charId, "$prefix/water", droplets)
    characterStore.setStoryVar(charId, "$prefix/waterAt", now)
    ctx.setMessageArg(0, textArg(0, playerName))
    ctx.setMessageArg(1, textArg(1, plantName))
    ctx.say(line(WATERED)) // "{00} watered the {01}."
    ctx.say(line(MOISTURE_NOW[droplets]))
    refresh(ctx, regionId, bankId, mapId, idx)
  }

  private fun clear(charId: Long, regionId: Int, bankId: Int, mapId: Int, idx: Int) {
    val prefix = "berry/$regionId/$bankId/$mapId/$idx"
    for (field in listOf("seeds", "planted", "water", "waterAt", "dry", "floods", "dead")) {
      characterStore.setStoryVar(charId, "$prefix/$field", 0)
    }
  }

  /** Re-send the plot object so the client redraws its stage and droplets. */
  private fun refresh(ctx: ScriptContext, regionId: Int, bankId: Int, mapId: Int, idx: Int) {
    npcService.get().spawnNpc(ctx.session, regionId, bankId, mapId, idx)
  }

  private fun adverb(view: BerryPlots.View): String =
      when {
        view.dryHours >= 4 -> "poorly"
        view.dryHours >= 2 -> "nicely"
        view.dryHours > 0 -> "beautifully"
        else -> "excellently"
      }

  private fun line(textId: Int) = object : de.fiereu.openmmo.common.dialog.DialogLine { override val textId = textId }

  private fun textArg(slot: Int, text: String): DialogMessageArg = RawMessageArg(slot = slot.toByte(), kind = 5, text = text)

  private fun numberArg(slot: Int, value: Int): DialogMessageArg = RawMessageArg(slot = slot.toByte(), kind = 3, intValue = value)

  private fun itemArg(slot: Int, wireId: Int): DialogMessageArg = RawMessageArg(slot = slot.toByte(), kind = 25, shorts = listOf(wireId.toShort()))

  private companion object {
    // Client strings, strings_en.xml 16780300+ (the loamy-soil block).
    const val PLANT_FIRST = 16780300
    const val NOT_ENOUGH_SEEDS = 16780304
    const val BAD_COMBINATION = 16780305
    const val PLANTED = 16780309
    const val SEEDS_PLANTED = 16780310
    const val SPROUTED = 16780311
    const val DEAD_DRY = 16780314
    const val DEAD_FLOODED = 16780319
    const val DEAD_UNPICKED = 16780322
    const val NEEDS_TOOL = 16780323
    const val GROWING_TALLER = 16780324
    const val GROWING_HOW = 16780325
    const val SOFT_SOIL = 16780326
    const val WATER_ASK = 16780327
    const val WATERED = 16780328
    const val RIPE_PICK = 16780329
    const val NOT_PICKED = 16780330
    const val PICKED = 16780331
    const val NOW_FLOODED = 16780340
    /** "The soil is dry / almost dry / moist / wet / almost flooded / flooded", by droplets 0..5. */
    val MOISTURE = listOf(16780315, 16780320, 16780316, 16780317, 16780321, 16780318)
    /** "The soil is now ..." after watering, same order. */
    val MOISTURE_NOW = listOf(16780335, 16780336, 16780337, 16780338, 16780339, 16780340)
  }
}
