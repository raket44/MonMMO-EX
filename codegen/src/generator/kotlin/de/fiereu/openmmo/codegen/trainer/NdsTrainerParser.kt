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
 */
class NdsTrainerParser(private val root: File) {
  private val json = Json { ignoreUnknownKeys = true }

  fun parseAll(): List<ParsedTrainer> =
      if (File(root, "res/trainers/data").isDirectory) platinum() else heartgold()

  private fun enumList(path: String): Map<String, Int> =
      File(root, path).readLines().map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }
          .withIndex().associate { (i, name) -> name to i }

  private fun platinum(): List<ParsedTrainer> {
    val constants = File(root, "generated/trainers.txt").readLines().map { it.trim() }.filter { it.isNotEmpty() }
    val classes = enumList("generated/trainer_classes.txt")
    val species = enumList("generated/species.txt")
    val moves = enumList("generated/moves.txt")
    return constants.withIndex().mapNotNull { (id, constant) ->
      if (id == 0) return@mapNotNull null
      val file = File(root, "res/trainers/data/${constant.removePrefix("TRAINER_").lowercase()}.json")
      if (!file.isFile) return@mapNotNull null
      val obj = json.parseToJsonElement(file.readText()).jsonObject
      val party =
          (obj["party"] as? JsonArray)?.mapNotNull { m ->
            val mon = m.jsonObject
            val dex = species[mon["species"]?.jsonPrimitive?.content] ?: return@mapNotNull null
            if (dex <= 0 || dex > 493) return@mapNotNull null
            ParsedTrainerMon(
                dexId = dex,
                level = mon["level"]?.jsonPrimitive?.intOrNull ?: 5,
                iv = mon["iv_scale"]?.jsonPrimitive?.intOrNull ?: 0,
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
  }

  private fun heartgold(): List<ParsedTrainer> {
    val classes = defines("include/constants/trainer_class.h", "TRAINERCLASS_")
    val species = defines("include/constants/species.h", "SPECIES_")
    val moves = defines("include/constants/moves.h", "MOVE_")
    val constants = defines("include/constants/trainers.h", "TRAINER_").entries.associate { (k, v) -> v to k }
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
                iv = mon["difficulty"]?.jsonPrimitive?.intOrNull ?: 0,
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
      )
    }
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
  }
}
