package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.Codec
import de.fiereu.bytecodec.ReadBuffer
import de.fiereu.bytecodec.S32LE
import de.fiereu.bytecodec.WriteBuffer

/**
 * Opcode 0x26, the client's inventory-and-party action channel.
 *
 * This was misread as a fixed four-byte "dialog option": the client actually sends a four-byte
 * action head followed by an action-specific body - using a bag item on a party member sends ten
 * more bytes - and the strict trailing-bytes check turned every such click into a disconnect. The
 * body is kept raw here so unknown actions decode cleanly and can be logged instead of killing the
 * session; handlers parse the shapes they understand.
 */
class ContainerActionPacket(val action: Int, val payload: ByteArray) {
  override fun toString(): String =
      "ContainerActionPacket(action=$action, payload=${payload.joinToString(" ") { "%02x".format(it) }})"
}

val ContainerActionPacketCodec: Codec<ContainerActionPacket> =
    object : Codec<ContainerActionPacket> {
      override fun read(buf: ReadBuffer): ContainerActionPacket {
        val action = S32LE.read(buf)
        val payload = ByteArray(buf.remaining())
        buf.readBytes(payload)
        return ContainerActionPacket(action, payload)
      }

      override fun write(buf: WriteBuffer, value: ContainerActionPacket) {
        S32LE.write(buf, value.action)
        buf.writeBytes(value.payload)
      }
    }
