package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.story.generated.hoenn.HoennFlags
import de.fiereu.openmmo.story.generated.kanto.KantoFlags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Key items and HMs sit on their own region's bag page and on no other (owner, 2026-09-22). The
 * bag under test is the owner's real one on that date, standing in Unova with Kanto and Hoenn
 * behind him.
 */
class BagRegionsTest :
    FunSpec({
      val kanto: Byte = 0
      val hoenn: Byte = 1
      val unova: Byte = 2
      val sinnoh: Byte = 3
      val johto: Byte = 4
      val everywhere: Byte = -1
      val none = emptySet<String>()

      test("Kanto's key items are on Kanto's page only") {
        for (id in listOf(349, 350, 353, 355, 356, 359, 363, 368)) {
          BagRegions.single(id) shouldBe kanto
        }
      }

      test("Hoenn's key items are on Hoenn's page only") {
        BagRegions.single(272) shouldBe hoenn // Acro Bike
        BagRegions.single(259) shouldBe hoenn // Mach Bike
        BagRegions.single(1272) shouldBe everywhere // Multi-Vitamin Pack: the 1000 band is PokeMMO's own
        BagRegions.single(1259) shouldBe everywhere // Super Carbos
      }

      test("the client's own Bicycle is FireRed's: Kanto's page (riding never needed it in the bag)") {
        BagRegions.single(360) shouldBe kanto
      }

      test("Unova: HMs and pocket-4 items are Unova's; balls and medicine are shared") {
        BagRegions.single(5420) shouldBe unova // HM01
        BagRegions.single(5425) shouldBe unova // HM06
        BagRegions.single(5433) shouldBe unova // Bicycle (Gen 5 key item)
        BagRegions.single(6433) shouldBe unova // the client's +1000 mirror
        BagRegions.single(5004) shouldBe everywhere // Poke Ball
        BagRegions.single(5017) shouldBe everywhere // Potion
        BagRegions.single(6017) shouldBe everywhere
        BagRegions.single(5410) shouldBe everywhere // a TM
      }

      test("Sinnoh and Johto: HMs and everything from 428 up are theirs; the rest is shared") {
        BagRegions.single(8420) shouldBe sinnoh
        BagRegions.single(8428) shouldBe sinnoh // Explorer Kit
        BagRegions.single(8001) shouldBe everywhere // Master Ball
        BagRegions.single(9420) shouldBe johto
        BagRegions.single(9481) shouldBe johto // Machine Part
        BagRegions.single(9017) shouldBe everywhere // Potion
      }

      test("a GBA HM earned in Kanto is one Kanto stack, gone the moment you leave") {
        BagRegions.stacks(339, 1, setOf(KantoFlags.FLAG_GOT_HM01)) shouldBe listOf(kanto to 1)
        BagRegions.stacks(341, 1, setOf(KantoFlags.FLAG_GOT_HM03)) shouldBe listOf(kanto to 1)
        BagRegions.stacks(345, 1, setOf(KantoFlags.FLAG_HIDE_FOUR_ISLAND_ICEFALL_CAVE_1F_HM07)) shouldBe
            listOf(kanto to 1)
      }

      test("a GBA HM earned in Hoenn is one Hoenn stack") {
        BagRegions.stacks(339, 1, setOf(HoennFlags.FLAG_RECEIVED_HM_CUT)) shouldBe listOf(hoenn to 1)
        BagRegions.stacks(346, 1, setOf(HoennFlags.FLAG_RECEIVED_HM_DIVE)) shouldBe listOf(hoenn to 1)
      }

      test("one HM Cut item: a Unova receipt puts it on Unova's page, and only there") {
        BagRegions.stacks(339, 1, setOf("unova/HM_RECEIVED_15")) shouldBe listOf(unova to 1)
        BagRegions.stacks(339, 2, setOf(KantoFlags.FLAG_GOT_HM01, "unova/HM_RECEIVED_15")) shouldBe
            listOf(kanto to 1, unova to 1)
      }

      test("earned in both: one stack per region, each on its own page") {
        BagRegions.stacks(339, 2, setOf(KantoFlags.FLAG_GOT_HM01, HoennFlags.FLAG_RECEIVED_HM_CUT)) shouldBe
            listOf(kanto to 1, hoenn to 1)
      }

      test("a shared GBA item with no receipt flag stays visible rather than vanish") {
        BagRegions.stacks(339, 1, none) shouldBe listOf(everywhere to 1)
        BagRegions.stacks(262, 1, none) shouldBe listOf(everywhere to 1) // Old Rod
      }

      test("the rods, Coin Case and S.S. Ticket follow the same per-region rule") {
        BagRegions.stacks(262, 1, setOf(KantoFlags.FLAG_GOT_OLD_ROD)) shouldBe listOf(kanto to 1)
        BagRegions.stacks(260, 1, setOf(HoennFlags.FLAG_RECEIVED_COIN_CASE)) shouldBe listOf(hoenn to 1)
        BagRegions.stacks(265, 1, setOf(KantoFlags.FLAG_GOT_SS_TICKET, HoennFlags.FLAG_RECEIVED_SS_TICKET)) shouldBe
            listOf(kanto to 1, hoenn to 1)
      }

      test("an item off the current page cannot be used there: the Kanto Bicycle in Unova") {
        BagRegions.visibleIn(360, 0, none) shouldBe true
        BagRegions.visibleIn(360, 2, none) shouldBe false
        BagRegions.visibleIn(1132, 2, none) shouldBe true // Rare Candy, every page
        BagRegions.visibleIn(339, 0, setOf(KantoFlags.FLAG_GOT_HM01)) shouldBe true
        BagRegions.visibleIn(339, 2, setOf(KantoFlags.FLAG_GOT_HM01)) shouldBe false
      }

      test("region-tagged items keep their quantity; the wardrobe's stock is on no page") {
        BagRegions.stacks(5420, 2, none) shouldBe listOf(unova to 2)
        BagRegions.stacks(1132, 100, none) shouldBe listOf(everywhere to 100) // Rare Candy
        for (id in CosmeticsRegistry.wardrobeStock.take(3)) BagRegions.single(id) shouldBe HIDDEN_BAG_REGION
      }
    })
