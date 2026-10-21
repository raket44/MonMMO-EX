package pirated.server.game.handler

import pirated.net.game.packets.AddFriendPacket
import pirated.net.game.packets.BlockPlayerPacket
import pirated.net.game.packets.ChatMessagePacket
import pirated.net.game.packets.CosmeticSlotApplyPacket
import pirated.net.game.packets.DeleteCharacterPacket
import pirated.net.game.packets.JoinPacket
import pirated.net.game.packets.KeepAlivePacket
import pirated.net.game.packets.MovementPacket
import pirated.net.game.packets.RemoveFriendPacket
import pirated.server.game.config.GameServerConfig
import pirated.server.game.di.DaggerGameServerComponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GameAppHandlerTest :
    FunSpec({
      test("dagger graph builds and handler registers expected packet types") {
        val config =
            GameServerConfig(
                host = "127.0.0.1",
                port = 0,
                checksumSize = 2,
                rootKeyResource = "game.private.pem",
                sessionSecret = "test-secret".toByteArray(),
            )
        val component = DaggerGameServerComponent.factory().create(config)
        val handler = component.handlerProvider().get()
        handler.isRegistered(JoinPacket::class) shouldBe true
        handler.isRegistered(MovementPacket::class) shouldBe true
        handler.isRegistered(ChatMessagePacket::class) shouldBe true
        handler.isRegistered(KeepAlivePacket::class) shouldBe true
        handler.isRegistered(AddFriendPacket::class) shouldBe true
        handler.isRegistered(RemoveFriendPacket::class) shouldBe true
        handler.isRegistered(BlockPlayerPacket::class) shouldBe true
        handler.isRegistered(DeleteCharacterPacket::class) shouldBe true
        handler.isRegistered(CosmeticSlotApplyPacket::class) shouldBe true
      }
    })
