package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.Utf16LeNullTerminated

data class StringCommandPacket(val command: String) {
  override fun toString(): String =
      "StringCommandPacket(command='$command' units=" + command.map { "%04x".format(it.code) }.joinToString(" ") + ")"
}

object StringCommandPacketCodec : PacketCodec<StringCommandPacket>() {
  override fun CodecScope<StringCommandPacket>.body(): StringCommandPacket {
    val command = field(Utf16LeNullTerminated) { it.command }
    return StringCommandPacket(command)
  }
}
