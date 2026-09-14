package de.fiereu.openmmo.pokemon.expansion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** The one regional-form rule the client staging and the server share (project owner, 2026-09-13). */
class RegionalFormsTest :
    FunSpec({
      val registry = ExpansionSpeciesRegistry()

      fun def(symbol: String) = registry.get(symbol)!!

      test("a regional form is listed in its region's tab, Hisui with Galar") {
        RegionalForms.regionOf(def("SPECIES_VULPIX_ALOLA")) shouldBe 7
        RegionalForms.regionOf(def("SPECIES_MEOWTH_GALAR")) shouldBe 8
        RegionalForms.regionOf(def("SPECIES_GROWLITHE_HISUI")) shouldBe 8
        RegionalForms.regionOf(def("SPECIES_WOOPER_PALDEA")) shouldBe 9
      }

      test("species, Megas, totems and Pikachu's caps are not regional forms") {
        RegionalForms.regionOf(def("SPECIES_VULPIX")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_CHARIZARD_MEGA_X")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_RATICATE_ALOLA_TOTEM")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_PIKACHU_ALOLA")).shouldBeNull()
      }

      test("a regional form counts toward its base species' National entry") {
        RegionalForms.baseWireOf(def("SPECIES_VULPIX_ALOLA").clientWireId!!) shouldBe 37
        RegionalForms.baseWireOf(def("SPECIES_WOOPER_PALDEA").clientWireId!!) shouldBe 194
        RegionalForms.baseWireOf(37).shouldBeNull()
      }

      test("every other form is an alternate form of its base, never a dex entry of its own") {
        RegionalForms.alternateFormBaseOf(def("SPECIES_CHARIZARD_MEGA_X").clientWireId!!) shouldBe 6
        RegionalForms.alternateFormBaseOf(def("SPECIES_RATICATE_ALOLA_TOTEM").clientWireId!!) shouldBe
            def("SPECIES_RATICATE").clientWireId
        RegionalForms.alternateFormBaseOf(def("SPECIES_VULPIX_ALOLA").clientWireId!!).shouldBeNull()
        RegionalForms.alternateFormBaseOf(37).shouldBeNull()
      }
    })
