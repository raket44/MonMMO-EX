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
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private val speciesRegistry = SpeciesRegistry()
private val moveRegistry = MoveRegistry()
private val engine = TurnEngine(moveRegistry, TypeChart(), ItemRegistry(), speciesRegistry, FormChangeRegistry())

private const val TACKLE: Short = 33
private const val SURF: Short = 57
private const val TRANSFORM: Short = 144
private const val SPLASH: Short = 150

private fun mon(dexId: Int, moves: List<Short>, id: Long, slot: Int?): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val p = Pokemon(id = id, ownerId = 0, container = PokemonContainer.PARTY, containerSlot = (slot ?: 0).toShort(), dexId = dexId, seed = 0, ot = "", nickname = "",
      level = 50, hp = Short.MAX_VALUE, xp = 0, eVs = EVs(), iVs = IVs(),
      moves = List(4) { i -> moves.getOrNull(i) ?: 0.toShort() }.map { PokemonMove(it, (moveRegistry.get(it.toInt())?.pp ?: 0).toByte()) }, heldItem = 0,
      isShiny = false, hasHiddenAbility = false, isAlpha = false, isSecret = false, isFatefulEncounter = false, isRaidEncounter = false, caughtAt = LocalDateTime.now())
  val stats = StatCalculator.computeAll(def, p)
  return BattleMonState(id, def, slot, p.copy(hp = stats.hp.toShort()), stats).also { it.ability = Ability.KEEN_EYE }
}

/** Two of the player's monsters against two foes that only Splash, so every hit is the player's. */
private fun doubles(player: List<BattleMonState>, foes: List<BattleMonState>): BattleInstance =
    BattleInstance(1L, 1L, FakeSession(1L), player, foes, BattleRng(3), format = BattleFormat.DOUBLES).also {
      it.playerPositions[1] = 1
      it.opponentPositions[1] = 1
    }

private fun List<BattleEvent>.damaged(): List<Long> = filterIsInstance<BattleEvent.DamageDealt>().map { it.targetId }

class FieldTargetingTest :
    FunSpec({
      test("Surf hits both foes and the user's partner") {
        val battle =
            doubles(
                listOf(mon(9, listOf(SURF), 10L, 0), mon(143, listOf(SPLASH), 11L, 1)),
                listOf(mon(19, listOf(SPLASH), 20L, null), mon(19, listOf(SPLASH), 21L, null)))
        val events =
            engine.resolveTurn(
                battle,
                listOf(
                    ChosenAction(0, ChosenAction.Kind.MOVE, SURF, targetSide = 1, targetPosition = 0),
                    ChosenAction(1, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0)))
        events.damaged().toSet() shouldBe setOf(20L, 21L, 11L)
      }

      test("a foe's Surf hits both of the player's monsters and the foe's own partner") {
        val battle =
            doubles(
                listOf(mon(143, listOf(SPLASH), 10L, 0), mon(143, listOf(SPLASH), 11L, 1)),
                listOf(mon(9, listOf(SURF), 20L, null), mon(143, listOf(SPLASH), 21L, null)))
        val events =
            engine.resolveTurn(
                battle,
                listOf(
                    ChosenAction(0, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0),
                    ChosenAction(1, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0)))
        events.damaged().toSet() shouldBe setOf(10L, 11L, 21L)
      }

      // The client packs a teammate as side 0 at its position (captured: position 1 sent 0x00).
      test("an attack aimed at the partner hits the partner") {
        val battle =
            doubles(
                listOf(mon(143, listOf(SPLASH), 10L, 0), mon(143, listOf(TACKLE), 11L, 1)),
                listOf(mon(19, listOf(SPLASH), 20L, null), mon(19, listOf(SPLASH), 21L, null)))
        val events =
            engine.resolveTurn(
                battle,
                listOf(
                    ChosenAction(0, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0),
                    ChosenAction(1, ChosenAction.Kind.MOVE, TACKLE, targetSide = 0, targetPosition = 0)))
        events.damaged() shouldContain 10L
        events.damaged() shouldNotContain 20L
      }

      test("an attack packed as the user's own position still goes at a foe") {
        val battle =
            doubles(
                listOf(mon(143, listOf(TACKLE), 10L, 0), mon(143, listOf(SPLASH), 11L, 1)),
                listOf(mon(19, listOf(SPLASH), 20L, null), mon(19, listOf(SPLASH), 21L, null)))
        val events =
            engine.resolveTurn(
                battle,
                listOf(
                    ChosenAction(0, ChosenAction.Kind.MOVE, TACKLE, targetSide = 0, targetPosition = 0),
                    ChosenAction(1, ChosenAction.Kind.MOVE, SPLASH, targetSide = 1, targetPosition = 0)))
        events.damaged() shouldNotContain 10L
        events.damaged() shouldNotContain 11L
      }

      test("Transform copies the target's species, stats, stages and moves for the battle only") {
        val ditto = mon(132, listOf(TRANSFORM), 20L, null)
        val snorlax = mon(143, listOf(TACKLE, SURF), 10L, 0)
        snorlax.changeStage(BattleStat.ATTACK, 2)
        val battle = BattleInstance(1L, 1L, FakeSession(1L), listOf(snorlax), listOf(ditto), BattleRng(3))
        val hpBefore = ditto.currentHp
        engine.resolveTurn(battle, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, SPLASH)))
        ditto.species.id shouldBe snorlax.species.id
        ditto.stats.atk shouldBe snorlax.stats.atk
        ditto.currentHp shouldBe hpBefore
        ditto.stage(BattleStat.ATTACK) shouldBe 2
        ditto.moves.map { it.id } shouldBe listOf<Short>(TACKLE, SURF, 0, 0)
        ditto.moves.filter { it.id.toInt() != 0 }.map { it.pp.toInt() } shouldBe listOf(5, 5)
        ditto.source.moves.map { it.id }.first() shouldBe TRANSFORM
        engine.endBattle(battle)
        ditto.species.id shouldBe 132
        ditto.moves.first().id shouldBe TRANSFORM
      }
    })
