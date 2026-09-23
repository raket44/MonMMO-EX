package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.utils.hexToBytes
import de.fiereu.openmmo.net.game.codecs.DefaultSkinSetCodec
import de.fiereu.openmmo.net.game.codecs.SkinSet

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
  /** Three a side (client f/t7 ordinal 5); the Crystal Onix raid is fought in it. */
  TRIPLES(5, 3, 3),
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
    /**
     * The byte after the name lands in the client's f/IL0.an0 - its own debug dump labels it
     * "gender=" (f/LA0) and the battle scene indexes the trainer back-sprite table with it
     * (f/Ad0 -> f/F90.ZV0(kind, outfit, gender)). 0 male, 1 female.
     */
    val gender: Byte,
    /**
     * The player's skin set (f/tK0.yF0: outfit byte, slot mask, packed slots), the same block
     * the 0x05 spawn carries. Until 2026-09-08 this was a fixed capture of one character, so
     * everyone fought in the captured player's clothes.
     */
    val appearance: SkinSet,
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
    /** The client's mode byte (f/my): 0 a normal battle, 1 the Safari Game (Ball / Bait / Rock panel). */
    val mode: Byte = 0,
    /**
     * An NPC trainer fighting BESIDE the player (Cheren in Unova, Steven at Mossdeep): the player's
     * side goes out as the client's human-list composite (f/bo4 wire 4, class f/s34) with two
     * entries, the player at key 0 and the ally at key 1. The ally's records in [playerParty]
     * carry owner 1 and their slot within the ally's team. Null: the plain single-player side.
     */
    val allyTrainerId: Short? = null,
    /** The ROM region the ally's trainer id indexes (same numbering as [trainerRegion]). */
    val allyRegion: Byte = 0,
) {
  init {
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
}

// Two sides, then two bytes the client skips; the format byte follows, then the client's `my`
// enum byte (0).
private val HEAD_SIDES = "020000".hexToBytes()
private val AFTER_BACKGROUND = "00000000000000ff000000000016000000".hexToBytes()
private val AFTER_OPPOSING = "0020".hexToBytes()

// Client f/f8.TL1 (r32645): a side opens with its kind byte (f/bo4 wire value) and, for every kind
// but 100, a sub byte the client sizes the side's record array with (6 party slots).
private const val KIND_PLAYER = 0
private const val KIND_TRAINER = 2
/** The composite list of human entries (f/bo4 ordinal 4: S01 "human" set, marks every entry dj). */
private const val KIND_HUMAN_LIST = 4
private const val PARTY_SLOTS = 6

object BattleFieldStatePacketCodec : PacketCodec<BattleFieldStatePacket>() {
  override fun CodecScope<BattleFieldStatePacket>.body(): BattleFieldStatePacket {
    constant(HEAD_SIDES)
    val formatByte = field(S8) { it.format.wireValue }
    val format =
        BattleFormat.byWireValue(formatByte)
            ?: throw MalformedPacketException("Unknown battle format $formatByte")
    val mode = field(S8) { it.mode }
    val background = field(S8) { it.background }
    constant(AFTER_BACKGROUND)
    val opposingByte = field(S8) { it.opposing.wireValue }
    val opposing =
        OpposingSide.byWireValue(opposingByte)
            ?: throw MalformedPacketException("Unknown opposing side $opposingByte")
    constant(AFTER_OPPOSING)
    // The player's side. Alone it is the plain player descriptor (kind 0). With an NPC ally it is
    // the client's human-list composite (kind 4, f/s34): an entry count, then per entry the owner
    // key, the field position the entry's monster stands on (f/db1.xX0: the client resolves an
    // action's position to its owner with it, f/s34.tq) and one byte the reader discards, followed
    // by the entry's own descriptor with its own kind and sub byte. The header's third byte (the
    // local key, f/ua5.kp1) is 0, so the player must be entry 0. Reader-verified layout
    // (f/f8.TL1 pswitch_33, 2026-09-22).
    val playerKind = field(S8) { (if (it.allyTrainerId != null) KIND_HUMAN_LIST else KIND_PLAYER).toByte() }.toInt()
    constant(PARTY_SLOTS)
    val allyTrainerId: Short?
    val allyRegion: Byte
    if (playerKind == KIND_HUMAN_LIST) {
      constant(2)
      constant(0)
      constant(0)
      constant(0)
      constant(KIND_PLAYER)
      constant(PARTY_SLOTS)
    }
    val playerName = field(Utf16LeNullTerminated) { it.playerName }
    val gender = field(S8) { it.gender }
    val playerId = field(S64LE) { it.playerId }
    field(S8) { it.opposing.wireValue }
    // Player descriptor (f/TB0.l70 kind 1): a 1 here announces a rating block (enum byte, short,
    // byte) before the skin set; nothing sent yet.
    constant(0)
    val appearance = field(DefaultSkinSetCodec) { it.appearance }
    if (playerKind == KIND_HUMAN_LIST) {
      // Entry 1: the ally, an NPC trainer descriptor (kind 2, f/rb4: region, trainer id, one byte).
      constant(1)
      constant(1)
      constant(0)
      constant(KIND_TRAINER)
      constant(PARTY_SLOTS)
      allyRegion = field(S8) { it.allyRegion }
      allyTrainerId = field(S16LE) { it.allyTrainerId!! }
      constant(0)
    } else {
      allyRegion = 0
      allyTrainerId = null
    }
    // The side's tail (f/f8.Rl0 after TL1): a zero marker for the optional block, then a zero int
    // of side flags.
    padding(5)

    // One group of monster records, then the field positions.
    constant(1)
    val partyCount = field(U8) { it.playerParty.size }
    val party =
        List(partyCount) { i ->
          // Each record opens with its owner key (f/at0.ci: side.Na1(key).jm1()[slot]): zero for
          // the player, 1 for the ally's monsters.
          val owner = field(S8) { it.playerParty[i].owner.toByte() }.toInt()
          field(BattleFullBlockCodec) { it.playerParty[i] }.copy(owner = owner)
        }
    val playerActive =
        List(format.playerSlots) { position ->
          val kind = field(S8) { if (it.playerActive[position] != null) 1.toByte() else 0.toByte() }
          if (kind.toInt() == 1) {
            val detail =
                field(BattleActiveDetailCodec) {
                  val index = it.playerActive[position]!!
                  val mon = it.playerParty[index]
                  // With an ally the client reads (owner key, slot within owner); alone it reads
                  // the position, which its single-entry side ignores (f/ni5.Na1 returns itself).
                  BattleActiveDetail.of(position, mon.slot, mon, owner = if (it.allyTrainerId != null) mon.owner else null)
                }
            // Back to the index into the party list: the ally's records sit after the player's.
            val owner = if (allyTrainerId != null) detail.position else 0
            party.indexOfFirst { it.owner == owner && it.slot == detail.slot }.takeIf { it >= 0 } ?: detail.slot
          } else null
        }

    // Opens the opposing side. A trainer adds its id and two more halfwords here. Two trainers
    // (double sighting) use the client's composite descriptor instead (f/TB0.l70 kind 4): a
    // count, then per entry two bytes (slot count, first position) and a nested kind-2 trainer
    // descriptor - reader-verified layout, entry bytes inferred.
    // Kind 5 is the NPC two-trainer list (f/pu1 code 5, flagged like a trainer); kind 4 is the same
    // list for human opponents and dresses the battle as a match (timer, turn counter).
    val kindByte = field(S8) { if (it.partnerTrainerId != null) 5.toByte() else if (it.opposing == OpposingSide.TRAINER) 2.toByte() else 1.toByte() }
    constant(6)
    val partnerTrainerId: Short?
    val trainerRegion: Byte
    val trainerId: Short
    if (kindByte.toInt() == 5 || kindByte.toInt() == 4) {
      // count; per entry: two bytes (slot count, first position), one byte the reader discards,
      // then the nested descriptor which reads its own kind (2) and sub (6).
      // Entry bytes (f/E71): key (the map key records refer to), first position, one discarded.
      constant(2)
      constant(0)
      constant(0)
      constant(0)
      constant(2)
      constant(6)
      trainerRegion = field(S8) { it.trainerRegion }
      trainerId = field(S16LE) { it.trainerId }
      constant(0)
      constant(1)
      constant(1)
      constant(0)
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
          // Record head: the owner key (kw0.ns0: side.qg1(key).VZ()[slot]); zero for one trainer.
          val owner = field(S8) { it.opponentParty[i].owner.toByte() }.toInt()
          field(BattleOpponentBlockCodec) { it.opponentParty[i] }.copy(owner = owner)
        }
    val opponentActive =
        List(format.opponentSlots) { position ->
          val kind = field(S8) { if (it.opponentActive[position] != null) 1.toByte() else 0.toByte() }
          if (kind.toInt() == 1) {
            val detail =
                field(BattleActiveDetailCodec) {
                  val index = it.opponentActive[position]!!
                  val mon = it.opponentParty[index]
                  BattleActiveDetail.of(position, mon, owner = if (it.partnerTrainerId != null) mon.owner else null)
                }
            val owner = if (partnerTrainerId != null) detail.position else 0
            opponents.indexOfFirst { it.owner == owner && it.slot == detail.slot }.takeIf { it >= 0 } ?: detail.slot
          } else null
        }
    padding(4)

    return BattleFieldStatePacket(
        playerName,
        playerId,
        gender,
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
        mode = mode,
        allyTrainerId = allyTrainerId,
        allyRegion = allyRegion,
    )
  }
}
