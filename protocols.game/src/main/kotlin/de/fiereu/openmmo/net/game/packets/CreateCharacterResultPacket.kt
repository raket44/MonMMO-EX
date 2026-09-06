package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S8

/**
 * The answer to a character creation (s2c 0x03, client f/SU1). Bytecode-verified: one byte
 * result code mapped through f/yl to a client string - 0 accepted (the record follows), 1 account
 * full (2100), 3 name already exists or cannot be used (2102), 4 system error (2103), 5 invalid
 * gender (2104), 6 invalid colour (2105), 7 invalid rival name (2106), 8 invalid appearance
 * (2107). On any non-zero code the creation window re-enables its buttons and shows the string.
 * The server only ever sends refusals here; an accepted character arrives as the refreshed list.
 */
data class CreateCharacterResultPacket(val code: Byte) {
  init {
    require(code != ACCEPTED) { "An accepted character is delivered as the character list" }
  }

  companion object {
    const val ACCEPTED: Byte = 0
    const val ACCOUNT_FULL: Byte = 1
    const val NAME_UNAVAILABLE: Byte = 3
    const val SYSTEM_ERROR: Byte = 4
    const val INVALID_GENDER: Byte = 5
  }
}

object CreateCharacterResultPacketCodec : PacketCodec<CreateCharacterResultPacket>() {
  override fun CodecScope<CreateCharacterResultPacket>.body(): CreateCharacterResultPacket =
      CreateCharacterResultPacket(field(S8) { it.code })
}
