package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

private const val MONEY = 0x1
private const val SAFARI = 0x2
private const val VALUE_4 = 0x4
private const val VALUE_8 = 0x8
private const val VALUE_16 = 0x10
private const val BATTLE_POINTS = 0x20
private const val VALUE_64 = 0x40

/** f/ig7.TE1, the "no lure" enum: its wire byte is -1. Kinds 0, 1, 2 are lure, premium, legendary. */
const val LURE_KIND_NONE = -1
private const val STATUS_CONDITIONS = 0x80
private const val VALUE_256 = 0x100

/**
 * The Safari Game's remaining steps and balls: client f/cd1 bit 2 -> f/ZZ.Iq1 / FJ1, then the battle
 * panel's ball count refreshes (f/h60.vI0). Read as a map location before 2026-09-09.
 */
data class SafariStatus(val steps: Short, val balls: Byte)

data class Value8Group(val a: Byte, val b: Byte, val c: Byte)

data class Value16Group(val a: Short, val b: Short)

/** [a] and [b] are only on the wire when [kind] is not zero. */
data class Value64Group(val kind: Byte, val a: Short?, val b: Short?)

/** A partial update to the player's own character. A null group is left off the wire. */
data class LocalCharacterDeltaPacket(
    val money: Int? = null,
    val safari: SafariStatus? = null,
    val value4: Short? = null,
    val value8: Value8Group? = null,
    val value16: Value16Group? = null,
    /**
     * The Battle Points balance: client f/jc3 bit 0x20 -> f/eu6.pI0, the number the trainer card
     * labels with string 1605. Read as an unnamed int until 2026-09-14.
     */
    val battlePoints: Int? = null,
    val value64: Value64Group? = null,
    val statusConditions: List<Byte>? = null,
    val value256: Byte? = null,
)

private fun LocalCharacterDeltaPacket.mask(): Short {
  var m = 0
  if (money != null) m = m or MONEY
  if (safari != null) m = m or SAFARI
  if (value4 != null) m = m or VALUE_4
  if (value8 != null) m = m or VALUE_8
  if (value16 != null) m = m or VALUE_16
  if (battlePoints != null) m = m or BATTLE_POINTS
  if (value64 != null) m = m or VALUE_64
  if (statusConditions != null) m = m or STATUS_CONDITIONS
  if (value256 != null) m = m or VALUE_256
  return m.toShort()
}

object LocalCharacterDeltaPacketCodec : PacketCodec<LocalCharacterDeltaPacket>() {
  override fun CodecScope<LocalCharacterDeltaPacket>.body(): LocalCharacterDeltaPacket {
    val m = field(S16LE) { it.mask() }.toInt()

    val money = optionalField(m and MONEY != 0, S32LE) { it.money }
    val safari =
        if (m and SAFARI != 0)
            SafariStatus(field(S16LE) { it.safari!!.steps }, field(S8) { it.safari!!.balls })
        else null
    val value4 = optionalField(m and VALUE_4 != 0, S16LE) { it.value4 }
    val value8 =
        if (m and VALUE_8 != 0)
            Value8Group(
                field(S8) { it.value8!!.a },
                field(S8) { it.value8!!.b },
                field(S8) { it.value8!!.c },
            )
        else null
    val value16 =
        if (m and VALUE_16 != 0)
            Value16Group(field(S16LE) { it.value16!!.a }, field(S16LE) { it.value16!!.b })
        else null
    val battlePoints = optionalField(m and BATTLE_POINTS != 0, S32LE) { it.battlePoints }
    val value64 =
        if (m and VALUE_64 != 0) {
          // The lure counter. The client (f/jc3) decodes this byte through f/ig7.sr0 and reads the
          // two shorts only when it is NOT the enum's TE1 - whose byte is -1, NOT 0. Zero is a real
          // lure kind (plain Lure; 1 premium, 2 legendary), so treating 0 as the empty case wrote a
          // short pair fewer than the client reads and desynced every packet after it. Never fired
          // because nothing sent this group until lures existed.
          val kind = field(S8) { it.value64!!.kind }
          if (kind.toInt() != LURE_KIND_NONE)
              Value64Group(
                  kind,
                  field(S16LE) { it.value64!!.a!! },
                  field(S16LE) { it.value64!!.b!! },
              )
          else Value64Group(kind, null, null)
        } else null
    val statusConditions =
        optionalField(m and STATUS_CONDITIONS != 0, S8.listPrefixed(U8)) { it.statusConditions }
    val value256 = optionalField(m and VALUE_256 != 0, S8) { it.value256 }

    return LocalCharacterDeltaPacket(
        money = money,
        safari = safari,
        value4 = value4,
        value8 = value8,
        value16 = value16,
        battlePoints = battlePoints,
        value64 = value64,
        statusConditions = statusConditions,
        value256 = value256,
    )
  }
}
