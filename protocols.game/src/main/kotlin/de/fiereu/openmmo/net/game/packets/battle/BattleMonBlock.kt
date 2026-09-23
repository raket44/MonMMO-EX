package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.MAX_MOVE_SLOTS

/**
 * One monster as the battle packets carry it, shared by the field state and the switch-in. The
 * values are server computed and the client displays them as sent. A full block carries hp,
 * ability, and moves. The 21-byte active detail names only species, level, and gender.
 *
 * [movesPresent] is the wire flag that guards the ability and move ids. The wild block clears it so
 * the client never learns the enemy moveset.
 */
data class BattleMonBlock(
    val slot: Int,
    val entityId: Long,
    val species: Short,
    val level: Byte,
    /** The client's gender byte (f/b54.Se): 0 male, 1 female, 2 genderless. */
    val gender: Byte,
    val abilityId: Short,
    val maxHp: Short,
    val currentHp: Short,
    val movesPresent: Boolean,
    val moveIds: List<Short>,
    /**
     * Rarity flags (client f/dl6.D20 bits): shiny 0x1, alpha 0x4, secret shiny 0x8. They ride in
     * the active detail's flags short (f/at0.VQ1 -> f/b54.c50: G80 = shiny|secret, E70 = alpha)
     * and in the full block's record-flags short after the hp pair (f/at0.ci -> f/qi0.mP ->
     * f/dl6.Lq0). f/lq1.rj tints the battle sprite for an alpha: gold (vh1.iS) when also shiny,
     * red (vh1.xx #F93822) otherwise.
     */
    val shiny: Boolean = false,
    val alpha: Boolean = false,
    val secret: Boolean = false,
    /**
     * Owner key on the player's side: 0 the player, 1 an NPC ally fighting beside them (the record
     * head byte, f/at0.ci). [slot] is then the slot within that owner's team.
     */
    val owner: Int = 0,
) {
  init {
    require(moveIds.size == MOVE_SLOTS) { "A battle mon block carries exactly $MOVE_SLOTS moves" }
  }

  companion object {
    const val MOVE_SLOTS = MAX_MOVE_SLOTS
  }
}

private fun seg(hex: String): ByteArray =
    ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

// Rarity flag bits shared by the record (f/dl6.D20) and the battle blocks.
internal const val RARITY_SHINY = 0x1
internal const val RARITY_ALPHA = 0x4
internal const val RARITY_SECRET = 0x8

internal fun rarityFlags(shiny: Boolean, alpha: Boolean, secret: Boolean): Short =
    ((if (shiny) RARITY_SHINY else 0) or (if (alpha) RARITY_ALPHA else 0) or (if (secret) RARITY_SECRET else 0)).toShort()

// After the full block's hp pair (f/at0.ci -> f/qi0.mP): a status byte (pv0, or-ed into the record),
// the record's rarity flags short (dl6.Lq0), the ball-kind enum byte (c89, -1 = none) and the ball
// index (3 = Poke Ball) - captured as 00 | 0000 | ff | 03.
private val BLOCK_TAIL_BALL = seg("ff03")
private val ACTIVE_TAIL = seg("03ff0000000066666666")

/**
 * The active monster's detail block: species, level, gender and the rarity flags.
 *
 * The client's reader (`f/at0.VQ1`, r32645) takes it as: position u8, party slot u8, species s16,
 * level u8, an empty nickname (two zero bytes), the rarity flags s16, gender u8, form u8, then the
 * tail (ball index 3, ball-kind -1, an int of flags, a float). The flags go to `f/b54.c50`: bits
 * 0x9 (shiny, secret) pick the shiny sprite, bit 0x4 marks the alpha (`b54.lU1`), which `f/lq1.rj`
 * tints gold (shiny) or red in battle and draws 1.25x.
 */
internal data class BattleActiveDetail(
    /** The field position the monster stands on; the client indexes its per-side active array with it. */
    val position: Int,
    val slot: Int,
    val species: Short,
    val level: Byte,
    val gender: Byte,
    val flags: Short = 0,
    /** Two-trainer battles: the client reads the first byte as the owner key, not the position. */
    val owner: Int? = null,
) {
  companion object {
    const val SHINY_FLAG: Short = RARITY_SHINY.toShort()

    fun of(position: Int, slot: Int, mon: BattleMonBlock, owner: Int? = null): BattleActiveDetail =
        BattleActiveDetail(position, slot, mon.species, mon.level, mon.gender, rarityFlags(mon.shiny, mon.alpha, mon.secret), owner)

    fun of(position: Int, mon: BattleOpponentBlock, owner: Int? = null): BattleActiveDetail =
        BattleActiveDetail(position, mon.slot, mon.species, mon.level, mon.gender, rarityFlags(mon.shiny, mon.alpha, mon.secret), owner)
  }
}

internal object BattleActiveDetailCodec : PacketCodec<BattleActiveDetail>() {
  const val WIRE_SIZE = 21

  override fun CodecScope<BattleActiveDetail>.body(): BattleActiveDetail {
    // The client reads this pair as (owner key, slot within owner): kw0.NQ1 -> side.qg1(key).VZ()[slot].
    val position = field(S8) { (it.owner ?: it.position).toByte() }.toInt()
    val slot = field(S8) { it.slot.toByte() }.toInt()
    val species = field(S16LE) { it.species }
    val level = field(S8) { it.level }
    padding(2) // empty nickname
    val flags = field(S16LE) { it.flags }
    val gender = field(S8) { it.gender }
    reserved(0) // form
    constant(ACTIVE_TAIL)
    return BattleActiveDetail(position, slot, species, level, gender, flags)
  }
}

internal object BattleFullBlockCodec : PacketCodec<BattleMonBlock>() {
  override fun CodecScope<BattleMonBlock>.body(): BattleMonBlock {
    val slot = field(S8) { it.slot.toByte() }.toInt()
    constant(1)
    val entityId = field(S64LE) { it.entityId }
    val species = field(S16LE) { it.species }
    val level = field(S8) { it.level }
    // The name: a UTF-16 string the client reads to its 0 char - always empty from us.
    padding(2)
    val gender = field(S8) { it.gender }
    reserved(0) // form (f/ct7.Mi1)
    padding(2) // f/ct7.SK
    val currentHp = field(S16LE) { it.currentHp }
    val maxHp = field(S16LE) { it.maxHp }
    reserved(0) // status bits (f/dl6.pv0)
    val flags = field(S16LE) { rarityFlags(it.shiny, it.alpha, it.secret) }.toInt()
    constant(BLOCK_TAIL_BALL)
    val movesPresent = field(Bool) { it.movesPresent }
    val abilityId = if (movesPresent) field(S16LE) { it.abilityId } else 0
    val moveIds =
        if (movesPresent) field(S16LE.repeat(BattleMonBlock.MOVE_SLOTS)) { it.moveIds }
        else List(BattleMonBlock.MOVE_SLOTS) { 0.toShort() }
    return BattleMonBlock(
        slot, entityId, species, level, gender, abilityId, maxHp, currentHp, movesPresent, moveIds,
        shiny = flags and RARITY_SHINY != 0, alpha = flags and RARITY_ALPHA != 0, secret = flags and RARITY_SECRET != 0)
  }
}

/**
 * One monster on the opposing side. A monster the player has not been shown yet rides as a stub
 * with no body at all, which is how a trainer's benched team is hidden until it is sent out.
 */
data class BattleOpponentBlock(
    /** Slot within the OWNING trainer's party (equals the global index for one trainer). */
    val slot: Int,
    val revealed: Boolean,
    val entityId: Long = 0,
    val species: Short = 0,
    val level: Byte = 0,
    val gender: Byte = 0,
    val maxHp: Short = 0,
    val currentHp: Short = 0,
    /** Owner key: the first byte of the trainer entry in a two-trainer header; 0 otherwise. */
    val owner: Int = 0,
    /** See BattleMonBlock.shiny / alpha / secret. */
    val shiny: Boolean = false,
    val alpha: Boolean = false,
    val secret: Boolean = false,
)

private const val REVEALED: Byte = 1
private const val HIDDEN: Byte = 2

internal object BattleOpponentBlockCodec : PacketCodec<BattleOpponentBlock>() {
  override fun CodecScope<BattleOpponentBlock>.body(): BattleOpponentBlock {
    val slot = field(S8) { it.slot.toByte() }.toInt()
    val kind = field(S8) { if (it.revealed) REVEALED else HIDDEN }
    if (kind != REVEALED) return BattleOpponentBlock(slot, revealed = false)
    val entityId = field(S64LE) { it.entityId }
    val species = field(S16LE) { it.species }
    val level = field(S8) { it.level }
    padding(2) // empty name
    val gender = field(S8) { it.gender }
    reserved(0) // form
    padding(2)
    val currentHp = field(S16LE) { it.currentHp }
    val maxHp = field(S16LE) { it.maxHp }
    reserved(0)
    val flags = field(S16LE) { rarityFlags(it.shiny, it.alpha, it.secret) }.toInt()
    constant(BLOCK_TAIL_BALL)
    // The opposing side never carries a moveset, so the client cannot read the enemy's moves.
    constant(0)
    return BattleOpponentBlock(
        slot, true, entityId, species, level, gender, maxHp, currentHp,
        shiny = flags and RARITY_SHINY != 0, alpha = flags and RARITY_ALPHA != 0, secret = flags and RARITY_SECRET != 0)
  }
}
