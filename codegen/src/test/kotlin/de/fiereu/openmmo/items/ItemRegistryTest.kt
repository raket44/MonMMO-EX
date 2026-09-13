package de.fiereu.openmmo.items

import de.fiereu.openmmo.items.generated.Items
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class ItemRegistryTest :
    FunSpec({
      // Kotest shares the spec instance, so the tests that register have to work on their own
      // registry rather than a shared one.
      fun registry() = ItemRegistry()

      test("loads the generated catalogue") { registry().size() shouldBeGreaterThan 500 }

      test("Gen 3 machine constants resolve by the move they teach") {
        val r = registry()
        // HM05 is Flash in FireRed and Emerald; the catalogue's own "HM05" (5424) is Gen 5's Waterfall.
        r.idOf(r.byScriptConstant("ITEM_HM05")!!) shouldBe 343
        r.byScriptConstant("ITEM_HM05")!!.name shouldBe "HM Flash"
        r.idOf(r.byScriptConstant("ITEM_HM01")!!) shouldBe 339
        // TM03 is Water Pulse (Misty), not Gen 5's Psyshock. Each resolves to the client's OWN tool
        // for that move - the imported "TM <move>" duplicates are no longer created (2026-09-13).
        fun taught(constant: String) = ClientTools.itemToMove[r.idOf(r.byScriptConstant(constant)!!)]
        taught("ITEM_TM03") shouldBe 352 // Water Pulse, PokeMMO's 1601
        taught("ITEM_TM39") shouldBe 317 // Rock Tomb, Gen 5 TM39 (5366)
        taught("ITEM_TM50") shouldBe 315 // Overheat (5377)
        r.idOf(r.byScriptConstant("ITEM_TM03")!!) shouldBe 1601
      }

      test("the client's own tools resolve by id, copies included (Route 4 hands out 1710 and 1709)") {
        val r = registry()
        r.get(1710)!!.name shouldBe "TM Mega Punch"
        r.get(1709)!!.name shouldBe "TM Mega Kick"
        r.get(7710) shouldBe r.get(1710)
        r.idOf(r.get(7710)!!) shouldBe 1710
      }

      test("resolves the ids the live client sends") {
        val items = registry()

        items.idOf(Items.POKE_BALL) shouldBe 5004
        items.idOf(Items.POTION) shouldBe 5017
        items.idOf(Items.ANTIDOTE) shouldBe 5018
        items.idOf(Items.PARLYZ_HEAL) shouldBe 5022
        items.idOf(Items.PARCEL) shouldBe 5459
      }

      test("carries the price the item table gives") {
        Items.POKE_BALL.price shouldBe 200
        Items.POTION.price shouldBe 300
        Items.ANTIDOTE.price shouldBe 100
        Items.PARLYZ_HEAL.price shouldBe 200
      }

      test("round trips every item through its ids") {
        val items = registry()

        for (item in items.all()) {
          for (id in items.idsOf(item)) {
            items.get(id) shouldBe item
          }
        }
      }

      test("answers nothing for an id no item claims") { registry().get(1).shouldBeNull() }

      test("an item is its identity, so a look-alike does not resolve") {
        val items = registry()

        items.idOrNull(ItemDef(Items.POTION.name, Items.POTION.price)).shouldBeNull()
      }

      test("refuses an id already claimed by another item") {
        val items = registry()

        shouldThrow<IllegalStateException> {
          items.register(ItemDef("Impostor", 0), items.idOf(Items.POTION))
        }
      }

      test("registering the same item twice merges its ids") {
        val items = registry()
        val item = ItemDef("Multi", 10)

        items.register(item, 90002)
        items.register(item, 90001, 90002)

        items.idsOf(item) shouldContainExactly listOf(90001, 90002)
        items.idOf(item) shouldBe 90001
      }

      test("throws by name for an item this build has no id for") {
        val thrown = shouldThrow<IllegalStateException> { registry().idOf(ItemDef("Teachy TV", 0)) }

        thrown.message shouldBe "Item 'Teachy TV' has no id in this build"
      }
    })
