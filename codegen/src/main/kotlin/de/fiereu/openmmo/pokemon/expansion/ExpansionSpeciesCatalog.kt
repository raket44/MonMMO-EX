package de.fiereu.openmmo.pokemon.expansion

import de.fiereu.openmmo.common.EXPANSION_SERVER_SPECIES_BASE
import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.common.enums.BodyColor
import de.fiereu.openmmo.common.enums.EggGroup
import de.fiereu.openmmo.common.enums.GrowthRate
import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.pokemon.SpeciesDef
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

const val EXPANSION_SERVER_ID_BASE = EXPANSION_SERVER_SPECIES_BASE

/** Highest National Dex number the stock client already provides from its ROM data. */
const val LAST_STOCK_CLIENT_DEX = 649

data class ExpansionLevelUpMove(
    val level: Int,
    val moveSymbol: String,
    val originalMoveId: Int,
)

data class ExpansionSpeciesAssets(
    val partyIcon: Boolean,
    val frontSprite: Boolean,
    val backSprite: Boolean,
    val cry: Boolean,
    val follower: Boolean,
    /** Expansion-relative source paths, blank when the file is absent from the parsed tree. */
    val frontPicPath: String = "",
    val backPicPath: String = "",
    val iconPath: String = "",
    val normalPalettePath: String = "",
    val shinyPalettePath: String = "",
    val cryPath: String = "",
    val iconPalIndex: Int = 0,
)

data class ExpansionSpeciesDef(
    val stableId: String,
    val originalId: Int,
    val serverId: Int,
    val symbol: String,
    val displayName: String,
    val nationalDexId: Int?,
    val baseSpeciesStableId: String,
    val isForm: Boolean,
    val baseHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseSpeed: Int,
    val baseSpAttack: Int,
    val baseSpDefense: Int,
    val typeSymbols: List<String>,
    val abilitySymbols: List<String>,
    val abilityIds: List<Int>,
    val levelUpLearnset: List<ExpansionLevelUpMove>,
    val evolutionTargetStableIds: List<String>,
    val catchRate: Int,
    val expYield: Int,
    val evYieldHp: Int,
    val evYieldAttack: Int,
    val evYieldDefense: Int,
    val evYieldSpeed: Int,
    val evYieldSpAttack: Int,
    val evYieldSpDefense: Int,
    val itemCommonSymbol: String,
    val itemRareSymbol: String,
    val genderRatio: Int,
    val eggCycles: Int,
    val friendship: Int,
    val growthRateSymbol: String,
    val eggGroupSymbols: List<String>,
    val bodyColorSymbol: String,
    val noFlip: Boolean,
    /** Client rarity class: 1 mythical, 2 legendary, 0 otherwise. */
    val rarity: Int,
    /** Dex category such as "Life", shown beneath the species name. */
    val categoryName: String,
    /** Decimetres and hectograms, as the decomp stores them. */
    val height: Int,
    val weight: Int,
    val assets: ExpansionSpeciesAssets,
    val clientWireId: Int?,
    /** The `sXFormChangeTable` symbol the species data names, "" when it has no forms. */
    val formChangeTableSymbol: String = "",
    /** This form's number on its base species (client form index); null for base species. */
    val formIndex: Int? = null,
    /**
     * The client's own record id when the client already has this form (base id for
     * appearance-only forms, 650-667 for record forms). Such a form is the retail form, never a
     * separately staged species.
     */
    val retailRecordId: Int? = null,
) {
  /** True when this is a form the retail client already owns - see [retailRecordId]. */
  val isRetailForm: Boolean
    get() = retailRecordId != null

  val usesClientUnsupportedType: Boolean
    // Fairy is patched into the client's type enum at ordinal 19; Stellar still has no slot.
    get() = typeSymbols.any { it == "TYPE_STELLAR" }

  /**
   * Knowing an ability by name is not the same as the engine implementing it. Every Expansion
   * ability now resolves, so species keep their real ability rather than collapsing to NONE, but
   * only the original set has battle mechanics behind it and BattleService still refuses a battle
   * rather than running an inert ability.
   */
  val abilityMechanicsSupported: Boolean
    get() =
        abilitySymbols
            .filterNot { it == "ABILITY_NONE" }
            .all { symbol -> runtimeAbility(symbol)?.mechanicsImplemented == true }

  /**
   * True for species the stock client does not already own. The client covers National Dex 1-649
   * from its ROMs, so only Dex 650 and above are genuinely new; forms and Megas of earlier species
   * carry their base Dex number and are therefore already present.
   */
  val isNewToClient: Boolean
    get() = (nationalDexId ?: 0) > LAST_STOCK_CLIENT_DEX

  /** True when every sprite source the client needs was found in the parsed Expansion tree. */
  val assetSourcesResolved: Boolean
    get() =
        assets.frontPicPath.isNotEmpty() &&
            assets.backPicPath.isNotEmpty() &&
            assets.iconPath.isNotEmpty() &&
            assets.normalPalettePath.isNotEmpty() &&
            assets.shinyPalettePath.isNotEmpty()

  val clientContentCompatible: Boolean
    get() =
        clientWireId != null &&
            !usesClientUnsupportedType &&
            !isExcludedForm &&
            assets.partyIcon &&
            assets.frontSprite &&
            assets.backSprite

  /**
   * Forms with no place on this server (owner, 2026-09-19: one row per real Pokemon, no cosmetic
   * spam): Totems (size only), Pikachu caps/cosplay and the LGPE starters (retail does caps as
   * costumes), Gigantamax (no Dynamax here), the fan "Mega Z"s, every Scatterbug/Spewpa pattern
   * but the first (identical art until Vivillon) and Alcremie's sweets (the cream is the form).
   * The wire ids stay assigned so nothing else renumbers; these are simply never staged or given.
   */
  val isExcludedForm: Boolean
    get() =
        isForm &&
            (symbol.contains("_TOTEM") ||
                symbol.startsWith("SPECIES_PIKACHU_") && symbol != "SPECIES_PIKACHU_MEGA" ||
                symbol == "SPECIES_PICHU_SPIKY_EARED" ||
                symbol == "SPECIES_EEVEE_STARTER" ||
                symbol.endsWith("_GMAX") || symbol.contains("_GMAX_") || symbol.endsWith("_ETERNAMAX") ||
                symbol.endsWith("_MEGA_Z") ||
                symbol.startsWith("SPECIES_SCATTERBUG_") && symbol != "SPECIES_SCATTERBUG_ICY_SNOW" ||
                symbol.startsWith("SPECIES_SPEWPA_") && symbol != "SPECIES_SPEWPA_ICY_SNOW" ||
                symbol.startsWith("SPECIES_ALCREMIE_") && !symbol.endsWith("_STRAWBERRY_VANILLA_CREAM") &&
                    !symbol.endsWith("_STRAWBERRY_RUBY_CREAM") && !symbol.endsWith("_STRAWBERRY_MATCHA_CREAM") &&
                    !symbol.endsWith("_STRAWBERRY_MINT_CREAM") && !symbol.endsWith("_STRAWBERRY_LEMON_CREAM") &&
                    !symbol.endsWith("_STRAWBERRY_SALTED_CREAM") && !symbol.endsWith("_STRAWBERRY_RUBY_SWIRL") &&
                    !symbol.endsWith("_STRAWBERRY_CARAMEL_SWIRL") && !symbol.endsWith("_STRAWBERRY_RAINBOW_SWIRL"))

  fun runtimeDefinition(runtimeId: Int = serverId): SpeciesDef? {
    val types = typeSymbols.mapNotNull(::runtimeType)
    val abilities = abilitySymbols.map { runtimeAbility(it) ?: Ability.NONE }
    val growth = enumValue<GrowthRate>(growthRateSymbol.removePrefix("GROWTH_")) ?: return null
    val eggGroups =
        eggGroupSymbols.mapNotNull { enumValue<EggGroup>(it.removePrefix("EGG_GROUP_")) }
    val bodyColor = enumValue<BodyColor>(bodyColorSymbol.removePrefix("BODY_COLOR_")) ?: return null
    if (types.isEmpty() || abilities.isEmpty() || abilityIds.isEmpty() || eggGroups.isEmpty()) {
      return null
    }
    return SpeciesDef(
        id = runtimeId,
        name = displayName,
        baseHp = baseHp,
        baseAttack = baseAttack,
        baseDefense = baseDefense,
        baseSpeed = baseSpeed,
        baseSpAttack = baseSpAttack,
        baseSpDefense = baseSpDefense,
        type1 = types[0],
        type2 = types.getOrElse(1) { types[0] },
        catchRate = catchRate,
        expYield = expYield,
        evYieldHp = evYieldHp,
        evYieldAttack = evYieldAttack,
        evYieldDefense = evYieldDefense,
        evYieldSpeed = evYieldSpeed,
        evYieldSpAttack = evYieldSpAttack,
        evYieldSpDefense = evYieldSpDefense,
        itemCommon = 0,
        itemRare = 0,
        genderRatio = genderRatio,
        eggCycles = eggCycles,
        friendship = friendship,
        growthRate = growth,
        eggGroup1 = eggGroups[0],
        eggGroup2 = eggGroups.getOrElse(1) { eggGroups[0] },
        ability1 = abilities[0],
        ability2 = abilities.getOrElse(1) { Ability.NONE },
        safariZoneFleeRate = 0,
        bodyColor = bodyColor,
        noFlip = noFlip,
        ability1Id = abilityIds[0],
        ability2Id = abilityIds.getOrElse(1) { 0 },
        abilityMechanicsSupported = abilityMechanicsSupported,
        hiddenAbility = abilities.getOrElse(2) { Ability.NONE },
        hiddenAbilityId = abilityIds.getOrElse(2) { 0 },
        weight = weight,
    )
  }

  private fun runtimeType(symbol: String): PokemonType? =
      when (symbol) {
        "TYPE_MYSTERY" -> PokemonType.QUESTIONQUESTIONQUESTION
        else -> enumValue(symbol.removePrefix("TYPE_"))
      }

  private fun runtimeAbility(symbol: String): Ability? = enumValue(symbol.removePrefix("ABILITY_"))

  private inline fun <reified T : Enum<T>> enumValue(name: String): T? =
      enumValues<T>().firstOrNull { it.name == name }
}

data class ExpansionCompatibilityReport(
    val totalSpeciesAndForms: Int,
    val baseSpecies: Int,
    val forms: Int,
    val highestOriginalId: Int,
    val duplicateOriginalIds: Set<Int>,
    val missingRequiredData: Map<String, Int>,
    val knownClientMappings: Int,
    val requiringClientMapping: Int,
    val stockClientMappings: Int,
    /** Expansion forms that resolve to a form the client already owns (never staged). */
    val retailFormMappings: Int,
    val generatedClientMappings: Int,
    val clientContentCompatible: Int,
    val unsupportedClientTypes: Int,
    val unsupportedAbilityMechanics: Int,
)

@Singleton
class ExpansionSpeciesRegistry @Inject constructor() {
  private val species = GeneratedExpansionSpeciesCatalog.species
  private val bySymbol = species.associateBy { it.symbol }
  private val byStableId = species.associateBy { it.stableId.lowercase() }
  private val byServerId = species.associateBy { it.serverId }
  // A form the client already owns shares its base species' client id (Unown B speaks 201, the
  // Arceus types speak 493), so it must not take that id over: the client id names the species.
  private val byClientWireId =
      species
          .filter { !it.isRetailForm }
          .mapNotNull { entry -> entry.clientWireId?.let { it to entry } }
          .toMap()

  fun all(): List<ExpansionSpeciesDef> = species

  fun get(symbol: String): ExpansionSpeciesDef? =
      byStableId[symbol.lowercase()]
          ?: bySymbol[
              if (symbol.startsWith("SPECIES_", ignoreCase = true)) symbol.uppercase()
              else "SPECIES_${symbol.uppercase()}"]

  fun getByServerId(serverId: Int): ExpansionSpeciesDef? = byServerId[serverId]

  fun getByClientWireId(clientWireId: Int): ExpansionSpeciesDef? = byClientWireId[clientWireId]

  /**
   * Server ids preserve Expansion identity. Verified Gen 1-5 client ids are also accepted as a
   * compatibility lookup, but the resulting runtime definition keeps the id requested by the caller
   * so packets continue to use the client's established numbering.
   */
  fun runtimeDefinition(id: Int): SpeciesDef? =
      byServerId[id]?.runtimeDefinition(id) ?: byClientWireId[id]?.runtimeDefinition(id)

  fun report(): ExpansionCompatibilityReport {
    val duplicateIds = species.groupBy { it.originalId }.filterValues { it.size > 1 }.keys
    val missing = linkedMapOf<String, Int>()
    fun count(name: String, predicate: (ExpansionSpeciesDef) -> Boolean) {
      val value = species.count(predicate)
      if (value > 0) missing[name] = value
    }
    count("runtime type/ability/egg-group mapping") { it.runtimeDefinition() == null }
    count("party icon source") { !it.assets.partyIcon }
    count("front battle sprite source") { !it.assets.frontSprite }
    count("back battle sprite source") { !it.assets.backSprite }
    count("cry source") { !it.assets.cry }
    count("follower source") { !it.assets.follower }
    return ExpansionCompatibilityReport(
        totalSpeciesAndForms = species.size,
        baseSpecies = species.count { !it.isForm },
        forms = species.count { it.isForm },
        highestOriginalId = species.maxOfOrNull { it.originalId } ?: 0,
        duplicateOriginalIds = duplicateIds,
        missingRequiredData = missing,
        knownClientMappings = species.count { it.clientWireId != null },
        requiringClientMapping = species.count { it.clientWireId == null },
        stockClientMappings = species.count { !it.isRetailForm && it.clientWireId in 1..649 },
        retailFormMappings = species.count { it.isRetailForm },
        generatedClientMappings = species.count { it.isNewToClient },
        clientContentCompatible = species.count { it.clientContentCompatible },
        unsupportedClientTypes = species.count { it.usesClientUnsupportedType },
        unsupportedAbilityMechanics = species.count { !it.abilityMechanicsSupported },
    )
  }
}

object GeneratedExpansionSpeciesCatalog {
  val species: List<ExpansionSpeciesDef> by lazy {
    ExpansionSpeciesDecoder.decode(GeneratedExpansionSpeciesData.chunks.joinToString(""))
  }
}

private object ExpansionSpeciesDecoder {
  private const val FORMAT_VERSION = 7

  fun decode(encoded: String): List<ExpansionSpeciesDef> {
    val bytes = Base64.getDecoder().decode(encoded)
    DataInputStream(GZIPInputStream(ByteArrayInputStream(bytes))).use { input ->
      check(input.readInt() == FORMAT_VERSION) { "Unsupported Expansion species format" }
      return List(input.readInt()) {
        ExpansionSpeciesDef(
            stableId = input.readUTF(),
            originalId = input.readInt(),
            serverId = input.readInt(),
            symbol = input.readUTF(),
            displayName = input.readUTF(),
            nationalDexId = input.readNullableInt(),
            baseSpeciesStableId = input.readUTF(),
            isForm = input.readBoolean(),
            baseHp = input.readInt(),
            baseAttack = input.readInt(),
            baseDefense = input.readInt(),
            baseSpeed = input.readInt(),
            baseSpAttack = input.readInt(),
            baseSpDefense = input.readInt(),
            typeSymbols = input.readStringList(),
            abilitySymbols = input.readStringList(),
            abilityIds = input.readIntList(),
            levelUpLearnset =
                List(input.readInt()) {
                  ExpansionLevelUpMove(
                      level = input.readInt(),
                      moveSymbol = input.readUTF(),
                      originalMoveId = input.readInt(),
                  )
                },
            evolutionTargetStableIds = input.readStringList().map { "expansion:$it" },
            catchRate = input.readInt(),
            expYield = input.readInt(),
            evYieldHp = input.readInt(),
            evYieldAttack = input.readInt(),
            evYieldDefense = input.readInt(),
            evYieldSpeed = input.readInt(),
            evYieldSpAttack = input.readInt(),
            evYieldSpDefense = input.readInt(),
            itemCommonSymbol = input.readUTF(),
            itemRareSymbol = input.readUTF(),
            genderRatio = input.readInt(),
            eggCycles = input.readInt(),
            friendship = input.readInt(),
            growthRateSymbol = input.readUTF(),
            eggGroupSymbols = input.readStringList(),
            bodyColorSymbol = input.readUTF(),
            noFlip = input.readBoolean(),
            rarity = input.readInt(),
            categoryName = input.readUTF(),
            height = input.readInt(),
            weight = input.readInt(),
            assets =
                ExpansionSpeciesAssets(
                    partyIcon = input.readBoolean(),
                    frontSprite = input.readBoolean(),
                    backSprite = input.readBoolean(),
                    cry = input.readBoolean(),
                    follower = input.readBoolean(),
                    frontPicPath = input.readUTF(),
                    backPicPath = input.readUTF(),
                    iconPath = input.readUTF(),
                    normalPalettePath = input.readUTF(),
                    shinyPalettePath = input.readUTF(),
                    cryPath = input.readUTF(),
                    iconPalIndex = input.readInt(),
                ),
            clientWireId = input.readNullableInt(),
            formChangeTableSymbol = input.readUTF(),
            formIndex = input.readNullableInt(),
            retailRecordId = input.readNullableInt(),
        )
      }
    }
  }

  private fun DataInputStream.readStringList(): List<String> = List(readInt()) { readUTF() }

  private fun DataInputStream.readIntList(): List<Int> = List(readInt()) { readInt() }

  private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null
}
