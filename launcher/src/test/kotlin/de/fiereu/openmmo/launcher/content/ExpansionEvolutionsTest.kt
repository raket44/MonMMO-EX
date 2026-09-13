package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.launcher.content.ExpansionEvolutions.Evolution
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * The Expansion's evolution entries in the client's method vocabulary (f/vj3 ROM keys, read off the
 * r32645 bytecode). The conditions decide the method; a wrong key draws the wrong condition in the
 * dex and evolves the wrong way on the server.
 */
class ExpansionEvolutionsTest :
    FunSpec({
      val moves = mapOf("MOVE_RAGE_FIST" to 889, "MOVE_ANCIENT_POWER" to 246, "MOVE_TRICK_OR_TREAT" to 567)

      fun convert(tuple: String) = ExpansionEvolutions.convert(tuple, moves)

      test("a plain level stays a level") {
        convert("{EVO_LEVEL, 16, SPECIES_QUILLADIN}") shouldBe Evolution(4, 16, "QUILLADIN")
      }

      test("friendship picks the happiness method, with its time of day in the key") {
        convert(
            "{EVO_LEVEL, 0, SPECIES_SYLVEON, CONDITIONS({IF_MIN_FRIENDSHIP, FRIENDSHIP_EVO_THRESHOLD}, {IF_KNOWS_MOVE_TYPE, TYPE_FAIRY})}") shouldBe
            Evolution(1, 0, "SYLVEON")
        convert(
            "{EVO_LEVEL, 0, SPECIES_UMBREON, CONDITIONS({IF_MIN_FRIENDSHIP, FRIENDSHIP_EVO_THRESHOLD}, {IF_TIME, TIME_NIGHT})}") shouldBe
            Evolution(3, 0, "UMBREON")
        convert(
            "{EVO_LEVEL, 0, SPECIES_ESPEON, CONDITIONS({IF_MIN_FRIENDSHIP, FRIENDSHIP_EVO_THRESHOLD}, {IF_NOT_TIME, TIME_NIGHT})}") shouldBe
            Evolution(2, 0, "ESPEON")
      }

      test("items keep the ROM item id and the item method, gendered where the condition says") {
        convert("{EVO_ITEM, ITEM_THUNDER_STONE, SPECIES_RAICHU}") shouldBe Evolution(8, 83, "RAICHU")
        convert("{EVO_ITEM, ITEM_DAWN_STONE, SPECIES_GALLADE, CONDITIONS({IF_GENDER, MON_MALE})}") shouldBe
            Evolution(17, 109, "GALLADE")
        convert(
            "{EVO_LEVEL, 0, SPECIES_WEAVILE, CONDITIONS({IF_HOLD_ITEM, ITEM_RAZOR_CLAW}, {IF_TIME, TIME_NIGHT})}") shouldBe
            Evolution(20, 326, "WEAVILE")
        convert("{EVO_TRADE, 0, SPECIES_SCIZOR, CONDITIONS({IF_HOLD_ITEM, ITEM_METAL_COAT})}") shouldBe
            Evolution(6, 233, "SCIZOR")
      }

      test("a known or used move is the level-with-skill method, with the client's move id") {
        convert("{EVO_LEVEL, 0, SPECIES_TANGROWTH, CONDITIONS({IF_KNOWS_MOVE, MOVE_ANCIENT_POWER})}") shouldBe
            Evolution(21, 246, "TANGROWTH")
        convert("{EVO_LEVEL, 0, SPECIES_ANNIHILAPE, CONDITIONS({IF_USED_MOVE_X_TIMES, MOVE_RAGE_FIST, 20})}") shouldBe
            Evolution(21, 889, "ANNIHILAPE")
      }

      test("a party species is named for the caller to resolve") {
        convert("{EVO_LEVEL, 0, SPECIES_MANTINE, CONDITIONS({IF_SPECIES_IN_PARTY, SPECIES_REMORAID})}") shouldBe
            Evolution(22, 0, "MANTINE", speciesParamSymbol = "REMORAID")
      }

      test("gendered levels and the attack/defense split keep their level") {
        convert("{EVO_LEVEL, 20, SPECIES_COMBEE_QUEEN, CONDITIONS({IF_GENDER, MON_FEMALE})}") shouldBe
            Evolution(24, 20, "COMBEE_QUEEN")
        convert("{EVO_LEVEL, 20, SPECIES_HITMONLEE, CONDITIONS({IF_ATK_GT_DEF})}") shouldBe
            Evolution(9, 20, "HITMONLEE")
      }

      test("a region-gated evolution is the night branch of its split, the other branch any time") {
        convert("{EVO_ITEM, ITEM_THUNDER_STONE, SPECIES_RAICHU_ALOLA, CONDITIONS({IF_REGION, REGION_ALOLA})}") shouldBe
            Evolution(8, 83, "RAICHU_ALOLA", time = "night")
        convert("{EVO_ITEM, ITEM_THUNDER_STONE, SPECIES_RAICHU, CONDITIONS({IF_NOT_REGION, REGION_ALOLA})}") shouldBe
            Evolution(8, 83, "RAICHU")
        convert(
            "{EVO_LEVEL, 28, SPECIES_MAROWAK_ALOLA, CONDITIONS({IF_REGION, REGION_ALOLA}, {IF_TIME, TIME_NIGHT})}") shouldBe
            Evolution(4, 28, "MAROWAK_ALOLA", time = "night")
        ExpansionEvolutions.convert(
            "{EVO_ITEM, ITEM_PEAT_BLOCK, SPECIES_URSALUNA, CONDITIONS({IF_REGION, REGION_HISUI}, {IF_TIME, TIME_NIGHT})}",
            moves,
            importedItems = mapOf("ITEM_PEAT_BLOCK" to 16012),
        ) shouldBe Evolution(8, 16012, "URSALUNA", time = "night")
      }

      test("an item only the full import creates still resolves (Leader's Crest)") {
        ExpansionEvolutions.convert(
            "{EVO_LEVEL, 0, SPECIES_KINGAMBIT, CONDITIONS({IF_DEFEAT_X_WITH_ITEMS, SPECIES_BISHARP, ITEM_LEADERS_CREST, 3})}",
            moves,
            importedItems = mapOf("ITEM_LEADERS_CREST" to 17235),
        ) shouldBe Evolution(19, 17235, "KINGAMBIT")
      }

      test("the Linking Cord is never created, so its stand-in evolutions are dropped, not relabelled") {
        convert("{EVO_ITEM, ITEM_LINKING_CORD, SPECIES_ALAKAZAM}").shouldBeNull()
        convert("{EVO_TRADE, 0, SPECIES_ALAKAZAM}") shouldBe Evolution(5, 0, "ALAKAZAM")
        EvoItemPlan.itemId("ITEM_LINKING_CORD").shouldBeNull()
        // Its slot stays reserved: Black Augurite keeps the id players may already hold.
        EvoItemPlan.itemId("ITEM_BLACK_AUGURITE") shouldBe 21011
      }

      test("a condition no client method describes is special, never a level 0") {
        convert("{EVO_LEVEL, 0, SPECIES_PAWMOT, CONDITIONS({IF_MIN_OVERWORLD_STEPS, 1000})}") shouldBe
            Evolution(0, 0, "PAWMOT")
        convert("{EVO_NONE, 0, SPECIES_RATICATE_ALOLA_TOTEM}").shouldBeNull()
      }

      test("a level at a time of day keeps its level, the time going to the server") {
        convert("{EVO_LEVEL, 25, SPECIES_LYCANROC_MIDNIGHT, CONDITIONS({IF_TIME, TIME_NIGHT})}") shouldBe
            Evolution(4, 25, "LYCANROC_MIDNIGHT", time = "night")
      }
    })
