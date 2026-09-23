package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.common.test.fixture
import de.fiereu.openmmo.common.utils.toHex
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
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
        patrat.alpha shouldBe false
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

      test("an NPC ally seats the player's side as the human-list composite and round-trips") {
        fun mon(slot: Int, species: Short, owner: Int) =
            BattleMonBlock(slot, 0x1000L + owner * 16 + slot, species, 12, 0, 0, 30, 30, true, listOf(33, 0, 0, 0), owner = owner)
        fun foe(slot: Int, species: Short, owner: Int) =
            BattleOpponentBlock(slot, true, 0x2000L + owner * 16 + slot, species, 11, 0, 20, 20, owner = owner)
        // Cheren (Unova trainer 56) beside the player against the two Plasma grunts.
        val packet =
            BattleFieldStatePacket(
                playerName = "Test",
                playerId = 0x19000L,
                gender = 0,
                appearance = CAPTURED_APPEARANCE,
                background = 0,
                opposing = OpposingSide.TRAINER,
                trainerId = 62,
                trainerRegion = 2,
                playerParty = listOf(mon(0, 495, 0), mon(1, 504, 0), mon(0, 501, 1), mon(1, 506, 1)),
                playerActive = listOf(0, 2),
                opponentParty = listOf(foe(0, 509, 0), foe(0, 506, 1)),
                opponentActive = listOf(0, 1),
                format = BattleFormat.DOUBLES,
                partnerTrainerId = 63,
                allyTrainerId = 56,
                allyRegion = 2,
            )
        val bytes = BattleFieldStatePacketCodec.encodeToBytes(packet)
        val hex = bytes.toHex()
        // After the opposing byte and the two fixed bytes: kind 4, sub 6, two entries, entry 0 =
        // (key 0, position 0, discard) + the player's kind 0 / sub 6 descriptor.
        hex shouldContain "040602000000" + "0006"
        // Entry 1 = (key 1, position 1, discard) + the NPC trainer's kind 2 / sub 6 descriptor:
        // region 2, trainer id 56, one zero byte; then the side's five tail bytes and the record
        // group (count 1, four records).
        hex shouldContain "010100" + "0206" + "02" + "3800" + "00" + "0000000000" + "01" + "04"
        // The ally's records open with owner key 1 and its own slot 0.
        hex shouldContain "01" + "00" + "01" + "1010000000000000"
        val decoded = BattleFieldStatePacketCodec.decodeBytes(bytes)
        decoded shouldBe packet
        // The ally's monster on position 1 resolves back to its index in the party list.
        decoded.playerActive shouldBe listOf(0, 2)
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

// r32645 reads the rarity flags twice: the active detail's short (f/at0.VQ1 -> f/b54.c50, where bit
// 0x4 sets E70 = alpha and f/lq1.rj tints the sprite) and the full block's short after the hp pair
// (f/at0.ci -> f/qi0.mP -> f/dl6.Lq0). The alpha mark used to ride in the gender byte, so no battle
// sprite ever glowed (2026-09-16).
class BattleBlockRarityTest :
    FunSpec({
      test("a shiny alpha rides its rarity bits in the active detail and the party block") {
        val decoded = BattleFieldStatePacketCodec.decodeBytes(fixture(WILD))
        val alphaLead = decoded.playerParty[0].copy(shiny = true, alpha = true, gender = 1)
        val wildAlpha = decoded.opponentParty.single().copy(alpha = true, secret = true)
        val packet = decoded.copy(playerParty = listOf(alphaLead, decoded.playerParty[1]), opponentParty = listOf(wildAlpha))
        val hex = BattleFieldStatePacketCodec.encodeToBytes(packet).toHex()
        val plain = BattleFieldStatePacketCodec.encodeToBytes(decoded).toHex()
        // Player block: gender byte 01, form 00, SK 0000, hp 1600 1600, status 00, flags 0500 (shiny|alpha), ff 03.
        hex shouldContain "0100000016001600" + "000500ff03"
        // Active detail: species ef01, level 06, empty name, flags 0500, gender 01, form 00, then the tail.
        hex shouldContain "ef01060000050001" + "0003ff"
        // Wild block flags 0c00 (alpha|secret) sit in the same tail slot.
        hex shouldContain "0e000e00000c00ff03"
        plain shouldNotContain "0500ff03"

        val back = BattleFieldStatePacketCodec.decodeBytes(BattleFieldStatePacketCodec.encodeToBytes(packet))
        back.playerParty[0].shiny shouldBe true
        back.playerParty[0].alpha shouldBe true
        back.playerParty[0].gender shouldBe 1.toByte()
        back.opponentParty.single().alpha shouldBe true
        back.opponentParty.single().secret shouldBe true
        back.opponentParty.single().shiny shouldBe false
      }
    })
