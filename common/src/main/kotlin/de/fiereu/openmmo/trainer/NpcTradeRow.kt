package de.fiereu.openmmo.trainer

/**
 * One in-game trade npc of a DS game (Platinum res/npc_trades, HeartGold files/data/tradelist.narc):
 * the monster handed over and the species asked for. Dex ids are national dex numbers, ivs are in
 * the games' own order (hp, atk, def, speed, spAtk, spDef); [heldItem] is the item's script constant.
 */
data class NpcTradeRow(
    val constant: String,
    val nickname: String,
    val dexId: Int,
    val ivs: IntArray,
    val otName: String,
    val personality: Int,
    val requestedDexId: Int,
    val heldItem: String?,
)
