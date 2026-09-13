package de.fiereu.openmmo.codegen.trainer

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The DS trainers out of the decomps, numbered the way the ROM (and so the client's per-region
 * trainer table, `f/W9.io(region, id)`) numbers them.
 *
 * Platinum keeps one json per trainer under `res/trainers/data`, ordered by
 * `generated/trainers.txt` (the constant lower-cased is the file name); class, species and move
 * ids are the positions in the matching `generated` enum lists. HeartGold keeps one array in
 * `files/poketool/trainer/trainers.json` whose index is the trainer id, with the pret-style
 * `include/constants` headers for every id.
 *
 * Trainer speech comes from each game's trainer message table (trtbl: u16 trainer, u16 message
 * kind per entry, the entry index being the line in the game's trainer text bank). Platinum's
 * table is rebuilt from the per-trainer json the way tools/dataproc/trainerproc.c does it,
 * HeartGold ships it as `files/poketool/trmsg/trtbl.narc`, Unova's rows come from tools/nds/Trn5.
 */
class NdsTrainerParser(private val root: File) {
  private val json = Json { ignoreUnknownKeys = true }

  fun parseAll(): List<ParsedTrainer> =
      when {
        File(root, "nds-trainers-2.txt").isFile -> unova()
        File(root, "res/trainers/data").isDirectory -> platinum()
        else -> heartgold()
      }

  /** Unova: tools/nds/Trn5 rows from the ROM's trdata/trpoke/trtbl archives (trn, mon and msg lines). */
  private fun unova(): List<ParsedTrainer> {
    val mons = HashMap<Int, MutableList<ParsedTrainerMon>>()
    val heads = LinkedHashMap<Int, IntArray>()
    val messages = HashMap<Int, MutableMap<Int, Int>>()
    File(root, "nds-trainers-2.txt").forEachLine { l ->
      val p = l.split(';')
      when (p[0]) {
        "trn" -> heads[p[2].toInt()] = intArrayOf(p[3].toInt(), p[4].toInt(), p[5].toInt(), p[6].toInt())
        "mon" -> {
          val dex = p[4].toInt()
          if (dex in 1..649) mons.getOrPut(p[2].toInt()) { mutableListOf() } +=
              ParsedTrainerMon(dex, p[5].toInt(), difficultyToIv(p[6].toInt()), 0, p.drop(8).mapNotNull { it.toIntOrNull() }.filter { it > 0 })
        }
        "msg" -> messages.getOrPut(p[2].toInt()) { linkedMapOf() }[p[3].toInt()] =
            (2 shl 28) or (UNOVA_TRAINER_TEXT_BANK shl 16) or p[4].toInt()
      }
    }
    return heads.mapNotNull { (id, h) ->
      val party = mons[id].orEmpty()
      if (party.isEmpty()) null
      else ParsedTrainer(id, "TRAINER_$id", "", h[0], h[2] != 0, DEFAULT_PRIZE_RATE, party, emptyList(), messages[id].orEmpty())
    }
  }

  /**
   * The DS games store a trainer mon's difficulty as 0-255 and derive the IV from it at battle time
   * (difficulty * 31 / 255, the rule the GBA parser already applies). The raw value was copied as the
   * IV, so DS trainers battled with IVs up to 250 - and Volkner's Electivire with 2500, a typo in the
   * Platinum decomp's own data - so the value is capped to the byte range before scaling.
   */
  private fun difficultyToIv(difficulty: Int): Int = difficulty.coerceIn(0, 255) * 31 / 255

  private fun enumList(path: String): Map<String, Int> =
      File(root, path).readLines().map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }
          .withIndex().associate { (i, name) -> name to i }

  private fun platinum(): List<ParsedTrainer> {
    val constants = File(root, "generated/trainers.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
    val classes = enumList("generated/trainer_classes.txt")
    val species = enumList("generated/species.txt")
    val moves = enumList("generated/moves.txt")
    val messageTypes = enumList("generated/trainer_message_types.txt")
    val messageKinds = HashMap<Int, List<Int>>()
    val trainers = constants.withIndex().mapNotNull { (id, constant) ->
      if (id == 0) return@mapNotNull null
      val file = File(root, "res/trainers/data/${constant.removePrefix("TRAINER_").lowercase()}.json")
      if (!file.isFile) return@mapNotNull null
      val obj = json.parseToJsonElement(file.readText()).jsonObject
      (obj["messages"] as? JsonArray)?.mapNotNull { m -> messageTypes[m.jsonObject["type"]?.jsonPrimitive?.content] }
          ?.takeIf { it.isNotEmpty() }?.let { messageKinds[id] = it }
      val party =
          (obj["party"] as? JsonArray)?.mapNotNull { m ->
            val mon = m.jsonObject
            val dex = species[mon["species"]?.jsonPrimitive?.content] ?: return@mapNotNull null
            if (dex <= 0 || dex > 493) return@mapNotNull null
            ParsedTrainerMon(
                dexId = dex,
                level = mon["level"]?.jsonPrimitive?.intOrNull ?: 5,
                iv = difficultyToIv(mon["iv_scale"]?.jsonPrimitive?.intOrNull ?: 0),
                heldItem = 0,
                moveIds = (mon["moves"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content?.let(moves::get) }?.filter { it > 0 }.orEmpty(),
            )
          }.orEmpty()
      if (party.isEmpty()) return@mapNotNull null
      ParsedTrainer(
          id = id,
          constant = constant,
          name = obj["name"]?.jsonPrimitive?.content.orEmpty(),
          trainerClass = classes[obj["class"]?.jsonPrimitive?.content] ?: 0,
          doubleBattle = obj["double_battle"]?.jsonPrimitive?.booleanOrNull ?: false,
          prizeRate = DEFAULT_PRIZE_RATE,
          party = party,
          rematchIds = emptyList(),
      )
    }
    val messages = platinumMessages(messageKinds)
    return trainers.map { t -> messages[t.id]?.let { t.copy(messages = it) } ?: t }
  }

  /**
   * trainerproc.c emits trtbl sorted by its vanilla `trtbl_indices` table (the ROM's own trainer
   * order for the message bank), one entry per message in json order; the text line is the running
   * entry count. The bank is TEXT_BANK_NPC_TRAINER_MESSAGES' position in generated/text_banks.txt.
   */
  private fun platinumMessages(kinds: Map<Int, List<Int>>): Map<Int, Map<Int, Int>> {
    val bank =
        File(root, "generated/text_banks.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .indexOf("TEXT_BANK_NPC_TRAINER_MESSAGES")
    check(bank >= 0) { "pokeplatinum generated/text_banks.txt has no TEXT_BANK_NPC_TRAINER_MESSAGES" }
    val src = File(root, "tools/dataproc/src/trainerproc.c").readText()
    val table = src.substringAfter("trtbl_indices[] = {").substringBefore("};")
    val indices = Regex("-?\\d+").findAll(table).map { it.value.toInt() }.toList()
    check(indices.size >= 900) { "pokeplatinum trainerproc.c trtbl_indices parsed ${indices.size} entries" }
    fun order(id: Int) = indices.getOrElse(id) { -1 }.let { if (it < 0) 0xFFFF else it }
    val out = HashMap<Int, Map<Int, Int>>()
    var line = 0
    for (id in kinds.keys.sortedWith(compareBy({ order(it) }, { it }))) {
      val perKind = linkedMapOf<Int, Int>()
      for (kind in kinds.getValue(id)) {
        if (kind !in perKind) perKind[kind] = (3 shl 28) or (bank shl 16) or line
        line++
      }
      out[id] = perKind
    }
    return out
  }

  private fun heartgold(): List<ParsedTrainer> {
    val classes = defines("include/constants/trainer_class.h", "TRAINERCLASS_")
    val species = defines("include/constants/species.h", "SPECIES_")
    val moves = defines("include/constants/moves.h", "MOVE_")
    val constants = defines("include/constants/trainers.h", "TRAINER_").entries.associate { (k, v) -> v to k }
    val messages = heartgoldMessages()
    val list = json.parseToJsonElement(File(root, "files/poketool/trainer/trainers.json").readText()).jsonObject["trainers"]?.jsonArray.orEmpty()
    return list.withIndex().mapNotNull { (id, element) ->
      if (id == 0) return@mapNotNull null
      val obj = element.jsonObject
      val party =
          (obj["party"] as? JsonArray)?.mapNotNull { m ->
            val mon = m.jsonObject
            val dex = species[mon["species"]?.jsonPrimitive?.content] ?: return@mapNotNull null
            if (dex <= 0 || dex > 493) return@mapNotNull null
            ParsedTrainerMon(
                dexId = dex,
                level = mon["level"]?.jsonPrimitive?.intOrNull ?: 5,
                iv = difficultyToIv(mon["difficulty"]?.jsonPrimitive?.intOrNull ?: 0),
                heldItem = 0,
                moveIds = (mon["moves"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content?.let(moves::get) }?.filter { it > 0 }.orEmpty(),
            )
          }.orEmpty()
      if (party.isEmpty()) return@mapNotNull null
      ParsedTrainer(
          id = id,
          constant = constants[id] ?: "TRAINER_$id",
          name = obj["name"]?.jsonPrimitive?.content.orEmpty().replace("{TRNAME}", "").trim(),
          trainerClass = classes[obj["class"]?.jsonPrimitive?.content] ?: 0,
          doubleBattle = (obj["double"]?.jsonPrimitive?.intOrNull ?: 0) != 0,
          prizeRate = DEFAULT_PRIZE_RATE,
          party = party,
          rematchIds = emptyList(),
          messages = messages[id].orEmpty(),
      )
    }
  }

  /** trtbl.narc member 0: (u16 trainer, u16 kind) entries; entry i is line i of msg bank 728. */
  private fun heartgoldMessages(): Map<Int, Map<Int, Int>> {
    val narc = File(root, "files/poketool/trmsg/trtbl.narc").readBytes()
    fun u16(o: Int) = (narc[o].toInt() and 0xFF) or ((narc[o + 1].toInt() and 0xFF) shl 8)
    fun u32(o: Int) = u16(o) or (u16(o + 2) shl 16)
    var p = 0x10
    var start = 0
    var end = 0
    var image = -1
    while (p + 8 <= narc.size) {
      val magic = String(narc, p, 4, Charsets.US_ASCII)
      val size = u32(p + 4)
      when (magic) {
        "BTAF" -> { start = u32(p + 12); end = u32(p + 16) }
        "GMIF" -> { image = p + 8 }
      }
      if (image >= 0 || size <= 0) break
      p += size
    }
    check(image >= 0) { "trtbl.narc has no GMIF section" }
    val out = HashMap<Int, MutableMap<Int, Int>>()
    var i = 0
    var o = image + start
    while (o + 4 <= image + end) {
      out.getOrPut(u16(o)) { linkedMapOf() }.putIfAbsent(u16(o + 2), (4 shl 28) or (HEARTGOLD_TRAINER_TEXT_BANK shl 16) or i)
      i++
      o += 4
    }
    return out
  }

  private fun defines(path: String, prefix: String): Map<String, Int> {
    val define = Regex("^#define\\s+($prefix\\w+)\\s+(0x[0-9A-Fa-f]+|\\d+)")
    val out = HashMap<String, Int>()
    File(root, path).forEachLine { line ->
      val m = define.find(line.trim()) ?: return@forEachLine
      val v = m.groupValues[2]
      out[m.groupValues[1]] = if (v.startsWith("0x")) v.drop(2).toInt(16) else v.toInt()
    }
    return out
  }

  private companion object {
    /** Gen 4 pays class base rate x last level x 4; the GBA parser's per-class table has no DS twin yet. */
    const val DEFAULT_PRIZE_RATE = 4
    /** HeartGold msg_0728: the trainer speech bank GetTrainerMessageByIdPair reads. */
    const val HEARTGOLD_TRAINER_TEXT_BANK = 728
    /** White /a/0/0/2 file 189: 1762 lines, one per /a/0/9/0 table entry (probed from the ROM). */
    const val UNOVA_TRAINER_TEXT_BANK = 189
  }
}
