package de.fiereu.openmmo.codegen.pokemon.expansion

import de.fiereu.openmmo.codegen.move.ExpansionConfig
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.util.Base64
import java.util.zip.GZIPOutputStream

data class ParsedExpansionSpecies(
    val stableId: String,
    val originalId: Int,
    val serverId: Int,
    val symbol: String,
    val displayName: String,
    val nationalDexId: Int?,
    val baseSpeciesStableId: String,
    val isForm: Boolean,
    val fields: Map<String, String>,
    val typeSymbols: List<String>,
    val abilitySymbols: List<String>,
    val abilityIds: List<Int>,
    val eggGroupSymbols: List<String>,
    val levelUpLearnset: List<ParsedExpansionLevelUpMove>,
    val evolutionTargetSymbols: List<String>,
    val rarity: Int,
    val categoryName: String,
    val height: Int,
    val weight: Int,
    val assets: ParsedExpansionAssets,
    val clientWireId: Int?,
)

data class ParsedExpansionLevelUpMove(
    val level: Int,
    val moveSymbol: String,
    val originalMoveId: Int,
)

data class ParsedExpansionAssets(
    val partyIcon: Boolean,
    val frontSprite: Boolean,
    val backSprite: Boolean,
    val cry: Boolean,
    val follower: Boolean,
    /** Expansion-relative source paths, blank when the file is absent from this tree. */
    val frontPicPath: String = "",
    val backPicPath: String = "",
    val iconPath: String = "",
    val normalPalettePath: String = "",
    val shinyPalettePath: String = "",
    val cryPath: String = "",
    val iconPalIndex: Int = 0,
)

class ExpansionSpeciesGenerator(private val rootDir: File) {
  fun parseAll(): List<ParsedExpansionSpecies> {
    require(rootDir.resolve("include/config/species_enabled.h").isFile) {
      "Not a configured pokeemerald-expansion tree: $rootDir"
    }
    val speciesIds = parseEnum(preprocess("include/constants/species.h"), "SPECIES_")
    val nationalDex = parseEnum(preprocess("include/constants/pokedex.h"), "NATIONAL_DEX_")
    val abilityIds = parseEnum(preprocess("include/constants/abilities.h"), "ABILITY_")
    val moveIds = parseEnum(preprocess("include/constants/moves.h"), "MOVE_")
    val learnsets = parseLevelUpLearnsets(configuredLearnsetFile(), moveIds)
    val graphics =
        parseGraphicsPaths(preprocess("src/data/graphics/pokemon.h")).filterValues { present(it) }
    val cries = parseCryPaths(rootDir.resolve(CRY_DATA).readText()).filterValues { present(it) }
    return parseSpeciesInfo(
        preprocess("src/data/pokemon/species_info.h"),
        speciesIds,
        nationalDex,
        abilityIds,
        learnsets,
        graphics,
        cries,
    )
  }

  internal fun parseSpeciesInfo(
      text: String,
      speciesIds: Map<String, Int>,
      nationalDex: Map<String, Int>,
      abilityIds: Map<String, Int> = emptyMap(),
      learnsets: Map<String, List<ParsedExpansionLevelUpMove>> = emptyMap(),
      graphics: Map<String, String> = emptyMap(),
      cries: Map<String, String> = emptyMap(),
  ): List<ParsedExpansionSpecies> {
    val raw =
        ENTRY_START.findAll(text)
            .mapNotNull { match ->
              val symbol = match.groupValues[1]
              if (symbol == "SPECIES_NONE" || symbol == "SPECIES_EGG") return@mapNotNull null
              val originalId = speciesIds[symbol] ?: return@mapNotNull null
              val open = text.indexOf('{', match.range.first)
              val close = matchingBrace(text, open)
              val fields = parseFields(text.substring(open + 1, close))
              if (!fields.containsKey("baseHP")) return@mapNotNull null
              val nationalSymbol = SYMBOL.find(fields["natDexNum"].orEmpty())?.value
              RawSpecies(
                  symbol = symbol,
                  originalId = originalId,
                  nationalDexId = nationalSymbol?.let(nationalDex::get),
                  displayName =
                      DISPLAY_NAME.find(fields["speciesName"].orEmpty())?.groupValues?.get(1)
                          ?: symbol
                              .removePrefix("SPECIES_")
                              .replace('_', ' ')
                              .lowercase()
                              .replaceFirstChar(Char::uppercase),
                  fields = fields,
              )
            }
            .toList()

    val baseByNationalDex =
        raw.filter { it.nationalDexId != null }
            .groupBy { it.nationalDexId }
            .mapValues { (_, entries) ->
              entries.minWith(
                  compareBy<RawSpecies>({ !it.isCanonicalNationalSymbol() }, { it.originalId }))
            }

    // Base species take their National Dex number so client ids read in Dex order. Forms share a
    // Dex number with the species they belong to, so they get slots after the last Dex entry.
    val formOrdinals =
        raw.sortedBy { it.originalId }
            .filter { entry ->
              val base = entry.nationalDexId?.let(baseByNationalDex::get) ?: entry
              base.symbol != entry.symbol || entry.nationalDexId == null
            }
            .withIndex()
            .associate { (index, entry) -> entry.symbol to index }

    return raw.sortedBy { it.originalId }
        .map { entry ->
          val base = entry.nationalDexId?.let(baseByNationalDex::get) ?: entry
          val types = configuredPair(references(entry.fields["types"], "TYPE_"))
          val abilities = references(entry.fields["abilities"], "ABILITY_").take(3)
          val learnsetLabel = IDENTIFIER.find(entry.fields["levelUpLearnset"].orEmpty())?.value
          val eggGroups = configuredPair(references(entry.fields["eggGroups"], "EGG_GROUP_"))
          ParsedExpansionSpecies(
              stableId = stableId(entry.symbol),
              originalId = entry.originalId,
              serverId = EXPANSION_SERVER_ID_BASE + entry.originalId,
              symbol = entry.symbol,
              displayName = entry.displayName,
              nationalDexId = entry.nationalDexId,
              baseSpeciesStableId = stableId(base.symbol),
              isForm = base.symbol != entry.symbol,
              fields = entry.fields,
              typeSymbols = types,
              abilitySymbols = abilities,
              abilityIds = abilities.mapNotNull(abilityIds::get),
              eggGroupSymbols = eggGroups,
              levelUpLearnset = learnsetLabel?.let(learnsets::get).orEmpty(),
              evolutionTargetSymbols =
                  references(entry.fields["evolutions"], "SPECIES_").distinct(),
              rarity =
                  when {
                    entry.fields.containsKey("isMythical") -> RARITY_MYTHICAL
                    entry.fields.containsKey("isRestrictedLegendary") ||
                        entry.fields.containsKey("isSubLegendary") -> RARITY_LEGENDARY
                    else -> RARITY_NONE
                  },
              categoryName =
                  DISPLAY_NAME.find(entry.fields["categoryName"].orEmpty())
                      ?.groupValues
                      ?.get(1)
                      .orEmpty(),
              height = entry.fields["height"]?.trim()?.toIntOrNull() ?: 0,
              weight = entry.fields["weight"]?.trim()?.toIntOrNull() ?: 0,
              assets =
                  ParsedExpansionAssets(
                      partyIcon = "iconSprite" in entry.fields,
                      frontSprite = "frontPic" in entry.fields,
                      backSprite = "backPic" in entry.fields,
                      cry = "cryId" in entry.fields,
                      follower = "overworldData" in entry.fields,
                      frontPicPath = graphics.reference(entry.fields, "frontPic"),
                      backPicPath = graphics.reference(entry.fields, "backPic"),
                      iconPath = graphics.reference(entry.fields, "iconSprite"),
                      normalPalettePath = graphics.reference(entry.fields, "palette"),
                      shinyPalettePath = graphics.reference(entry.fields, "shinyPalette"),
                      cryPath = cryPath(cries, entry.fields),
                      iconPalIndex = entry.fields["iconPalIndex"]?.trim()?.toIntOrNull() ?: 0,
                  ),
              clientWireId =
                  clientWireId(entry, base.symbol == entry.symbol, formOrdinals[entry.symbol]),
          )
        }
  }

  /**
   * Maps every graphics symbol in the preprocessed pokemon.h to its source file. Preprocessing
   * matters: the same symbol is declared several times behind configuration guards, and only the
   * configured branch survives.
   */
  internal fun parseGraphicsPaths(text: String): Map<String, String> =
      GRAPHICS_ENTRY.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

  /**
   * Maps CRY_* constants to the playable .wav beside the .bin the ROM build embeds. The label and
   * the incbin sit on separate lines, so a label is carried until its sample is found.
   */
  internal fun parseCryPaths(text: String): Map<String, String> {
    val paths = linkedMapOf<String, String>()
    var pending: String? = null
    text.lineSequence().forEach { line ->
      CRY_LABEL.find(line)?.let { pending = crySymbol("CRY_" + it.groupValues[1]) }
      val symbol = pending ?: return@forEach
      val sample = INCBIN_PATH.find(line)?.groupValues?.get(1) ?: return@forEach
      paths.putIfAbsent(symbol, sample.removeSuffix(".bin") + ".wav")
      pending = null
    }
    return paths
  }

  /**
   * The decomp writes cry labels in CamelCase and cry constants in SNAKE_CASE, and the two disagree
   * about where the word boundaries fall: Cry_Porygon2 pairs with CRY_PORYGON2 while Cry_Zygarde50
   * pairs with CRY_ZYGARDE_50. Comparing without separators resolves every cry the tree defines
   * except CRY_EISCUE_NOICE_FACE, whose label is missing the trailing word upstream.
   */
  private fun crySymbol(name: String): String = name.replace("_", "").uppercase()

  private fun cryPath(cries: Map<String, String>, fields: Map<String, String>): String =
      IDENTIFIER.find(fields["cryId"].orEmpty())?.value?.let { cries[crySymbol(it)] }.orEmpty()

  private fun present(path: String): Boolean = rootDir.resolve(path).isFile

  /** Resolves a species_info field such as frontPic = gMonFrontPic_Tyrunt to its source file. */
  private fun Map<String, String>.reference(fields: Map<String, String>, field: String): String =
      IDENTIFIER.find(fields[field].orEmpty())?.value?.let(::get).orEmpty()

  internal fun parseLevelUpLearnsets(
      file: File,
      moveIds: Map<String, Int>,
  ): Map<String, List<ParsedExpansionLevelUpMove>> {
    val text = file.readText()
    return LEARNSET.findAll(text).associate { match ->
      val label = match.groupValues[1]
      val moves =
          LEVEL_UP_MOVE.findAll(match.groupValues[2])
              .mapNotNull { move ->
                val symbol = move.groupValues[2]
                moveIds[symbol]?.let {
                  ParsedExpansionLevelUpMove(move.groupValues[1].toInt(), symbol, it)
                }
              }
              .toList()
      label to moves
    }
  }

  private fun configuredLearnsetFile(): File {
    val config = rootDir.resolve("include/config/pokemon.h").readText()
    val configured =
        Regex("""(?m)^\s*#define\s+P_LVL_UP_LEARNSETS\s+(GEN_LATEST|GEN_(\d+))""")
            .find(config)
            ?.groupValues ?: error("P_LVL_UP_LEARNSETS is not defined in include/config/pokemon.h")
    val directory = rootDir.resolve("src/data/pokemon/level_up_learnsets")
    val generation =
        configured[2].toIntOrNull()
            ?: directory
                .listFiles()
                .orEmpty()
                .mapNotNull {
                  Regex("""gen_(\d+)\.h""").matchEntire(it.name)?.groupValues?.get(1)?.toInt()
                }
                .maxOrNull()
            ?: error("No Expansion level-up learnset files found in $directory")
    return directory.resolve("gen_$generation.h").also {
      require(it.isFile) { "Configured Expansion learnset does not exist: $it" }
    }
  }

  private fun preprocess(path: String): String {
    val cpp = findCpp()
    val command =
        listOf(
            cpp,
            "-E",
            "-P",
            "-iquote",
            "include",
            "-iquote",
            "src",
            "-DMODERN=1",
            "-DTESTING=0",
            "-DTRUE=1",
            "-DFALSE=0",
            "-include",
            "config/general.h",
            "-include",
            "config/pokemon.h",
            "-include",
            "config/battle.h",
            "-include",
            "config/overworld.h",
            path,
        )
    val process = ProcessBuilder(command).directory(rootDir).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "Expansion preprocessor failed for $path:\n$output" }
    return output
  }

  private fun findCpp(): String {
    val configured = System.getenv("EXPANSION_CPP")
    val candidates =
        listOfNotNull(
            configured,
            "gcc",
            "C:/devkitPro/msys2/usr/bin/gcc.exe".takeIf { File(it).isFile },
        )
    return candidates.firstOrNull(::canRun)
        ?: error("No C preprocessor found. Set EXPANSION_CPP to gcc or clang.")
  }

  private fun canRun(command: String): Boolean =
      runCatching {
            val process = ProcessBuilder(command, "--version").redirectErrorStream(true).start()
            process.inputStream.close()
            process.waitFor() == 0
          }
          .getOrDefault(false)

  internal fun parseEnum(text: String, prefix: String): Map<String, Int> {
    val allValues = linkedMapOf<String, Int>()
    var next = 0
    var started = false
    ENUM_VALUE.findAll(text).forEach { match ->
      val symbol = match.groupValues[1]
      if (!started && !symbol.startsWith(prefix)) return@forEach
      started = true
      val expression = match.groupValues[2].trim()
      val value = if (expression.isEmpty()) next else enumValue(expression, allValues)
      allValues[symbol] = value
      next = value + 1
    }
    return allValues.filterKeys { it.startsWith(prefix) }
  }

  private fun enumValue(expression: String, known: Map<String, Int>): Int {
    expression.toIntOrNull()?.let {
      return it
    }
    if (expression.startsWith("0x")) return expression.drop(2).toInt(16)
    known[expression]?.let {
      return it
    }
    val arithmetic =
        Regex("""(\w+)\s*([+-])\s*(\d+)""").matchEntire(expression)
            ?: error("Cannot resolve enum expression $expression")
    val base = known[arithmetic.groupValues[1]] ?: error("Unknown enum value in $expression")
    val amount = arithmetic.groupValues[3].toInt()
    return if (arithmetic.groupValues[2] == "+") base + amount else base - amount
  }

  private fun parseFields(body: String): Map<String, String> {
    val fields = linkedMapOf<String, String>()
    var i = 0
    while (i < body.length) {
      if (body[i] != '.') {
        i++
        continue
      }
      val nameStart = ++i
      while (i < body.length && (body[i].isLetterOrDigit() || body[i] == '_')) i++
      val name = body.substring(nameStart, i)
      while (i < body.length && body[i].isWhitespace()) i++
      if (i >= body.length || body[i] != '=') continue
      i++
      val valueStart = i
      var braces = 0
      var parentheses = 0
      var brackets = 0
      var quoted = false
      var escaped = false
      while (i < body.length) {
        val char = body[i]
        if (quoted) {
          if (escaped) escaped = false
          else if (char == '\\') escaped = true else if (char == '"') quoted = false
        } else {
          when (char) {
            '"' -> quoted = true
            '{' -> braces++
            '}' -> braces--
            '(' -> parentheses++
            ')' -> parentheses--
            '[' -> brackets++
            ']' -> brackets--
            ',' -> if (braces == 0 && parentheses == 0 && brackets == 0) break
          }
        }
        i++
      }
      fields[name] = body.substring(valueStart, i).trim()
      i++
    }
    return fields
  }

  private fun matchingBrace(text: String, open: Int): Int {
    var depth = 0
    var quoted = false
    var escaped = false
    for (i in open until text.length) {
      val char = text[i]
      if (quoted) {
        if (escaped) escaped = false
        else if (char == '\\') escaped = true else if (char == '"') quoted = false
      } else {
        when (char) {
          '"' -> quoted = true
          '{' -> depth++
          '}' -> if (--depth == 0) return i
        }
      }
    }
    error("Unclosed species initializer at offset $open")
  }

  private fun references(value: String?, prefix: String): List<String> =
      Regex("$prefix[A-Z0-9_]+").findAll(value.orEmpty()).map { it.value }.toList()

  /** Expansion's DEFAULT(first, second) macro survives preprocessing in initializers. */
  private fun configuredPair(values: List<String>): List<String> =
      if (values.size >= 3 && values[0] == values[1]) listOf(values[0], values[2])
      else values.take(2)

  private fun RawSpecies.isCanonicalNationalSymbol(): Boolean {
    val national = fields["natDexNum"]?.let(SYMBOL::find)?.value ?: return false
    return symbol.removePrefix("SPECIES_") == national.removePrefix("NATIONAL_DEX_")
  }

  private fun stableId(symbol: String): String = "expansion:$symbol"

  /**
   * Keeps client ids in National Dex order instead of jumping to a separate block, stepping over
   * every id the client has already claimed. There are TWO such blocks, not one: 1000-1052 for
   * the client's own event species, and **650-667 for its retail FORM records** - Deoxys,
   * Wormadam, Shaymin, Giratina, Rotom, Castform, Basculin, Darmanitan and Meloetta. Missing the
   * second block put the eighteen Kalos species from Chespin to Litleo straight on top of those
   * forms, so a Greninja was a Rotom form as far as the client was concerned.
   *
   * A wire id is an identity, not a Dex number: the dex TAB order comes from the regional dex
   * lists (section 11), where a species' position in the list is the number it displays. So the
   * Kalos species still read 1, 2, 3... in their own tab while living at free ids.
   */
  private fun clientWireId(entry: RawSpecies, isBaseSpecies: Boolean, formOrdinal: Int?): Int {
    val dex = entry.nationalDexId
    val id =
        if (isBaseSpecies && dex != null) dexWireId(dex)
        else FIRST_FORM_CLIENT_ID + (formOrdinal ?: 0)
    require(id <= 0xFFFF) { "Expansion species ${entry.symbol} exceeds the client species field" }
    return id
  }

  private fun dexWireId(dex: Int): Int {
    var id = dex
    // Walk the reserved blocks in order, pushing past each one the id lands inside or after.
    RESERVED_CLIENT_BLOCKS.forEach { block ->
      if (id >= block.first) id += block.last - block.first + 1
    }
    return id
  }

  private data class RawSpecies(
      val symbol: String,
      val originalId: Int,
      val nationalDexId: Int?,
      val displayName: String,
      val fields: Map<String, String>,
  )

  private companion object {
    const val EXPANSION_SERVER_ID_BASE = 0x10000
    /**
     * Id ranges the client has already filled, lowest first: its retail FORM records (Deoxys
     * through Meloetta) and its own event species. Generated ids step over both.
     */
    val RESERVED_CLIENT_BLOCKS = listOf(650..667, 1000..1052)
    /** First slot after the highest Dex number the catalogue produces (1025 -> 1096). */
    const val FIRST_FORM_CLIENT_ID = 1097
    const val LAST_KNOWN_CLIENT_SPECIES = 649
    /** The client's rarity class: Mew is 1, Mewtwo is 2. */
    const val RARITY_NONE = 0
    const val RARITY_MYTHICAL = 1
    const val RARITY_LEGENDARY = 2
    val ENTRY_START = Regex("""\[(SPECIES_[A-Z0-9_]+)]\s*=\s*\{""")
    val ENUM_VALUE = Regex("""(?m)^\s*([A-Z][A-Z0-9_]+)\s*(?:=\s*([^,]+))?,""")
    val DISPLAY_NAME = Regex("""_\("([^"]*)"\)""")
    val SYMBOL = Regex("""[A-Z][A-Z0-9_]+""")
    val IDENTIFIER = Regex("""[A-Za-z_][A-Za-z0-9_]*""")
    val GRAPHICS_ENTRY = Regex("""(g[A-Za-z0-9_]+)\[\]\s*=\s*INC[A-Z0-9_]*\(\s*"([^"]+)"""")
    val CRY_LABEL = Regex("""^\s*Cry_([A-Za-z0-9_]+)::""")
    val INCBIN_PATH = Regex("""\.incbin\s+"([^"]+)"""")
    const val CRY_DATA = "sound/direct_sound_data.inc"
    val LEARNSET =
        Regex(
            """static\s+const\s+struct\s+LevelUpMove\s+(s[A-Za-z0-9_]+)\[\]\s*=\s*\{(.*?)\};""",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
    val LEVEL_UP_MOVE = Regex("""LEVEL_UP_MOVE\(\s*(\d+)\s*,\s*(MOVE_[A-Z0-9_]+)\s*\)""")
  }
}

object ExpansionSpeciesBinary {
  /** Set before encoding: stats can be generation-gated expressions rather than plain numbers. */
  private lateinit var config: ExpansionConfig
  private const val FORMAT_VERSION = 5

  fun encode(species: List<ParsedExpansionSpecies>, settings: ExpansionConfig): String {
    config = settings
    val bytes = ByteArrayOutputStream()
    DataOutputStream(GZIPOutputStream(bytes)).use { output ->
      output.writeInt(FORMAT_VERSION)
      output.writeInt(species.size)
      species.forEach { entry ->
        output.writeUTF(entry.stableId)
        output.writeInt(entry.originalId)
        output.writeInt(entry.serverId)
        output.writeUTF(entry.symbol)
        output.writeUTF(entry.displayName)
        output.writeNullableInt(entry.nationalDexId)
        output.writeUTF(entry.baseSpeciesStableId)
        output.writeBoolean(entry.isForm)
        output.writeFieldInt(entry, "baseHP")
        output.writeFieldInt(entry, "baseAttack")
        output.writeFieldInt(entry, "baseDefense")
        output.writeFieldInt(entry, "baseSpeed")
        output.writeFieldInt(entry, "baseSpAttack")
        output.writeFieldInt(entry, "baseSpDefense")
        output.writeStringList(entry.typeSymbols)
        output.writeStringList(entry.abilitySymbols)
        output.writeIntList(entry.abilityIds)
        output.writeInt(entry.levelUpLearnset.size)
        entry.levelUpLearnset.forEach { move ->
          output.writeInt(move.level)
          output.writeUTF(move.moveSymbol)
          output.writeInt(move.originalMoveId)
        }
        output.writeStringList(entry.evolutionTargetSymbols)
        output.writeFieldInt(entry, "catchRate")
        output.writeFieldInt(entry, "expYield")
        output.writeFieldInt(entry, "evYield_HP")
        output.writeFieldInt(entry, "evYield_Attack")
        output.writeFieldInt(entry, "evYield_Defense")
        output.writeFieldInt(entry, "evYield_Speed")
        output.writeFieldInt(entry, "evYield_SpAttack")
        output.writeFieldInt(entry, "evYield_SpDefense")
        output.writeUTF(entry.fields["itemCommon"]?.trim().orEmpty())
        output.writeUTF(entry.fields["itemRare"]?.trim().orEmpty())
        output.writeInt(genderRatio(entry.fields["genderRatio"]))
        output.writeFieldInt(entry, "eggCycles")
        output.writeInt(intValue(entry.fields["friendship"], 70))
        output.writeUTF(entry.fields["growthRate"]?.trim().orEmpty())
        output.writeStringList(entry.eggGroupSymbols)
        output.writeUTF(entry.fields["bodyColor"]?.trim().orEmpty())
        output.writeBoolean(intValue(entry.fields["noFlip"], 0) != 0)
        output.writeInt(entry.rarity)
        output.writeUTF(entry.categoryName)
        output.writeInt(entry.height)
        output.writeInt(entry.weight)
        output.writeBoolean(entry.assets.partyIcon)
        output.writeBoolean(entry.assets.frontSprite)
        output.writeBoolean(entry.assets.backSprite)
        output.writeBoolean(entry.assets.cry)
        output.writeBoolean(entry.assets.follower)
        output.writeUTF(entry.assets.frontPicPath)
        output.writeUTF(entry.assets.backPicPath)
        output.writeUTF(entry.assets.iconPath)
        output.writeUTF(entry.assets.normalPalettePath)
        output.writeUTF(entry.assets.shinyPalettePath)
        output.writeUTF(entry.assets.cryPath)
        output.writeInt(entry.assets.iconPalIndex)
        output.writeNullableInt(entry.clientWireId)
      }
    }
    return Base64.getEncoder().encodeToString(bytes.toByteArray())
  }

  private fun DataOutputStream.writeFieldInt(entry: ParsedExpansionSpecies, field: String) {
    writeInt(intValue(entry.fields[field], 0))
  }

  private fun intValue(value: String?, default: Int): Int {
    val token = value?.trim() ?: return default
    token.toIntOrNull()?.let {
      return it
    }
    if (token.startsWith("0x")) return token.drop(2).toIntOrNull(16) ?: default
    // A generation-gated number. Falling through to the default instead silently produced a stat of
    // zero - Jigglypuff with no Special Attack, Zacian and Zamazenta with no Attack at all.
    STAT_TERNARY.find(token)?.let { ternary ->
      val setting = config.value(ternary.groupValues[1])
      val threshold = config.value(ternary.groupValues[3])
      if (setting != null && threshold != null) {
        val takeFirst =
            when (ternary.groupValues[2]) {
              ">=" -> setting >= threshold
              "<=" -> setting <= threshold
              ">" -> setting > threshold
              "<" -> setting < threshold
              "==" -> setting == threshold
              else -> setting != threshold
            }
        val chosen = if (takeFirst) ternary.groupValues[4] else ternary.groupValues[5]
        return intValue(chosen, default)
      }
    }
    return when (token) {
      "STANDARD_FRIENDSHIP" -> 70
      "TRUE" -> 1
      "FALSE" -> 0
      else -> default
    }
  }

  private fun genderRatio(value: String?): Int {
    val token = value?.trim() ?: return 255
    return when (token) {
      "MON_MALE" -> 0
      "MON_FEMALE" -> 254
      "MON_GENDERLESS" -> 255
      else -> {
        val percent =
            Regex("""([0-9]+(?:\.[0-9]+)?)\s*\*\s*255""")
                .find(token)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
        if (percent == null) intValue(token, 255) else minOf(254, (percent * 255 / 100).toInt())
      }
    }
  }

  private fun DataOutputStream.writeStringList(values: List<String>) {
    writeInt(values.size)
    values.forEach(::writeUTF)
  }

  private fun DataOutputStream.writeIntList(values: List<Int>) {
    writeInt(values.size)
    values.forEach(::writeInt)
  }

  private fun DataOutputStream.writeNullableInt(value: Int?) {
    writeBoolean(value != null)
    if (value != null) writeInt(value)
  }
}

private val STAT_TERNARY =
    Regex("""(\w+)\s*(>=|<=|==|!=|>|<)\s*(\w+)\s*\?\s*([A-Za-z0-9_]+)\s*:\s*([A-Za-z0-9_]+)""")
