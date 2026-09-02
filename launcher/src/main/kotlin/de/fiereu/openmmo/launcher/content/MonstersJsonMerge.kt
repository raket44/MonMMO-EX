@file:JvmName("MonstersJsonMerge")

package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import java.nio.file.Files
import java.nio.file.Path

/**
 * Merges the Expansion's new species INTO the client's dex dump (`data/pokemmo/monsters.json`) in
 * the dump's exact schema.
 *
 * Operator-directed: the dump is the shape of a COMPLETE dex entry, so a new species is only
 * finished when it carries every field a retail one does - types, egg groups, all three abilities,
 * category, height and weight, learnsets (level, taught, egg, prevo), evolutions and yields. Every
 * value comes from the Expansion catalogue and its own data files; nothing is invented.
 *
 * Entries are keyed by the CLIENT WIRE id, the id the dex and every packet use, so a merged record
 * answers for the species the client actually shows. Retail entries are never touched: only ids
 * missing from the dump are appended.
 *
 * Usage: <monsters.json> <expansion-root> [output]
 */
fun main(args: Array<String>) {
  require(args.size >= 2) { "Usage: <monsters.json> <expansion-root> [output]" }
  val source = Path.of(args[0])
  val expansionRoot = Path.of(args[1])
  val output = Path.of(args.getOrElse(2) { args[0] })
  require(Files.isRegularFile(source)) { "Dex dump not found: " + source }
  require(Files.isDirectory(expansionRoot)) { "Expansion root not found: " + expansionRoot }

  val text = Files.readString(source)
  val existingIds =
      Regex("\"id\":\\s*(\\d+),\\s*\\R\\s*\"name\"")
          .findAll(text)
          .map { it.groupValues[1].toInt() }
          .toSet()

  val moveIds = MoveText.ids(expansionRoot)
  val moveNames = MoveText.parse(expansionRoot, moveIds).associate { it.id to it.name }
  val evolutions = ExpansionEvolutions.parse(expansionRoot)
  val learnsets = PorymovesLearnsets.parse(expansionRoot, moveIds)
  val catalog = ExpansionSpeciesRegistry().all()
  val wireBySymbol =
      catalog.filter { it.clientWireId != null }.associate { it.symbol to it.clientWireId!! }
  val nameBySymbol = catalog.associate { it.symbol to it.displayName }

  val additions =
      catalog
          .filter { it.clientWireId != null && it.clientWireId !in existingIds }
          .sortedBy { it.clientWireId }
          .map { entry ->
            entryJson(entry, moveNames, evolutions, learnsets, wireBySymbol, nameBySymbol)
          }

  if (additions.isEmpty()) {
    println("[monsters-merge] nothing to add: every catalogue species is already in the dump")
    return
  }
  val closing = text.lastIndexOf(']')
  require(closing > 0) { "Dex dump is not a JSON array" }
  val head = text.substring(0, closing).trimEnd().trimEnd(',')
  val merged = head + ",\n" + additions.joinToString(",\n") + "\n]\n"
  Files.createDirectories(output.parent)
  Files.writeString(output, merged)
  println(
      "[monsters-merge] added " +
          additions.size +
          " species to " +
          existingIds.size +
          " existing -> " +
          output)
}

private fun familyMoves(
    table: Map<String, List<Int>>,
    entry: ExpansionSpeciesDef,
): List<Int> {
  var current = entry.symbol.removePrefix("SPECIES_")
  while (current.isNotEmpty()) {
    table[current]?.let {
      return it
    }
    val cut = current.lastIndexOf('_')
    if (cut <= 0) break
    current = current.substring(0, cut)
  }
  return emptyList()
}

private fun entryJson(
    entry: ExpansionSpeciesDef,
    moveNames: Map<Int, String>,
    evolutions: Map<String, List<ExpansionEvolutions.Evolution>>,
    learnsets: PorymovesLearnsets.Learnsets,
    wireBySymbol: Map<String, Int>,
    nameBySymbol: Map<String, String>,
): String {
  val wire = entry.clientWireId!!
  val growth = entry.growthRateSymbol.removePrefix("GROWTH_")
  val expType = GROWTH_ORDER.indexOf(growth).coerceAtLeast(0)

  val moveRows = mutableListOf<String>()
  entry.levelUpLearnset
      .sortedBy { it.level }
      .forEach { move ->
        val name = moveNames[move.originalMoveId] ?: return@forEach
        moveRows += moveRow(move.originalMoveId, name, "level", move.level)
      }
  listOf(learnsets.taught to "TM??", learnsets.egg to "EGG", learnsets.prevo to "PREVO").forEach {
      (table, label) ->
    familyMoves(table, entry).sorted().forEach { id ->
      val name = moveNames[id] ?: return@forEach
      moveRows += moveRow(id, name, label, 0)
    }
  }

  val abilityRows =
      (0..2).joinToString(",\n") { slot ->
        val id = entry.abilityIds.getOrElse(slot) { entry.abilityIds.getOrElse(0) { 0 } }
        val symbol =
            entry.abilitySymbols.getOrElse(slot) { entry.abilitySymbols.getOrElse(0) { "" } }
        "\t\t{\n\t\t\t\"id\": " +
            id +
            ",\n\t\t\t\"name\": " +
            quote(titleCase(symbol.removePrefix("ABILITY_"))) +
            "\n\t\t}"
      }

  val eggRows =
      entry.eggGroupSymbols
          .mapNotNull { DUMP_EGG_GROUPS[it.removePrefix("EGG_GROUP_")] }
          .distinct()
          .ifEmpty { listOf("cannot breed") }
          .joinToString(",\n") { "\t\t" + quote(it) }

  val evoRows =
      (evolutions[entry.symbol.removePrefix("SPECIES_")] ?: emptyList())
          .mapNotNull { evo ->
            val targetWire = wireBySymbol["SPECIES_" + evo.targetSymbol] ?: return@mapNotNull null
            val targetName = nameBySymbol["SPECIES_" + evo.targetSymbol] ?: evo.targetSymbol
            "\t\t{\n\t\t\t\"id\": " +
                targetWire +
                ",\n\t\t\t\"name\": " +
                quote(targetName) +
                ",\n\t\t\t\"type\": " +
                quote(EVO_METHODS.getOrElse(evo.method) { "LEVEL" }) +
                ",\n\t\t\t\"val\": " +
                evo.param +
                "\n\t\t}"
          }
          .joinToString(",\n")

  val typeRows =
      entry.typeSymbols
          .map { it.removePrefix("TYPE_") }
          .distinct()
          .joinToString(",\n") { "\t\t" + quote(it) }

  val builder = StringBuilder()
  builder.append("{\n")
  builder.append("\t\"id\": ").append(wire).append(",\n")
  builder.append("\t\"name\": ").append(quote(entry.displayName)).append(",\n")
  builder.append("\t\"exp_type\": ").append(expType).append(",\n")
  builder
      .append("\t\"exp_type_name\": ")
      .append(quote(GROWTH_NAMES.getOrElse(expType) { "Medium Fast" }))
      .append(",\n")
  builder.append("\t\"catch_rate\": ").append(entry.catchRate).append(",\n")
  builder.append("\t\"obtainable\": true,\n")
  builder.append("\t\"gender_ratio\": ").append(entry.genderRatio).append(",\n")
  builder.append("\t\"height\": ").append(entry.height).append(",\n")
  builder.append("\t\"weight\": ").append(entry.weight).append(",\n")
  builder.append("\t\"category\": ").append(quote(entry.categoryName)).append(",\n")
  builder.append("\t\"egg_groups\": [\n").append(eggRows).append("\n\t],\n")
  builder.append("\t\"abilities\": [\n").append(abilityRows).append("\n\t],\n")
  builder.append("\t\"forms\": [\n\t\t{\n\t\t\t\"form_id\": 0,\n\t\t\t\"id\": ")
  builder
      .append(wire)
      .append(",\n\t\t\t\"name\": ")
      .append(quote(entry.displayName))
      .append(",\n\t\t\t\"is_costume\": false,\n\t\t\t\"is_released\": true\n\t\t}\n\t],\n")
  builder.append("\t\"evolutions\": [\n").append(evoRows).append("\n\t],\n")
  builder.append("\t\"moves\": [\n").append(moveRows.joinToString(",\n")).append("\n\t],\n")
  builder.append("\t\"types\": [\n").append(typeRows).append("\n\t],\n")
  builder.append("\t\"stats\": {\n")
  builder.append("\t\t\"hp\": ").append(entry.baseHp).append(",\n")
  builder.append("\t\t\"attack\": ").append(entry.baseAttack).append(",\n")
  builder.append("\t\t\"defense\": ").append(entry.baseDefense).append(",\n")
  builder.append("\t\t\"speed\": ").append(entry.baseSpeed).append(",\n")
  builder.append("\t\t\"sp_attack\": ").append(entry.baseSpAttack).append(",\n")
  builder.append("\t\t\"sp_defense\": ").append(entry.baseSpDefense).append("\n\t},\n")
  builder.append("\t\"yields\": {\n")
  builder.append("\t\t\"exp\": ").append(entry.expYield).append(",\n")
  builder.append("\t\t\"ev_hp\": ").append(entry.evYieldHp).append(",\n")
  builder.append("\t\t\"ev_attack\": ").append(entry.evYieldAttack).append(",\n")
  builder.append("\t\t\"ev_defense\": ").append(entry.evYieldDefense).append(",\n")
  builder.append("\t\t\"ev_speed\": ").append(entry.evYieldSpeed).append(",\n")
  builder.append("\t\t\"ev_sp_attack\": ").append(entry.evYieldSpAttack).append(",\n")
  builder.append("\t\t\"ev_sp_defense\": ").append(entry.evYieldSpDefense).append("\n\t},\n")
  builder.append("\t\"tiers\": [\n\t\t\"Untiered\"\n\t],\n")
  builder.append("\t\"held_items\": [],\n")
  builder.append("\t\"locations\": []\n")
  builder.append("}")
  return builder.toString()
}

private fun moveRow(id: Int, name: String, type: String, level: Int): String =
    "\t\t{\n\t\t\t\"id\": " +
        id +
        ",\n\t\t\t\"name\": " +
        quote(name) +
        ",\n\t\t\t\"type\": " +
        quote(type) +
        ",\n\t\t\t\"level\": " +
        level +
        "\n\t\t}"

private fun quote(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private fun titleCase(symbol: String): String =
    symbol.split('_').joinToString(" ") { word ->
      word.lowercase().replaceFirstChar { it.uppercase() }
    }

/** GrowthRate enum order, which is also the dump's exp_type numbering. */
private val GROWTH_ORDER =
    listOf("MEDIUM_FAST", "ERRATIC", "FLUCTUATING", "MEDIUM_SLOW", "FAST", "SLOW")

/** The dump's own spelling, "Fluctating" included. */
private val GROWTH_NAMES =
    listOf("Medium Fast", "Erratic", "Fluctating", "Medium Slow", "Fast", "Slow")

/** Server egg-group name to the dump's vocabulary - the reverse of RetailMonsterData's map. */
private val DUMP_EGG_GROUPS =
    mapOf(
        "MONSTER" to "monster",
        "GRASS" to "plant",
        "WATER_1" to "water a",
        "WATER_2" to "water b",
        "WATER_3" to "water c",
        "BUG" to "bug",
        "FLYING" to "flying",
        "FIELD" to "field",
        "FAIRY" to "fairy",
        "HUMAN_LIKE" to "humanoid",
        "MINERAL" to "mineral",
        "AMORPHOUS" to "chaos",
        "DITTO" to "ditto",
        "DRAGON" to "dragon",
        "NO_EGGS_DISCOVERED" to "cannot breed",
    )

/** Evolution method ordinal to the dump's type word, as far as its vocabulary reaches. */
private val EVO_METHODS =
    listOf(
        "LEVEL",
        "HAPPINESS",
        "HAPPINESS_DAY",
        "HAPPINESS_NIGHT",
        "LEVEL",
        "TRADE",
        "TRADE_ITEM",
        "ITEM",
        "ITEM",
        "LEVEL_ATK_GT_DEF",
        "LEVEL_ATK_EQ_DEF",
        "LEVEL_ATK_LT_DEF",
        "LEVEL_SILCOON",
        "LEVEL_CASCOON",
        "LEVEL_NINJASK",
        "LEVEL_SHEDINJA",
        "BEAUTY",
        "ITEM_MALE",
        "ITEM_FEMALE",
        "LEVEL_MALE",
        "LEVEL_FEMALE",
    )
