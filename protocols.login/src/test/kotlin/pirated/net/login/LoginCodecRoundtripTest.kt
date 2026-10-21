package pirated.net.login

import pirated.common.test.decodeBytes
import pirated.common.test.encodeToBytes
import pirated.net.login.packets.JoinGameServerPacket
import pirated.net.login.packets.JoinGameServerPacketCodec
import pirated.net.login.packets.MfaResponsePacket
import pirated.net.login.packets.MfaResponsePacketCodec
import pirated.net.login.packets.RequestGameServerListPacket
import pirated.net.login.packets.RequestGameServerListPacketCodec
import pirated.net.login.packets.ToSConfirmationPacket
import pirated.net.login.packets.ToSConfirmationPacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LoginCodecRoundtripTest :
    FunSpec({
      test("RequestGameServerListPacket has no payload") {
        RequestGameServerListPacketCodec.encodeToBytes(RequestGameServerListPacket()).size shouldBe
            0
      }

      test("JoinGameServerPacket roundtrips") {
        val pkt = JoinGameServerPacket(gameServerId = 0x07u)
        val bytes = JoinGameServerPacketCodec.encodeToBytes(pkt)
        bytes.size shouldBe 1
        JoinGameServerPacketCodec.decodeBytes(bytes) shouldBe pkt
      }

      test("ToSConfirmationPacket roundtrip") {
        val pkt = ToSConfirmationPacket(confirmationKey = 1)
        ToSConfirmationPacketCodec.decodeBytes(
            ToSConfirmationPacketCodec.encodeToBytes(pkt)) shouldBe pkt
      }

      test("MfaResponsePacket roundtrips a string") {
        val pkt = MfaResponsePacket(mfaCode = "123456")
        MfaResponsePacketCodec.decodeBytes(MfaResponsePacketCodec.encodeToBytes(pkt)) shouldBe pkt
      }
    })
