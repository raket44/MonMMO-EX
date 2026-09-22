package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.items.ItemRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * One script constant, one right answer per region: the catalogue lists a Gen 3 and a Gen 5
 * namesake for most key items, and the name index used to hand a Kanto script the Gen 5 one
 * (the owner's Secret Key and S.S. Tickets turned up on Unova's bag page, 2026-09-22).
 */
class ItemConstantRegionTest :
    FunSpec({
      val items = ItemRegistry()
      fun id(token: String, region: Int?) = items.byScriptConstant(token, region)?.let { items.idsOf(it) }

      test("the same constant is the Gen 3 item in Kanto and the Gen 5 item in Unova") {
        id("ITEM_SUPER_ROD", 0)!! shouldBe listOf(264)
        id("ITEM_SUPER_ROD", 1)!! shouldBe listOf(264)
        id("ITEM_SUPER_ROD", 2)!!.first() shouldBe 5447
        id("ITEM_TOWN_MAP", 0)!! shouldBe listOf(358)
        id("ITEM_TOWN_MAP", 2)!!.first() shouldBe 5442
      }

      test("the five that leaked onto Unova's page now resolve to Kanto's ids") {
        id("ITEM_SS_TICKET", 0)!! shouldBe listOf(265)
        id("ITEM_SECRET_KEY", 0)!! shouldBe listOf(351)
        id("ITEM_COIN_CASE", 0)!! shouldBe listOf(260)
        id("ITEM_OLD_ROD", 0)!! shouldBe listOf(262)
        id("ITEM_GOOD_ROD", 0)!! shouldBe listOf(263)
      }

      test("Hoenn's bikes are their Gen 3 ids, not the 1000 band (Super Carbos, Multi-Vitamin Pack)") {
        id("ITEM_ACRO_BIKE", 1)!! shouldBe listOf(272)
        id("ITEM_MACH_BIKE", 1)!! shouldBe listOf(259)
        HOENN_BIKE_ITEMS shouldBe listOf(259, 272)
      }

      test("Kanto-only key items and the Bicycle are unchanged") {
        id("ITEM_OAKS_PARCEL", 0)!! shouldBe listOf(349)
        id("ITEM_SILPH_SCOPE", 0)!! shouldBe listOf(359)
        items.byScriptConstant("ITEM_BICYCLE", 0)!!.let { items.idOf(it) } shouldBe 360
      }

      test("machines still route by move (Roxanne's TM39 is Rock Tomb, Oak's aide's HM05 is Flash)") {
        items.byScriptConstant("ITEM_TM39", 1) shouldBe items.byScriptConstant("ITEM_TM39")
        id("ITEM_HM05", 0)!!.first() shouldBe 343 // one HM item under 343, 1298 and 6397; idOf takes the lowest
      }

      test("without a region the old answer stands, so the support analyzer sees no change") {
        (items.byScriptConstant("ITEM_SUPER_ROD") != null) shouldBe true
        (items.byScriptConstant("ITEM_NOT_A_THING") == null) shouldBe true
      }
    })
