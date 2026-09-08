package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.common.test.fixture
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.battle.BattleFieldStatePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleFormat
import de.fiereu.openmmo.net.game.packets.battle.BattleFieldStatePacketCodec
import de.fiereu.openmmo.net.game.packets.battle.BattleMonBlock
import de.fiereu.openmmo.net.game.packets.battle.BattleOpponentBlock
import de.fiereu.openmmo.net.game.packets.battle.OpposingSide
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private const val WILD = "game/s2c/30/wild_two_party_scrubbed.bin"
private const val TRAINER = "game/s2c/30/trainer_one_opponent_scrubbed.bin"

// The captured player's skin set (bytes 02 4c03 1aac 0f00 0380 01a4 0004): outfit 2, then
// hair, eyes, top, footwear and leggings as type | color << 10.
private val CAPTURED_APPEARANCE =
    SkinSet(
        2,
        listOf(
                Skin(SkinSlot.HAIR, 26u, 43u),
                Skin(SkinSlot.EYES, 15u, 0u),
                Skin(SkinSlot.TOP, 3u, 32u),
                Skin(SkinSlot.FOOTWEAR, 1u, 41u),
                Skin(SkinSlot.LEGGINGS, 0u, 1u))
            .associateBy { it.slot })

class BattleFieldStatePacketTest :
    FunSpec({
      test("round-trips a two-party wild field state byte for byte") {
        val bytes = fixture(WILD)
        val decoded = BattleFieldStatePacketCodec.decodeBytes(bytes)
        decoded.playerName shouldBe "Test"
        decoded.playerParty.size shouldBe 2
        decoded.activeSlot shouldBe 0
        decoded.opposing shouldBe OpposingSide.WILD
        BattleFieldStatePacketCodec.encodeToBytes(decoded) shouldBe bytes
      }

      test("decodes the structured mon fields from the capture") {
        val decoded = BattleFieldStatePacketCodec.decodeBytes(fixture(WILD))

        val snivy = decoded.playerParty[0]
        snivy.slot shouldBe 0
        snivy.species shouldBe 495.toShort()
        snivy.level shouldBe 6.toByte()
        snivy.gender shouldBe 0.toByte()
        snivy.abilityId shouldBe 65.toShort()
        snivy.maxHp shouldBe 22.toShort()
        snivy.currentHp shouldBe 22.toShort()
        snivy.movesPresent shouldBe true
        snivy.moveIds shouldBe listOf<Short>(33, 43, 0, 0)

        val patrat = decoded.playerParty[1]
        patrat.slot shouldBe 1
        patrat.species shouldBe 504.toShort()
        patrat.level shouldBe 2.toByte()
        patrat.gender shouldBe 1.toByte()
        patrat.abilityId shouldBe 50.toShort()
        patrat.moveIds shouldBe listOf<Short>(33, 0, 0, 0)

        val wild = decoded.opponentParty.single()
        wild.revealed shouldBe true
        wild.species shouldBe 504.toShort()
        wild.level shouldBe 2.toByte()
        wild.maxHp shouldBe 14.toShort()
        wild.currentHp shouldBe 14.toShort()
      }

      test("round-trips a trainer field state byte for byte") {
        val bytes = fixture(TRAINER)
        val decoded = BattleFieldStatePacketCodec.decodeBytes(bytes)

        decoded.opposing shouldBe OpposingSide.TRAINER
        decoded.trainerId shouldBe 0x68.toShort()
        decoded.playerParty.size shouldBe 3
        val weedle = decoded.opponentParty.single()
        weedle.revealed shouldBe true
        weedle.species shouldBe 13.toShort()
        weedle.level shouldBe 9.toByte()
        weedle.currentHp shouldBe 26.toShort()

        BattleFieldStatePacketCodec.encodeToBytes(decoded) shouldBe bytes
      }

      test("an unrevealed opponent rides as a stub and round-trips") {
        val decoded = BattleFieldStatePacketCodec.decodeBytes(fixture(TRAINER))
        val benched = BattleOpponentBlock(slot = 1, revealed = false)
        val twoMon = decoded.copy(opponentParty = decoded.opponentParty + benched)

        val reDecoded =
            BattleFieldStatePacketCodec.decodeBytes(
                BattleFieldStatePacketCodec.encodeToBytes(twoMon))

        reDecoded.opponentParty.size shouldBe 2
        reDecoded.opponentParty[1].revealed shouldBe false
        reDecoded shouldBe twoMon
      }

      test("round-trips a synthetic field state for a species without a capture") {
        val rattata =
            BattleMonBlock(
                slot = 0,
                entityId = 0x123C000L,
                species = 19,
                level = 7,
                gender = 1,
                abilityId = 50,
                maxHp = 21,
                currentHp = 17,
                movesPresent = true,
                moveIds = listOf(33, 39, 45, 98),
            )
        val geodude =
            BattleOpponentBlock(
                slot = 0,
                revealed = true,
                entityId = 0x456C000L,
                species = 74,
                level = 9,
                maxHp = 26,
                currentHp = 26,
            )
        val packet =
            BattleFieldStatePacket(
                playerName = "Ash",
                playerId = 0x19000L,
                gender = 0,
                appearance = SkinSet(),
                background = 0,
                opposing = OpposingSide.WILD,
                trainerId = 0,
                playerParty = listOf(rattata),
                playerActive = listOf(0),
                opponentParty = listOf(geodude),
                opponentActive = listOf(0),
            )
        val decoded =
            BattleFieldStatePacketCodec.decodeBytes(
                BattleFieldStatePacketCodec.encodeToBytes(packet))
        decoded shouldBe packet
      }
    })

/**
 * Not an assertion beyond the round trip: writes a doubles and a horde field state under
 * build/samples so the client's own reader (scratch Oracle2 over f/TB0.qP1) can be run on them.
 */
class BattleFieldStateSamplesTest :
    FunSpec({
      fun mon(slot: Int, species: Short, level: Byte) =
          BattleMonBlock(slot, 0x1000L + slot, species, level, 0, 0, 30, 30, false, listOf(0, 0, 0, 0))
      fun foe(slot: Int, species: Short, level: Byte) =
          BattleOpponentBlock(slot, true, 0x2000L + slot, species, level, 0, 20, 20)
      fun write(name: String, packet: BattleFieldStatePacket) {
        val bytes = BattleFieldStatePacketCodec.encodeToBytes(packet)
        BattleFieldStatePacketCodec.decodeBytes(bytes) shouldBe packet
        val dir = java.io.File("build/samples").apply { mkdirs() }
        java.io.File(dir, name).writeBytes(bytes)
      }
      test("doubles and horde field states round-trip and are written as samples") {
        write(
            "field-doubles.bin",
            BattleFieldStatePacket(
                playerName = "Test",
                playerId = 0x19000L,
                gender = 0,
                appearance = CAPTURED_APPEARANCE,
                background = 0,
                opposing = OpposingSide.TRAINER,
                trainerId = 0x68,
                playerParty = listOf(mon(0, 495, 12), mon(1, 504, 11), mon(2, 19, 9)),
                playerActive = listOf(0, 1),
                opponentParty = listOf(foe(0, 13, 9), foe(1, 10, 9), BattleOpponentBlock(2, false)),
                opponentActive = listOf(0, 1),
                format = BattleFormat.DOUBLES,
            ))
        // A player with a hat and a bike variant (mask bit 15 -> a variant byte per slot): RaKeT's
        // stored set on 2026-09-08, for the client-reader oracle.
        write(
            "field-cosmetics.bin",
            BattleFieldStatePacket(
                playerName = "RaKeT",
                playerId = 2327266921807450112L,
                gender = 0,
                appearance =
                    SkinSet(
                        1,
                        listOf(
                                Skin(SkinSlot.HAT, 80u, 0u),
                                Skin(SkinSlot.HAIR, 30u, 45u),
                                Skin(SkinSlot.EYES, 2u, 0u),
                                Skin(SkinSlot.FACIAL_HAIR, 1u, 42u),
                                Skin(SkinSlot.TOP, 5u, 5u),
                                Skin(SkinSlot.FOOTWEAR, 0u, 5u),
                                Skin(SkinSlot.LEGGINGS, 0u, 5u),
                                Skin(SkinSlot.BIKE, 62u, 1u, 1u))
                            .associateBy { it.slot }),
                background = 0,
                opposing = OpposingSide.WILD,
                trainerId = 0,
                playerParty = listOf(mon(0, 66, 23)),
                playerActive = listOf(0),
                opponentParty = listOf(foe(0, 66, 23)),
                opponentActive = listOf(0),
            ))
        write(
            "field-horde.bin",
            BattleFieldStatePacket(
                playerName = "Test",
                playerId = 0x19000L,
                gender = 0,
                appearance = CAPTURED_APPEARANCE,
                background = 0,
                opposing = OpposingSide.WILD,
                trainerId = 0,
                playerParty = listOf(mon(0, 495, 12), mon(1, 504, 11)),
                playerActive = listOf(0),
                opponentParty = listOf(foe(0, 504, 3), foe(1, 504, 4), foe(2, 506, 5)),
                opponentActive = listOf(0, 1, 2, null, null),
                format = BattleFormat.HORDE,
            ))
      }
    })
