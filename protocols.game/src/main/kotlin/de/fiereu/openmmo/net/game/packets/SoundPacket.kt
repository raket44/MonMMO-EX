package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S8

/**
 * s2c 0x00, the client's sound call (`f/k62`, bytecode-verified 2026-09-21).
 *
 * `c00()` dispatches on [mode]: a negative [song] stops everything (`aw3.AI0`), 0 plays music
 * (`aw3.ob1(region, song, false)`), 1 is `aw3.i4(region, song)`, and 2 and 3 are one-shots through
 * the SSEQ loader `vh7.YJ0(song, sequenceRegion, volume, 0f, flag)` - 2 at full volume, 3 at 0.6
 * with the flag set. Modes 2 and 3 are the only ones that read [sequenceRegion].
 *
 * The scripts' own `PlaySound <id>` is mode 2: the id is a sequence in that region's SDAT.
 *
 * CARE: one id the engine cannot load throws inside `aw3.DR1`, which sets `km0.Es = false` and
 * silences every ROM sound for the rest of the session (see the client-sound-engine notes). Send
 * only ids the ROM's own scripts carry.
 */
data class SoundPacket(
    val region: Byte,
    val song: Short,
    val mode: Byte,
    val sequenceRegion: Byte?,
) {
  companion object {
    const val MUSIC = 0.toByte()
    const val MODE_1 = 1.toByte()
    const val EFFECT = 2.toByte()
    const val EFFECT_QUIET = 3.toByte()

    fun effect(region: Int, song: Int) =
        SoundPacket(region.toByte(), song.toShort(), EFFECT, region.toByte())

    fun music(region: Int, song: Int) = SoundPacket(region.toByte(), song.toShort(), MUSIC, null)

    fun stop(region: Int) = SoundPacket(region.toByte(), (-1).toShort(), MUSIC, null)
  }
}

object SoundPacketCodec : PacketCodec<SoundPacket>() {
  override fun CodecScope<SoundPacket>.body(): SoundPacket {
    val region = field(S8) { it.region }
    val song = field(S16LE) { it.song }
    val mode = field(S8) { it.mode }
    val sequenceRegion =
        if (mode.toInt() == 2 || mode.toInt() == 3) field(S8) { it.sequenceRegion ?: it.region }
        else null
    return SoundPacket(region, song, mode, sequenceRegion)
  }
}
