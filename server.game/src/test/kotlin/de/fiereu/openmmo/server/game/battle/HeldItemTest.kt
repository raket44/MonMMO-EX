package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.items.generated.Items
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.typechart.TypeChart
import de.fiereu.openmmo.pokemon.expansion.FormChangeRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

private const val PLAYER_ID = 0x1C000L
private const val WILD_ID = 0x3C000L

private const val TACKLE: Short = 33
private const val SPLASH: Short = 150
private const val THUNDER_WAVE: Short = 86
private const val THIEF: Short = 168

private const val RATTATA = 19
private const val PIDGEOT = 18
private const val SNORLAX = 143
private const val DARMANITAN = 555

private val speciesRegistry = SpeciesRegistry()
private val itemRegistry = ItemRegistry()
private val engine = TurnEngine(MoveRegistry(), TypeChart(), itemRegistry, speciesRegistry, FormChangeRegistry())

private fun itemId(item: de.fiereu.openmmo.items.ItemDef): Int = itemRegistry.idsOf(item).first()

private val moveRegistry = MoveRegistry()

private fun pokemon(dexId: Int, level: Int, moves: List<Short>, id: Long, heldItem: Int): Pokemon {
  val padded = List(4) { i -> moves.getOrNull(i) ?: 0 }
  return Pokemon(
      id = id,
      ownerId = 0,
      container = PokemonContainer.PARTY,
      containerSlot = 0,
      dexId = dexId,
      seed = 0,
      ot = "",
      nickname = "",
      level = level.toByte(),
      hp = Short.MAX_VALUE,
      xp = 0,
      eVs = EVs(),
      iVs = IVs(),
      moves = padded.map { PokemonMove(it, (moveRegistry.get(it.toInt())?.pp ?: 0).toByte()) },
      heldItem = heldItem,
      isShiny = false,
      hasHiddenAbility = false,
      isAlpha = false,
      isSecret = false,
      isFatefulEncounter = false,
      isRaidEncounter = false,
      caughtAt = LocalDateTime.now(),
  )
}

private fun state(dexId: Int, level: Int, moves: List<Short>, id: Long, ability: Ability, item: Int = 0): BattleMonState {
  val def = speciesRegistry.get(dexId)!!
  val mon = pokemon(dexId, level, moves, id, item)
  val stats = StatCalculator.computeAll(def, mon)
  val state = BattleMonState(id, def, if (id == PLAYER_ID) 0 else null, mon.copy(hp = stats.hp.toShort()), stats)
  state.ability = ability
  return state
}

private fun battle(player: BattleMonState, wild: BattleMonState, seed: Long): BattleInstance =
    BattleInstance(1L, 100L, FakeSession(100L), listOf(player), listOf(wild), BattleRng(seed))

class HeldItemTest :
    FunSpec({
      test("Leftovers restores a sixteenth at the end of the turn") {
        val player = state(SNORLAX, 50, listOf(SPLASH), PLAYER_ID, Ability.THICK_FAT, itemId(Items.LEFTOVERS))
        val wild = state(RATTATA, 5, listOf(SPLASH), WILD_ID, Ability.RUN_AWAY)
        player.currentHp = player.maxHp / 2
        engine.resolveTurn(battle(player, wild, 1), SPLASH)
        player.currentHp shouldBe player.maxHp / 2 + player.maxHp / 16
      }

      test("Focus Sash leaves the holder at 1 hp once and is used up") {
        val player = state(SNORLAX, 100, listOf(TACKLE), PLAYER_ID, Ability.THICK_FAT)
        val wild = state(RATTATA, 5, listOf(SPLASH), WILD_ID, Ability.RUN_AWAY, itemId(Items.FOCUS_SASH))
        val events = engine.resolveTurn(battle(player, wild, 1), TACKLE)
        wild.currentHp shouldBe 1
        wild.heldItem shouldBe 0
        events.filterIsInstance<BattleEvent.ItemChanged>().first { it.targetId == WILD_ID }.itemId shouldBe 0
      }

      test("Life Orb costs a tenth of the attacker's hp per hit") {
        val player = state(PIDGEOT, 50, listOf(TACKLE), PLAYER_ID, Ability.KEEN_EYE, itemId(Items.LIFE_ORB))
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID, Ability.THICK_FAT)
        engine.resolveTurn(battle(player, wild, 1), TACKLE)
        player.currentHp shouldBe player.maxHp - player.maxHp / 10
      }

      test("Sitrus Berry fires once hp drops to half") {
        val player = state(PIDGEOT, 50, listOf(TACKLE), PLAYER_ID, Ability.KEEN_EYE)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID, Ability.THICK_FAT, itemId(Items.SITRUS_BERRY))
        wild.currentHp = wild.maxHp / 2 + 1
        val events = engine.resolveTurn(battle(player, wild, 1), TACKLE)
        val heal = events.filterIsInstance<BattleEvent.Line>().first { it.line == de.fiereu.openmmo.net.game.packets.battle.BattleLine.ITEM_HEAL }
        heal.targetId shouldBe WILD_ID
        wild.heldItem shouldBe 0
        wild.currentHp shouldBeGreaterThan wild.maxHp / 2 + 1 - player.maxHp
      }

      test("Lum Berry cures paralysis the moment it lands") {
        val player = state(PIDGEOT, 50, listOf(THUNDER_WAVE), PLAYER_ID, Ability.KEEN_EYE)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID, Ability.THICK_FAT, itemId(Items.LUM_BERRY))
        engine.resolveTurn(battle(player, wild, 1), THUNDER_WAVE)
        wild.status shouldBe 0
        wild.heldItem shouldBe 0
      }

      test("Thief takes the target's item when the thief holds none") {
        val player = state(PIDGEOT, 50, listOf(THIEF), PLAYER_ID, Ability.KEEN_EYE)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID, Ability.THICK_FAT, itemId(Items.LEFTOVERS))
        engine.resolveTurn(battle(player, wild, 1), THIEF)
        player.heldItem shouldBe itemId(Items.LEFTOVERS)
        wild.heldItem shouldBe 0
      }

      test("Sticky Hold keeps Thief from taking anything") {
        val player = state(PIDGEOT, 50, listOf(THIEF), PLAYER_ID, Ability.KEEN_EYE)
        val wild = state(SNORLAX, 50, listOf(SPLASH), WILD_ID, Ability.STICKY_HOLD, itemId(Items.LEFTOVERS))
        engine.resolveTurn(battle(player, wild, 1), THIEF)
        player.heldItem shouldBe 0
        wild.heldItem shouldBe itemId(Items.LEFTOVERS)
      }

      test("Zen Mode swaps Darmanitan's form at half hp and back above it") {
        val player = state(DARMANITAN, 50, listOf(SPLASH), PLAYER_ID, Ability.ZEN_MODE)
        val wild = state(RATTATA, 5, listOf(SPLASH), WILD_ID, Ability.RUN_AWAY)
        val original = player.species.id
        player.currentHp = player.maxHp / 4
        val events = engine.resolveTurn(battle(player, wild, 1), SPLASH)
        player.species.id shouldNotBe original
        events.filterIsInstance<BattleEvent.SpeciesShown>().first().targetId shouldBe PLAYER_ID
        player.currentHp = player.maxHp
        engine.resolveTurn(battle(player, wild, 2), SPLASH)
        player.species.id shouldBe original
        events.filterIsInstance<BattleEvent.Fainted>().shouldBeEmpty()
      }
    })
