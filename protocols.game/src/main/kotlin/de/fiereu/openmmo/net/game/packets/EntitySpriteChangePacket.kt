package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.net.game.codecs.DefaultSkinSetCodec
import de.fiereu.openmmo.net.game.codecs.SkinSet

/**
 * Applies a skin set to a live entity (s2c 0x90, client f/uz). [staged] routes it: false into
 * f/IL0.v4, the set the renderer draws, true into IL0.JQ1 where nothing reads it. [gender] is
 * the trailing byte, which lands in E41.fZ0 and IL0.an0 (and ZZ.FZ0 on the local player) - it
 * was named "direction" and sent as 0 until 2026-09-08, turning every re-dressed player male.
 */
data class EntitySpriteChangePacket(
    val entityId: Long,
    val staged: Boolean,
    val appearance: SkinSet,
    val gender: Byte,
)

object EntitySpriteChangePacketCodec : PacketCodec<EntitySpriteChangePacket>() {
  override fun CodecScope<EntitySpriteChangePacket>.body(): EntitySpriteChangePacket {
    val entityId = field(S64LE) { it.entityId }
    val staged = field(Bool) { it.staged }
    val appearance = field(DefaultSkinSetCodec) { it.appearance }
    val gender = field(S8) { it.gender }
    return EntitySpriteChangePacket(entityId, staged, appearance, gender)
  }
}
