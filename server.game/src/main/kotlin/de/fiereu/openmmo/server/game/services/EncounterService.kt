package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.LURE_KIND_NONE
import de.fiereu.openmmo.net.game.packets.Value16Group
import de.fiereu.openmmo.net.game.packets.Value64Group
import de.fiereu.openmmo.common.enums.EncounterMethod
import de.fiereu.openmmo.common.enums.MapType
import de.fiereu.openmmo.server.game.battle.EncounterContext
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

/** Share of non-horde dark grass encounters that come out as a 2v2 (owner: "at least 50/50"). */
private const val DARK_GRASS_DOUBLE_PERCENT = 50

/** The level cap a lured foe is never pushed past. */
private const val MAX_LEVEL = 100

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
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry,
    private val items: de.fiereu.openmmo.items.ItemRegistry,
    private val ndsLand: NdsLand = NdsLand(),
) {

  private val random: Random = Random.Default

  /** The lead monster's ability and held item, FireRed's wild_encounter.c rules (see OverworldAbilities). */
  private val abilities by lazy { OverworldAbilities(speciesRegistry, items.idOf(de.fiereu.openmmo.items.generated.Items.CLEANSE_TAG)) }

  /** Called after a completed step. Starts a wild battle if the tile and roll call for one. */
  fun onStep(session: SessionContext, charId: Long, map: MapDef, x: Int, y: Int) {
    val tile = map.tileAt(x, y) ?: return
    val state = session.attributes[PLAYER_STATE]
    // Surfing over water rolls the Water tables. Grass rolls the Grass tables. A map with retail
    // Cave or Inside entries (Pokemon Tower's floors are typed Inside) rolls them on every
    // walkable step, the way caves have always worked - the decomp-only path never covered caves.
    val waterStep = state?.surfing == true && tile.behavior.isSurfable
    val grassStep = !waterStep && isLandEncounterTile(tile.behavior)
    // Floor rolls belong to maps with no grass at all (caves, towers). The retail dump files a
    // mountain's exterior and its caves under one name (Mt. Ember: Grass, Cave and Rocks rows), so
    // a Cave row must not turn every rock step outside into an encounter.
    val floorStep =
        !waterStep &&
            !grassStep &&
            !hasGrass(map) &&
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
    val lead = abilities.leadOf(characterStore, charId)
    val encounter = EncounterContext(surfing = waterStep, cave = map.mapType == MapType.UNDERGROUND)

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
    // The whole table, horde slots included: a horde is always possible off a single encounter, in
    // Kanto and Hoenn too (owner, 2026-09-21). The retail rows sum to 100% only when the horde slots
    // are counted, so filtering them out also renormalised every single's odds upwards.
    val table = RetailEncounters.tableForSource(map.sourceName, map.regionId.toInt(), types, season, time, includeHordes = true)
    if (table.isNotEmpty()) {
      val tier = activeLure(charId)
      val base = decompTable?.encounterRate ?: if (waterStep) DEFAULT_WATER_RATE else DEFAULT_ENCOUNTER_RATE
      // A lure raises the encounter rate by its tier's percentage.
      val rate = base * (100 + (tier?.encounterRatePercent ?: 0)) / 100
      // Water rates are low by design (FireRed's 4 against grass's 21), which reads as "no water
      // encounters"; every surfed step says what it rolled against.
      if (waterStep) log.info { "[Encounter] char=$charId surf step at ($x, $y): rate $rate, pool ${table.size}" }
      if (random.nextInt(ENCOUNTER_ROLL_MAX) >= abilities.scaledRate(rate, lead)) return
      val lured =
          withLure(table, RetailEncounters.lureSlotsForSource(map.sourceName, map.regionId.toInt(), types, season, time), tier)
      // Bias the PAIRED list: biasSlot filters the pool for a Static or Magnet Pull lead, so picking
      // by index into a separate list would attach the wrong horde size to the wrong species.
      val picked = pickTerrainSlot(abilities.biasSlot(lured, { it.slot.dexId }, lead, random)) ?: return
      val slot = picked.slot
      val level = luredLevel(abilities.levelFor(slot.minLevel, slot.maxLevel, lead, random) ?: return, tier)
      if (repelBlocks(charId, lead, level)) return
      freeze(session, charId, map, x, y)
      if (picked.hordeSize > 0) {
        val size = luredHordeSize(picked.hordeSize, tier)
        val specs = List(size) { hordeSpec(slot, lead, tier) }
        log.info { "Wild horde of $size x ${slot.dexId} for char=$charId at ($x, $y) [$season/$time]${lureTag(tier)}" }
        battleService.startHordeBattle(session, specs, encounter, secretBonusPercent = tier?.secretBonusPercent ?: 0)
        return
      }
      // A lure may bring one or two more foes, each its own draw of the table. There is no dark
      // grass outside Unova, so the double never comes from the terrain here.
      val foes = foeCount(darkGrass = false, tier = tier)
      if (foes > 1) {
        val extra =
            (2..foes).mapNotNull {
              pickTerrainSlot(abilities.biasSlot(lured, { s -> s.slot.dexId }, lead, random))?.slot
            }
        if (extra.isNotEmpty()) {
          val specs = (listOf(slot) + extra).map { hordeSpec(it, lead, tier) }
          log.info { "Wild ${specs.size}-foe encounter for char=$charId at ($x, $y) [$season/$time]${lureTag(tier)}: ${specs.joinToString { s -> s.dexId.toString() }}" }
          battleService.startHordeBattle(session, specs, encounter, secretBonusPercent = tier?.secretBonusPercent ?: 0)
          return
        }
      }
      log.info {
        "Wild encounter for char=$charId at ($x, $y) [$season/$time]${lureTag(tier)}: " +
            "species ${slot.dexId} level $level"
      }
      battleService.startWildBattle(session, slot.dexId, level, abilities.hints(lead, slot.dexId, random), encounter, secretBonusPercent = tier?.secretBonusPercent ?: 0)
      return
    }

    // "Encounters stopped" investigations start here: standing in grass with no roll happening
    // means one of these gates, and each says so instead of failing silently.
    if (decompTable == null) {
      log.info { "[Encounter] grass at ($x, $y) but map ${map.sourceName} has no table at all" }
      return
    }
    if (!rollsEncounter(decompTable, lead)) return
    val slot = pickSlot(decompTable) ?: return
    val level = abilities.levelFor(slot.minLevel, slot.maxLevel, lead, random) ?: return
    if (repelBlocks(charId, lead, level)) return
    log.info {
      "Wild encounter for char=$charId at ($x, $y): species ${slot.speciesId} level $level"
    }
    freeze(session, charId, map, x, y)
    battleService.startWildBattle(session, slot.speciesId, level, abilities.hints(lead, slot.speciesId, random), encounter)
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
              elevation = gbaElevationAt(map, x, y, DEFAULT_GBA_ELEVATION),
              direction = state.facingDirection))
    }
  }

  /** True while the character has a battle running - steps that land then are stale. */
  fun inBattle(charId: Long): Boolean = battleService.inBattle(charId)

  /**
   * Every completed step burns one repel step and one lure step (src/item_use.c /
   * field_player_avatar.c): the HUD's "{00} repel step(s)" line follows through the local delta's
   * 0x10 group and the lure's through the 0x40 group, and the last step of each announces the end
   * the way the cartridge does. They are independent - a lure burns whether or not a repel runs.
   */
  fun onAnyStep(session: SessionContext, charId: Long) {
    burnRepelStep(session, charId)
    burnLureStep(session, charId)
  }

  private fun burnRepelStep(session: SessionContext, charId: Long) {
    val stored = characterStore.getCharacter(charId) ?: return
    val left = stored.info.repelLeft.toInt()
    if (left <= 0) return
    val next = left - 1
    val item: Short = if (next == 0) 0 else stored.info.repelItemId
    characterStore.updateCharacter(stored.info.copy(repelLeft = next.toShort(), repelItemId = item))
    session.send(LocalCharacterDeltaPacket(value16 = Value16Group(next.toShort(), item)))
    if (next == 0) {
      characterStore.flushCharacterAsync(charId)
      session.send(notice("The Repel's effect wore off."))
      log.info { "[Repel] char=$charId wore off" }
    }
  }

  /**
   * The lure's own step burn, the repel's twin. Its HUD line rides the local delta's 0x40 group:
   * the f/ig7 kind byte that picks the label, then the steps left. When it runs out the kind goes
   * to LURE_KIND_NONE, which is the client's -1, and the line disappears.
   */
  private fun burnLureStep(session: SessionContext, charId: Long) {
    val stored = characterStore.getCharacter(charId) ?: return
    val left = stored.info.lureLeft.toInt()
    if (left <= 0) return
    val next = left - 1
    val item: Short = if (next == 0) 0 else stored.info.lureItemId
    characterStore.updateCharacter(stored.info.copy(lureLeft = next.toShort(), lureItemId = item))
    val kind = if (next == 0) LURE_KIND_NONE else Lures.of(stored.info.lureItemId.toInt())?.kind ?: LURE_KIND_NONE
    session.send(
        LocalCharacterDeltaPacket(value64 = Value64Group(kind.toByte(), next.toShort(), item)))
    if (next == 0) {
      characterStore.flushCharacterAsync(charId)
      session.send(notice("The lure's effect wore off."))
      log.info { "[Lure] char=$charId wore off" }
    }
  }

  /** The tier of the lure running for this character, null when none is. */
  private fun activeLure(charId: Long): Lures.Tier? {
    val info = characterStore.getCharacter(charId)?.info ?: return null
    if (info.lureLeft <= 0) return null
    return Lures.of(info.lureItemId.toInt())
  }

  /** An active repel turns away any wild monster below the lead's level (wild_encounter.c IsWildLevelAllowed). */
  private fun repelBlocks(charId: Long, lead: OverworldAbilities.Lead?, wildLevel: Int): Boolean {
    if (lead == null) return false
    val left = characterStore.getCharacter(charId)?.info?.repelLeft ?: 0
    if (left <= 0 || wildLevel >= lead.level) return false
    log.debug { "[Repel] char=$charId repelled a level $wildLevel wild (lead ${lead.level})" }
    return true
  }

  fun onNdsStep(session: SessionContext, charId: Long, region: Int, bank: Int, map: Int, x: Int, y: Int) {
    val type = ndsLand.typeAt(region, bank, map, x, y) ?: return
    // The retail dump's location_id IS this header for the DS regions, so the tables resolve per
    // FLOOR - a cave's floors share one directory name but not one pool.
    val header = (map shl 8) or bank
    // Gen 5 has no cave-floor tile behaviour: a cave's floor reads as ordinary walkable ground
    // (Wellspring Cave is 851 of ~1024 tiles at behaviour 0x0000), so being in a cave is a property
    // of the MAP, not of the tile, and isCaveFloor never matched - which is why all 1798 Unova Cave
    // entries were unreachable. A cave roll is therefore a walkable plain tile on a header that has
    // a Cave table at all.
    val cave =
        type == 0 &&
            ndsLand.blocked(region, bank, map, x, y) == false &&
            RetailEncounters.ndsHeaderHasType(region, header, "Cave")
    // Only where the map really has the separate pool: Gen 4's maps use the same tile number for
    // plain very tall grass and have no Dark Grass tables at all.
    val darkGrass =
        ndsLand.isDarkGrass(type) && RetailEncounters.ndsHeaderHasType(region, header, "Dark Grass")
    val types =
        when {
          darkGrass -> setOf("Dark Grass")
          ndsLand.isGrass(type) -> setOf("Grass")
          cave -> setOf("Cave")
          else -> return
        }
    if (battleService.inBattle(charId) || !hasUsablePartyMon(charId)) return
    val name = NdsMapTypes.nameOf(region, bank, map) ?: return
    val season = WorldClock.season()
    val time = WorldClock.timeOfDay()
    // The WHOLE table, horde slots included: a horde is always possible off a single encounter, on
    // every terrain and in every region (owner, 2026-09-21). The rarities are the chance of that
    // species. horde showing up, not a mix.
    val table = RetailEncounters.ndsTableForHeader(header, region, types, season, time, includeHordes = true)
    if (table.isEmpty()) {
      log.debug { "[Encounter] DS map $region:$bank:$map '$name' (header $header) has no ${types.first()} table" }
      return
    }
    val lead = abilities.leadOf(characterStore, charId)
    val tier = activeLure(charId)
    // A lure raises the encounter rate by its tier's percentage.
    val rate = DEFAULT_ENCOUNTER_RATE * (100 + (tier?.encounterRatePercent ?: 0)) / 100
    if (random.nextInt(ENCOUNTER_ROLL_MAX) >= abilities.scaledRate(rate, lead)) return
    val lured = withLure(table, RetailEncounters.ndsLureSlotsForHeader(header, region, types, season, time), tier)
    // Bias the PAIRED list: biasSlot filters the pool for a Static or Magnet Pull lead, so picking
    // by index into a separate list would attach the wrong horde size to the wrong species.
    val picked = pickTerrainSlot(abilities.biasSlot(lured, { it.slot.dexId }, lead, random)) ?: return
    val slot = picked.slot
    val level = luredLevel(abilities.levelFor(slot.minLevel, slot.maxLevel, lead, random) ?: return, tier)
    if (repelBlocks(charId, lead, level)) return
    freeze(session, charId, null, x, y)
    if (picked.hordeSize > 0) {
      val size = luredHordeSize(picked.hordeSize, tier)
      val specs = List(size) { hordeSpec(slot, lead, tier) }
      log.info { "Wild horde of $size x ${slot.dexId} for char=$charId on DS map '$name' at ($x, $y) [${types.first()}, $season/$time]${lureTag(tier)}" }
      battleService.startHordeBattle(session, specs, EncounterContext(cave = cave), secretBonusPercent = tier?.secretBonusPercent ?: 0)
      return
    }
    // Dark grass turns half of its non-horde encounters into a 2v2, and a lure may bring one or two
    // more foes; each extra is its own draw of the same table, so they can differ and duplicates
    // are fine. A horde row drawn as an extra contributes a single of that species.
    val foes = foeCount(darkGrass, tier)
    if (foes > 1) {
      val extra =
          (2..foes).mapNotNull {
            pickTerrainSlot(abilities.biasSlot(lured, { s -> s.slot.dexId }, lead, random))?.slot
          }
      if (extra.isNotEmpty()) {
        val specs = (listOf(slot) + extra).map { hordeSpec(it, lead, tier) }
        log.info { "Wild ${specs.size}-foe encounter for char=$charId on DS map '$name' at ($x, $y) [${types.first()}, $season/$time]${lureTag(tier)}: ${specs.joinToString { s -> s.dexId.toString() }}" }
        battleService.startHordeBattle(session, specs, EncounterContext(cave = cave), secretBonusPercent = tier?.secretBonusPercent ?: 0)
        return
      }
    }
    log.info { "Wild encounter for char=$charId on DS map '$name' at ($x, $y) [${types.first()}, $season/$time]${lureTag(tier)}: species ${slot.dexId} level $level" }
    battleService.startWildBattle(session, slot.dexId, level, abilities.hints(lead, slot.dexId, random), EncounterContext(cave = cave), secretBonusPercent = tier?.secretBonusPercent ?: 0)
  }

  /** One member of a multi-foe wild battle, at its own rolled level. */
  private fun hordeSpec(slot: RetailEncounters.Slot, lead: OverworldAbilities.Lead?, tier: Lures.Tier?) =
      BattleService.OpponentSpec(
          slot.dexId,
          luredLevel(random.nextInt(slot.minLevel, slot.maxLevel + 1), tier),
          emptyList(),
          hints = abilities.hints(lead, slot.dexId, random),
      )

  private fun lureTag(tier: Lures.Tier?) = if (tier == null) "" else " [${tier.name}]"

  /**
   * The terrain table as an active lure reshapes it: the lure-exclusive rows take the tier's share
   * of the roll (5%, 10% or 8%, the client's own numbers) and everything already there is scaled
   * down to the rest, which keeps the normal species' odds RELATIVE to each other and keeps the
   * table summing to 100% - the invariant that caught the horde bug. With no lure up, or no
   * exclusive rows on this terrain, the table is returned untouched.
   */
  private fun withLure(
      table: List<RetailEncounters.TerrainSlot>,
      lureRows: List<RetailEncounters.TerrainSlot>,
      tier: Lures.Tier?,
  ): List<RetailEncounters.TerrainSlot> {
    if (tier == null || lureRows.isEmpty() || table.isEmpty()) return table
    val total = table.sumOf { it.slot.weight }
    if (total <= 0) return table
    val share = total * tier.exclusivePercent / 100
    if (share <= 0) return table
    val scaled = table.map { it.copy(slot = it.slot.copy(weight = it.slot.weight * (100 - tier.exclusivePercent) / 100)) }
    val each = (share / lureRows.size).coerceAtLeast(1)
    return scaled + lureRows.map { it.copy(slot = it.slot.copy(weight = each)) }
  }

  /**
   * How many foes a single encounter brings. A lure "may encounter more foes at the same time":
   * one, two or three at a third each, every one rolled on its own, so unlike a Sweet Scent horde
   * they can differ. Dark grass independently makes half its encounters a 2v2. Where both apply the
   * LARGER wins rather than the two adding up (owner, 2026-09-21), so dark grass under a lure is
   * two or three, never one - which leaves about one in six of those still a single.
   */
  private fun foeCount(darkGrass: Boolean, tier: Lures.Tier?): Int {
    val fromLure = if (tier != null) 1 + random.nextInt(3) else 1
    val fromGrass = if (darkGrass && random.nextInt(100) < DARK_GRASS_DOUBLE_PERCENT) 2 else 1
    return maxOf(fromLure, fromGrass)
  }

  /** A lured foe is a few levels stronger, never past the species' own cap for the slot. */
  private fun luredLevel(level: Int, tier: Lures.Tier?): Int =
      if (tier == null) level
      else (level + Lures.MIN_LEVEL_BONUS + random.nextInt(Lures.MAX_LEVEL_BONUS - Lures.MIN_LEVEL_BONUS + 1))
          .coerceAtMost(MAX_LEVEL)

  /** Premium lures say "small hordes (3) may increase in size"; the other tiers leave them alone. */
  private fun luredHordeSize(size: Int, tier: Lures.Tier?): Int =
      if (size == 3 && tier?.growsHordes == true) 5 else size

  /** [pickRetailSlot] over the paired terrain table, so the horde size rides with its species. */
  private fun pickTerrainSlot(pool: List<RetailEncounters.TerrainSlot>): RetailEncounters.TerrainSlot? {
    val total = pool.sumOf { it.slot.weight }
    if (total <= 0) return null
    var roll = random.nextInt(total)
    for (entry in pool) {
      roll -= entry.slot.weight
      if (roll < 0) return entry
    }
    return pool.lastOrNull()
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
    val lead = abilities.leadOf(characterStore, charId)
    val specs =
        List(ready.count) {
          BattleService.OpponentSpec(slot.dexId, random.nextInt(slot.minLevel, slot.maxLevel + 1), emptyList(), hints = abilities.hints(lead, slot.dexId, random))
        }
    log.info { "Horde of ${ready.count} x ${slot.dexId} for char=$charId: levels ${specs.joinToString { it.level.toString() }}" }
    // The same terrain read hordePlan made: a DS tile that is not grass scents the Cave table.
    val cave =
        if (map != null) map.mapType == MapType.UNDERGROUND
        else ndsLand.typeAt(state.regionId, state.bankId, state.mapId, state.x.toInt(), state.y.toInt()).let { it == null || !ndsLand.isGrass(it) }
    battleService.startHordeBattle(session, specs, EncounterContext(surfing = state.surfing, cave = cave), sweetScent = true)
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
      // Surfing scents the Water table (it never did: a scent on the water was refused, 2026-09-10).
      val types =
          when {
            state.surfing && tile?.behavior?.isSurfable == true -> setOf("Water")
            tile != null && isLandEncounterTile(tile.behavior) -> setOf("Grass", "Dark Grass")
            else -> FLOOR_TYPES
          }
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

  private val grassMaps = java.util.concurrent.ConcurrentHashMap<MapDef, Boolean>()

  /** Whether any tile of [map] is grass: such a map rolls grass, never its floor rows. */
  private fun hasGrass(map: MapDef): Boolean =
      grassMaps.getOrPut(map) { map.tiles.any { isLandEncounterTile(it.behavior) } }

  private fun hasUsablePartyMon(charId: Long): Boolean =
      characterStore.getCharacter(charId)?.pokemon?.any { it.hp > 0 } ?: false

  private fun rollsEncounter(table: WildEncounterTable, lead: OverworldAbilities.Lead?): Boolean {
    val chance = abilities.scaledRate(table.encounterRate, lead).coerceAtMost(ENCOUNTER_ROLL_MAX)
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
