package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacketCodec
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnReplyPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnReplyPacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val ENTITY_ID = 0x1ACEADEF2AC8C000L
private const val FURY_SWIPES: Short = 154
private const val ROUND: Short = 496

/** Client 31914's layout for both directions: uid s64, slot s8, move s16 (f/QX and f/com4). */
private fun frame(entityId: Long, slot: Int, move: Short): ByteArray =
    ByteBuffer.allocate(11)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putLong(entityId)
        .put(slot.toByte())
        .putShort(move)
        .array()

class MoveLearnPacketsTest :
    FunSpec({
      test("a prompt that asks which move to forget carries slot -1") {
        val bytes = frame(ENTITY_ID, -1, ROUND)
        val decoded = MoveLearnPromptPacketCodec.decodeBytes(bytes)
        decoded shouldBe MoveLearnPromptPacket(ENTITY_ID, MoveLearnPromptPacket.ASK, ROUND)
        MoveLearnPromptPacketCodec.encodeToBytes(decoded) shouldBe bytes
      }

      test("a prompt for a move learned into a free slot carries that slot") {
        val bytes = frame(ENTITY_ID, 2, FURY_SWIPES)
        val decoded = MoveLearnPromptPacketCodec.decodeBytes(bytes)
        decoded shouldBe MoveLearnPromptPacket(ENTITY_ID, 2, FURY_SWIPES)
        MoveLearnPromptPacketCodec.encodeToBytes(decoded) shouldBe bytes
      }

      test("a reply that swapped the move into slot 1") {
        val bytes = frame(ENTITY_ID, 1, ROUND)
        val decoded = MoveLearnReplyPacketCodec.decodeBytes(bytes)
        decoded shouldBe MoveLearnReplyPacket(ENTITY_ID, 1, ROUND)
        MoveLearnReplyPacketCodec.encodeToBytes(decoded) shouldBe bytes
      }

      test("a reply that declined carries a negative slot") {
        val bytes = frame(ENTITY_ID, -1, ROUND)
        val decoded = MoveLearnReplyPacketCodec.decodeBytes(bytes)
        decoded shouldBe MoveLearnReplyPacket(ENTITY_ID, -1, ROUND)
        MoveLearnReplyPacketCodec.encodeToBytes(decoded) shouldBe bytes
      }
    })
