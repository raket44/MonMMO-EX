package de.fiereu.openmmo.items

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Cinnabar Lab scientist names the fossil through `bufferitemname STR_VAR_1, ITEM_OLD_AMBER`
 * (project owner, 2026-09-13: he said "Oh! That is Cut!"). Script item constants must resolve to the
 * item they name, never to a machine that happens to share a number or a mangled name.
 */
class FossilItemConstantsTest :
    FunSpec({
      val registry = ItemRegistry()

      test("the fossils resolve to themselves") {
        registry.byScriptConstant("ITEM_OLD_AMBER")?.name shouldBe "Old Amber"
        registry.byScriptConstant("ITEM_HELIX_FOSSIL")?.name shouldBe "Helix Fossil"
        registry.byScriptConstant("ITEM_DOME_FOSSIL")?.name shouldBe "Dome Fossil"
      }
    })
