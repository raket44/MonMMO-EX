package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.utils.hexToBytes

/** Who the player is fighting. The byte rides twice, before the name and again after the id. */
enum class OpposingSide(val wireValue: Byte) {
  WILD(0xFF.toByte()),
  TRAINER(0x04);

  companion object {
    fun byWireValue(value: Byte): OpposingSide? = entries.find { it.wireValue == value }
  }
}

/**
 * The battle format, the fourth header byte (client `f/oD1.zS`). It sizes each side's array of
 * field positions: the player's side gets [playerSlots] cells, the opposing side [opponentSlots]
 * (client `f/TB0.qP1`: `Ak[side] = new QL1[side > 0 ? CN0 : uK1]`). A horde always has five
 * cells; a three-horde leaves two of them empty.
 */
enum class BattleFormat(val wireValue: Byte, val playerSlots: Int, val opponentSlots: Int) {
  SINGLES(0, 1, 1),
  DOUBLES(1, 2, 2),
  HORDE(6, 1, 5);

  companion object {
    fun byWireValue(value: Byte): BattleFormat? = entries.find { it.wireValue == value }
  }
}

/**
 * The field state that opens a battle (opcode 0x30). Each side sends its monsters as a group of
 * records (client `f/kw0.ns0`), then one entry per field position: a filled position carries the
 * monster's active detail (client `f/kw0.NQ1`), an empty one a single zero byte.
 *
 * A trainer battle differs from a wild one in more than values: [opposing] flips two bytes, and the
 * opposing side carries [trainerId] plus two extra halfwords that a wild battle leaves out.
 */
data class BattleFieldStatePacket(
    val playerName: String,
    val playerId: Long,
    /** The player's own overworld appearance, the same bytes their 0x05 spawn carries. */
    val playerAppearance: ByteArray,
    /** Picks the battle backdrop. Outdoors is 0, forest 9, caves 12. */
    val background: Byte,
    val opposing: OpposingSide,
    /** The decomp trainer id, which the client draws the name and sprite from. Zero on a wild. */
    val trainerId: Short,
    /** The ROM region the trainer id indexes: 0 Kanto, 1 Hoenn, 2 Unova, 3 Sinnoh, 4 Johto. */
    val trainerRegion: Byte = 0,
    val playerParty: List<BattleMonBlock>,
    /** The player's field positions: the party slot standing on each, null for an empty one. */
    val playerActive: List<Int?>,
    val opponentParty: List<BattleOpponentBlock>,
    /** The opposing field positions: the opponent list index on each, null for an empty one. */
    val opponentActive: List<Int?>,
    val format: BattleFormat = BattleFormat.SINGLES,
    /** A second trainer on the opposing side (double sighting): the header names both. */
    val partnerTrainerId: Short? = null,
) {
  init {
    require(playerAppearance.size == APPEARANCE_SIZE) {
      "A field state carries exactly $APPEARANCE_SIZE appearance bytes"
    }
    require(playerActive.size == format.playerSlots) {
      "$format has ${format.playerSlots} player positions, got ${playerActive.size}"
    }
    require(opponentActive.size == format.opponentSlots) {
      "$format has ${format.opponentSlots} opposing positions, got ${opponentActive.size}"
    }
  }

  /** The party slot on the player's first filled position (the only one in singles). */
  val activeSlot: Int
    get() = playerActive.firstNotNullOf { it }

  /** The opponent index on the first filled opposing position. */
  val opponentActiveSlot: Int
    get() = opponentActive.firstNotNullOf { it }

  // ByteArray breaks the generated equals, and the fixtures compare whole packets.
  override fun equals(other: Any?): Boolean =
      this === other ||
          (other is BattleFieldStatePacket &&
              playerName == other.playerName &&
              playerId == other.playerId &&
              playerAppearance.contentEquals(other.playerAppearance) &&
              background == other.background &&
              opposing == other.opposing &&
              trainerId == other.trainerId &&
              trainerRegion == other.trainerRegion &&
              playerParty == other.playerParty &&
              playerActive == other.playerActive &&
              opponentParty == other.opponentParty &&
              opponentActive == other.opponentActive &&
              format == other.format)

  override fun hashCode(): Int =
      listOf<Any>(
              playerName,
              playerId,
              playerAppearance.contentHashCode(),
              background,
              opposing,
              trainerId,
              trainerRegion,
              playerParty,
              playerActive,
              opponentParty,
              opponentActive,
              format)
          .hashCode()

  companion object {
    const val APPEARANCE_SIZE = 14
  }
}

// Two sides, then two bytes the client skips; the format byte follows, then the client's `my`
// enum byte (0).
private val HEAD_SIDES = "020000".hexToBytes()
private val AFTER_BACKGROUND = "00000000000000ff000000000016000000".hexToBytes()
private val AFTER_OPPOSING = "00200006".hexToBytes()

object BattleFieldStatePacketCodec : PacketCodec<BattleFieldStatePacket>() {
  override fun CodecScope<BattleFieldStatePacket>.body(): BattleFieldStatePacket {
    constant(HEAD_SIDES)
    val formatByte = field(S8) { it.format.wireValue }
    val format =
        BattleFormat.byWireValue(formatByte)
            ?: throw MalformedPacketException("Unknown battle format $formatByte")
    constant(0)
    val background = field(S8) { it.background }
    constant(AFTER_BACKGROUND)
    val opposingByte = field(S8) { it.opposing.wireValue }
    val opposing =
        OpposingSide.byWireValue(opposingByte)
            ?: throw MalformedPacketException("Unknown opposing side $opposingByte")
    constant(AFTER_OPPOSING)
    val playerName = field(Utf16LeNullTerminated) { it.playerName }
    reserved(0)
    val playerId = field(S64LE) { it.playerId }
    field(S8) { it.opposing.wireValue }
    val appearance =
        field(fixedBytes(BattleFieldStatePacket.APPEARANCE_SIZE)) { it.playerAppearance }
    padding(5)

    // One group of monster records, then the field positions.
    constant(1)
    val partyCount = field(U8) { it.playerParty.size }
    val party =
        List(partyCount) { i ->
          // Each record opens with a zero byte before the party slot.
          constant(0)
          field(BattleFullBlockCodec) { it.playerParty[i] }
        }
    val playerActive =
        List(format.playerSlots) { position ->
          val kind = field(S8) { if (it.playerActive[position] != null) 1.toByte() else 0.toByte() }
          if (kind.toInt() == 1) {
            field(BattleActiveDetailCodec) {
                  val slot = it.playerActive[position]!!
                  BattleActiveDetail.of(position, slot, it.playerParty[slot])
                }
                .slot
          } else null
        }

    // Opens the opposing side. A trainer adds its id and two more halfwords here. Two trainers
    // (double sighting) use the client's composite descriptor instead (f/TB0.l70 kind 4): a
    // count, then per entry two bytes (slot count, first position) and a nested kind-2 trainer
    // descriptor - reader-verified layout, entry bytes inferred.
    val kindByte = field(S8) { if (it.partnerTrainerId != null) 4.toByte() else if (it.opposing == OpposingSide.TRAINER) 2.toByte() else 1.toByte() }
    constant(6)
    val partnerTrainerId: Short?
    val trainerRegion: Byte
    val trainerId: Short
    if (kindByte.toInt() == 4) {
      constant(2)
      constant(1)
      constant(0)
      constant(2)
      constant(6)
      trainerRegion = field(S8) { it.trainerRegion }
      trainerId = field(S16LE) { it.trainerId }
      constant(0)
      constant(1)
      constant(1)
      constant(2)
      constant(6)
      field(S8) { it.trainerRegion }
      partnerTrainerId = field(S16LE) { it.partnerTrainerId!! }
      constant(0)
      padding(3)
      padding(2)
    } else {
      // Descriptor sub-type 6, then the region byte the client keys its trainer table with.
      trainerRegion = field(S8) { it.trainerRegion }
      trainerId = if (opposing == OpposingSide.TRAINER) field(S16LE) { it.trainerId } else 0.toShort()
      partnerTrainerId = null
      padding(4)
      if (opposing == OpposingSide.TRAINER) padding(2)
    }

    constant(1)
    val opponentCount = field(U8) { it.opponentParty.size }
    val opponents =
        List(opponentCount) { i ->
          constant(0)
          field(BattleOpponentBlockCodec) { it.opponentParty[i] }
        }
    val opponentActive =
        List(format.opponentSlots) { position ->
          val kind = field(S8) { if (it.opponentActive[position] != null) 1.toByte() else 0.toByte() }
          if (kind.toInt() == 1) {
            field(BattleActiveDetailCodec) {
                  val index = it.opponentActive[position]!!
                  val mon = it.opponentParty[index]
                  BattleActiveDetail(position, index, mon.species, mon.level, mon.gender)
                }
                .slot
          } else null
        }
    padding(4)

    return BattleFieldStatePacket(
        playerName,
        playerId,
        appearance,
        background,
        opposing,
        trainerId,
        trainerRegion,
        party,
        playerActive,
        opponents,
        opponentActive,
        format,
        partnerTrainerId = partnerTrainerId,
    )
  }
}
