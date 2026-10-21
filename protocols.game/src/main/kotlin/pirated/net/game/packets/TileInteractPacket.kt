package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec

/**
 * Sent when the player presses the action button on the tile they face. It has no fields, the
 * server uses the player position and facing. Signs and furniture come through here, npcs use an
 * entity interaction instead.
 */
class TileInteractPacket

object TileInteractPacketCodec : PacketCodec<TileInteractPacket>() {
  override fun CodecScope<TileInteractPacket>.body(): TileInteractPacket {
    return TileInteractPacket()
  }
}
