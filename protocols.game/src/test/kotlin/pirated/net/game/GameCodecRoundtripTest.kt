package pirated.net.game

import pirated.common.enums.Direction
import pirated.common.test.decodeBytes
import pirated.common.test.encodeToBytes
import pirated.net.game.packets.DialogStatePacket
import pirated.net.game.packets.DialogStatePacketCodec
import pirated.net.game.packets.EntityLeavePacket
import pirated.net.game.packets.EntityLeavePacketCodec
import pirated.net.game.packets.FaceDirectionPacket
import pirated.net.game.packets.FaceDirectionPacketCodec
import pirated.net.game.packets.MovementPacket
import pirated.net.game.packets.MovementPacketCodec
import pirated.net.game.packets.RenderScreenPacket
import pirated.net.game.packets.RenderScreenPacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GameCodecRoundtripTest :
    FunSpec({
      test("RenderScreenPacket roundtrip") {
        RenderScreenPacketCodec.encodeToBytes(RenderScreenPacket(true)) shouldBe byteArrayOf(1)
        RenderScreenPacketCodec.decodeBytes(byteArrayOf(0)) shouldBe RenderScreenPacket(false)
      }

      test("EntityLeavePacket roundtrip") {
        val pkt = EntityLeavePacket(entityId = 0x0102030405060708L)
        EntityLeavePacketCodec.decodeBytes(EntityLeavePacketCodec.encodeToBytes(pkt)) shouldBe pkt
      }

      test("FaceDirectionPacket roundtrip") {
        val pkt = FaceDirectionPacket(direction = Direction.UP)
        FaceDirectionPacketCodec.decodeBytes(FaceDirectionPacketCodec.encodeToBytes(pkt)) shouldBe
            pkt
      }

      test("MovementPacket roundtrip") {
        val pkt = MovementPacket(x = 100, y = 250, direction = Direction.DOWN)
        MovementPacketCodec.decodeBytes(MovementPacketCodec.encodeToBytes(pkt)) shouldBe pkt
      }

      test("MovementPacket decodes the running flag") {
        val pkt = MovementPacket(x = 100, y = 250, direction = Direction.UP, running = true)
        MovementPacketCodec.encodeToBytes(pkt) shouldBe byteArrayOf(100, 0, -6, 0, -127)
        // Decoding also fills stateRaw with the whole byte (0x81 here). The field was added for the
        // unmapped bits 2-6 without updating this expectation, which left the suite red.
        MovementPacketCodec.decodeBytes(byteArrayOf(100, 0, -6, 0, -127)) shouldBe pkt.copy(stateRaw = 0x81)
      }

      test("DialogStatePacket roundtrip") {
        DialogStatePacketCodec.encodeToBytes(DialogStatePacket(true)) shouldBe byteArrayOf(1)
        DialogStatePacketCodec.decodeBytes(byteArrayOf(0)) shouldBe DialogStatePacket(false)
      }
    })
