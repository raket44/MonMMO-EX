package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.EXPANSION_SERVER_SPECIES_BASE
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

private const val VENUSAUR = 3
private const val CHARIZARD = 6
private const val BODY_SLAM = 34
private const val METRONOME = 118
private const val ROCK_SLIDE = 157
private const val SNORE = 173
private const val SLEEP_TALK = 214
private const val DYNAMIC_PUNCH = 223

class TutorCompatibilityTest :
    FunSpec({
      val compatibility = TutorCompatibility.load()

      test("retail species keep the ROM tutor_learnsets.h answers") {
        // Venusaur's pokefirered row: Swords Dance, Body Slam, Double-Edge, Mimic, Substitute.
        compatibility.canLearn(Region.KANTO, VENUSAUR, BODY_SLAM) shouldBe true
        compatibility.canLearn(Region.KANTO, VENUSAUR, METRONOME) shouldBe false
        compatibility.canLearn(Region.KANTO, VENUSAUR, TutorCompatibility.FRENZY_PLANT) shouldBe true
        compatibility.canLearn(Region.KANTO, CHARIZARD, TutorCompatibility.FRENZY_PLANT) shouldBe false
        compatibility.canLearn(Region.KANTO, CHARIZARD, TutorCompatibility.BLAST_BURN) shouldBe true
        // Charizard's pokeemerald row has Dynamic Punch; Venusaur's does not.
        compatibility.canLearn(Region.HOENN, CHARIZARD, DYNAMIC_PUNCH) shouldBe true
        compatibility.canLearn(Region.HOENN, VENUSAUR, DYNAMIC_PUNCH) shouldBe false
      }

      test("an Expansion species follows the Expansion's taught list") {
        val chespin = ExpansionSpeciesRegistry().get("CHESPIN")
        chespin shouldNotBe null
        val id = chespin!!.serverId
        id shouldBeGreaterThanOrEqual EXPANSION_SERVER_SPECIES_BASE

        compatibility.canLearn(Region.KANTO, id, BODY_SLAM) shouldBe true
        compatibility.canLearn(Region.KANTO, id, ROCK_SLIDE) shouldBe true
        compatibility.canLearn(Region.HOENN, id, SNORE) shouldBe true
        compatibility.canLearn(Region.HOENN, id, SLEEP_TALK) shouldBe true
        compatibility.canLearn(Region.KANTO, id, METRONOME) shouldBe false
        compatibility.canLearn(Region.KANTO, id, TutorCompatibility.FRENZY_PLANT) shouldBe false
      }

      test("the Expansion table never answers for a retail dex id") {
        val table =
            TutorCompatibility(kanto = emptyMap(), hoenn = emptyMap(), expansion = mapOf(VENUSAUR to setOf(METRONOME)))
        table.canLearn(Region.KANTO, VENUSAUR, METRONOME) shouldBe false
      }
    })
