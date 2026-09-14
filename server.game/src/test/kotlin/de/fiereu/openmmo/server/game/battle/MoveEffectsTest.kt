package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.expansion.FormChangeRegistry
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private val speciesRegistry = SpeciesRegistry()
private val moveRegistry = MoveRegistry()
private val engine = TurnEngine(moveRegistry, TypeChart(), ItemRegistry(), speciesRegistry, FormChangeRegistry())

private const val TACKLE: Short = 33
private const val GROWL: Short = 45
private const val SPLASH: Short = 150
private const val DRAGON_DANCE: Short = 349
private const val PERISH_SONG: Short = 195
private const val FAKE_OUT: Short = 252
private const val FUTURE_SIGHT: Short = 248

private fun mon(dexId: Int, moves: List<Short>, id: Long, player: Boolean): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val p = Pokemon(id = id, ownerId = 0, container = PokemonContainer.PARTY, containerSlot = 0, dexId = dexId, seed = 0, ot = "", nickname = "",
      level = 50, hp = Short.MAX_VALUE, xp = 0, eVs = EVs(), iVs = IVs(),
      moves = List(4) { i -> moves.getOrNull(i) ?: 0.toShort() }.map { PokemonMove(it, (moveRegistry.get(it.toInt())?.pp ?: 0).toByte()) }, heldItem = 0,
      isShiny = false, hasHiddenAbility = false, isAlpha = false, isSecret = false, isFatefulEncounter = false, isRaidEncounter = false, caughtAt = LocalDateTime.now())
  val stats = StatCalculator.computeAll(def, p)
  return BattleMonState(id, def, if (player) 0 else null, p.copy(hp = stats.hp.toShort()), stats).also { it.ability = Ability.KEEN_EYE }
}

private fun singles(player: BattleMonState, wild: BattleMonState) = BattleInstance(1L, 1L, FakeSession(1L), listOf(player), listOf(wild), BattleRng(5))

private fun BattleInstance.turn(moveId: Short) = engine.resolveTurn(this, listOf(ChosenAction(0, ChosenAction.Kind.MOVE, moveId)))

class MoveEffectsTest :
    FunSpec({
      test("Dragon Dance raises Attack and Speed") {
        val player = mon(143, listOf(DRAGON_DANCE), 10L, true)
        singles(player, mon(19, listOf(SPLASH), 20L, false)).turn(DRAGON_DANCE)
        player.stage(BattleStat.ATTACK) shouldBe 1
        player.stage(BattleStat.SPEED) shouldBe 1
      }

      test("a Substitute takes a hit in the monster's place") {
        val player = mon(143, listOf(SPLASH), 10L, true)
        player.substituteHp = 50
        val hp = player.currentHp
        singles(player, mon(19, listOf(TACKLE), 20L, false)).turn(SPLASH)
        player.currentHp shouldBe hp
        (player.substituteHp < 50) shouldBe true
      }

      test("a taunted monster cannot use a status move") {
        val player = mon(143, listOf(SPLASH), 10L, true)
        val wild = mon(19, listOf(GROWL), 20L, false)
        wild.tauntTurns = 3
        val events = singles(player, wild).turn(SPLASH)
        player.stage(BattleStat.ATTACK) shouldBe 0
        events.any { it is BattleEvent.MoveFailed && it.attackerId == 20L } shouldBe true
      }

      test("Stealth Rock cuts an eighth off a neutral monster coming in") {
        val player = mon(143, listOf(SPLASH), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        val battle = singles(player, wild)
        battle.opponentSide.stealthRock = true
        engine.switchIn(battle, wild, mutableListOf())
        wild.currentHp shouldBe wild.maxHp - wild.maxHp / 8
      }

      test("Encore holds a monster to its last move") {
        val player = mon(143, listOf(SPLASH), 10L, true)
        val wild = mon(19, listOf(GROWL, TACKLE), 20L, false)
        wild.encoreTurns = 3
        wild.encoreMoveId = TACKLE.toInt()
        repeat(2) {
          val used = singles(player, wild).turn(SPLASH).filterIsInstance<BattleEvent.MoveUsed>().filter { it.attackerId == 20L }
          used.map { it.moveId } shouldBe listOf(TACKLE)
        }
      }

      test("Perish Song faints everyone who heard it at the end of the third turn after") {
        val player = mon(143, listOf(PERISH_SONG, SPLASH), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        val battle = singles(player, wild)
        battle.turn(PERISH_SONG)
        battle.turn(SPLASH)
        battle.turn(SPLASH)
        wild.fainted shouldBe false
        battle.turn(SPLASH)
        wild.fainted shouldBe true
        player.fainted shouldBe true
      }

      test("Fake Out only works on the first turn out") {
        val player = mon(143, listOf(FAKE_OUT), 10L, true)
        val battle = singles(player, mon(143, listOf(SPLASH), 20L, false))
        battle.turn(FAKE_OUT).any { it is BattleEvent.DamageDealt && it.targetId == 20L } shouldBe true
        battle.turn(FAKE_OUT).any { it is BattleEvent.MoveFailed && it.attackerId == 10L } shouldBe true
      }

      test("Future Sight lands at the end of the second turn after it was used") {
        val player = mon(65, listOf(FUTURE_SIGHT, SPLASH), 10L, true)
        val wild = mon(143, listOf(SPLASH), 20L, false)
        val battle = singles(player, wild)
        battle.turn(FUTURE_SIGHT)
        battle.turn(SPLASH)
        wild.currentHp shouldBe wild.maxHp
        battle.turn(SPLASH)
        (wild.currentHp < wild.maxHp) shouldBe true
      }

      test("Trick Room lets the slower monster move first") {
        val player = mon(143, listOf(TACKLE), 10L, true)
        val battle = singles(player, mon(101, listOf(TACKLE), 20L, false))
        battle.field.trickRoomTurns = 5
        battle.turn(TACKLE).filterIsInstance<BattleEvent.MoveUsed>().first().attackerId shouldBe 10L
      }

      test("Memento faints the user and sharply lowers the target's attacks") {
        val player = mon(143, listOf(262), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        singles(player, wild).turn(262)
        player.fainted shouldBe true
        wild.stage(BattleStat.ATTACK) shouldBe -2
        wild.stage(BattleStat.SP_ATTACK) shouldBe -2
      }

      test("Defog lowers evasion and blows every hazard away") {
        val player = mon(143, listOf(432), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        val battle = singles(player, wild)
        battle.playerSide.stealthRock = true
        battle.opponentSide.spikes = 2
        battle.turn(432)
        wild.stage(BattleStat.EVASION) shouldBe -1
        battle.playerSide.stealthRock shouldBe false
        battle.opponentSide.spikes shouldBe 0
      }

      test("Venom Drench only works on a poisoned target") {
        val player = mon(143, listOf(599), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        singles(player, wild).turn(599).any { it is BattleEvent.MoveFailed && it.attackerId == 10L } shouldBe true
        wild.status = de.fiereu.openmmo.common.StatusCondition.POISON
        singles(player, wild).turn(599)
        wild.stage(BattleStat.SPEED) shouldBe -1
      }

      test("Sweet Scent lowers evasion by its generation's two stages") {
        val player = mon(143, listOf(230), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        singles(player, wild).turn(230)
        wild.stage(BattleStat.EVASION) shouldBe -2
      }

      test("Soak turns the target into a pure Water type") {
        val player = mon(143, listOf(487), 10L, true)
        val wild = mon(19, listOf(SPLASH), 20L, false)
        singles(player, wild).turn(487)
        wild.hasType(PokemonType.WATER) shouldBe true
        wild.hasType(PokemonType.NORMAL) shouldBe false
      }

      test("Burn Up burns away the user's Fire type, and fails once it is gone") {
        val burnUp: Short = 645
        val player = mon(4, listOf(burnUp), 10L, true)
        val battle = singles(player, mon(143, listOf(SPLASH), 20L, false))
        battle.turn(burnUp)
        player.hasType(PokemonType.FIRE) shouldBe false
        battle.turn(burnUp).any { it is BattleEvent.MoveFailed && it.attackerId == 10L } shouldBe true
      }

      test("Freeze-Dry is super effective on a Water type") {
        val freezeDry: Short = 573
        val player = mon(143, listOf(freezeDry), 10L, true)
        val events = singles(player, mon(7, listOf(SPLASH), 20L, false)).turn(freezeDry)
        events.filterIsInstance<BattleEvent.DamageDealt>().first { it.targetId == 20L }.effectiveness shouldBe 2 * TypeChart.NEUTRAL
      }

      test("Electric Terrain keeps a grounded monster awake") {
        val spore: Short = 147
        val player = mon(143, listOf(spore), 10L, true)
        val wild = mon(143, listOf(SPLASH), 20L, false)
        val battle = singles(player, wild)
        battle.field.terrain = Terrain.ELECTRIC
        battle.field.terrainTurns = 5
        battle.turn(spore)
        wild.status shouldBe 0
      }

      test("U-turn waits for the player's pick, then the turn goes on") {
        val uTurn: Short = 369
        val player = mon(19, listOf(uTurn), 10L, true)
        val bench = mon(143, listOf(SPLASH), 11L, true)
        val wild = mon(143, listOf(TACKLE), 20L, false)
        val battle = BattleInstance(1L, 1L, FakeSession(1L), listOf(player, bench), listOf(wild), BattleRng(5))
        val first = battle.turn(uTurn)
        battle.pendingSelfSwitch?.position shouldBe 0
        first.none { it is BattleEvent.MoveUsed && it.attackerId == 20L } shouldBe true
        battle.pendingSelfSwitch = null
        val events = mutableListOf<BattleEvent>()
        engine.swapIn(battle, true, 0, 1, 0, false, events)
        events += engine.resumeTurn(battle)
        events.any { it is BattleEvent.SwitchedIn && it.position == 0 } shouldBe true
        events.filterIsInstance<BattleEvent.DamageDealt>().any { it.targetId == 11L } shouldBe true
      }

      test("Baton Pass hands the stat stages to the monster coming in") {
        val batonPass: Short = 226
        val player = mon(19, listOf(batonPass), 10L, true)
        player.changeStage(BattleStat.ATTACK, 2)
        val bench = mon(143, listOf(SPLASH), 11L, true)
        val battle = BattleInstance(1L, 1L, FakeSession(1L), listOf(player, bench), listOf(mon(143, listOf(SPLASH), 20L, false)), BattleRng(5))
        battle.turn(batonPass)
        battle.pendingSelfSwitch?.batonPass shouldBe true
        engine.swapIn(battle, true, 0, 1, 0, true, mutableListOf())
        bench.stage(BattleStat.ATTACK) shouldBe 2
        player.stage(BattleStat.ATTACK) shouldBe 0
      }

      test("Roar ends a wild battle") {
        val roar: Short = 46
        val battle = singles(mon(143, listOf(roar), 10L, true), mon(19, listOf(SPLASH), 20L, false))
        val events = battle.turn(roar)
        battle.moveEnded shouldBe MoveEnding.BLOWN_AWAY
        events.any { it is BattleEvent.BlownAway && it.targetId == 20L } shouldBe true
      }

      test("Sketch keeps the target's last move for good") {
        val sketch: Short = 166
        val player = mon(235, listOf(sketch), 10L, true)
        val wild = mon(143, listOf(TACKLE), 20L, false)
        wild.lastMoveId = TACKLE.toInt()
        singles(player, wild).turn(sketch)
        player.moves.first().id shouldBe TACKLE
        player.ownMoves().first().id shouldBe TACKLE
        player.source.moves.first().id shouldBe TACKLE
      }

      test("Snatch takes a Dragon Dance for its own user") {
        val snatch: Short = 289
        val player = mon(143, listOf(snatch), 10L, true)
        val wild = mon(19, listOf(DRAGON_DANCE), 20L, false)
        singles(player, wild).turn(snatch)
        player.stage(BattleStat.ATTACK) shouldBe 1
        wild.stage(BattleStat.ATTACK) shouldBe 0
      }

      test("Magic Coat sends a Growl back") {
        val magicCoat: Short = 277
        val player = mon(143, listOf(magicCoat), 10L, true)
        val wild = mon(19, listOf(GROWL), 20L, false)
        singles(player, wild).turn(magicCoat)
        player.stage(BattleStat.ATTACK) shouldBe 0
        wild.stage(BattleStat.ATTACK) shouldBe -1
      }

      test("Imprison stops a foe using a move the user knows") {
        val player = mon(143, listOf(TACKLE), 10L, true)
        player.imprisoning = true
        val wild = mon(19, listOf(TACKLE), 20L, false)
        singles(player, wild).turn(SPLASH).any { it is BattleEvent.MoveFailed && it.attackerId == 20L } shouldBe true
      }

      test("Mimic's copy is gone once the battle ends") {
        val mimic: Short = 102
        val player = mon(19, listOf(mimic), 10L, true)
        val wild = mon(143, listOf(TACKLE), 20L, false)
        wild.lastMoveId = TACKLE.toInt()
        val battle = singles(player, wild)
        battle.turn(mimic)
        player.moves.first().id shouldBe TACKLE
        player.ownMoves().first().id shouldBe mimic
        engine.endBattle(battle)
        player.moves.first().id shouldBe mimic
      }

      test("Pursuit catches a monster being switched out, at double power") {
        val pursuit: Short = 228
        val player = mon(143, listOf(SPLASH), 10L, true)
        val wild = mon(19, listOf(pursuit), 20L, false)
        val battle = BattleInstance(1L, 1L, FakeSession(1L), listOf(player, mon(143, listOf(SPLASH), 11L, true)), listOf(wild), BattleRng(5))
        engine.pursuit(battle, player).any { it is BattleEvent.DamageDealt && it.targetId == 10L } shouldBe true
        wild.movedThisTurn shouldBe true
      }

      test("Fling throws the held Iron Ball") {
        val fling: Short = 374
        val player = mon(143, listOf(fling), 10L, true)
        player.heldItem = ItemRegistry().idsOf(de.fiereu.openmmo.items.generated.Items.IRON_BALL).first()
        val wild = mon(143, listOf(SPLASH), 20L, false)
        singles(player, wild).turn(fling)
        player.heldItem shouldBe 0
        (wild.currentHp < wild.maxHp) shouldBe true
      }
    })
