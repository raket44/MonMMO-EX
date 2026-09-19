plugins {
  id("buildsrc.convention.kotlin-jvm")
  id("buildsrc.convention.spotless")
  id("buildsrc.convention.sonarlint")
  id("buildsrc.convention.jte-codegen")
}

dependencies {
  api(project(":common"))
  api(project(":protocols.game"))
  api(libs.dagger)
  implementation(libs.kotlinx.serialization.json)
  "generatorImplementation"(project(":common"))
  "generatorImplementation"(libs.jte)
  "generatorImplementation"(libs.kotlinx.serialization.json)
  testImplementation(sourceSets["generator"].output)
  testImplementation(libs.kotlinx.serialization.json)
  testImplementation(libs.bundles.kotest)
}

// GBA regions sharing the pret map format.
val regionSources =
    mapOf(
        "hoenn" to "pokeemerald",
        "kanto" to "pokefirered",
    )

// Single source-of-truth decomp for the non-region-specific data (moves, species). The two
// GBA decomps agree on the national dex; where they differ (held items, safari flee rate) this
// is the canonical pick, same as byRegion is for maps.
val sourceDecompDir = rootProject.layout.projectDirectory.dir("decomp/pokeemerald")

// Kept separate from retail Emerald. This sibling is the user's configured Expansion source tree.
val expansionDecompDir = rootProject.layout.projectDirectory.dir("../pokeemerald-expansion")

// The Gen 5 table the live client speaks, not the GBA decomps, which number the same items
// differently. Committed rather than a submodule because openmmo-org/pokeblack is private, taken
// from its src/data/items.json and container 54 of its src/data/text1.json.
val itemDataDir = rootProject.layout.projectDirectory.dir("decomp/pokeblack")

// Gitignored, and only the manual refresh tasks read them.
val romsDir = rootProject.layout.projectDirectory.dir("roms")

// Committed, because a ROM is the only place these ids exist and no runner has one.
val dialogDataDir = layout.projectDirectory.dir("dialog")

jteCodegen {
  register("maps") {
    mainClass.set("de.fiereu.openmmo.codegen.maps.Main")
    inputDirs.from(
        regionSources.values.map { rootProject.layout.projectDirectory.dir("decomp/$it") })
    extraArgs.set(
        regionSources.map { (region, decomp) ->
          "$region|${rootProject.layout.projectDirectory.dir("decomp/$decomp").asFile.absolutePath}"
        })
  }
  register("item") {
    mainClass.set("de.fiereu.openmmo.codegen.item.Main")
    inputDirs.from(itemDataDir)
    extraArgs.set(listOf(itemDataDir.asFile.absolutePath))
  }
  register("moves") {
    mainClass.set("de.fiereu.openmmo.codegen.move.Main")
    templatesSubdir.set("move")
    inputDirs.from(expansionDecompDir)
    extraArgs.set(listOf(expansionDecompDir.asFile.absolutePath))
  }
  register("pokemon") {
    mainClass.set("de.fiereu.openmmo.codegen.pokemon.Main")
    inputDirs.from(sourceDecompDir)
    extraArgs.set(listOf(sourceDecompDir.asFile.absolutePath))
  }
  register("expansionPokemon") {
    mainClass.set("de.fiereu.openmmo.codegen.pokemon.expansion.ExpansionPokemonMain")
    // MonMMO-EX's own species (Crystal Onix), appended after the Expansion's in its syntax.
    val customSpecies = layout.projectDirectory.dir("custom-species")
    inputDirs.from(expansionDecompDir, customSpecies)
    // The client's own form catalogue (launcher :stageRetailData), so Expansion forms the client
    // already has resolve to its records instead of becoming duplicate species.
    val retailForms = layout.projectDirectory.file("src/main/resources/monmmo/retail-forms.csv")
    // Declared as an input, or Gradle keeps a catalog generated before the csv existed: that is
    // how 75 forms retail already draws (Unown, Rotom, Castform...) shipped again as species.
    inputDirs.from(retailForms)
    extraArgs.set(
        listOf(
            expansionDecompDir.asFile.absolutePath,
            retailForms.asFile.absolutePath,
            customSpecies.asFile.absolutePath))
  }
  register("learnset") {
    mainClass.set("de.fiereu.openmmo.codegen.learnset.Main")
    inputDirs.from(sourceDecompDir)
    extraArgs.set(listOf(sourceDecompDir.asFile.absolutePath))
  }
  // Trainers differ per game, so this is by region like maps rather than from the canonical decomp.
  register("trainer") {
    mainClass.set("de.fiereu.openmmo.codegen.trainer.Main")
    val trainerSources = regionSources + mapOf("sinnoh" to "pokeplatinum", "johto" to "pokeheartgold")
    // Unova trainer rows extracted from the ROM by tools/nds/Trn5 (no decomp exists for it).
    val unovaTrainerDir = rootProject.layout.projectDirectory.dir("rom-data")
    inputDirs.from(
        trainerSources.values.map { rootProject.layout.projectDirectory.dir("decomp/$it") } + unovaTrainerDir)
    extraArgs.set(
        trainerSources.map { (region, decomp) ->
          "$region|${rootProject.layout.projectDirectory.dir("decomp/$decomp").asFile.absolutePath}"
        } + "unova|${unovaTrainerDir.asFile.absolutePath}")
  }
  // Per region flag and var key constants for scripts. Names come from each decomp, so this is by
  // region like maps. The generic story store in server.game does not depend on these, they are
  // the adapter that gives ported scripts readable keys. Story covers the NDS regions too -
  // pokeheartgold has pret-style flags.h/vars.h, pokeplatinum ships a flattened enum list.
  register("story") {
    mainClass.set("de.fiereu.openmmo.codegen.story.Main")
    val storyRegionSources =
        regionSources + mapOf("johto" to "pokeheartgold", "sinnoh" to "pokeplatinum")
    inputDirs.from(
        storyRegionSources.values.map { rootProject.layout.projectDirectory.dir("decomp/$it") })
    extraArgs.set(
        storyRegionSources.map { (region, decomp) ->
          "$region|${rootProject.layout.projectDirectory.dir("decomp/$decomp").asFile.absolutePath}"
        })
  }
  register("typechart") {
    mainClass.set("de.fiereu.openmmo.codegen.typechart.Main")
    inputDirs.from(sourceDecompDir)
    extraArgs.set(listOf(sourceDecompDir.asFile.absolutePath))
  }
  register("dialog") {
    mainClass.set("de.fiereu.openmmo.codegen.dialog.Main")
    inputDirs.from(dialogDataDir)
    extraArgs.set(listOf(dialogDataDir.asFile.absolutePath) + regionSources.keys)
  }
  register("scriptCorpus") {
    mainClass.set("de.fiereu.openmmo.codegen.script.ScriptCorpusMain")
    inputDirs.from(
        regionSources.values.map { rootProject.layout.projectDirectory.dir("decomp/$it") } +
            dialogDataDir)
    extraArgs.set(
        listOf(dialogDataDir.asFile.absolutePath) +
            listOf(
                "kanto|firered|BPRE|${rootProject.layout.projectDirectory.dir("decomp/pokefirered").asFile.absolutePath}",
                "hoenn|emerald|BPEE|${rootProject.layout.projectDirectory.dir("decomp/pokeemerald").asFile.absolutePath}",
                // DS regions: the decomps' disassembled ROM scripts, bound by ROM map header id.
                "sinnoh|platinum|CPUE|${rootProject.layout.projectDirectory.dir("decomp/pokeplatinum").asFile.absolutePath}",
                "johto|heartgold|IPKE|${rootProject.layout.projectDirectory.dir("decomp/pokeheartgold").asFile.absolutePath}",
                // Unova: no decomp, the ROM script archive disassembled by tools/nds/Dis5 into server.game.
                "unova|white|IRAO|${rootProject.layout.projectDirectory.dir("server.game").asFile.absolutePath}",
            ))
  }
}

tasks.register<JavaExec>("refreshDialogTable") {
  group = "codegen"
  description =
      "Re-resolve codegen/dialog from the ROMs in roms/ (manual, run after a decomp bump, commit the result)"
  val fireredDir = rootProject.layout.projectDirectory.dir("decomp/pokefirered")
  classpath = sourceSets["generator"].runtimeClasspath
  mainClass.set("de.fiereu.openmmo.codegen.dialog.RefreshMain")
  args(
      romsDir.asFile.absolutePath,
      dialogDataDir.asFile.absolutePath,
      "hoenn|BPEE|${sourceDecompDir.asFile.absolutePath}",
      "kanto|BPRE|${fireredDir.asFile.absolutePath}",
  )
}

// One shot bootstrap of the overworld script stubs into server.game. Run by hand with
// `gradlew :codegen:generateScriptStubs`. Deliberately not wired into the build, the emitted files
// are committed source so hand written ports are never overwritten on a normal build.
tasks.register<JavaExec>("generateScriptStubs") {
  group = "codegen"
  description = "Bootstrap overworld script stubs into server.game (manual, not part of the build)"
  val fireredDir = rootProject.layout.projectDirectory.dir("decomp/pokefirered")
  val serverGameSrc = rootProject.layout.projectDirectory.dir("server.game/src/main/kotlin")
  classpath = sourceSets["generator"].runtimeClasspath
  mainClass.set("de.fiereu.openmmo.codegen.script.Main")
  args(
      serverGameSrc.asFile.absolutePath,
      romsDir.asFile.absolutePath,
      "hoenn|BPEE|${sourceDecompDir.asFile.absolutePath}",
      "kanto|BPRE|${fireredDir.asFile.absolutePath}",
  )
}
