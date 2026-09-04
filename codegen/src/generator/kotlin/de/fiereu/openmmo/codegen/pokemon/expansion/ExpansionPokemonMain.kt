@file:JvmName("ExpansionPokemonMain")

package de.fiereu.openmmo.codegen.pokemon.expansion

import de.fiereu.openmmo.codegen.move.ExpansionConfig
import de.fiereu.openmmo.common.enums.BodyColor
import de.fiereu.openmmo.common.enums.EggGroup
import de.fiereu.openmmo.common.enums.GrowthRate
import de.fiereu.openmmo.common.enums.PokemonType
import java.io.File

fun main(args: Array<String>) {
  require(args.size == 4) { "Usage: <output> <templates> <cache> <expansion-root>" }
  val outputDir = File(args[0])
  val expansionRoot = File(args[3])
  val species = ExpansionSpeciesGenerator(expansionRoot).parseAll()
  val forms = species.count { it.isForm }
  val highest = species.maxOfOrNull { it.originalId } ?: 0
  val mapped = species.count { it.clientWireId != null }
  val duplicateIds = species.groupBy { it.originalId }.count { it.value.size > 1 }
  val missingRequired = species.count { entry -> REQUIRED_FIELDS.any { it !in entry.fields } }
  val runtimeUnavailable = species.count { !it.canBuildRuntimeDefinition() }
  val supportedAbilities = de.fiereu.openmmo.common.enums.Ability.entries.map { it.name }.toSet()
  val unsupportedAbilityMechanics =
      species.count { entry ->
        entry.abilitySymbols
            .filterNot { it == "ABILITY_NONE" }
            .any { it.removePrefix("ABILITY_") !in supportedAbilities }
      }
  println(
      "[expansion-pokemon] enabled=${species.size}, forms=$forms, highestId=$highest, " +
          "duplicateIds=$duplicateIds, clientMapped=$mapped, clientUnmapped=${species.size - mapped}")
  println(
      "[expansion-pokemon] content learnsetEntries=${species.sumOf { it.levelUpLearnset.size }}, " +
          "evolutionTargets=${species.sumOf { it.evolutionTargetSymbols.size }}, " +
          "generatedWireIds=${species.count { (it.clientWireId ?: 0) > 2000 }}")
  println(
      "[expansion-pokemon] data missingRequired=$missingRequired, " +
          "runtimeAdaptersMissing=$runtimeUnavailable, " +
          "unsupportedAbilityMechanics=$unsupportedAbilityMechanics")
  println(
      "[expansion-pokemon] assets missing: icons=${species.count { !it.assets.partyIcon }}, " +
          "front=${species.count { !it.assets.frontSprite }}, " +
          "back=${species.count { !it.assets.backSprite }}, " +
          "cries=${species.count { !it.assets.cry }}, " +
          "followers=${species.count { !it.assets.follower }}")

  val formRows = FormChangeTableParser.parse(expansionRoot)
  println("[expansion-pokemon] formChangeRows=${formRows.size}, tables=${formRows.map { it.table }.distinct().size}")
  File(outputDir, "de/fiereu/openmmo/pokemon/expansion/GeneratedFormChanges.kt").also { it.parentFile.mkdirs() }.writeText(
      buildString {
        appendLine("package de.fiereu.openmmo.pokemon.expansion")
        appendLine()
        appendLine("internal object GeneratedFormChanges {")
        appendLine("  val rows: Array<String> =")
        appendLine("      arrayOf(")
        formRows.forEach { row ->
          val cells = listOf(row.table, row.kind, row.target) + row.params
          appendLine("          \"" + cells.joinToString("\t") + "\",")
        }
        appendLine("      )")
        appendLine("}")
      })

  val encoded = ExpansionSpeciesBinary.encode(species, ExpansionConfig.read(expansionRoot))
  val target =
      File(
          outputDir,
          "de/fiereu/openmmo/pokemon/expansion/GeneratedExpansionSpeciesData.kt",
      )
  target.parentFile.mkdirs()
  target.writeText(
      buildString {
        appendLine("package de.fiereu.openmmo.pokemon.expansion")
        appendLine()
        appendLine("internal object GeneratedExpansionSpeciesData {")
        appendLine("  val chunks: Array<String> =")
        appendLine("      arrayOf(")
        encoded.chunked(12_000).forEach { appendLine("          \"$it\",") }
        appendLine("      )")
        appendLine("}")
      })
}

private val REQUIRED_FIELDS =
    setOf(
        "baseHP",
        "baseAttack",
        "baseDefense",
        "baseSpeed",
        "baseSpAttack",
        "baseSpDefense",
        "types",
        "catchRate",
        "expYield",
        "abilities",
        "genderRatio",
        "eggCycles",
        "friendship",
        "growthRate",
        "eggGroups",
        "bodyColor",
    )

private fun ParsedExpansionSpecies.canBuildRuntimeDefinition(): Boolean {
  val types = PokemonType.entries.map { it.name }.toSet() + "MYSTERY"
  val eggGroups = EggGroup.entries.map { it.name }.toSet()
  val growthRates = GrowthRate.entries.map { it.name }.toSet()
  val bodyColors = BodyColor.entries.map { it.name }.toSet()
  return typeSymbols.all { it.removePrefix("TYPE_") in types } &&
      abilitySymbols.isNotEmpty() &&
      abilityIds.isNotEmpty() &&
      eggGroupSymbols.isNotEmpty() &&
      eggGroupSymbols.all { it.removePrefix("EGG_GROUP_") in eggGroups } &&
      fields["growthRate"]?.removePrefix("GROWTH_") in growthRates &&
      fields["bodyColor"]?.removePrefix("BODY_COLOR_") in bodyColors
}
