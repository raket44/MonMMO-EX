package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The client lists a hidden species once it is seen, so an alternate form's own tier bit must stay
 * clear: it counts on its base only. Regional forms keep their own bit for their region's tab.
 */
class DexAlternateFormsTest :
    FunSpec({
      val registry = ExpansionSpeciesRegistry()
      fun wire(symbol: String) = registry.get(symbol)!!.clientWireId!!

      test("a Mega counts on its base species and never on its own id") {
        DexProgressService.withBaseSpecies(listOf(wire("SPECIES_CHARIZARD_MEGA_X"))) shouldBe setOf(6)
      }

      test("a regional form keeps its own id and adds its base") {
        DexProgressService.withBaseSpecies(listOf(wire("SPECIES_VULPIX_ALOLA"))) shouldBe setOf(wire("SPECIES_VULPIX_ALOLA"), 37)
      }
    })
