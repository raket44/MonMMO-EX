package pirated.net.login.packets

import pirated.bytecodec.Codec
import pirated.bytecodec.Utf16LeNullTerminated
import pirated.bytecodec.imap

data class MfaResponsePacket(val mfaCode: String)

val MfaResponsePacketCodec: Codec<MfaResponsePacket> =
    Utf16LeNullTerminated.imap(
        decode = { MfaResponsePacket(it) },
        encode = { it.mfaCode },
    )
