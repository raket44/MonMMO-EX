package pirated.net.game.packets

import pirated.bytecodec.Bool
import pirated.bytecodec.Codec
import pirated.bytecodec.imap

data class DialogStatePacket(val active: Boolean)

val DialogStatePacketCodec: Codec<DialogStatePacket> =
    Bool.imap(
        decode = { DialogStatePacket(it) },
        encode = { it.active },
    )
