package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.items.ItemRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Owner's rule (2026-09-22): one TM or HM item per MOVE, never per number - TM27 is Return in
 * one game and something else in another, so the number means nothing. Every game's tool for a
 * move is one item, the stored id is its lowest, and a second grant anywhere stacks on it.
 */
class OneItemPerMoveTest :
    FunSpec({
      val items = ItemRegistry()

      test("HM Cut is one item across every region, stored as 339") {
        val cut = items.get(5420)!!
        cut shouldBe items.get(339)
        cut shouldBe items.get(8420)
        cut shouldBe items.get(9420)
        items.idOf(cut) shouldBe 339
        cut.name shouldBe "HM Cut"
      }

      test("HM Flash and HM Rock Smash are HMs everywhere, TM70/TM94 included") {
        val flash = items.get(343)!!
        flash shouldBe items.get(1298) // the client's universal HM Flash
        flash shouldBe items.get(5397) // Unova's TM70 folds into it
        flash.name shouldBe "HM Flash"
        val smash = items.get(344)!!
        smash shouldBe items.get(1297)
        smash shouldBe items.get(5619) // Unova's TM94
        smash.name shouldBe "HM Rock Smash"
      }

      test("a TM is one item per move too: Gen 3 TM25 Thunder and Gen 5 TM25 Thunder stack") {
        val thunder = items.byScriptConstant("ITEM_TM25", 0)!!
        thunder shouldBe items.get(5352) // Gen 5 TM25 = Thunder (item 352)
        thunder.name shouldBe "TM Thunder"
      }
    })
