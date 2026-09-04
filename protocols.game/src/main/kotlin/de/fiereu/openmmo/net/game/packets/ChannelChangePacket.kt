package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.Bool
import de.fiereu.bytecodec.Codec
import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.U8

/**
 * Client -> server 0x12: the Change Channel window. [channel] is zero-based (ch. 4 sends 3) and
 * [preferred] is the "Set Preferred Channel" tick. Observed on the wire as `12 03 01`.
 */
data class ChannelChangePacket(val channel: Int, val preferred: Boolean)

val ChannelChangePacketCodec: Codec<ChannelChangePacket> =
    object : PacketCodec<ChannelChangePacket>() {
      override fun CodecScope<ChannelChangePacket>.body(): ChannelChangePacket {
        val channel = field(U8, ChannelChangePacket::channel)
        val preferred = field(Bool, ChannelChangePacket::preferred)
        return ChannelChangePacket(channel, preferred)
      }
    }
