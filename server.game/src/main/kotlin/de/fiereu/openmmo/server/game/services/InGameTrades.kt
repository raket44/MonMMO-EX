package de.fiereu.openmmo.server.game.services

/**
 * FireRed's in-game trade NPCs (src/data/ingame_trades.h, the FIRERED rows): the monster the NPC
 * hands over and the species they ask for, indexed by INGAME_TRADE_*. Dex ids are national dex
 * numbers, which are FireRed's own species ids for these Kanto monsters.
 */
data class InGameTrade(
    val nickname: String,
    val dexId: Int,
    /** hp, atk, def, speed, spAtk, spDef - the GBA order of the table's ivs array. */
    val ivs: IntArray,
    val otName: String,
    val personality: Int,
    val requestedDexId: Int,
    val heldItem: String? = null,
)

object InGameTrades {
  /** The DS games' tables (codegen GeneratedNdsNpcTrades), by script source. */
  fun forSource(source: String): List<InGameTrade> =
      when (source) {
        "platinum" -> SINNOH
        "heartgold" -> JOHTO
        else -> FIRERED
      }

  private fun rows(rows: List<de.fiereu.openmmo.trainer.NpcTradeRow>) =
      rows.map { InGameTrade(it.nickname, it.dexId, it.ivs, it.otName, it.personality, it.requestedDexId, it.heldItem) }

  val SINNOH: List<InGameTrade> by lazy { rows(de.fiereu.openmmo.trainer.generated.GeneratedNdsNpcTrades.SINNOH) }
  val JOHTO: List<InGameTrade> by lazy { rows(de.fiereu.openmmo.trainer.generated.GeneratedNdsNpcTrades.JOHTO) }

  val FIRERED: List<InGameTrade> =
      listOf(
          InGameTrade("MIMIEN", 122, intArrayOf(20, 15, 17, 24, 23, 22), "REYLEY", 0x00009cae, 63),
          InGameTrade("ZYNX", 124, intArrayOf(18, 17, 18, 22, 25, 21), "DONTAE", 0x498a2e1d, 61, "ITEM_FAB_MAIL"),
          InGameTrade("MS. NIDO", 29, intArrayOf(22, 18, 25, 19, 15, 22), "SAIGE", 0x4c970b89, 32, "ITEM_TINY_MUSHROOM"),
          InGameTrade("CH'DING", 83, intArrayOf(20, 25, 21, 24, 15, 20), "ELYSSA", 0x151943d7, 21, "ITEM_STICK"),
          InGameTrade("NINA", 30, intArrayOf(22, 25, 18, 19, 22, 15), "TURNER", 0x00eeca15, 33),
          InGameTrade("MARC", 108, intArrayOf(24, 19, 21, 15, 23, 21), "HADEN", 0x451308ab, 55),
          InGameTrade("ESPHERE", 101, intArrayOf(19, 16, 18, 25, 25, 19), "CLIFTON", 0x06341016, 26),
          InGameTrade("TANGENY", 114, intArrayOf(22, 17, 25, 16, 23, 20), "NORMA", 0x5c77ecfa, 48, "ITEM_STARDUST"),
          InGameTrade("SEELOR", 86, intArrayOf(24, 15, 22, 16, 23, 22), "GARETT", 0x482cac89, 77),
      )
}
