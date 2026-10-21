package pirated.server.game.battle

import pirated.common.Pokemon
import pirated.common.PokemonMove
import pirated.common.enums.Ability
import pirated.common.enums.EVs
import pirated.common.enums.IVs
import pirated.common.enums.PokemonContainer
import pirated.items.ItemRegistry
import pirated.items.generated.Items
import pirated.moves.MoveRegistry
import pirated.net.game.packets.battle.BattleEntityMoveEventPacket
import pirated.pokemon.SpeciesRegistry
import pirated.pokemon.expansion.FormChangeRegistry
import pirated.server.game.testsupport.FakeSession
import pirated.server.game.world.interest.InterestManager
import pirated.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private val speciesRegistry = SpeciesRegistry()
private val moveRegistry = MoveRegistry()
private val itemRegistry = ItemRegistry()
private val engine = TurnEngine(moveRegistry, TypeChart(), itemRegistry, speciesRegistry, FormChangeRegistry())

private fun mon(dexId: Int, level: Int, moves: List<Short>, id: Long, ability: Ability, item: Int, player: Boolean): BattleMonState {
  val padded = List(4) { i -> moves.getOrNull(i) ?: 0 }
  val def = speciesRegistry.get(dexId)!!
  val p = Pokemon(id = id, ownerId = 0, container = PokemonContainer.PARTY, containerSlot = 0, dexId = dexId, seed = 0, ot = "", nickname = "",
      level = level.toByte(), hp = Short.MAX_VALUE, xp = 0, eVs = EVs(), iVs = IVs(),
      moves = padded.map { PokemonMove(it, (moveRegistry.get(it.toInt())?.pp ?: 0).toByte()) }, heldItem = item,
      isShiny = false, hasHiddenAbility = false, isAlpha = false, isSecret = false, isFatefulEncounter = false, isRaidEncounter = false, caughtAt = LocalDateTime.now())
  val stats = StatCalculator.computeAll(def, p)
  val s = BattleMonState(id, def, if (player) 0 else null, p.copy(hp = stats.hp.toShort()), stats)
  s.ability = ability
  return s
}

class DoubleMoveTextProbeTest :
    FunSpec({
      test("a plain attack announces itself once, with or without a held item") {
        for (item in listOf(0, itemRegistry.idsOf(Items.LEFTOVERS).first())) {
          val session = FakeSession(1L)
          val interest = InterestManager()
          val emitter = BattlePacketEmitter(interest)
          val player = mon(18, 50, listOf(33), 10L, Ability.KEEN_EYE, item, true)
          val wild = mon(50, 18, listOf(33), 20L, Ability.ARENA_TRAP, itemRegistry.idsOf(Items.SOFT_SAND).first(), false)
          val battle = BattleInstance(1L, 1L, session, listOf(player), listOf(wild), BattleRng(7))
          interest.join(session, battle.key)
          val events = engine.resolveTurn(battle, 33)
          events.filterIsInstance<BattleEvent.MoveUsed>().count { it.attackerId == 10L } shouldBe 1
          emitter.sendEvents(battle, events)
          session.sent.filterIsInstance<BattleEntityMoveEventPacket>().count { it.sourceEntity == 10L } shouldBe 1
        }
      }
    })
