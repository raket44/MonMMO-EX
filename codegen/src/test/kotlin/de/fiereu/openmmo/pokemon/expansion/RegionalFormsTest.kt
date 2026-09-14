package de.fiereu.openmmo.pokemon.expansion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/** The one regional-form rule the client staging and the server share (project owner, 2026-09-13/14). */
class RegionalFormsTest :
    FunSpec({
      val registry = ExpansionSpeciesRegistry()

      fun def(symbol: String) = registry.get(symbol)!!

      test("a regional form is listed in its region's tab") {
        RegionalForms.regionOf(def("SPECIES_VULPIX_ALOLA")) shouldBe 7
        RegionalForms.regionOf(def("SPECIES_MEOWTH_GALAR")) shouldBe 8
        RegionalForms.regionOf(def("SPECIES_WOOPER_PALDEA")) shouldBe 9
        RegionalForms.regionOf(def("SPECIES_TAUROS_PALDEA_COMBAT")) shouldBe 9
        RegionalForms.regionOf(def("SPECIES_DARMANITAN_GALAR_STANDARD")) shouldBe 8
      }

      test("species, Megas, totems, Pikachu's caps, Hisuian forms and further breeds are not") {
        RegionalForms.regionOf(def("SPECIES_VULPIX")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_CHARIZARD_MEGA_X")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_RATICATE_ALOLA_TOTEM")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_PIKACHU_ALOLA")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_GROWLITHE_HISUI")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_TAUROS_PALDEA_BLAZE")).shouldBeNull()
        RegionalForms.regionOf(def("SPECIES_DARMANITAN_GALAR_ZEN")).shouldBeNull()
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
        RegionalForms.alternateFormBaseOf(def("SPECIES_GROWLITHE_HISUI").clientWireId!!) shouldBe 58
        RegionalForms.alternateFormBaseOf(def("SPECIES_TAUROS_PALDEA_AQUA").clientWireId!!) shouldBe 128
        RegionalForms.alternateFormBaseOf(def("SPECIES_VULPIX_ALOLA").clientWireId!!).shouldBeNull()
        RegionalForms.alternateFormBaseOf(37).shouldBeNull()
      }

      test("each tab follows its region's own dex: Chespin, Rowlet, Grookey and Sprigatito open them") {
        RegionalForms.dexOrder(6).first() shouldBe 650
        RegionalForms.dexOrder(7).first() shouldBe 722
        RegionalForms.dexOrder(8).first() shouldBe 810
        RegionalForms.dexOrder(9).first() shouldBe 906
        RegionalForms.DEX_TABS.associateWith { RegionalForms.dexOrder(it).size } shouldBe
            mapOf(6 to 457, 7 to 403, 8 to 584, 9 to 664)
        RegionalForms.DEX_TABS.forEach { tab ->
          RegionalForms.dexOrder(tab).distinct().size shouldBe RegionalForms.dexOrder(tab).size
        }
      }

      test("every regional form has its species' place in its region's dex") {
        registry.all().forEach { entry ->
          val tab = RegionalForms.regionOf(entry) ?: return@forEach
          RegionalForms.dexOrder(tab) shouldContain entry.nationalDexId!!
        }
      }
    })
