package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.EncounterMethod
import de.fiereu.openmmo.common.enums.TileBehavior
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.WildEncounterSlot
import de.fiereu.openmmo.maps.WildEncounterTable
import de.fiereu.openmmo.net.game.packets.DialogStatePacket
import de.fiereu.openmmo.net.game.packets.GbaEntityMovePacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
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

// The GBA's usual surfing rate (FireRed's water tables), for maps whose decomp table lacks one.
private const val DEFAULT_WATER_RATE = 4

// Retail types rolled on every walkable step of a map that has them: caves, and building
// interiors like Pokemon Tower, whose floors are plain tiles with no grass to stand in.
private val FLOOR_TYPES = setOf("Cave", "Inside")

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
    val state = session.attributes[PLAYER_STATE]
    // Surfing over water rolls the Water tables. Grass rolls the Grass tables. A map with retail
    // Cave or Inside entries (Pokemon Tower's floors are typed Inside) rolls them on every
    // walkable step, the way caves have always worked - the decomp-only path never covered caves.
    val waterStep = state?.surfing == true && tile.behavior.isSurfable
    val grassStep = !waterStep && isLandEncounterTile(tile.behavior)
    val floorStep =
        !waterStep &&
            !grassStep &&
            RetailEncounters.entriesFor(map.sourceName, map.regionId.toInt()).any { it.type in FLOOR_TYPES }
    if (!waterStep && !grassStep && !floorStep) return
    if (battleService.inBattle(charId)) {
      // A battle that never closed - a disconnect mid-fight, a crash - would block encounters
      // forever while looking like nothing; grass steps say so.
      log.info { "[Encounter] char=$charId blocked: still marked in battle" }
      return
    }
    // TODO: fishing encounters (the Old/Good/Super Rod tables) once rods can be used.
    if (!hasUsablePartyMon(charId)) {
      log.info { "[Encounter] char=$charId blocked: no usable party monster" }
      return
    }

    val decompTable = map.encounterTable(if (waterStep) EncounterMethod.WATER else EncounterMethod.LAND)

    // Retail tables first: they carry season, time of day and retail-accurate rarity. The decomp
    // table stays as the fallback for maps the retail dump does not know.
    val season = WorldClock.season()
    val time = WorldClock.timeOfDay()
    val types =
        when {
          waterStep -> setOf("Water")
          grassStep -> setOf("Grass", "Dark Grass")
          else -> FLOOR_TYPES
        }
    val pool = RetailEncounters.wildPool(map.sourceName, map.regionId.toInt(), types, season, time)
    if (pool.isNotEmpty()) {
      val rate = decompTable?.encounterRate ?: if (waterStep) DEFAULT_WATER_RATE else DEFAULT_ENCOUNTER_RATE
      if (random.nextInt(ENCOUNTER_ROLL_MAX) >= (rate * ENCOUNTER_RATE_SCALE)) return
      val slot = pickRetailSlot(pool) ?: return
      val level = random.nextInt(slot.minLevel, slot.maxLevel + 1)
      log.info {
        "Wild encounter for char=$charId at ($x, $y) [$season/$time]: " +
            "species ${slot.dexId} level $level"
      }
      freeze(session, charId, map, x, y)
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
    freeze(session, charId, map, x, y)
    battleService.startWildBattle(session, slot.speciesId, level)
  }

  /**
   * A step on a DS map (Johto, Sinnoh; Unova once its land file exists): the ROM's tile type says
   * grass or cave floor, the map directory names the map, and the dex tables of that region roll
   * the encounter exactly as on the GBA maps.
   */
  /**
   * The cartridge stops the player dead on the tile the encounter rolled on. The client keeps
   * walking on its own until the battle screen arrives, so input is removed at once (scripted
   * state ON) and the player is put back on the encounter tile; the hold lifts when the client
   * reports itself back in the overworld, where a trainer's line of sight is checked again.
   */
  private fun freeze(session: SessionContext, charId: Long, map: MapDef?, x: Int, y: Int) {
    val state = session.attributes[PLAYER_STATE] ?: return
    state.encounterHold = true
    session.send(DialogStatePacket(active = true))
    if (map != null) {
      session.send(
          GbaEntityMovePacket(
              entityId = charId,
              bankId = map.bankId.toInt() and 0xff,
              mapId = map.mapId.toInt() and 0xff,
              x = x,
              y = y,
              movementMode = 2,
              direction = state.facingDirection))
    }
  }

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
    freeze(session, charId, null, x, y)
    battleService.startWildBattle(session, slot.dexId, level)
  }

  /**
   * Sweet Scent: what stops a horde on the tile the player stands on, null when one can start.
   */
  fun hordeAvailable(charId: Long, state: de.fiereu.openmmo.server.game.session.PlayerState, map: MapDef?, size: Int): String? =
      hordePlan(charId, state, map, size).let { if (it is HordePlan.Blocked) it.reason else null }

  /**
   * Sweet Scent: a horde of [size] from the terrain the player stands on, sized down to three when
   * the map has no five-strong entries. A horde is ONE species: the table's rarities are the
   * chance of that species' horde showing up, not a mix. Returns what stopped it, null when the
   * battle started.
   */
  fun startHorde(
      session: SessionContext,
      charId: Long,
      state: de.fiereu.openmmo.server.game.session.PlayerState,
      map: MapDef?,
      size: Int,
  ): String? {
    val plan = hordePlan(charId, state, map, size)
    if (plan is HordePlan.Blocked) return plan.reason
    val ready = plan as HordePlan.Ready
    val slot = pickRetailSlot(ready.pool) ?: return "Nothing here answers the scent."
    val specs =
        List(ready.count) {
          BattleService.OpponentSpec(slot.dexId, random.nextInt(slot.minLevel, slot.maxLevel + 1), emptyList())
        }
    log.info { "Horde of ${ready.count} x ${slot.dexId} for char=$charId: levels ${specs.joinToString { it.level.toString() }}" }
    battleService.startHordeBattle(session, specs)
    return null
  }

  private sealed interface HordePlan {
    class Blocked(val reason: String) : HordePlan

    class Ready(val count: Int, val pool: List<RetailEncounters.Slot>) : HordePlan
  }

  private fun hordePlan(charId: Long, state: de.fiereu.openmmo.server.game.session.PlayerState, map: MapDef?, size: Int): HordePlan {
    if (battleService.inBattle(charId)) return HordePlan.Blocked("Already in a battle.")
    if (!hasUsablePartyMon(charId)) return HordePlan.Blocked("No usable party monster.")
    val season = WorldClock.season()
    val time = WorldClock.timeOfDay()
    val pools: (Int) -> List<RetailEncounters.Slot>
    if (map != null) {
      val tile = map.tileAt(state.x.toInt(), state.y.toInt())
      val types = if (tile != null && isLandEncounterTile(tile.behavior)) setOf("Grass", "Dark Grass") else FLOOR_TYPES
      pools = { n -> RetailEncounters.hordePool(map.sourceName, map.regionId.toInt(), types, season, time, n) }
    } else {
      val type = ndsLand.typeAt(state.regionId, state.bankId, state.mapId, state.x.toInt(), state.y.toInt())
      val types = if (type != null && ndsLand.isGrass(type)) setOf("Grass", "Dark Grass") else setOf("Cave")
      val name = NdsMapTypes.nameOf(state.regionId, state.bankId, state.mapId) ?: return HordePlan.Blocked("This map has no encounter table.")
      pools = { n -> RetailEncounters.hordePoolForNdsName(name, state.regionId, types, season, time, n) }
    }
    val wanted = if (size >= 5) 5 else 3
    val (count, pool) =
        listOf(wanted, 3).map { it to pools(it) }.firstOrNull { it.second.isNotEmpty() }
            ?: return HordePlan.Blocked("Nothing here answers the scent.")
    return HordePlan.Ready(count, pool)
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
