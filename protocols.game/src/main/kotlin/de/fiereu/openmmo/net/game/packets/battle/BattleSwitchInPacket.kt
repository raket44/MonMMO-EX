package de.fiereu.openmmo.net.game.packets.battle

import de.fiereu.bytecodec.*

/**
 * Brings a monster onto the field after a switch (opcode 0x35).
 *
 * The layout here is the one the client's own parser (`f/n8.qP1`) reads, decoded from bytecode
 * after the first real player switch crashed the client with a null battle monster:
 * - one packed byte: the new slot in the high nibble, the side in the low nibble (`f/fd1.Po0`)
 * - one kind byte, an enum the client resolves leniently; zero is the plain send-out
 * - one flag byte: 1 means the full monster block follows, anything else means it does not
 * - the full block plus its trailing last-block marker, when flagged
 * - the 21-byte active detail, always
 *
 * The previous codec wrote a four-byte header (side, zero, new slot, old slot): a misreading of one
 * capture whose new-slot byte happened to be 1, which the client actually treats as the full-block
 * flag. Every synthesized switch-in was therefore shifted by one byte and the client dropped the
 * monster, leaving a null in the field model that crashed the renderer on send-out. The old slot is
 * not on the wire at all.
 */
data class BattleSwitchInPacket(
    val newSlot: Int,
    val oldSlot: Int,
    val mon: BattleMonBlock,
    val fullBlock: Boolean,
    /** 0 sends out one of the player's own, 1 one of the opponent's. */
    val side: Byte = 0,
    /** Two-trainer battles: owner key of the entering monster (record head + active detail). */
    val owner: Int? = null,
    /**
     * Send-out kind (r32645 q94, via cs2.gB): 0 recalls a monster still standing at the position
     * ("come back!" and the withdraw animation) before the send-out; 5 skips the recall - the
     * dragged-in switch of Roar and Dragon Tail.
     */
    val kind: Byte = 0,
)

private val NO_MOVES = List(BattleMonBlock.MOVE_SLOTS) { 0.toShort() }

object BattleSwitchInPacketCodec : PacketCodec<BattleSwitchInPacket>() {
  override fun CodecScope<BattleSwitchInPacket>.body(): BattleSwitchInPacket {
    val packed = field(U8) { (it.newSlot shl 4) or (it.side.toInt() and 0x0F) }
    val side = (packed and 0x0F).toByte()
    val newSlot = (packed ushr 4) and 0x0F
    val kind = field(U8) { it.kind.toInt() and 0xFF }.toByte()
    // Decoded against both the client's parser (f/n8.qP1 -> ns0 -> NQ1) and the captured retail
    // switch-in, which agree byte for byte:
    // - the flag byte says whether the ns0 block follows; the original codec mislabelled it
    //   "newSlot", which held 1 in the capture by coincidence, so every switch from another slot
    //   broke the parse.
    // - a zero sub-side byte opens the block; ns0 then reads slot, the presence constant, and the
    //   u64 monster uid (the block's entityId - consumed by the client's gb1()).
    // - the byte before the detail is NQ1's presence marker, required in BOTH branches - it is
    //   not a "last block" flag, so it also precedes a detail sent without a block.
    val fullBlock = field(U8) { if (it.fullBlock) 1 else 0 } == 1
    val block =
        if (fullBlock) {
          field(U8) { it.owner ?: 0 } // ns0 owner key (sub-side); zero for one trainer
          field(BattleFullBlockCodec) { it.mon }
        } else null
    field(S8) { 1 } // NQ1 presence: the monster is on the field
    val active = field(BattleActiveDetailCodec) { BattleActiveDetail.of(it.newSlot, it.mon.slot, it.mon, it.owner) }
    val mon =
        block
            ?: BattleMonBlock(
                slot = active.slot,
                entityId = 0,
                species = active.species,
                level = active.level,
                gender = active.gender,
                abilityId = 0,
                maxHp = 0,
                currentHp = 0,
                movesPresent = false,
                moveIds = NO_MOVES,
            )
    return BattleSwitchInPacket(
        newSlot = newSlot, oldSlot = -1, mon = mon, fullBlock = fullBlock, side = side, kind = kind)
  }
}
