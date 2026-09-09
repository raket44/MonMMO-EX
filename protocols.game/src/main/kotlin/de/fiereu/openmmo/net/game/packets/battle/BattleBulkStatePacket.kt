package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.*

internal sealed class BattleMessageArgPayload

internal object EmptyArgPayload : BattleMessageArgPayload()

internal data class LongArgPayload(val value: Long) : BattleMessageArgPayload()

internal data class IntArgPayload(val value: Int) : BattleMessageArgPayload()

internal data class StringArgPayload(val value: String) : BattleMessageArgPayload()

internal data class ShortListArgPayload(val values: List<Short>) : BattleMessageArgPayload()

internal data class BattleMessageArg(
    val id: Byte,
    val type: Byte,
    val hasExtra: Boolean,
    val extra: Byte,
    val payload: BattleMessageArgPayload,
)

internal val BattleMessageArgCodec: Codec<BattleMessageArg> =
    object : Codec<BattleMessageArg> {
      override fun read(buf: ReadBuffer): BattleMessageArg {
        val id = S8.read(buf)
        var type = S8.read(buf)
        var hasExtra = false
        var extra: Byte = 0
        if (type.toInt() and 0x80 != 0) {
          type = (type.toInt() and 0x7F).toByte()
          hasExtra = true
          extra = S8.read(buf)
        }
        val t = type.toInt()
        val payload: BattleMessageArgPayload =
            when (t) {
              5 -> StringArgPayload(Utf16LeNullTerminated.read(buf))
              28 -> EmptyArgPayload
              30 -> LongArgPayload(S64LE.read(buf))
              9,
              10,
              17 -> IntArgPayload(S32LE.read(buf))
              18 -> StringArgPayload(Utf16LeNullTerminated.read(buf))
              else -> {
                val n = U8.read(buf)
                ShortListArgPayload((0 until n).map { S16LE.read(buf) })
              }
            }
        return BattleMessageArg(id, type, hasExtra, extra, payload)
      }

      override fun write(buf: WriteBuffer, value: BattleMessageArg) {
        S8.write(buf, value.id)
        val rawType = if (value.hasExtra) (value.type.toInt() or 0x80).toByte() else value.type
        S8.write(buf, rawType)
        if (value.hasExtra) S8.write(buf, value.extra)
        val t = value.type.toInt()
        when (t) {
          5 -> Utf16LeNullTerminated.write(buf, (value.payload as StringArgPayload).value)
          28 -> Unit
          30 -> S64LE.write(buf, (value.payload as LongArgPayload).value)
          9,
          10,
          17 -> S32LE.write(buf, (value.payload as IntArgPayload).value)
          18 -> Utf16LeNullTerminated.write(buf, (value.payload as StringArgPayload).value)
          else -> {
            val values = (value.payload as ShortListArgPayload).values
            U8.write(buf, values.size)
            for (s in values) S16LE.write(buf, s)
          }
        }
      }
    }

sealed class BattleSerializedEntry

internal object NullSerializedEntry : BattleSerializedEntry()

internal data class CreatureDataEntry(val value: Int, val args: List<BattleMessageArg>) :
    BattleSerializedEntry()

internal data class CreatureMovesEntry(val value: Byte, val moves: List<Short>) :
    BattleSerializedEntry()

internal data class MoveAnimationEntry(val a: Byte, val b: Byte, val moveId: Short) :
    BattleSerializedEntry()

internal data class TextCreatureEntry(val a: Byte, val typeId: Byte, val x: Short, val y: Short) :
    BattleSerializedEntry()

internal object EmptyCreatureDataEntry : BattleSerializedEntry()

internal val BattleSerializedEntryCodec: Codec<BattleSerializedEntry> =
    object : Codec<BattleSerializedEntry> {
      override fun read(buf: ReadBuffer): BattleSerializedEntry {
        return when (val tag = S8.read(buf).toInt()) {
          -1 -> NullSerializedEntry
          0 -> {
            val value = S32LE.read(buf)
            val n = U8.read(buf)
            CreatureDataEntry(value, (0 until n).map { BattleMessageArgCodec.read(buf) })
          }

          1 -> {
            val v = S8.read(buf)
            val n = U8.read(buf)
            CreatureMovesEntry(v, (0 until n).map { S16LE.read(buf) })
          }

          2 -> {
            val moveId = S16LE.read(buf)
            val a = S8.read(buf)
            val b = S8.read(buf)
            MoveAnimationEntry(a, b, moveId)
          }

          3 -> {
            val a = S8.read(buf)
            val typeId = S8.read(buf)
            val x = S16LE.read(buf)
            val y = S16LE.read(buf)
            TextCreatureEntry(a, typeId, x, y)
          }

          else -> EmptyCreatureDataEntry
        }
      }

      override fun write(buf: WriteBuffer, value: BattleSerializedEntry) {
        when (value) {
          is NullSerializedEntry -> S8.write(buf, (-1).toByte())
          is CreatureDataEntry -> {
            S8.write(buf, 0)
            S32LE.write(buf, value.value)
            U8.write(buf, value.args.size)
            for (a in value.args) BattleMessageArgCodec.write(buf, a)
          }

          is CreatureMovesEntry -> {
            S8.write(buf, 1)
            S8.write(buf, value.value)
            U8.write(buf, value.moves.size)
            for (m in value.moves) S16LE.write(buf, m)
          }

          is MoveAnimationEntry -> {
            S8.write(buf, 2)
            S16LE.write(buf, value.moveId)
            S8.write(buf, value.a)
            S8.write(buf, value.b)
          }

          is TextCreatureEntry -> {
            S8.write(buf, 3)
            S8.write(buf, value.a)
            S8.write(buf, value.typeId)
            S16LE.write(buf, value.x)
            S16LE.write(buf, value.y)
          }

          is EmptyCreatureDataEntry ->
              throw MalformedPacketException("cannot encode default battle entry")
        }
      }
    }

data class BattleBulkStatePacket(
    val phase: Byte,
    val firstGroup: List<BattleSerializedEntry>,
    val secondGroup: List<BattleSerializedEntry>,
    val prizeMoney: Int,
    val valueB: Int,
    val flag: Byte,
    val thirdGroup: List<BattleSerializedEntry>,
) {
  companion object {
    /**
     * Terminal marker sent when a wild encounter resolves, telling the client to leave the scene.
     * [defeatTextId] rides in the winner-side message group: the client's end-of-battle sequence
     * (f/ju.Aj1 -> YS.hR1) shows each entry in the battle text box before the prize-money line, and
     * its id resolver (f/mk1.zD1) falls through to the GBA ROM-text converter (f/W7.OH) for ids
     * outside the XML string table - so a ROM dialog id renders the trainer's real defeat speech,
     * bytecode-verified.
     */
    fun battleEnd(prizeMoney: Int = 0, defeatTextId: Int? = null, partnerDefeatTextId: Int? = null): BattleBulkStatePacket =
        BattleBulkStatePacket(
            phase = 0,
            // One entry per beaten trainer: a double sighting plays both lines back to back, then
            // the single merged prize.
            firstGroup =
                listOfNotNull(defeatTextId, partnerDefeatTextId).map { CreatureDataEntry(it, emptyList()) }.ifEmpty { listOf(NullSerializedEntry) },
            secondGroup = listOf(NullSerializedEntry),
            prizeMoney = prizeMoney,
            valueB = 0,
            flag = 2,
            thirdGroup = emptyList(),
        )

    /**
     * The wild monster fled (a safari battle): the client's own "The wild {00} fled!" (ROM slot 2,
     * bank 15, line 75 - a bank-line entry, f/n1) in the message group, no "Got away safely".
     */
    fun wildFled(): BattleBulkStatePacket = endWith(listOf(TextCreatureEntry(2, 0, 15, 75)))

    /** Out of Safari Balls after a miss: the PA's ROM line ([textId], a FireRed text offset) ends the fight. */
    fun safariOutOfBalls(textId: Int): BattleBulkStatePacket = endWith(listOf(CreatureDataEntry(textId, emptyList())))

    private fun endWith(lines: List<BattleSerializedEntry>): BattleBulkStatePacket =
        BattleBulkStatePacket(
            phase = 0,
            firstGroup = lines,
            secondGroup = listOf(NullSerializedEntry),
            prizeMoney = 0,
            valueB = 0,
            flag = 2,
            thirdGroup = emptyList(),
        )

    /** Terminal marker sent when the player flees. */
    fun fled(): BattleBulkStatePacket =
        BattleBulkStatePacket(
            phase = -1,
            firstGroup = emptyList(),
            secondGroup = emptyList(),
            prizeMoney = 0,
            valueB = 0,
            flag = 2,
            thirdGroup = emptyList(),
        )
  }
}

object BattleBulkStatePacketCodec : PacketCodec<BattleBulkStatePacket>() {
  override fun CodecScope<BattleBulkStatePacket>.body(): BattleBulkStatePacket {
    val phase = field(S8) { it.phase }
    val firstGroup = field(BattleSerializedEntryCodec.listPrefixed(U8)) { it.firstGroup }
    val secondGroup = field(BattleSerializedEntryCodec.listPrefixed(U8)) { it.secondGroup }
    val prizeMoney = field(S32LE) { it.prizeMoney }
    val valueB = field(S32LE) { it.valueB }
    val flag = field(S8) { it.flag }
    val thirdGroup = field(BattleSerializedEntryCodec.listPrefixed(U8)) { it.thirdGroup }
    return BattleBulkStatePacket(
        phase, firstGroup, secondGroup, prizeMoney, valueB, flag, thirdGroup)
  }
}
