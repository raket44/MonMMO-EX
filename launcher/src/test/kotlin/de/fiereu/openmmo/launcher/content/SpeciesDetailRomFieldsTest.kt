package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Section 6's MonMMO-EX bits (0x800 ROM scalars, 0x1000 evolutions, 0x2000 types) are read by the
 * client code in f/fi7 in bit order after the form list; the codec has to write exactly that order.
 */
class SpeciesDetailRomFieldsTest :
    FunSpec({
      val record =
          SpeciesDetail(
              speciesId = 668,
              flags =
                  SpeciesDetail.EGG_GROUPS or
                      SpeciesDetail.SPECIAL_VARIANTS or
                      SpeciesDetail.ROM_SCALARS or
                      SpeciesDetail.EVOLUTIONS or
                      SpeciesDetail.TYPES,
              eggGroups = 5 to 5,
              specialVariants = listOf(1) + formEntry(2, 1215, 0, 151215),
              romScalars = RomScalars(growthRate = 3, baseExp = 63, height = 4, weight = 90),
              evolutions = listOf(ClientEvolution(4, 16, 669), ClientEvolution(8, 5083, 700, time = "night")),
              types = 12 to 19,
          )

      test("the new bits survive a round trip") {
        SpeciesDetailCodec.decode(SpeciesDetailCodec.encode(listOf(record))) shouldBe listOf(record)
      }

      test("the payload after the form list is in the order the client code reads it") {
        val bytes = SpeciesDetailCodec.encode(listOf(record)).map { it.toInt() and 0xff }
        // count(2) + id(2) + flags(2) + egg groups(2) + form list(1 + 17)
        val tail = bytes.drop(2 + 2 + 2 + 2 + 18)
        tail shouldBe
            listOf(3, 63, 0, 4, 0, 90, 0) + // growth, base exp, height, weight
                // two evolutions, each ending in its time byte (0 any, 2 night)
                listOf(2, 4, 16, 0, 0x9d, 0x02, 0, 8, 0xdb, 0x13, 0xbc, 0x02, 2) +
                listOf(12, 19) // types
      }

      test("a form entry is the 17 bytes the client's form list reads") {
        formEntry(2, 1215, 0x01020304, 151215) shouldBe
            listOf(2, 0xbf, 0x04, 0xbf, 0x04, 0, 0, 0x04, 0x03, 0x02, 0x01, 0xaf, 0x4e, 0x02, 0x00, 0, 1)
      }

      test("the EV word puts two bits per stat in the ROM's order") {
        val chespin =
            de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry().get("SPECIES_CHESPIN")!!
        // Chespin yields 1 Defense: bits 4-5.
        evYieldWord(chespin) shouldBe (chespin.evYieldDefense shl 4)
      }
    })
