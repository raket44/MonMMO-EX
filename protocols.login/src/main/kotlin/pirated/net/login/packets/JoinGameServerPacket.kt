package pirated.net.login.packets

import pirated.bytecodec.Codec
import pirated.bytecodec.U8
import pirated.bytecodec.imap

data class JoinGameServerPacket(val gameServerId: UByte)

val JoinGameServerPacketCodec: Codec<JoinGameServerPacket> =
    U8.imap(
        decode = { JoinGameServerPacket(it.toUByte()) },
        encode = { it.gameServerId.toInt() },
    )
