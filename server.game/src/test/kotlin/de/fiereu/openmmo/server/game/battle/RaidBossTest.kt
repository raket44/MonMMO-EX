package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.battle.BattleFormat
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.expansion.FormChangeRegistry
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private val speciesRegistry = SpeciesRegistry()
private val moveRegistry = MoveRegistry()
private val engine = TurnEngine(moveRegistry, TypeChart(), ItemRegistry(), speciesRegistry, FormChangeRegistry())

private const val TACKLE: Short = 33
private const val SPLASH: Short = 150
private const val SNORLAX = 143
private const val ONIX = 95

private fun mon(dexId: Int, moves: List<Short>, id: Long, slot: Int?): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val p = Pokemon(id = id, ownerId = 0, container = PokemonContainer.PARTY, containerSlot = (slot ?: 0).toShort(), dexId = dexId, seed = 0, ot = "", nickname = "",
      level = 50, hp = Short.MAX_VALUE, xp = 0, eVs = EVs(), iVs = IVs(),
      moves = List(4) { i -> moves.getOrNull(i) ?: 0.toShort() }.map { PokemonMove(it, (moveRegistry.get(it.toInt())?.pp ?: 0).toByte()) }, heldItem = 0,
      isShiny = false, hasHiddenAbility = false, isAlpha = false, isSecret = false, isFatefulEncounter = false, isRaidEncounter = false, caughtAt = LocalDateTime.now())
  val stats = StatCalculator.computeAll(def, p)
  return BattleMonState(id, def, slot, p.copy(hp = stats.hp.toShort()), stats).also { it.ability = Ability.KEEN_EYE }
}

/**
 * The raid's field: one of the player's monsters against the boss (entity 20) standing in the
 * middle between two summoned Onix that only Splash, as BattleService.createBattle lines it up.
 */
private fun raid(player: BattleMonState, boss: BattleMonState, turn: Int): BattleInstance =
    BattleInstance(
            1L, 1L, FakeSession(1L), listOf(player),
            listOf(boss, mon(ONIX, listOf(SPLASH), 21L, null), mon(ONIX, listOf(SPLASH), 22L, null)),
            BattleRng(3), catchable = false, escapable = false, format = BattleFormat.TRIPLES)
        .also {
          it.opponentPositions[0] = 1
          it.opponentPositions[1] = 0
          it.opponentPositions[2] = 2
          it.raid = RaidBossState(boss.entityId)
          it.turn = turn
        }

private fun splash(battle: BattleInstance) =
    engine.resolveTurn(battle, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 1)))

class RaidBossTest :
    FunSpec({
      test("while the crystal holds, a knockout hit leaves the boss standing and it mends") {
        val boss = mon(ONIX, listOf(SPLASH), 20L, null).also { it.currentHp = 1 }
        val battle = raid(mon(SNORLAX, listOf(TACKLE), 10L, 0), boss, turn = 1)

        engine.resolveTurn(battle, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, TACKLE, targetSide = 1, targetPosition = 1)))

        boss.fainted shouldBe false
        battle.raid!!.hurtThisTurn shouldBe true
        // Down to 1 hp from the hit, then half its max hp back at the end of the turn.
        boss.currentHp shouldBe 1 + boss.maxHp / 2
      }

      test("on turn 4 the crystal cracks: defenses up, Sandstorm, the cracked move set") {
        val boss = mon(ONIX, CrystalOnixRaid.OPENING_MOVES.map { it.toShort() }, 20L, null)
        val battle = raid(mon(SNORLAX, listOf(SPLASH), 10L, 0), boss, turn = CrystalOnixRaid.CRACK_TURN)

        splash(battle)

        battle.raid!!.cracked shouldBe true
        // Iron Defense may stack on top in the same turn.
        boss.stage(BattleStat.DEFENSE) shouldBeGreaterThanOrEqual 2
        boss.stage(BattleStat.SP_DEFENSE) shouldBe 2
        battle.weather shouldBe Weather.SANDSTORM
        boss.moves.map { it.id.toInt() } shouldBe CrystalOnixRaid.CRACKED_MOVES
      }

      test("every third turn crystal shards take a third of the player's max hp") {
        val boss = mon(ONIX, listOf(SPLASH), 20L, null)
        val player = mon(SNORLAX, listOf(SPLASH), 10L, 0)
        val battle = raid(player, boss, turn = CrystalOnixRaid.SHARD_EVERY)

        splash(battle)

        // The shards, then the Sandstorm they whipped up (Snorlax is not immune).
        player.currentHp shouldBe player.maxHp - player.maxHp / 3 - player.maxHp / 16
        battle.weather shouldBe Weather.SANDSTORM
      }

      test("once the crystal has cracked a knockout hit faints the boss") {
        val boss = mon(ONIX, listOf(SPLASH), 20L, null).also { it.currentHp = 1 }
        val battle = raid(mon(SNORLAX, listOf(TACKLE), 10L, 0), boss, turn = 1)
        battle.raid!!.cracked = true

        engine.resolveTurn(battle, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, TACKLE, targetSide = 1, targetPosition = 1)))

        boss.fainted shouldBe true
      }

      // The MonMMO client patch in f/dw2.Kk0 draws graphics 20000 + N as species N's follower; the
      // Crystal Onix form's client id is 1643, the id its follower sheet is staged under.
      test("the boss npc asks the client for the Crystal Onix follower sprite") {
        de.fiereu.openmmo.server.game.services.CrystalOnixRaidPlacement.SPRITE_ID shouldBe 31643
      }

      test("the rare drop lands on about 2 percent of wins") {
        val random = kotlin.random.Random(7)
        val drops = (1..100_000).count { CrystalOnixRaid.rareDrop(listOf("cosmetic"), random) != null }
        (drops in 1_700..2_300) shouldBe true
      }

      test("there is no rare drop when nothing is left to give") {
        val random = kotlin.random.Random(7)
        (1..1_000).all { CrystalOnixRaid.rareDrop(emptyList<String>(), random) == null } shouldBe true
      }

      // Free text (kind -22) crashed the client once the player's side had an empty position.
      test("a raid power fires as its ability banner, then its client string line, never free text") {
        val boss = mon(ONIX, CrystalOnixRaid.OPENING_MOVES.map { it.toShort() }, 20L, null)
        val battle = raid(mon(SNORLAX, listOf(SPLASH), 10L, 0), boss, turn = CrystalOnixRaid.CRACK_TURN)

        val events = splash(battle)

        events.filterIsInstance<BattleEvent.FreeLine>() shouldBe emptyList()
        val banner = events.indexOf(BattleEvent.RaidAbilityShown(20L, CrystalOnixRaid.CRYSTAL_BREAK))
        banner shouldBeGreaterThanOrEqual 1
        events[banner - 1] shouldBe BattleEvent.TurnEffect(20L)
        events[banner + 1] shouldBe BattleEvent.ClientLine(20L, CrystalOnixRaid.CRACK_LINE, shape = 0)
      }

      test("a knockout hit the crystal holds shows Crystal Shell and its line under the hit") {
        val boss = mon(ONIX, listOf(SPLASH), 20L, null).also { it.currentHp = 1 }
        val battle = raid(mon(SNORLAX, listOf(TACKLE), 10L, 0), boss, turn = 1)

        val events = engine.resolveTurn(battle, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, TACKLE, targetSide = 1, targetPosition = 1)))

        val hit = events.indexOfFirst { it is BattleEvent.DamageDealt && it.targetId == 20L }
        events[hit + 1] shouldBe BattleEvent.RaidAbilityShown(20L, CrystalOnixRaid.CRYSTAL_SHELL)
        events[hit + 2] shouldBe BattleEvent.ClientLine(20L, CrystalOnixRaid.SHELL_LINE, shape = 0)
        // The end-of-turn mend is its own power.
        events shouldContain BattleEvent.RaidAbilityShown(20L, CrystalOnixRaid.LIVING_CRYSTAL)
      }

      test("a spread move's move checks run once: a flinched user loses one action, not one per target") {
        val boss = mon(ONIX, listOf(CrystalOnixRaid.EARTHQUAKE.toShort()), 20L, null).also { it.flinched = true }
        val player = listOf(mon(SNORLAX, listOf(SPLASH), 10L, 0), mon(SNORLAX, listOf(SPLASH), 11L, 1))
        val battle =
            BattleInstance(1L, 1L, FakeSession(1L), player, listOf(boss, mon(ONIX, listOf(SPLASH), 21L, null)), BattleRng(3),
                    format = BattleFormat.TRIPLES)
                .also {
                  it.playerPositions[0] = 0
                  it.playerPositions[1] = 1
                  it.opponentPositions[0] = 0
                  it.opponentPositions[1] = 1
                }

        val events =
            engine.resolveTurn(
                battle,
                listOf(
                    ChosenAction(0, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0),
                    ChosenAction(1, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0)))

        events.count { it is BattleEvent.CantMove && it.attackerId == 20L } shouldBe 1
        events.filterIsInstance<BattleEvent.DamageDealt>() shouldBe emptyList()
      }

      test("Stealth Rock is one action on the foe's side, not one failing use per foe") {
        val boss = mon(ONIX, listOf(CrystalOnixRaid.STEALTH_ROCK.toShort()), 20L, null)
        val player = listOf(mon(SNORLAX, listOf(SPLASH), 10L, 0), mon(SNORLAX, listOf(SPLASH), 11L, 1))
        val battle =
            BattleInstance(1L, 1L, FakeSession(1L), player, listOf(boss), BattleRng(3), format = BattleFormat.TRIPLES).also {
              it.playerPositions[0] = 0
              it.playerPositions[1] = 1
              it.opponentPositions[0] = 0
            }

        val events =
            engine.resolveTurn(
                battle,
                listOf(
                    ChosenAction(0, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0),
                    ChosenAction(1, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0)))

        events.filterIsInstance<BattleEvent.MoveFailed>().filter { it.attackerId == 20L } shouldBe emptyList()
        battle.playerSide.stealthRock shouldBe true
      }

      test("the boss's Earthquake hits the player's side but not its summoned Onix") {
        val boss = mon(ONIX, listOf(CrystalOnixRaid.EARTHQUAKE.toShort()), 20L, null)
        val battle = raid(mon(SNORLAX, listOf(SPLASH), 10L, 0), boss, turn = 1)
        battle.raid!!.cracked = true

        val damaged = splash(battle).filterIsInstance<BattleEvent.DamageDealt>().map { it.targetId }

        damaged shouldBe listOf(10L)
      }

      test("in a triple battle the far corner is out of reach") {
        val player = listOf(mon(SNORLAX, listOf(SPLASH), 10L, 0), mon(SNORLAX, listOf(SPLASH), 11L, 1), mon(SNORLAX, listOf(SPLASH), 12L, 2))
        val foes = listOf(mon(ONIX, listOf(SPLASH), 20L, null), mon(ONIX, listOf(SPLASH), 21L, null), mon(ONIX, listOf(SPLASH), 22L, null))
        val battle =
            BattleInstance(1L, 1L, FakeSession(1L), player, foes, BattleRng(3), format = BattleFormat.TRIPLES).also {
              for (i in 0..2) {
                it.playerPositions[i] = i
                it.opponentPositions[i] = i
              }
            }

        // Position p faces the other side's position p (the mirrored mapping was the wrong way round
        // on the client for both sides, project owner 2026-09-14).
        battle.reaches(player[0], foes[0]) shouldBe true
        battle.reaches(player[0], foes[1]) shouldBe true
        battle.reaches(player[0], foes[2]) shouldBe false
        battle.reaches(foes[2], player[0]) shouldBe false
        battle.reaches(foes[0], player[1]) shouldBe true
        battle.reaches(player[1], foes[0]) shouldBe true
        battle.reaches(player[1], foes[2]) shouldBe true
        battle.reaches(player[0], player[2]) shouldBe false
      }
    })
