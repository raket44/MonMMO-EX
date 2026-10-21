package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

/**
 * Opcode 0x2A (s2c). Sets one region-specific story flag OR var on the client - its store is a
 * single short->short map per region (a flag is a var whose value is nonzero), so the same packet
 * carries both. The client accepts only whitelisted ids (see ClientStoryWhitelist); region 0x80
 * addresses its unvalidated global store.
 */
data class StoryFlagUpdatePacket(
    val regionId: Byte,
    val flagId: Int,
    /** The s16 wire value. 0 clears; for plain flags use 1. */
    val value: Int,
) {
  constructor(
      regionId: Byte,
      flagId: Int,
      enabled: Boolean,
  ) : this(regionId, flagId, if (enabled) 1 else 0)

  val enabled: Boolean
    get() = value != 0
}

object StoryFlagUpdatePacketCodec : PacketCodec<StoryFlagUpdatePacket>() {
  override fun CodecScope<StoryFlagUpdatePacket>.body(): StoryFlagUpdatePacket {
    val regionId = field(S8) { it.regionId }
    val flagId = field(S16LE) { it.flagId.toShort() }.toInt() and 0xffff
    val value = field(S16LE) { it.value.toShort() }.toInt()
    return StoryFlagUpdatePacket(regionId, flagId, value)
  }
}
