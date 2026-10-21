package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.U8
import pirated.common.enums.Direction

data class MovementPacket(
    val x: Int,
    val y: Int,
    val direction: Direction,
    val running: Boolean = false,
    /** The full state byte; bits 2-6 are unmapped and may carry rail data on Gen 5 rail maps. */
    val stateRaw: Int = 0,
)

object MovementPacketCodec : PacketCodec<MovementPacket>() {
  override fun CodecScope<MovementPacket>.body(): MovementPacket {
    val x = field(S16LE) { it.x.toShort() }
    val y = field(S16LE) { it.y.toShort() }
    val state = field(U8) { it.direction.ordinal or (if (it.running) 0x80 else 0) }
    val direction = Direction.entries[state and 0x03]
    val running = state and 0x80 != 0
    return MovementPacket(x.toInt(), y.toInt(), direction, running, state)
  }
}
