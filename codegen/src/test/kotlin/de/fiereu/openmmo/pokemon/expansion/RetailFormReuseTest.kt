package de.fiereu.openmmo.pokemon.expansion

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Forms the retail client already owns are reused, never duplicated (project owner, 2026-09-13).
 * Pinned against the generated catalogue, which reads the client's own form catalogue
 * (retail-forms.csv from the dex dump).
 */
class RetailFormReuseTest :
    FunSpec({
      val bySymbol = ExpansionSpeciesRegistry().all().associateBy { it.symbol }

      fun form(symbol: String) = bySymbol.getValue("SPECIES_$symbol")

      test("an appearance-only retail form is the base species under the client's form number") {
        form("UNOWN_B").retailRecordId shouldBe 201
        form("UNOWN_B").clientWireId shouldBe 201
        form("UNOWN_B").formIndex shouldBe 1
        form("UNOWN_QUESTION").formIndex shouldBe 27
        form("GENESECT_CHILL").retailRecordId shouldBe 649
      }

      test("a retail form with its own record speaks that record id") {
        form("ROTOM_HEAT").clientWireId shouldBe 657
        form("ROTOM_HEAT").formIndex shouldBe 1
        form("DEOXYS_SPEED").clientWireId shouldBe 652
        form("MELOETTA_PIROUETTE").clientWireId shouldBe 667
      }

      test("a new form of a retail species is numbered after the client's own forms") {
        // Charizard's form 1 is the Royal costume.
        form("CHARIZARD_MEGA_X").retailRecordId.shouldBeNull()
        form("CHARIZARD_MEGA_X").formIndex shouldBe 2
        form("CHARIZARD_MEGA_Y").formIndex shouldBe 3
        // The client's Arceus has 17 forms (0-16); Fairy is new.
        form("ARCEUS_FAIRY").retailRecordId.shouldBeNull()
        form("ARCEUS_FAIRY").formIndex shouldBe 17
      }

      test("regional forms and forms of new species are never mistaken for retail forms") {
        form("NINETALES_ALOLA").retailRecordId.shouldBeNull()
        form("GRENINJA_ASH").retailRecordId.shouldBeNull()
      }

      test("a client id shared with retail forms still names the base species") {
        val registry = ExpansionSpeciesRegistry()
        for (dex in listOf(201, 493, 649)) {
          val named = registry.getByClientWireId(dex)!!
          named.isForm shouldBe false
          named.nationalDexId shouldBe dex
        }
        // 657 is the client's own Rotom Heat record: no Expansion species claims it.
        registry.getByClientWireId(657).shouldBeNull()
        registry.getByClientWireId(479)!!.symbol shouldBe "SPECIES_ROTOM"
      }

      test("no two species the client must render share a wire id") {
        ExpansionSpeciesRegistry()
            .all()
            .filter { !it.isRetailForm && it.clientWireId != null }
            .groupBy { it.clientWireId }
            .filterValues { it.size > 1 }
            .keys
            .shouldBeEmpty()
      }
    })
