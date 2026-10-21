package pirated.net.login

import pirated.network.Protocol
import pirated.network.c2s
import pirated.network.s2c
import pirated.net.login.packets.ExistingSessionPacket
import pirated.net.login.packets.ExistingSessionPacketCodec
import pirated.net.login.packets.GameServerListPacket
import pirated.net.login.packets.GameServerListPacketCodec
import pirated.net.login.packets.GameServerNodesPacket
import pirated.net.login.packets.GameServerNodesPacketCodec
import pirated.net.login.packets.JoinGameServerPacket
import pirated.net.login.packets.JoinGameServerPacketCodec
import pirated.net.login.packets.LoginKickPacket
import pirated.net.login.packets.LoginKickPacketCodec
import pirated.net.login.packets.LoginRequestPacket
import pirated.net.login.packets.LoginRequestPacketCodec
import pirated.net.login.packets.LoginResponsePacket
import pirated.net.login.packets.LoginResponsePacketCodec
import pirated.net.login.packets.MfaChallengePacket
import pirated.net.login.packets.MfaChallengePacketCodec
import pirated.net.login.packets.MfaResponsePacket
import pirated.net.login.packets.MfaResponsePacketCodec
import pirated.net.login.packets.RequestGameServerListPacket
import pirated.net.login.packets.RequestGameServerListPacketCodec
import pirated.net.login.packets.SentCredentialsPacket
import pirated.net.login.packets.SentCredentialsPacketCodec
import pirated.net.login.packets.ToSConfirmationPacket
import pirated.net.login.packets.ToSConfirmationPacketCodec
import pirated.net.login.packets.ToSPacket
import pirated.net.login.packets.ToSPacketCodec

object LoginProtocol : Protocol() {
  init {
    s2c<LoginResponsePacket>(0x01u, LoginResponsePacketCodec)
    c2s<RequestGameServerListPacket>(0x02u, RequestGameServerListPacketCodec)
    c2s<JoinGameServerPacket>(0x03u, JoinGameServerPacketCodec)
    s2c<GameServerNodesPacket>(0x03u, GameServerNodesPacketCodec)
    c2s<ToSConfirmationPacket>(0x04u, ToSConfirmationPacketCodec)
    s2c<LoginKickPacket>(0x05u, LoginKickPacketCodec)
    s2c<SentCredentialsPacket>(0x07u, SentCredentialsPacketCodec)
    s2c<MfaChallengePacket>(0x08u, MfaChallengePacketCodec)
    c2s<MfaResponsePacket>(0x08u, MfaResponsePacketCodec)
    c2s<LoginRequestPacket>(0x11u, LoginRequestPacketCodec)
    s2c<ToSPacket>(0x14u, ToSPacketCodec)
    s2c<GameServerListPacket>(0x22u, GameServerListPacketCodec)
    s2c<ExistingSessionPacket>(0x26u, ExistingSessionPacketCodec)
  }
}
