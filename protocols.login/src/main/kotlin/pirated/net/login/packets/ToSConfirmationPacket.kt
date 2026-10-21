package pirated.net.login.packets

import pirated.bytecodec.Codec
import pirated.bytecodec.S8
import pirated.bytecodec.imap

data class ToSConfirmationPacket(val confirmationKey: Byte)

val ToSConfirmationPacketCodec: Codec<ToSConfirmationPacket> =
    S8.imap(
        decode = { ToSConfirmationPacket(it) },
        encode = { it.confirmationKey },
    )
