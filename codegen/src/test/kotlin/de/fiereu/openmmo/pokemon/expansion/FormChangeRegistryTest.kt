package de.fiereu.openmmo.pokemon.expansion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldNotBe

class FormChangeRegistryTest :
    FunSpec({
      test("form tables resolve for Darmanitan") {
        val expansion = ExpansionSpeciesRegistry()
        val registry = FormChangeRegistry(expansion)
        val standard = expansion.get("SPECIES_DARMANITAN_STANDARD")!!
        registry.of(standard.serverId).size shouldBeGreaterThan 3
        registry.of(555).size shouldBeGreaterThan 3
        registry.targetServerId(registry.of(555).first { it.targetSymbol == "SPECIES_DARMANITAN_ZEN" }) shouldNotBe null
        registry.tableCount() shouldBeGreaterThan 100
      }
    })
