package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.common.test.fixture
import de.fiereu.openmmo.common.utils.toHex
import de.fiereu.openmmo.net.game.packets.battle.BattleActionEvent
import de.fiereu.openmmo.net.game.packets.battle.BattleEffectTarget
import de.fiereu.openmmo.net.game.packets.battle.BattleEntityMoveEventPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleEntityMoveEventPacketCodec
import de.fiereu.openmmo.net.game.packets.battle.BattleEventBody
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BattleEntityMoveEventPacketTest :
    FunSpec({
      // A captured Growl, which is a status move whose only effect is one stat drop.
      test("round-trips a captured stat change byte for byte") {
        val bytes = fixture("game/s2c/33/stat_change_growl.bin")
        val decoded = BattleEntityMoveEventPacketCodec.decodeBytes(bytes)

        decoded.sourceMove shouldBe 45.toShort()
        val target = decoded.targets.single()
        target.targetMove shouldBe 0.toShort()
        // Retail (31914 era) left bit 0x80 clear here, so the capture decodes with the animation on.
        target.subEvents.single().body shouldBe
            BattleEventBody.StatChange(stat = 1, stageDelta = -1, animate = true)
        BattleEntityMoveEventPacketCodec.encodeToBytes(decoded).toHex() shouldBe bytes.toHex()
      }

      test("round-trips every body type and entity-flag combination") {
        val packet =
            BattleEntityMoveEventPacket(
                sourceEntity = 0x1122334455667788L,
                sourceMove = 0x0200,
                kind = 1,
                targets =
                    listOf(
                        BattleEffectTarget(
                            entityId = 10L,
                            targetMove = 0x0200,
                            subEvents =
                                listOf(
                                    BattleActionEvent(null, null, BattleEventBody.HpUpdate(14)),
                                    BattleActionEvent(20L, null, BattleEventBody.StatChange(1, -2)),
                                    BattleActionEvent(null, 30L, BattleEventBody.Faint(true)),
                                    BattleActionEvent(
                                        40L, 50L, BattleEventBody.EffectivenessMessage),
                                    BattleActionEvent(null, null, BattleEventBody.MoveFailed(33)),
                                    BattleActionEvent(null, null, BattleEventBody.FieldEffect(1, 446)),
                                    BattleActionEvent(
                                        null, null, BattleEventBody.FieldEffect(0, 191, set = false, animate = false)),
                                    BattleActionEvent(null, null, BattleEventBody.ClientLine(0, 0, romBank = 10, romIndex = 46)))),
                        BattleEffectTarget(
                            entityId = 60L, targetMove = 0, subEvents = emptyList())))

        val bytes = BattleEntityMoveEventPacketCodec.encodeToBytes(packet)
        BattleEntityMoveEventPacketCodec.decodeBytes(bytes) shouldBe packet
      }

      test("the safari bait event rides the signed kind byte -33 with the thrower only for a toss") {
        val packet =
            BattleEntityMoveEventPacket(
                sourceEntity = 1L,
                sourceMove = 0,
                kind = 1,
                targets =
                    listOf(
                        BattleEffectTarget(
                            entityId = 2L,
                            targetMove = 0,
                            subEvents =
                                listOf(
                                    BattleActionEvent(null, null, BattleEventBody.SafariBait(3, "RaKeT")),
                                    BattleActionEvent(null, null, BattleEventBody.SafariBait(0)),
                                    BattleActionEvent(null, null, BattleEventBody.ClientLine(1, 5130))))))

        val bytes = BattleEntityMoveEventPacketCodec.encodeToBytes(packet)
        bytes.toHex() shouldContain "df"
        BattleEntityMoveEventPacketCodec.decodeBytes(bytes) shouldBe packet
      }

      // f/ko1 kind 76 reads shape, form and flag bytes, then the int string id: a fourth byte
      // before the id made the client drop every packet with a raid line (2026-09-14).
      test("the client string line is shape, form 1, flag 1, then the string id") {
        val packet =
            BattleEntityMoveEventPacket(
                sourceEntity = 1L,
                sourceMove = 0,
                kind = 1,
                targets =
                    listOf(
                        BattleEffectTarget(
                            entityId = 2L,
                            targetMove = 0,
                            subEvents = listOf(BattleActionEvent(null, null, BattleEventBody.ClientLine(0, 16790010))))))

        val hex = BattleEntityMoveEventPacketCodec.encodeToBytes(packet).toHex()
        // Kind 76 (0x4c), the event's flags byte (no entities), then shape 00, form 01, flag 01 and
        // 16790010 = 0x010031FA little-endian.
        hex shouldContain "4c00000101fa310001"
        BattleEntityMoveEventPacketCodec.decodeBytes(BattleEntityMoveEventPacketCodec.encodeToBytes(packet)) shouldBe packet
      }
    })
