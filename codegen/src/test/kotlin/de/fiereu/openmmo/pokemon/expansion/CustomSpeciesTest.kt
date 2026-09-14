package de.fiereu.openmmo.pokemon.expansion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith

/** MonMMO-EX's own species (codegen/custom-species) reach the catalogue like Expansion ones. */
class CustomSpeciesTest :
    FunSpec({
      val registry = ExpansionSpeciesRegistry()

      test("Crystal Onix is a Rock/Fairy form of Onix numbered after every Expansion species") {
        val crystal = registry.get("SPECIES_ONIX_CRYSTAL").shouldNotBeNull()
        val onix = registry.get("SPECIES_ONIX").shouldNotBeNull()

        crystal.isForm shouldBe true
        crystal.baseSpeciesStableId shouldBe onix.stableId
        crystal.nationalDexId shouldBe 95
        crystal.displayName shouldBe "Crystal Onix"
        crystal.typeSymbols shouldContainExactly listOf("TYPE_ROCK", "TYPE_FAIRY")
        crystal.baseHp shouldBe 150
        crystal.baseDefense shouldBe 180
        // Last in id order, so it is the last form ordinal and shifts no existing wire id.
        crystal.originalId shouldBe registry.all().maxOf { it.originalId }
        crystal.clientWireId shouldBe registry.all().mapNotNull { it.clientWireId }.max()
        crystal.formIndex.shouldNotBeNull()
        crystal.retailRecordId shouldBe null
      }

      test("Crystal Onix's art comes from the custom species folder and its cry is Onix's") {
        val crystal = registry.get("SPECIES_ONIX_CRYSTAL").shouldNotBeNull()
        val onix = registry.get("SPECIES_ONIX").shouldNotBeNull()

        crystal.assets.frontPicPath.replace('\\', '/') shouldEndWith "custom-species/onix_crystal/anim_front.png"
        crystal.assets.backPicPath.replace('\\', '/') shouldEndWith "custom-species/onix_crystal/back.png"
        crystal.assets.iconPath.replace('\\', '/') shouldEndWith "custom-species/onix_crystal/icon.png"
        crystal.assets.normalPalettePath.replace('\\', '/') shouldEndWith "custom-species/onix_crystal/normal.pal"
        crystal.assets.cryPath shouldBe onix.assets.cryPath
      }
    })
