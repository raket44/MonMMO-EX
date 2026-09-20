package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S8

/**
 * Plays a map door's open or close animation at a tile (s2c 0x1F, client f/wi7 -> f/mm5 case 12).
 * Bytecode-read 2026-09-20 (r32645): the handler's whole body is
 * `world.JB0(kind, (x, y), arg, open = flag == 1, false, false)` - f/gq7.JB0 is the door animation
 * every region's world implements (BW f/wk7: the BUILDING at that position whose model name holds
 * "door" - door_, door_auto, ele_door1, kk_door3, p_door; also "elevator" and "badgegate").
 *
 * This is how a script opens a door for an npc. The client opens doors by itself only for the LOCAL
 * player (f/mx3.K51 returns at once for any other entity), so Cheren and Bianca walked into a shut
 * lab. It was registered here as "OverworldParticleSpawn" with x/y/z names and never sent.
 * [kind] and [arg] are passed straight to JB0; their meaning is not read yet (probe: /probe door).
 */
data class DoorAnimationPacket(
    val kind: Byte,
    /** 1 opens, anything else closes. */
    val open: Short,
    val x: Short,
    val y: Short,
    val arg: Short,
)

object DoorAnimationPacketCodec : PacketCodec<DoorAnimationPacket>() {
  override fun CodecScope<DoorAnimationPacket>.body(): DoorAnimationPacket {
    val kind = field(S8) { it.kind }
    val open = field(S16LE) { it.open }
    val x = field(S16LE) { it.x }
    val y = field(S16LE) { it.y }
    val arg = field(S16LE) { it.arg }
    return DoorAnimationPacket(kind, open, x, y, arg)
  }
}
