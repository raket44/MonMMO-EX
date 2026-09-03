package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * The operator's Gen 5-style battle sprite packs (reference/sprite-packs/gen5-style), which replace
 * the Expansion's GBA-style art for every species and form they cover.
 *
 * Layout, as shipped: one folder per generation holding 96x96 indexed PNGs named
 * `<dex>[f|m][_<form>][s][b].png` - `s` shiny, `b` back, `f`/`m` a gender variant, `_N` the Nth
 * form in the games' own order (`_1` is the base form again). The `mega-primal` folder keeps
 * `<dex>_2` (and `_3` for the X/Y pairs); `icons`, `gmax`, `Pikachu` and `regional variants` are
 * not consumed (icons are the wrong shape for the client, and the client stages neither Gigantamax
 * nor the regional forms of retail species yet). Every file is a single still: the packs carry no
 * APNG frames, so the staging's idle bounce supplies the motion.
 *
 * Forms resolve through the decomp's form tables: a form symbol's position in its family's table
 * is the pack's `_N`, accepted only when the pack holds exactly as many `_N` files as the table has
 * entries - the families whose pack numbering is known to differ get [OVERRIDES] instead, and
 * anything else keeps the Expansion's art.
 */
class Gen5StyleSpritePack(private val root: Path, expansionRoot: Path) {

  data class Sprites(
      val front: Path,
      val frontShiny: Path?,
      val back: Path?,
      val backShiny: Path?,
  )

  private data class Key(val dex: Int, val gender: String, val form: String, val shiny: Boolean, val back: Boolean)

  private val files = linkedMapOf<Key, Path>()
  private val megaFiles = linkedMapOf<Key, Path>()
  /** dex -> the `_N` indices present at the top level. */
  private val formIndices = mutableMapOf<Int, MutableSet<Int>>()
  /** Form symbol (no SPECIES_ prefix) -> base symbol and 1-based position in its family table. */
  private val formTables: Map<String, Pair<String, Int>>
  /** Base symbol -> number of entries in its family table. */
  private val tableSizes: Map<String, Int>

  val available: Boolean
    get() = files.isNotEmpty()

  init {
    if (Files.isDirectory(root)) {
      Files.walk(root).use { paths ->
        paths.filter { it.name.endsWith(".png") }.forEach { path -> index(path) }
      }
    }
    val (tables, sizes) = parseFormTables(expansionRoot)
    formTables = tables
    tableSizes = sizes
  }

  private fun index(path: Path) {
    val folder = path.parent.name.lowercase()
    val topLevel = folder.startsWith("gen")
    val mega = folder == "mega-primal"
    if (!topLevel && !mega) return
    val match = FILE_NAME.matchEntire(path.name.removeSuffix(".png")) ?: return
    val (dex, gender, shinyBefore, form, shinyAfter, back) = match.destructured
    val key =
        Key(dex.toInt(), gender, form, shinyBefore.isNotEmpty() || shinyAfter.isNotEmpty(), back.isNotEmpty())
    if (mega) {
      megaFiles.putIfAbsent(key, path)
    } else {
      files.putIfAbsent(key, path)
      form.toIntOrNull()?.let { formIndices.getOrPut(key.dex) { mutableSetOf() }.add(it) }
    }
  }

  /** National Dex number of [entry]'s family - the number the pack's file names use. */
  fun nationalDex(entry: ExpansionSpeciesDef, bySymbol: Map<String, ExpansionSpeciesDef>): Int? {
    val symbol = entry.symbol.removePrefix("SPECIES_")
    val base = formTables[symbol]?.first ?: symbol.substringBefore("_MEGA")
    return dexOf(base, bySymbol) ?: dexOf(symbol, bySymbol)
  }

  /** The pack's sprites for [entry], or null when the pack does not cover it. */
  fun resolve(entry: ExpansionSpeciesDef, bySymbol: Map<String, ExpansionSpeciesDef>): Sprites? {
    val symbol = entry.symbol.removePrefix("SPECIES_")
    if (symbol.endsWith("_MEGA") || symbol.endsWith("_MEGA_X") || symbol.endsWith("_MEGA_Y")) {
      val base = symbol.substringBefore("_MEGA")
      val dex = dexOf(base, bySymbol) ?: return null
      val form = if (symbol.endsWith("_MEGA_Y")) "3" else "2"
      return lookup(megaFiles, dex, "", form)
    }
    val (base, position) = formTables[symbol] ?: (symbol to 1)
    val dex = dexOf(base, bySymbol) ?: return null
    // A gender pair (MEOWSTIC_M/_F, INDEEDEE_M/_F) is the pack's f/m file pair, not a numbered form.
    val genderVariant = symbol != base && symbol.endsWith("_F")
    val override = OVERRIDES[symbol]
    val form: String =
        when {
          genderVariant -> ""
          override != null -> override
          position == 1 -> ""
          else -> {
            // The table's position must exist in the pack and lie inside the table; families whose
            // pack numbering skips an entry are pinned in OVERRIDES instead.
            val present = formIndices[dex].orEmpty()
            val expected = tableSizes[base] ?: return null
            if (position <= expected && position in present) position.toString() else return null
          }
        }
    val gender = if (genderVariant) "f" else ""
    return lookup(files, dex, gender, form)
        ?: if (form.isEmpty()) lookup(files, dex, gender, "1") else null
  }

  private fun lookup(source: Map<Key, Path>, dex: Int, gender: String, form: String): Sprites? {
    fun find(shiny: Boolean, back: Boolean): Path? =
        source[Key(dex, gender, form, shiny, back)]
            ?: if (gender.isEmpty()) source[Key(dex, "m", form, shiny, back)] else null
    val front = find(shiny = false, back = false) ?: return null
    return Sprites(
        front = front,
        frontShiny = find(shiny = true, back = false),
        back = find(shiny = false, back = true),
        backShiny = find(shiny = true, back = true),
    )
  }

  /**
   * National Dex number: the Expansion numbers its base species by it. A family whose table leads
   * with a suffixed symbol (MEOWSTIC_M, INDEEDEE_M, TOXTRICITY_AMPED...) is walked back one suffix
   * at a time until a base-range id turns up.
   */
  private fun dexOf(baseSymbol: String, bySymbol: Map<String, ExpansionSpeciesDef>): Int? {
    var candidate = baseSymbol
    while (candidate.isNotEmpty()) {
      bySymbol["SPECIES_$candidate"]?.originalId?.takeIf { it in 1..MAX_NATIONAL_DEX }?.let {
        return it
      }
      val cut = candidate.lastIndexOf('_')
      if (cut <= 0) return null
      candidate = candidate.substring(0, cut)
    }
    return null
  }

  private fun parseFormTables(expansionRoot: Path): Pair<Map<String, Pair<String, Int>>, Map<String, Int>> {
    val tables = mutableMapOf<String, Pair<String, Int>>()
    val sizes = mutableMapOf<String, Int>()
    val file = expansionRoot.resolve("src/data/pokemon/form_species_tables.h")
    if (!Files.isRegularFile(file)) return tables to sizes
    var members = mutableListOf<String>()
    var inTable = false
    Files.readAllLines(file).forEach { raw ->
      val line = raw.trim()
      when {
        line.endsWith("FormSpeciesIdTable[] = {") -> {
          inTable = true
          members = mutableListOf()
        }
        inTable && line.startsWith("SPECIES_") -> members.add(line.removePrefix("SPECIES_").removeSuffix(","))
        inTable && line.startsWith("FORM_SPECIES_END") -> {
          inTable = false
          val base = members.firstOrNull() ?: return@forEach
          sizes[base] = members.size
          members.forEachIndexed { index, member -> tables[member] = base to index + 1 }
        }
      }
    }
    return tables to sizes
  }

  private companion object {
    val FILE_NAME = Regex("""^(\d+)([fm]?)(s?)(?:_([A-Za-z0-9]+?))?(s?)(b?)$""")
    const val MAX_NATIONAL_DEX = 1025

    /**
     * Families whose pack numbering is not the table order. Values are the pack's form token ("" =
     * the plain base file).
     */
    val OVERRIDES =
        mapOf(
            "GRENINJA_BATTLE_BOND" to "",
            "GRENINJA_ASH" to "2",
            "ZYGARDE_10_AURA_BREAK" to "2",
            "ZYGARDE_10_POWER_CONSTRUCT" to "2",
            "ZYGARDE_50_POWER_CONSTRUCT" to "",
            "ZYGARDE_COMPLETE" to "3",
            "MINIOR_METEOR_ORANGE" to "",
            "MINIOR_METEOR_YELLOW" to "",
            "MINIOR_METEOR_GREEN" to "",
            "MINIOR_METEOR_BLUE" to "",
            "MINIOR_METEOR_INDIGO" to "",
            "MINIOR_METEOR_VIOLET" to "",
            "MINIOR_CORE_RED" to "2",
            "MINIOR_CORE_ORANGE" to "3",
            "MINIOR_CORE_YELLOW" to "4",
            "MINIOR_CORE_GREEN" to "5",
            "MINIOR_CORE_BLUE" to "6",
            "MINIOR_CORE_INDIGO" to "7",
            "MINIOR_CORE_VIOLET" to "8",
            // Same art as the base form in the games.
            "ROCKRUFF_OWN_TEMPO" to "",
            "SINISTEA_ANTIQUE" to "",
            "POLTEAGEIST_ANTIQUE" to "",
            "PUMPKABOO_SMALL" to "",
            "PUMPKABOO_LARGE" to "",
            "PUMPKABOO_SUPER" to "",
            "GOURGEIST_SMALL" to "",
            "GOURGEIST_LARGE" to "",
            "GOURGEIST_SUPER" to "",
        )
  }
}
