package de.fiereu.openmmo.codegen.trainer

import de.fiereu.openmmo.trainer.NpcTradeRow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The DS games' in-game trade npcs, the same table FireRed's InGameTrades hand-lists:
 * - Platinum: `res/npc_trades/<name>.json` in the order of `include/constants/npc_trades.h`
 *   (NPC_TRADE_KAZZA_ABRA = 0, ...), species and requested species as SPECIES_ tokens.
 * - HeartGold: `files/a/1/1/2` (NARC_a_1_1_2, NPCTradeApp_Init), one 0x54-byte record per NpcTradeNum (struct NPCTrade
 *   in include/npc_trade.h: give_species, six ivs, ability, otId, five contest stats, pid, heldItem,
 *   gender, sheen, language, ask_species, one unused int); nicknames are msg_0200 entries 0..12 and
 *   the OT names entries 13..25 (NPC_TRADE_OT_NUM = tradeNum + NPC_TRADE_MAX).
 */
class NdsNpcTradeParser(private val root: File) {
  private val json = Json { ignoreUnknownKeys = true }

  fun parseAll(): List<NpcTradeRow> =
      when {
        File(root, "res/npc_trades").isDirectory -> platinum()
        File(root, "files/a/1/1/2").isFile -> heartgold()
        else -> emptyList()
      }

  private fun platinum(): List<NpcTradeRow> {
    val header = File(root, "include/constants/npc_trades.h").readText()
    val order = Regex("\\b(NPC_TRADE_[A-Z0-9_]+)\\b").findAll(header).map { it.groupValues[1] }.filter { it != "NPC_TRADE_COUNT" }.distinct().toList()
    val species = File(root, "generated/species.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }.withIndex().associate { (i, n) -> n to i }
    return order.mapNotNull { constant ->
      val file = File(root, "res/npc_trades/${constant.removePrefix("NPC_TRADE_").lowercase()}.json")
      if (!file.isFile) return@mapNotNull null
      val o = json.parseToJsonElement(file.readText()).jsonObject
      fun int(key: String) = o[key]?.jsonPrimitive?.intOrNull ?: 0
      fun str(key: String) = o[key]?.jsonPrimitive?.content.orEmpty()
      val dex = species[str("species")] ?: return@mapNotNull null
      val wanted = species[str("requestedSpecies")] ?: return@mapNotNull null
      NpcTradeRow(
          constant = constant,
          nickname = str("name"),
          dexId = dex,
          ivs = intArrayOf(int("hpIV"), int("atkIV"), int("defIV"), int("speedIV"), int("spAtkIV"), int("spDefIV")),
          otName = str("otName"),
          personality = int("personality"),
          requestedDexId = wanted,
          heldItem = str("heldItem").takeIf { it.isNotEmpty() && it != "ITEM_NONE" },
      )
    }
  }

  private fun heartgold(): List<NpcTradeRow> {
    val records = narcFiles(File(root, "files/a/1/1/2").readBytes())
    val constants =
        Regex("\\b(NPC_TRADE_[A-Z0-9_]+)\\b").findAll(File(root, "include/constants/npc_trade.h").readText())
            .map { it.groupValues[1] }.filter { it != "NPC_TRADE_MAX" && it != "NPC_TRADE_OT_NUM" }.distinct().toList()
    val names = gmmStrings(File(root, "files/msgdata/msg/msg_0200.gmm"))
    val itemNames =
        Regex("#define\\s+(ITEM_\\w+)\\s+(\\d+)").findAll(File(root, "include/constants/items.h").readText())
            .groupBy({ it.groupValues[2].toInt() }, { it.groupValues[1] }).mapValues { it.value.first() }
    val count = constants.size
    return records.take(count).mapIndexed { i, bytes ->
      val b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      val fields = IntArray(bytes.size / 4) { b.getInt(it * 4) }
      NpcTradeRow(
          constant = constants[i],
          nickname = names.getOrElse(i) { constants[i] },
          dexId = fields[0],
          ivs = intArrayOf(fields[1], fields[2], fields[3], fields[4], fields[5], fields[6]),
          otName = names.getOrElse(count + i) { "" },
          personality = fields[14],
          requestedDexId = fields[19],
          heldItem = fields[15].takeIf { it != 0 }?.let { itemNames[it] },
      )
    }
  }

  /** The `<language name="English">` strings of a gmm file, in row order. */
  private fun gmmStrings(file: File): List<String> =
      Regex("<language name=\"English\">([^<]*)</language>").findAll(file.readText()).map { it.groupValues[1].trim() }.toList()

  /** The member files of a Nitro archive: BTAF (start, end per file) and the GMIF data block. */
  private fun narcFiles(data: ByteArray): List<ByteArray> {
    val b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
    require(b.getInt(0) == 0x4352414E) { "not a NARC" }
    var offset = b.getShort(0x0C).toInt() and 0xFFFF
    var entries: List<Pair<Int, Int>> = emptyList()
    var dataStart = -1
    while (offset + 8 <= data.size) {
      val magic = b.getInt(offset)
      val size = b.getInt(offset + 4)
      when (magic) {
        0x46415442 -> { // "BTAF"
          val count = b.getShort(offset + 8).toInt() and 0xFFFF
          entries = (0 until count).map { i -> b.getInt(offset + 12 + i * 8) to b.getInt(offset + 16 + i * 8) }
        }
        0x46494D47 -> dataStart = offset + 8 // "GMIF"
      }
      if (size <= 0) break
      offset += size
    }
    require(dataStart >= 0) { "NARC without a GMIF block" }
    return entries.map { (start, end) -> data.copyOfRange(dataStart + start, dataStart + end) }
  }
}
