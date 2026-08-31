package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S64LE
import de.fiereu.bytecodec.S8

/**
 * S2C 0x28 - live update of an entity's transportation byte (client class f.OJ0, handled by
 * f.tS1.D40). D40 looks the entity up and OVERWRITES f.ti.aU1 with this byte, so it works on an
 * entity that already exists - no respawn, no map reload. Bits (f.ti accessors):
 * - bit 0 (fh0): surfing
 * - bit 1 (U7): RIDING - the gate f.F90.aX checks before selecting the mounted frame set; the drawn
 *   bike/mount art is the BIKE skin slot's type (0=Red Bicycle .. 11=Yellow Bicycle,
 *   12/17/28/30/40/51/52/57/62/67 = event mounts, see strings 31000+)
 * - bit 4 (Y3): read by the movement code, pairs with bit 1 in D40's checks
 * - bit 6: checked by D40 before the ride sound - when going from not-riding to riding it plays the
 *   per-region bike bell (sounds 305/365/1013/1151/1014 for regions 0-4)
 */
data class EntityTransportationPacket(
    val entityId: Long,
    val transportation: Byte,
)

object EntityTransportationPacketCodec : PacketCodec<EntityTransportationPacket>() {
  override fun CodecScope<EntityTransportationPacket>.body(): EntityTransportationPacket {
    val entityId = field(S64LE) { it.entityId }
    val transportation = field(S8) { it.transportation }
    return EntityTransportationPacket(entityId, transportation)
  }
}
