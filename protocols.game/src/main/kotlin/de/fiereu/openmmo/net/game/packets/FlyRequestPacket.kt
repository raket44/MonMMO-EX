package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8

/**
 * Fly: the destination picked on the town map (c2s 0x10, client f/nb1 - one byte). Sent by the
 * town-map window (f/xr0) with the chosen location's id (f/rG1.Xq): the client's town table
 * (f/FY0) numbers each region's fly spots 0-based in the ROM heal-location order - Kanto 0 =
 * Pallet Town ... 12 = Route 10, 13-19 the Sevii Islands; Hoenn 2 = Petalburg ... Server-side
 * table: monmmo/fly-destinations.csv (FlyService). Opcode 0x10 is LoadMap in the other direction;
 * the server never received a LoadMap from the client, this was what those frames were.
 */
data class FlyRequestPacket(val destination: Byte)

object FlyRequestPacketCodec : PacketCodec<FlyRequestPacket>() {
  override fun CodecScope<FlyRequestPacket>.body(): FlyRequestPacket {
    val destination = field(S8) { it.destination }
    return FlyRequestPacket(destination)
  }
}
