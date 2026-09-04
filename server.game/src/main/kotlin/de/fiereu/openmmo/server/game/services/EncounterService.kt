package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.EncounterMethod
import de.fiereu.openmmo.common.enums.TileBehavior
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.WildEncounterSlot
import de.fiereu.openmmo.maps.WildEncounterTable
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private val log = KotlinLogging.logger {}

// The GBA rolls a land encounter out of 2880 with the table rate scaled by 16.
private const val ENCOUNTER_ROLL_MAX = 2880
private const val ENCOUNTER_RATE_SCALE = 16

// The GBA's standard cave rate, for retail-covered maps with no decomp table of their own.
private const val DEFAULT_ENCOUNTER_RATE = 10

/** Rolls a wild encounter when a player steps onto grass and starts the battle if one is met. */
@Singleton
class EncounterService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val battleService: BattleService,
    private val ndsLand: NdsLand = NdsLand(),
) {

  private val random: Random = Random.Default

  /** Called after a completed step. Starts a wild battle if the tile and roll call for one. */
  fun onStep(session: SessionContext, charId: Long, map: MapDef, x: Int, y: Int) {
    val tile = map.tileAt(x, y) ?: return
    // Grass rolls the Grass tables. A map with retail Cave entries rolls them on every walkable
    // step, the way caves have always worked - the decomp-only path never covered caves at all.
    val grassStep = isLandEncounterTile(tile.behavior)
    val caveStep =
        !grassStep &&
            RetailEncounters.entriesFor(map.sourceName, map.regionId.toInt()).any {
              it.type == "Cave"
            }
    if (!grassStep && !caveStep) return
    if (battleService.inBattle(charId)) {
      // A battle that never closed - a disconnect mid-fight, a crash - would block encounters
      // forever while looking like nothing; grass steps say so.
      log.info { "[Encounter] char=$charId blocked: still marked in battle" }
      return
    }
    // TODO: Add water and fishing wild encounters
    //  Water encounters should fire while surfing over water tiles and fishing when a rod is used.
    //  Both need the surf and rod features to exist first. Once they do, branch here on the tile
    //  behavior and roll the WATER or FISHING table the same way land is rolled below.
    if (!hasUsablePartyMon(charId)) {
      log.info { "[Encounter] char=$charId blocked: no usable party monster" }
      return
    }

    val decompTable = map.encounterTable(EncounterMethod.LAND)

    // Retail tables first: they carry season, time of day and retail-accurate rarity. The decomp
    // table stays as the fallback for maps the retail dump does not know.
    val season = WorldClock.season()
    val time = WorldClock.timeOfDay()
    val types = if (grassStep) setOf("Grass", "Dark Grass") else setOf("Cave")
    val pool = RetailEncounters.wildPool(map.sourceName, map.regionId.toInt(), types, season, time)
    if (pool.isNotEmpty()) {
      val rate = decompTable?.encounterRate ?: DEFAULT_ENCOUNTER_RATE
      if (random.nextInt(ENCOUNTER_ROLL_MAX) >= (rate * ENCOUNTER_RATE_SCALE)) return
      val slot = pickRetailSlot(pool) ?: return
      val level = random.nextInt(slot.minLevel, slot.maxLevel + 1)
      log.info {
        "Wild encounter for char=$charId at ($x, $y) [$season/$time]: " +
            "species ${slot.dexId} level $level"
      }
      battleService.startWildBattle(session, slot.dexId, level)
      return
    }

    // "Encounters stopped" investigations start here: standing in grass with no roll happening
    // means one of these gates, and each says so instead of failing silently.
    if (decompTable == null) {
      log.info { "[Encounter] grass at ($x, $y) but map ${map.sourceName} has no table at all" }
      return
    }
    if (!rollsEncounter(decompTable)) return
    val slot = pickSlot(decompTable) ?: return
    val level = random.nextInt(slot.minLevel, slot.maxLevel + 1)
    log.info {
      "Wild encounter for char=$charId at ($x, $y): species ${slot.speciesId} level $level"
    }
    battleService.startWildBattle(session, slot.speciesId, level)
  }

  /**
   * A step on a DS map (Johto, Sinnoh; Unova once its land file exists): the ROM's tile type says
   * grass or cave floor, the map directory names the map, and the dex tables of that region roll
   * the encounter exactly as on the GBA maps.
   */
  fun onNdsStep(session: SessionContext, charId: Long, region: Int, bank: Int, map: Int, x: Int, y: Int) {
    val type = ndsLand.typeAt(region, bank, map, x, y) ?: return
    val types =
        when {
          ndsLand.isGrass(type) -> setOf("Grass", "Dark Grass")
          ndsLand.isCaveFloor(type) -> setOf("Cave")
          else -> return
        }
    if (battleService.inBattle(charId) || !hasUsablePartyMon(charId)) return
    val name = NdsMapTypes.nameOf(region, bank, map) ?: return
    val season = WorldClock.season()
    val time = WorldClock.timeOfDay()
    val pool = RetailEncounters.wildPoolForNdsName(name, region, types, season, time)
    if (pool.isEmpty()) {
      log.debug { "[Encounter] DS map $region:$bank:$map '$name' has no ${types.first()} table" }
      return
    }
    if (random.nextInt(ENCOUNTER_ROLL_MAX) >= DEFAULT_ENCOUNTER_RATE * ENCOUNTER_RATE_SCALE) return
    val slot = pickRetailSlot(pool) ?: return
    val level = random.nextInt(slot.minLevel, slot.maxLevel + 1)
    log.info { "Wild encounter for char=$charId on DS map '$name' at ($x, $y) [$season/$time]: species ${slot.dexId} level $level" }
    battleService.startWildBattle(session, slot.dexId, level)
  }

  /** Picks from the retail pool weighted by its per-time rarity. */
  private fun pickRetailSlot(pool: List<RetailEncounters.Slot>): RetailEncounters.Slot? {
    val total = pool.sumOf { it.weight }
    if (total <= 0) return null
    var roll = random.nextInt(total)
    for (slot in pool) {
      roll -= slot.weight
      if (roll < 0) return slot
    }
    return pool.lastOrNull()
  }

  private fun isLandEncounterTile(behavior: TileBehavior): Boolean =
      behavior == TileBehavior.TALL_GRASS || behavior == TileBehavior.LONG_GRASS

  private fun hasUsablePartyMon(charId: Long): Boolean =
      characterStore.getCharacter(charId)?.pokemon?.any { it.hp > 0 } ?: false

  private fun rollsEncounter(table: WildEncounterTable): Boolean {
    val chance = (table.encounterRate * ENCOUNTER_RATE_SCALE).coerceAtMost(ENCOUNTER_ROLL_MAX)
    return random.nextInt(ENCOUNTER_ROLL_MAX) < chance
  }

  /** Picks a slot weighted by its encounter rate. */
  private fun pickSlot(table: WildEncounterTable): WildEncounterSlot? {
    val total = table.slots.sumOf { it.weight }
    if (total <= 0) return null
    var roll = random.nextInt(total)
    for (slot in table.slots) {
      roll -= slot.weight
      if (roll < 0) return slot
    }
    return table.slots.lastOrNull()
  }
}
