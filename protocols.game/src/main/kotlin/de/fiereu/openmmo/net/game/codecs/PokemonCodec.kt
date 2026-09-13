package de.fiereu.openmmo.net.game.codecs

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.canonicalSpeciesId
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.*
import de.fiereu.openmmo.common.utils.hexToBytes
import de.fiereu.openmmo.net.game.monsterRecordHasLongTrailer

private fun reserved(hex: String): Codec<Unit> =
    object : Codec<Unit> {
      private val bytes = hex.hexToBytes()

      override fun read(buf: ReadBuffer) {
        if (bytes.isNotEmpty()) buf.readBytes(ByteArray(bytes.size))
      }

      override fun write(buf: WriteBuffer, value: Unit) {
        buf.writeBytes(bytes)
      }
    }

// Four shorts the client reads into k91.CQ (f/tK0.wG, bytecode-walked against the captures),
// purpose unknown - q00(i)/vm1() expose them, always zero in both captures.
private const val CQ_SHORTS = "0000000000000000"
// Bytes between the EVs and the IV word, client fields Vx/Ik/TJ/LC/js + 1 discarded byte +
// wY0/pub/YJ1 + the cT0 int + dg1/Jw1 (wG's read order). Semantics unknown; the capture values
// are written back for every monster. NOT constant across captures: monster_record_32710.bin has
// 580500 where this writes 040502, so wY0/pub/YJ1 are per-monster fields still modelled as fixed
// bytes - same width either way, so a wrong value cannot desync the reader.
private const val TAIL_A_REST = "000000000000040502ffffffff03"
// Trailer B long that follows the unknown byte, purpose unknown. Species-dependent, so the captured
// value is written back for now.
private const val TRAILER_B_LONG = 0x200000L
// Record end. Measured against the installed 31914 client rather than the 32710 fixtures: the
// client consumed 136 bytes of a record we wrote as 137, so this tail is one byte shorter than the
// captured one. f.tK0.wG finishes by reading a U8 list count and doing new QB[count] with it as a
// SIGNED byte, so the final byte must stay zero; a misaligned read landing on 0xff is what produced
// NegativeArraySizeException: -1 at character select.
private const val TRAILER = "00ffff00"

/**
 * The record's final bytes depend on the client build. 31914 consumes exactly [TRAILER]; the 32710
 * captures and the r32645 Android client carry one more zero byte (139-byte record, not 138). The
 * client's reader finishes by taking the next byte as an array length, so a record one byte short
 * makes that read land on 0xff: `NegativeArraySizeException: -1` at character select, which is
 * exactly what the phone hit. The width comes from the revision the peer declared at join
 * ([monsterRecordHasLongTrailer]); with no session in flight the captured (long) form is used.
 */
private val RevisionTrailer: Codec<Unit> =
    object : Codec<Unit> {
      private val base = TRAILER.hexToBytes()

      override fun read(buf: ReadBuffer) {
        buf.readBytes(ByteArray(base.size))
        if (monsterRecordHasLongTrailer()) buf.readBytes(ByteArray(1))
      }

      override fun write(buf: WriteBuffer, value: Unit) {
        buf.writeBytes(base)
        if (monsterRecordHasLongTrailer()) buf.writeByte(0)
      }
    }

private fun packRarity(p: Pokemon): Int =
    (if (p.isShiny) PokemonRarityFlag.SHINY.mask else 0) or
        (if (p.hasHiddenAbility) PokemonRarityFlag.HIDDEN_ABILITY.mask else 0) or
        (if (p.isAlpha) PokemonRarityFlag.ALPHA.mask else 0) or
        (if (p.isSecret) PokemonRarityFlag.SECRET_SHINY.mask else 0) or
        (if (p.isFatefulEncounter) PokemonRarityFlag.FATEFUL_ENCOUNTER.mask else 0) or
        (if (p.isRaidEncounter) PokemonRarityFlag.RAID_ENCOUNTER.mask else 0)

// EVs are six raw bytes in wire order hp, atk, def, spd, spAtk, spDef.
private fun evsFromWire(hp: Int, atk: Int, def: Int, spd: Int, spAtk: Int, spDef: Int): EVs =
    EVs().apply {
      this.hp = hp
      this.atk = atk
      this.def = def
      this.spd = spd
      this.spAtk = spAtk
      this.spDef = spDef
    }

// IVs are a packed 30-bit word (six 5-bit stats). The top two bits carry unidentified flags and are
// dropped on write, so they stay zero for constructed monsters.
private fun ivsFromBits(bits: Int): IVs = decompressIVs(bits)

/**
 * The monster record. The named fields are decoded from real captures. The reserved segments hold
 * species metadata and still-unlabelled structure kept as reference bytes, so the layout
 * round-trips exactly.
 */
object PokemonCodec : PacketCodec<Pokemon>() {
  override fun CodecScope<Pokemon>.body(): Pokemon {
    val id = field(S64LE, Pokemon::id)
    field(reserved("0000")) {}
    val ownerId = field(S64LE, Pokemon::ownerId)
    field(S64LE, Pokemon::ownerId)
    // The container byte (client f/tK0.wG reads it through f/Cy's byte lookup into k91.Pf1): 0 =
    // PC, 1 = party. It was a fixed 01, which filed every boxed monster as "party" on the client,
    // so a container/slot delta for a PC monster took the same-container branch and only moved the
    // slot - the monster stayed in its box and showed up in the party as well.
    val container = PokemonContainer.entries[field(U8) { it.container.ordinal }]
    val containerSlot = field(S16LE, Pokemon::containerSlot)
    val wireDexId = field(U16LE) { clientSpeciesId(it.dexId) }
    val seed = field(S32LE, Pokemon::seed)
    field(S64LE, Pokemon::ownerId)
    val ot = field(Utf16LeNullTerminated, Pokemon::ot)
    val nickname = field(Utf16LeNullTerminated, Pokemon::nickname)
    field(U8) { 0 }
    // Non-volatile status (client k91.Vy1, read straight after the nickname's trailing byte):
    // sleep turns in the low three bits, then poison 8, burn 16, freeze 32, paralysis 64, toxic
    // 128 - the party cell's status icon (k91.OX -> Rp0.fu0) switches on exactly those values.
    val status = field(U8) { it.status and 0xFF }
    val level = field(S8, Pokemon::level)
    val hp = field(S16LE, Pokemon::hp)
    // Held item id, 0 for none. Client field k91.eE0: the held-item getter vh1() returns the
    // battle override z21 when set and falls back to THIS short (bytecode-verified via the breed
    // window's "Held Item: {00}" render chain, f/pM1 -> vh1 -> f/YY0.lPt9 registry lookup).
    val heldItem = field(S16LE) { it.heldItem.toShort() }
    val xp = field(S32LE, Pokemon::xp)
    field(reserved("00")) {}
    // Friendship s16 (client k91.COn) - the summary happiness meter reads this; it was the
    // hardcoded 0x0032 half of the old reserved block.
    val friendship = field(S16LE) { it.friendship.toShort() }
    val moveIds = List(4) { i -> field(S16LE) { it.moves[i].id } }
    val movePps = List(4) { i -> field(S8) { it.moves[i].pp } }
    // The client reads FOUR SHORTS here (k91.CQ) and only THEN the six EV bytes - the old
    // 4-byte-list guess had the EVs starting 4 bytes early, so the client showed them shifted.
    field(reserved(CQ_SHORTS)) {}
    val evHp = field(U8) { it.eVs.hp and 0xFF }
    val evAtk = field(U8) { it.eVs.atk and 0xFF }
    val evDef = field(U8) { it.eVs.def and 0xFF }
    val evSpd = field(U8) { it.eVs.spd and 0xFF }
    val evSpAtk = field(U8) { it.eVs.spAtk and 0xFF }
    val evSpDef = field(U8) { it.eVs.spDef and 0xFF }
    field(reserved(TAIL_A_REST)) {}
    // The form byte (client k91.Jw1, last of that block): species + form picks the form's record and
    // sprite on the client (k91.K90). It was a constant 0, so every form showed as its base.
    val form = field(U8) { it.form and 0xFF }
    val ivBits = field(S32LE) { it.iVs.compress() }
    // The ability slot (client k91.WJ0, read here by f/tK0.wG): the summary's gT0.In0 looks the
    // species ability up by it and kd1() == 2 draws the hidden-ability label. It was written as 0,
    // so every monster showed its first ability while battles used the personality's pick.
    val abilitySlot = field(U8) { it.abilitySlot and 0xFF }
    field(S64LE) { TRAILER_B_LONG }
    val rarityBits = field(U16LE) { packRarity(it) }
    val caughtAt = field(TimestampLE, Pokemon::caughtAt)
    val isEgg = field(Bool, Pokemon::isEgg)
    field(RevisionTrailer) {}
    return Pokemon(
        id = id,
        ownerId = ownerId,
        container = container,
        containerSlot = containerSlot,
        status = status,
        dexId = canonicalSpeciesId(wireDexId),
        seed = seed,
        ot = ot,
        nickname = nickname,
        level = level,
        hp = hp,
        xp = xp,
        heldItem = heldItem.toInt(),
        friendship = friendship.toInt(),
        eVs = evsFromWire(evHp, evAtk, evDef, evSpd, evSpAtk, evSpDef),
        iVs = ivsFromBits(ivBits),
        moves = List(4) { PokemonMove(moveIds[it], movePps[it]) },
        isShiny = PokemonRarityFlag.SHINY.isSet(rarityBits),
        hasHiddenAbility = PokemonRarityFlag.HIDDEN_ABILITY.isSet(rarityBits),
        isAlpha = PokemonRarityFlag.ALPHA.isSet(rarityBits),
        isSecret = PokemonRarityFlag.SECRET_SHINY.isSet(rarityBits),
        isFatefulEncounter = PokemonRarityFlag.FATEFUL_ENCOUNTER.isSet(rarityBits),
        isRaidEncounter = PokemonRarityFlag.RAID_ENCOUNTER.isSet(rarityBits),
        caughtAt = caughtAt,
        isEgg = isEgg,
        abilitySlot = abilitySlot,
        form = form,
    )
  }
}
