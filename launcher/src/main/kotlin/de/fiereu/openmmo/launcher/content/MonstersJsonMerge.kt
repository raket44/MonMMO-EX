@file:JvmName("MonstersJsonMerge")

package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import java.nio.file.Files
import java.nio.file.Path

/**
 * Merges the Expansion's species into the client's dex dump, writing entries in the dump's exact
 * schema and vocabulary.
 *
 * Operator-directed: this file governs encounters, dex population and evolution rules, so every
 * field follows the retail convention rather than a plausible-looking approximation. The
 * conventions below were read off the retail entries and the client itself:
 * - abilities are ALWAYS three, `[primary, secondary, hidden]`, with a missing slot written as the
 *   client's own placeholder `{"id": 0, "name": "--"}` and a missing hidden ability repeating the
 *   primary the way retail does.
 * - evolution `type` words are the CLIENT's own method enum (f/kx, decompiled: 0 BREEDING_ONLY, 1
 *   HAPPINESS ... 4 LEVEL ... 8 ITEM ... 27 LEVEL_LOCATION_3), confirmed against a live runtime
 *   dump (Eevee's ITEM#8 and LEVEL_LOCATION_2#26 entries).
 * - item-based evolutions carry the CLIENT item id in `val` (the +5000 shift the ROM loader
 *   applies) plus an `item_name`, exactly as retail's Thunderstone 5083 rows do; every other method
 *   puts its level or parameter there.
 * - `held_items` are client item ids with names, resolved from the Expansion's own common/rare item
 *   symbols through the client's dumped item table.
 *
 * The base dump is never edited: the retail reference is read, the Expansion entries are appended,
 * and the result is written to the output path, so the merge is repeatable and diffable.
 *
 * Usage: <retail-dump.json> <expansion-root> <output.json>
 */
fun main(args: Array<String>) {
  require(args.size >= 3) { "Usage: <retail-dump.json> <expansion-root> <output.json>" }
  val source = Path.of(args[0])
  val expansionRoot = Path.of(args[1])
  val output = Path.of(args[2])
  require(Files.isRegularFile(source)) { "Retail dex dump not found: " + source }
  require(Files.isDirectory(expansionRoot)) { "Expansion root not found: " + expansionRoot }

  val text = Files.readString(source)
  // A species entry is a line indented by exactly ONE tab: the nested move and ability objects
  // carry id and name too, and matching those made every move id look like a taken species id,
  // which silently skipped real species whose wire ids happened to equal a move number.
  val speciesEntries =
      Regex("(?m)^\\t\"id\": (\\d+),\\R\\t\"name\": \"([^\"]+)\"").findAll(text).associate {
        it.groupValues[1].toInt() to it.groupValues[2]
      }
  val existingIds = speciesEntries.keys

  val moveIds = MoveText.ids(expansionRoot)
  val moveNames = MoveText.parse(expansionRoot, moveIds).associate { it.id to it.name }
  val evolutions = ExpansionEvolutions.parse(expansionRoot)
  val learnsets = PorymovesLearnsets.parse(expansionRoot, moveIds)
  val catalog = ExpansionSpeciesRegistry().all()
  val wireBySymbol =
      catalog.filter { it.clientWireId != null }.associate { it.symbol to it.clientWireId!! }
  val nameBySymbol = catalog.associate { it.symbol to it.displayName }
  val formsByBase = catalog.filter { it.clientWireId != null }.groupBy { it.baseSpeciesStableId }
  val items = ItemNames(expansionRoot)

  // A catalogue species whose wire id is already taken by a DIFFERENT client record would render
  // as that record: the client keeps 650-667 for retail form records (Deoxys, Rotom, Castform)
  // and 1000-1052 for its own event species. Every such overlap is reported rather than skipped
  // quietly - this file governs the dex, so a silent collision is a wrong species on screen.
  val retailNames = speciesEntries
  val collisions =
      catalog
          .filter { it.clientWireId != null && it.clientWireId in existingIds }
          .mapNotNull { entry ->
            val holder = retailNames[entry.clientWireId] ?: return@mapNotNull null
            if (holder.equals(entry.displayName, ignoreCase = true)) null
            else "" + entry.clientWireId + " " + entry.displayName + " -> holds " + holder
          }
  if (collisions.isNotEmpty()) {
    println("[monsters-merge] WIRE ID COLLISIONS: " + collisions.size)
    collisions.take(30).forEach { println("[monsters-merge]   " + it) }
  }
  val additions =
      catalog
          .filter { it.clientWireId != null && it.clientWireId !in existingIds }
          .sortedBy { it.clientWireId }
          .map { entry ->
            entryJson(
                entry,
                moveNames,
                evolutions,
                learnsets,
                wireBySymbol,
                nameBySymbol,
                formsByBase,
                items,
            )
          }

  if (additions.isEmpty()) {
    println("[monsters-merge] nothing to add: every catalogue species is already in the dump")
    return
  }
  val closing = text.lastIndexOf(']')
  require(closing > 0) { "Dex dump is not a JSON array" }
  val head = text.substring(0, closing).trimEnd().trimEnd(',')
  Files.createDirectories(output.parent)
  Files.writeString(output, head + ",\n" + additions.joinToString(",\n") + "\n]\n")
  println(
      "[monsters-merge] " +
          existingIds.size +
          " retail + " +
          additions.size +
          " Expansion species -> " +
          output)
}

/**
 * Expansion item symbol to the client's own item id and name.
 *
 * The client's live item table was dumped to a calibration file, so a match is by NAME between the
 * Expansion's item and the client's - the same technique the item import uses. Items the client
 * never had resolve through the import plan's assigned ids instead, so an Expansion-only evolution
 * stone still names a real item.
 */
internal class ItemNames(expansionRoot: Path) {
  private val byNormalizedName = linkedMapOf<String, Pair<Int, String>>()
  private val expansionNames = ExpansionItems.parse(expansionRoot).mapValues { it.value.name }

  init {
    javaClass.getResourceAsStream("/monmmo/item-names.csv")?.bufferedReader()?.useLines { lines ->
      lines.forEach { line ->
        val id = line.substringBefore(';').toIntOrNull() ?: return@forEach
        val name = line.substringAfter(';', "")
        if (name.isNotBlank() && id > 0) {
          byNormalizedName.putIfAbsent(normalize(name), id to name)
        }
      }
    }
    EvoItemPlan.ITEMS.forEachIndexed { index, (_, name) ->
      byNormalizedName.putIfAbsent(normalize(name), (EvoItemPlan.FIRST_ITEM_ID + index) to name)
    }
    ItemImportPlan.compute(expansionRoot).forEach { item ->
      byNormalizedName.putIfAbsent(normalize(item.name), item.itemId to item.name)
    }
  }

  /** The client id and name for an Expansion item symbol, or null when it maps to nothing. */
  fun bySymbol(symbol: String): Pair<Int, String>? {
    if (symbol.isBlank() || symbol == "ITEM_NONE") return null
    val name = expansionNames[symbol] ?: return null
    return byNormalizedName[normalize(name)]
  }

  /** The client name for an id the client already knows, for evolution rows. */
  fun byId(id: Int): String? = byNormalizedName.values.firstOrNull { it.first == id }?.second

  private fun normalize(value: String): String = value.lowercase().filter { it.isLetterOrDigit() }
}

private fun familyMoves(table: Map<String, List<Int>>, entry: ExpansionSpeciesDef): List<Int> {
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
    formsByBase: Map<String, List<ExpansionSpeciesDef>>,
    items: ItemNames,
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

  // Retail writes three ability slots always: primary, secondary (the primary again when there is
  // only one) and hidden, with a missing hidden written as the client's own "--" placeholder.
  val primaryId = entry.abilityIds.getOrElse(0) { 0 }
  val primaryName = abilityName(entry.abilitySymbols.getOrElse(0) { "" })
  val abilityRows =
      (0..2).joinToString(",\n") { slot ->
        val rawId = entry.abilityIds.getOrElse(slot) { 0 }
        val rawName = abilityName(entry.abilitySymbols.getOrElse(slot) { "" })
        val id = if (slot == 1 && rawId == 0) primaryId else rawId
        val name = if (slot == 1 && rawId == 0) primaryName else rawName
        val finalId = if (name == NO_ABILITY) 0 else id
        abilityRow(finalId, name)
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
            val method = EVO_METHODS.getOrElse(evo.method) { "LEVEL" }
            // Item-based methods carry the CLIENT item id, the +5000 shift the ROM loader applies
            // to their parameter - the same rows retail writes as Thunderstone 5083.
            val itemMethod = evo.method in ITEM_EVO_METHODS
            // A species parameter (the party member Mantyke needs) names the client's id for it.
            val value =
                if (itemMethod) evo.param + CLIENT_ITEM_SHIFT
                else evo.speciesParamSymbol?.let { wireBySymbol["SPECIES_$it"] } ?: evo.param
            val itemName = if (itemMethod) items.byId(value) else null
            val builder = StringBuilder()
            builder.append("\t\t{\n\t\t\t\"id\": ").append(targetWire)
            builder.append(",\n\t\t\t\"name\": ").append(quote(targetName))
            builder.append(",\n\t\t\t\"type\": ").append(quote(method))
            builder.append(",\n\t\t\t\"val\": ").append(value)
            if (itemName != null) {
              builder.append(",\n\t\t\t\"item_name\": ").append(quote(itemName))
            }
            builder.append("\n\t\t}")
            builder.toString()
          }
          .joinToString(",\n")

  val typeRows =
      entry.typeSymbols
          .map { it.removePrefix("TYPE_") }
          .distinct()
          .joinToString(",\n") { "\t\t" + quote(it) }

  // Held items come from the Expansion's own common and rare item slots, named by the client.
  val heldRows =
      listOfNotNull(items.bySymbol(entry.itemCommonSymbol), items.bySymbol(entry.itemRareSymbol))
          .distinctBy { it.first }
          .joinToString(",\n") { (id, name) ->
            "\t\t{\n\t\t\t\"id\": " + id + ",\n\t\t\t\"name\": " + quote(name) + "\n\t\t}"
          }

  // Retail lists a species' whole form family in its entry; forms also stand as entries of their
  // own, which is how the dump carries both the base and its Alolan or Mega records.
  val family = formsByBase[entry.baseSpeciesStableId] ?: listOf(entry)
  val formRows =
      family
          .sortedBy { it.clientWireId }
          .mapIndexed { index, form ->
            val builder = StringBuilder()
            builder.append("\t\t{\n\t\t\t\"form_id\": ").append(index)
            builder.append(",\n\t\t\t\"id\": ").append(form.clientWireId)
            builder.append(",\n\t\t\t\"name\": ").append(quote(form.displayName))
            builder.append(",\n\t\t\t\"is_costume\": false")
            builder.append(",\n\t\t\t\"is_released\": ").append(form.clientContentCompatible)
            builder.append("\n\t\t}")
            builder.toString()
          }

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
  builder.append("\t\"obtainable\": ").append(entry.clientContentCompatible).append(",\n")
  builder.append("\t\"gender_ratio\": ").append(entry.genderRatio).append(",\n")
  builder.append("\t\"height\": ").append(entry.height).append(",\n")
  builder.append("\t\"weight\": ").append(entry.weight).append(",\n")
  builder.append("\t\"category\": ").append(quote(entry.categoryName)).append(",\n")
  builder.append("\t\"egg_cycles\": ").append(entry.eggCycles).append(",\n")
  builder.append("\t\"friendship\": ").append(entry.friendship).append(",\n")
  builder.append("\t\"egg_groups\": [\n").append(eggRows).append("\n\t],\n")
  builder.append("\t\"abilities\": [\n").append(abilityRows).append("\n\t],\n")
  builder.append("\t\"forms\": [\n").append(formRows.joinToString(",\n")).append("\n\t],\n")
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
  builder
      .append("\t\"held_items\": [")
      .append(if (heldRows.isEmpty()) "" else "\n" + heldRows + "\n\t")
  builder.append("],\n")
  builder.append("\t\"locations\": []\n")
  builder.append("}")
  return builder.toString()
}

private fun abilityRow(id: Int, name: String): String =
    "\t\t{\n\t\t\t\"id\": " + id + ",\n\t\t\t\"name\": " + quote(name) + "\n\t\t}"

private fun abilityName(symbol: String): String {
  val bare = symbol.removePrefix("ABILITY_")
  if (bare.isBlank() || bare == "NONE") return NO_ABILITY
  return titleCase(bare)
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

/** The client's own placeholder for an ability slot a species does not fill. */
private const val NO_ABILITY = "--"

/** The ROM loader shifts item parameters into the client's item space by this much. */
private const val CLIENT_ITEM_SHIFT = 5000

/** Evolution methods whose parameter is an item id (DexPatch applies the same shift to these). */
private val ITEM_EVO_METHODS = setOf(6, 8, 17, 18, 19, 20)

/** GrowthRate enum order, which is also the dump's exp_type numbering. */
internal val GROWTH_ORDER =
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

/**
 * The CLIENT's evolution method enum (f/kx), in ordinal order - the same words the retail dump
 * uses, verified against a live runtime dump of Eevee's entries.
 */
private val EVO_METHODS =
    listOf(
        "BREEDING_ONLY",
        "HAPPINESS",
        "HAPPINESS_DAY",
        "HAPPINESS_NIGHT",
        "LEVEL",
        "TRADE",
        "TRADE_WITH_ITEM",
        "TRADE_FOR_OPPOSITE",
        "ITEM",
        "ATK_GREATER_THAN_DEF",
        "ATK_EQUAL_TO_DEF",
        "ATK_LESS_THAN_DEF",
        "PERSONALITY_HIGH",
        "PERSONALITY_LOW",
        "ALLOW_MONSTER_CREATION",
        "CREATE_EXTRA_MONSTER",
        "MAX_BEAUTY",
        "ITEM_MALE",
        "ITEM_FEMALE",
        "LEVEL_ITEM_DAY",
        "LEVEL_ITEM_NIGHT",
        "LEVEL_WITH_SKILL",
        "LEVEL_WITH_MONSTER",
        "LEVEL_MALE",
        "LEVEL_FEMALE",
        "LEVEL_LOCATION_1",
        "LEVEL_LOCATION_2",
        "LEVEL_LOCATION_3",
    )
