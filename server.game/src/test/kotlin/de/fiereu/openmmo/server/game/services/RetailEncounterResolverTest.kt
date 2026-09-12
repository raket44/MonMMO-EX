package de.fiereu.openmmo.server.game.services

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

/**
 * The retail dump splits an area into floors and zones through location_name_full; the server
 * must hand each map exactly its own rows, in every region, or a cave's whole roster spawns on
 * every floor and Pokemon Tower's ground floors get 3F's ghosts.
 */
class RetailEncounterResolverTest : StringSpec({
  fun fulls(source: String, region: Int) = RetailEncounters.entriesFor(source, region).map { it.fullName }.toSet()

  "Kanto: a cave inside an island resolves to the cave's floor" {
    fulls("FourIsland_IcefallCave_B1F", 0) shouldContainExactly setOf("Icefall Cave (B1F)")
    fulls("FourIsland_IcefallCave_Back", 0) shouldContainExactly setOf("Icefall Cave (Back Room)")
    // The entrance floor names no floor the dump has: the area's unsuffixed rows.
    fulls("FourIsland_IcefallCave_1F", 0) shouldContainExactly setOf("Icefall Cave")
  }

  "Kanto: Pokemon Tower's ground floors have no wild rows" {
    RetailEncounters.entriesFor("PokemonTower_1F", 0).shouldBeEmpty()
    RetailEncounters.entriesFor("PokemonTower_2F", 0).shouldBeEmpty()
    fulls("PokemonTower_3F", 0) shouldContainExactly setOf("Pokémon Tower (3F)")
  }

  "Kanto: safari zones, Mt. Ember's paths and Route 21's halves" {
    fulls("SafariZone_East", 0) shouldContainExactly setOf("Safari Zone (East Area)")
    fulls("SafariZone_Center", 0) shouldContainExactly setOf("Safari Zone (Center Area)")
    fulls("MtEmber_Exterior", 0) shouldContainExactly setOf("Mt. Ember (Outside)")
    fulls("MtEmber_RubyPath_B1F", 0) shouldContainExactly setOf("Mt. Ember (B1F)")
    fulls("MtEmber_SummitPath_2F", 0) shouldContainExactly setOf("Mt. Ember (2F)")
    fulls("Route21_North", 0) shouldContainExactly setOf("Route 21 (North)")
    fulls("MtMoon_B2F", 0) shouldContainExactly setOf("Mt. Moon (B2F)")
    fulls("SeafoamIslands_B4F", 0) shouldContainExactly setOf("Seafoam Islands (B4F)")
  }

  "Hoenn: floors, safari areas and the outside of Mt. Pyre" {
    fulls("GraniteCave_B2F", 1) shouldContainExactly setOf("Granite Cave (B2F)")
    fulls("SafariZone_Northeast", 1) shouldContainExactly setOf("Safari Zone (Northeast Area)")
    fulls("MtPyre_6F", 1) shouldContainExactly setOf("Mt. Pyre (6F)")
    fulls("NewMauville_Inside", 1) shouldContainExactly setOf("New Mauville (Interior)")
    fulls("SkyPillar_3F", 1) shouldContainExactly setOf("Sky Pillar (3F)")
  }

  "Unova, Sinnoh, Johto: directory names resolve the same way" {
    fulls("Chargestone Cave B1f", 2) shouldContainExactly setOf("Chargestone Cave (B1F)")
    fulls("Bell Tower 10f", 4) shouldContainExactly setOf("Bell Tower (10F)")
    fulls("Burned Tower B1f", 4) shouldContainExactly setOf("Burned Tower (B1F)")
    // A 4F room the dump splits by compass: every 4F row, none of the other floors.
    fulls("Mt Coronet 4f Rooms 1 And 2", 3) shouldContainExactly setOf("Mt. Coronet (4F North)", "Mt. Coronet (4F South)")
    fulls("Mt Coronet 2f", 3) shouldContainExactly setOf("Mt. Coronet (2F)")
  }

  "a plain route keeps its rows and a region never borrows another's" {
    fulls("Route3", 0) shouldContainExactly setOf("Route 3")
    RetailEncounters.entriesFor("Route3", 0).all { it.regionId == 0 } shouldBe true
  }
})
