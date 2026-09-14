package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Section 6's MonMMO-EX bit 0x4000: the dex listing byte, after the types, as f/fi7 reads it. */
class SpeciesDexListingTest :
    FunSpec({
      test("the listing byte follows the types and round-trips") {
        val record =
            SpeciesDetail(
                speciesId = 1147,
                flags = SpeciesDetail.TYPES or SpeciesDetail.DEX_LISTING,
                types = 17 to 0,
                dexListing = SpeciesDetail.LISTED_OUTSIDE_NATIONAL)
        val bytes = SpeciesDetailCodec.encode(listOf(record))
        bytes.toList().takeLast(3) shouldBe listOf<Byte>(17, 0, 2)
        SpeciesDetailCodec.decode(bytes) shouldBe listOf(record)
      }

      test("a record without the bit carries no listing byte") {
        val record = SpeciesDetail(speciesId = 1215, flags = SpeciesDetail.TYPES, types = 13 to 13)
        SpeciesDetailCodec.encode(listOf(record)).size shouldBe 8
      }
    })
