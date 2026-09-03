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
 * form in the games' own order (`_1` is the base form again). `mega-primal` keeps `<dex>_2` (and
 * `_3` for the X/Y pairs), `gmax` keeps `<dex>_g` (`_g1`/`_g2` for Urshifu, `841-842_g` shared by
 * Flapple and Appletun), and `regional variants` holds one file set per species - Alolan forms in
 * the Gen 7 pack, Galarian in the Gen 8 pack - under whatever `_N` that species' regional form has.
 * `icons` and `Pikachu` are not consumed. Every file is a single still: the packs carry no APNG
 * frames, so the staging's idle bounce supplies the motion.
 *
 * Forms resolve through the decomp's form tables: a form symbol's position in its family's table
 * is the pack's `_N`, accepted when that file exists; families whose pack numbering skips an entry
 * are pinned in [OVERRIDES], families whose forms share one drawing in the games map to the base
 * file ([SAME_ART_FAMILIES]), and Totem forms resolve as the form they enlarge.
 */
class Gen5StyleSpritePack(private val root: Path, expansionRoot: Path) {

  data class Sprites(
      val front: Path,
      val frontShiny: Path?,
      val back: Path?,
      val backShiny: Path?,
  )

  private data class Key(
      val dex: Int,
      val gender: String,
      val form: String,
      val shiny: Boolean,
      val back: Boolean,
  )

  private val files = linkedMapOf<Key, Path>()
  private val megaFiles = linkedMapOf<Key, Path>()
  private val gmaxFiles = linkedMapOf<Key, Path>()
  /** Region suffix (ALOLA/GALAR) -> files; the form token varies per species there. */
  private val regionalFiles = mapOf("ALOLA" to linkedMapOf<Key, Path>(), "GALAR" to linkedMapOf<Key, Path>())
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
    val generation = path.parent.parent?.name?.lowercase().orEmpty()
    val stem = path.name.removeSuffix(".png")
    val target: MutableMap<Key, Path> =
        when {
          folder.startsWith("gen") -> files
          folder == "mega-primal" -> megaFiles
          folder == "gmax" -> gmaxFiles
          folder == "regional variants" && generation == "gen7" -> regionalFiles.getValue("ALOLA")
          folder == "regional variants" && generation == "gen8" -> regionalFiles.getValue("GALAR")
          else -> return
        }
    // A file two species share ("841-842_g") is registered for both.
    val shared = SHARED_NAME.matchEntire(stem)
    val stems =
        if (shared == null) listOf(stem)
        else listOf(shared.groupValues[1] + shared.groupValues[3], shared.groupValues[2] + shared.groupValues[3])
    for (name in stems) {
      val match = FILE_NAME.matchEntire(name) ?: continue
      val (dex, gender, shinyBefore, form, shinyAfter, back) = match.destructured
      val key =
          Key(
              dex.toInt(),
              gender,
              form,
              shinyBefore.isNotEmpty() || shinyAfter.isNotEmpty(),
              back.isNotEmpty(),
          )
      target.putIfAbsent(key, path)
      if (target === files) {
        form.toIntOrNull()?.let { formIndices.getOrPut(key.dex) { mutableSetOf() }.add(it) }
      }
    }
  }

  /** National Dex number of [entry]'s family - the number the pack's file names use. */
  fun nationalDex(entry: ExpansionSpeciesDef, bySymbol: Map<String, ExpansionSpeciesDef>): Int? {
    val symbol = entry.symbol.removePrefix("SPECIES_").replace("_TOTEM", "")
    val base = formTables[symbol]?.first ?: symbol
    return dexOf(base, bySymbol) ?: dexOf(symbol, bySymbol)
  }

  /** The pack's sprites for [entry], or null when the pack does not cover it. */
  fun resolve(entry: ExpansionSpeciesDef, bySymbol: Map<String, ExpansionSpeciesDef>): Sprites? =
      resolveSymbol(entry.symbol.removePrefix("SPECIES_"), bySymbol)

  private fun resolveSymbol(symbol: String, bySymbol: Map<String, ExpansionSpeciesDef>): Sprites? {
    // Totems are the same drawing scaled up in the games.
    if ("_TOTEM" in symbol) return resolveSymbol(symbol.replace("_TOTEM", ""), bySymbol)
    if (symbol.endsWith("_GMAX")) {
      val dex = dexOf(symbol.removeSuffix("_GMAX"), bySymbol) ?: return null
      val token =
          when {
            symbol.startsWith("URSHIFU_SINGLE") -> "g1"
            symbol.startsWith("URSHIFU_RAPID") -> "g2"
            else -> "g"
          }
      return lookup(gmaxFiles, dex, "", token)
    }
    if (symbol.endsWith("_MEGA") || symbol.endsWith("_MEGA_X") || symbol.endsWith("_MEGA_Y")) {
      val dex = dexOf(symbol.substringBefore("_MEGA"), bySymbol) ?: return null
      return lookup(megaFiles, dex, "", if (symbol.endsWith("_MEGA_Y")) "3" else "2")
    }
    REGIONS.firstOrNull { "_$it" in symbol }?.let { region ->
      val dex = dexOf(symbol.substringBefore("_$region"), bySymbol) ?: return null
      val pool = regionalFiles.getValue(region)
      val tokens = pool.keys.filter { it.dex == dex }.map { it.form }.distinct().sorted()
      if (tokens.isEmpty()) return null
      // Darmanitan's Galarian pair: standard first, Zen second.
      val token = if ("ZEN" in symbol) tokens.last() else tokens.first()
      return lookup(pool, dex, "", token)
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
          base.substringBefore('_') in SAME_ART_FAMILIES -> ""
          position == 1 -> ""
          else -> {
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
      bySymbol["SPECIES_$candidate"]?.nationalDexId?.takeIf { it in 1..MAX_NATIONAL_DEX }?.let {
        return it
      }
      val cut = candidate.lastIndexOf('_')
      if (cut <= 0) return null
      candidate = candidate.substring(0, cut)
    }
    return null
  }

  private fun parseFormTables(
      expansionRoot: Path
  ): Pair<Map<String, Pair<String, Int>>, Map<String, Int>> {
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
        inTable && line.startsWith("SPECIES_") ->
            members.add(line.removePrefix("SPECIES_").removeSuffix(","))
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
    val SHARED_NAME = Regex("""^(\d+)-(\d+)(_.*)$""")
    val REGIONS = listOf("ALOLA", "GALAR")
    const val MAX_NATIONAL_DEX = 1025

    /** Families whose every form is drawn with the base sprite in the games. */
    val SAME_ART_FAMILIES =
        setOf("SCATTERBUG", "SPEWPA", "PUMPKABOO", "GOURGEIST", "ROCKRUFF", "SINISTEA", "POLTEAGEIST")

    /**
     * Forms whose pack numbering is not the table order. Values are the pack's form token ("" =
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
        )
  }
}
