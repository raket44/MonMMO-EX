package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.battle.BattleActionEvent
import de.fiereu.openmmo.net.game.packets.battle.BattleEffectTarget
import de.fiereu.openmmo.net.game.packets.battle.BattleEffectTargetCodec
import de.fiereu.openmmo.net.game.packets.battle.BattleEventBody
import de.fiereu.openmmo.net.game.packets.battle.BattleLine
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val JIGGLYPUFF_UID = 0x0102030405060708L

private fun le64(v: Long) = ByteArray(8) { i -> (v ushr (8 * i)).toByte() }

/** One line sub-event on one target, no entity slots: target uid, outcome 0, one event. */
private fun onTarget(line: BattleLine, values: List<Int>) =
    BattleEffectTarget(JIGGLYPUFF_UID, 0, listOf(BattleActionEvent(null, null, BattleEventBody.Line(line, values))))

private fun header(kind: Int) = le64(JIGGLYPUFF_UID) + byteArrayOf(0x00, 0x00, 0x01) + byteArrayOf(kind.toByte(), 0x00)

/** Line layouts as r32645 f/ko1 reads them (bytecode 2026-09-14). */
class ItemAndTrapLineTest :
    FunSpec({
      // Case 55 -> f/fw0(item, hp): an Oran Berry (5155) bringing Jigglypuff to 22 hp.
      test("the item heal line carries the item first, then the hp") {
        val target = onTarget(BattleLine.ITEM_HEAL, listOf(5155, 22))
        val bytes = header(55) + byteArrayOf(0x23, 0x14, 0x16, 0x00)
        BattleEffectTargetCodec.encodeToBytes(target) shouldBe bytes
        BattleEffectTargetCodec.decodeBytes(bytes) shouldBe target
      }

      // Case 24: kind, move id, and the hp only when kind is 1.
      test("a trap hurting its victim carries the new hp") {
        val target = onTarget(BattleLine.TRAP, listOf(1, 35, 40))
        val bytes = header(24) + byteArrayOf(0x01, 0x23, 0x00, 0x28, 0x00)
        BattleEffectTargetCodec.encodeToBytes(target) shouldBe bytes
        BattleEffectTargetCodec.decodeBytes(bytes) shouldBe target
      }

      test("a trap letting go ends after the move id") {
        val target = onTarget(BattleLine.TRAP, listOf(2, 35))
        val bytes = header(24) + byteArrayOf(0x02, 0x23, 0x00)
        BattleEffectTargetCodec.encodeToBytes(target) shouldBe bytes
        BattleEffectTargetCodec.decodeBytes(bytes) shouldBe target
      }
    })
