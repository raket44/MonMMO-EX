package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.MAX_MOVE_SLOTS
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.config.GameServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/** Original-trainer tag that marks a seeded test monster, so a restart does not add them again. */
private const val TEST_BOX_OT = "TestBox"
private const val TEST_LEVEL = 50

/** One test monster: species and the four moves that exercise a battle mechanic. */
private data class TestMon(val dexId: Int, val moves: List<Int>)

/**
 * Drops a box of level-50 monsters into the operator's PC storage, each carrying moves that
 * exercise one part of the battle engine (status, weather, screens, healing, recoil, multi-hit,
 * two-turn moves...). Built through [WildMonFactory] so stats, IVs and xp match a real monster,
 * then handed the moveset directly. Runs once: monsters are tagged by their OT and skipped when
 * present.
 */
@Singleton
class TestBoxSeeder
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val factory: WildMonFactory,
    private val moves: MoveRegistry,
    private val config: GameServerConfig,
) {

  suspend fun seed() {
    if (!config.db.seedDev) return
    for (userId in listOf(1, 2, 3)) {
      for (stored in characterStore.getCharactersByUser(userId)) {
        if (stored.info.name in DEV_CHARACTER_NAMES) continue
        seedInto(stored.info.id, stored.info.name)
      }
    }
  }

  private suspend fun seedInto(characterId: Long, characterName: String) {
    val stored = characterStore.getOrLoadCharacter(characterId) ?: return
    if (stored.pcStorage.any { it.ot == TEST_BOX_OT }) return
    val used = stored.pcStorage.map { it.containerSlot.toInt() }.toMutableSet()
    var slot = 0
    var added = 0
    for (entry in TEST_BOX) {
      while (slot in used) slot++
      val base = factory.create(entry.dexId, TEST_LEVEL, BattleRng()) ?: continue
      val moveset =
          entry.moves.map { PokemonMove(it.toShort(), (moves.get(it)?.pp ?: 5).toByte()) } +
              List(MAX_MOVE_SLOTS - entry.moves.size) { PokemonMove(0, 0) }
      val mon =
          base.copy(
              ownerId = characterId,
              ot = TEST_BOX_OT,
              container = PokemonContainer.PC,
              containerSlot = slot.toShort(),
              moves = moveset,
          )
      if (characterStore.addPokemon(characterId, mon)) {
        used += slot
        added++
      }
    }
    characterStore.flushCharacterAsync(characterId)
    log.info { "Seeded $added test monsters into the PC of '$characterName'" }
  }

  private companion object {
    val DEV_CHARACTER_NAMES = setOf("Kanto", "Hoenn")

    /** Species and moves, each row one mechanic: see the engine for what every move exercises. */
    val TEST_BOX =
        listOf(
            // Thunder Wave (paralysis), Thunderbolt (10% paralysis), Thunder (sure hit in rain), Quick Attack
            TestMon(25, listOf(86, 85, 87, 98)),
            // Hypnosis (sleep), Dream Eater (needs sleep, drains), Confuse Ray, Shadow Ball
            TestMon(94, listOf(95, 138, 109, 247)),
            // Toxic, Fly (semi-invulnerable), Wing Attack, Protect
            TestMon(169, listOf(92, 19, 17, 182)),
            // Will-O-Wisp (burn), Flamethrower (10% burn), Sunny Day, Solar Beam (no charge in sun)
            TestMon(6, listOf(261, 53, 241, 76)),
            // Leech Seed, Giga Drain, Sleep Powder, Synthesis (weather healing)
            TestMon(3, listOf(73, 202, 79, 235)),
            // Rain Dance, Hydro Pump, Protect, Mirror Coat (reflects special damage)
            TestMon(9, listOf(240, 56, 182, 243)),
            // Rest, Sleep Talk, Body Slam (30% paralysis), Hyper Beam (recharge)
            TestMon(143, listOf(156, 214, 34, 63)),
            // Bulk Up (two stats), Cross Chop (high crit), Counter, Focus Energy
            TestMon(68, listOf(339, 238, 68, 116)),
            // Ice Beam (10% freeze), Hail, Rest, Surf
            TestMon(131, listOf(58, 258, 156, 57)),
            // Sandstorm, Earthquake (doubles on Dig), Explosion, Rock Slide (30% flinch)
            TestMon(76, listOf(201, 89, 153, 157)),
            // Reflect, Light Screen, Psychic, Recover
            TestMon(65, listOf(115, 113, 94, 105)),
            // Dig, Slash (high crit), Sand Attack (accuracy down), Earthquake
            TestMon(51, listOf(91, 163, 28, 89)),
            // Fury Attack (2-5 hits), Twineedle (two hits, poison), Poison Sting, Agility
            TestMon(15, listOf(31, 41, 40, 97)),
            // Metronome, Soft-Boiled, Belly Drum, Sing
            TestMon(36, listOf(118, 135, 187, 47)),
            // Double-Edge (recoil), Bite (30% flinch), Thunder Wave, Curse
            TestMon(135, listOf(38, 44, 86, 174)),
            // Dragon Dance (two stats), Fly, Outrage, Haze
            TestMon(149, listOf(349, 19, 200, 114)),
            // Pain Split, Will-O-Wisp, Explosion, Sludge (30% poison)
            TestMon(110, listOf(220, 261, 153, 124)),
            // High Jump Kick (crash on miss), Endure, Reversal (low-hp power), Mind Reader
            TestMon(106, listOf(136, 203, 179, 170)),
            // Wish, Mean Look, Moonlight, Toxic
            TestMon(197, listOf(273, 212, 236, 92)),
            // Swords Dance, False Swipe (leaves 1 hp), Metal Claw, Agility
            TestMon(212, listOf(14, 206, 232, 97)),
            // Guillotine (OHKO), Crabhammer (high crit), Swords Dance, Protect
            TestMon(99, listOf(12, 152, 14, 182)),
            // Seismic Toss (level damage), Toxic, Soft-Boiled, Heal Bell
            TestMon(242, listOf(69, 92, 135, 215)),
            // Yawn (drowsy), Safeguard, Light Screen, Swift (never misses)
            TestMon(101, listOf(281, 219, 113, 129)),
        )
  }
}
