package pirated.net.game.packets

import pirated.bytecodec.CodecScope
import pirated.bytecodec.PacketCodec
import pirated.bytecodec.S16LE
import pirated.bytecodec.S8

data class NpcDialogueChoicePacket(
    val dialogueType: Byte,
    val choiceIndex: Byte,
    val subOption: Short?,
)

object NpcDialogueChoicePacketCodec : PacketCodec<NpcDialogueChoicePacket>() {
  override fun CodecScope<NpcDialogueChoicePacket>.body(): NpcDialogueChoicePacket {
    val dialogueType = field(S8) { it.dialogueType }
    val choiceIndex = field(S8) { it.choiceIndex }
    val subOption =
        if (choiceIndex != 0.toByte() && dialogueType == BRANCHING_DIALOGUE_TYPE)
            field(S16LE) { it.subOption ?: 0 }
        else null
    return NpcDialogueChoicePacket(dialogueType, choiceIndex, subOption)
  }

  private const val BRANCHING_DIALOGUE_TYPE: Byte = 1
}
