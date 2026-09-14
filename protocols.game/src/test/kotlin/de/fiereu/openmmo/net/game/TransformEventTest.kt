package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.battle.BattleEffectTarget
import de.fiereu.openmmo.net.game.packets.battle.BattleEffectTargetCodec
import de.fiereu.openmmo.net.game.packets.battle.BattleActionEvent
import de.fiereu.openmmo.net.game.packets.battle.BattleEventBody
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val NINETALES_UID = 0x0102030405060708L
private const val DITTO_UID = 0x1112131415161718L

private fun le64(v: Long) = ByteArray(8) { i -> (v ushr (8 * i)).toByte() }

/**
 * Sub-event 31 as r32645 f/ko1.kQ0 reads it (bytecode-derived layout): a Ditto transforming into
 * a Ninetales with Flash Fire, moves 53/83/98/261, Fire/Fire and no stages.
 */
class TransformEventTest :
    FunSpec({
      test("a transform sub-event encodes the client's layout") {
        val event =
            BattleActionEvent(
                entityA = NINETALES_UID,
                entityB = DITTO_UID,
                body =
                    BattleEventBody.Transform(
                        species = 38, moves = listOf(53, 83, 98, 261), abilityId = 18, gender = 0, type1 = 10, type2 = 10))
        val target = BattleEffectTarget(NINETALES_UID, 0, listOf(event))
        val expectedEvent =
            byteArrayOf(0x1F, 0x03) + le64(NINETALES_UID) + le64(DITTO_UID) +
                byteArrayOf(0x26, 0x00, 0x00, 0x35, 0x00, 0x53, 0x00, 0x62, 0x00, 0x05, 0x01, 0x12, 0x00, 0x00, 0x00, 0x00,
                    0x66, 0x66, 0x66, 0x66, 0x0A, 0x0A)
        val bytes = le64(NINETALES_UID) + byteArrayOf(0x00, 0x00, 0x01) + expectedEvent
        BattleEffectTargetCodec.encodeToBytes(target) shouldBe bytes
        BattleEffectTargetCodec.decodeBytes(bytes) shouldBe target
      }

      // Sub-event 33 (f/wz2): target in the flag-1 slot, attacker in the flag-2 slot, then the move.
      test("the blown-away sub-event carries the move that did it") {
        val event = BattleActionEvent(entityA = NINETALES_UID, entityB = DITTO_UID, body = BattleEventBody.BlownAway(46))
        val target = BattleEffectTarget(NINETALES_UID, 0, listOf(event))
        val bytes =
            le64(NINETALES_UID) + byteArrayOf(0x00, 0x00, 0x01) + byteArrayOf(0x21, 0x03) + le64(NINETALES_UID) + le64(DITTO_UID) +
                byteArrayOf(0x2E, 0x00)
        BattleEffectTargetCodec.encodeToBytes(target) shouldBe bytes
        BattleEffectTargetCodec.decodeBytes(bytes) shouldBe target
      }
    })
