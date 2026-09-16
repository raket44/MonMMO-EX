package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.optional
import de.fiereu.openmmo.common.CharacterInfo
import de.fiereu.openmmo.net.game.codecs.CharacterInfoCodecLong

data class SelectedCharacterPacket(val character: CharacterInfo?)

// The client reads THIS packet with the long variant: f/x15 calls ih6.lQ(true), which takes the
// extra 8-byte field after the login timestamp. Writing the short form shifted everything from
// createdAt onwards, so the trainer card's "Time played" (f/eu6.zz / 3600) read zero however many
// hours the character had actually banked (owner-reported 2026-09-16).
private val OptionalCharacterInfo = CharacterInfoCodecLong.optional()

object SelectedCharacterPacketCodec : PacketCodec<SelectedCharacterPacket>() {
  override fun CodecScope<SelectedCharacterPacket>.body() =
      SelectedCharacterPacket(
          character = field(OptionalCharacterInfo, SelectedCharacterPacket::character),
      )
}
