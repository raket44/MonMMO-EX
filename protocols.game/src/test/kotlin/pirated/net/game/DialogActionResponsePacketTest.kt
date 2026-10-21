package pirated.net.game

import pirated.common.test.decodeBytes
import pirated.common.test.encodeToBytes
import pirated.common.test.fixture
import pirated.net.game.packets.dialog.DialogActionResponsePacket
import pirated.net.game.packets.dialog.DialogActionResponsePacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DialogActionResponsePacketTest :
    FunSpec({
      // The client replies to a 0x21 dialog action with the action id and a response byte.
      test("round-trips the captured dialog reply") {
        val bytes = fixture("game/c2s/21/dialog_reply.bin")
        val packet = DialogActionResponsePacketCodec.decodeBytes(bytes)
        packet shouldBe DialogActionResponsePacket(id = 0x0c, unk = 0)
        DialogActionResponsePacketCodec.encodeToBytes(packet) shouldBe bytes
      }
    })
