package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pins "retail plus additions": a retail record keeps everything it had, in its own order, and the
 * Expansion can only add to it. The client applies these sections by assignment, so anything that
 * appended a second record instead would silently replace retail.
 */
class RetailMergeTest :
    FunSpec({
      fun moves(vararg pairs: Pair<Int, Int>) = pairs.map { (move, level) -> LevelUpMove(move, level) }

      test("a retail level-up list keeps every entry in order and gains only moves it lacks") {
        val retail = LevelUpLearnset(35, moves(1 to 1, 45 to 1, 47 to 7, 1 to 13))
        val expansion = LevelUpLearnset(35, moves(45 to 1, 574 to 1, 585 to 10, 47 to 40))

        RetailMerge.levelUp(listOf(retail), listOf(expansion)) shouldBe
            listOf(LevelUpLearnset(35, moves(1 to 1, 45 to 1, 574 to 1, 47 to 7, 585 to 10, 1 to 13)))
      }

      test("a species retail has no list for gets the Expansion's list after the retail ones") {
        val retail = LevelUpLearnset(1, moves(33 to 1))
        val newcomer = LevelUpLearnset(700, moves(574 to 1))

        RetailMerge.levelUp(listOf(retail), listOf(newcomer)) shouldBe listOf(retail, newcomer)
      }

      test("additions past the one-byte move count are dropped, never retail entries") {
        val full = LevelUpLearnset(10, List(LevelUpLearnset.MAX_MOVES) { LevelUpMove(it + 1, 1) })

        RetailMerge.levelUp(listOf(full), listOf(LevelUpLearnset(10, moves(600 to 1)))) shouldBe
            listOf(full)
      }

      test("when retail lists a species twice only the last record, the one the client keeps, grows") {
        val first = LevelUpLearnset(5, moves(10 to 1))
        val last = LevelUpLearnset(5, moves(52 to 7))

        RetailMerge.levelUp(listOf(first, last), listOf(LevelUpLearnset(5, moves(600 to 20)))) shouldBe
            listOf(first, LevelUpLearnset(5, moves(52 to 7, 600 to 20)))
      }

      test("egg and teachable lists keep retail's order and append the missing moves per category") {
        val retail = ExtraLearnset(636, ExtraLearnset.EGG_MOVES, listOf(106, 193, 234))
        val expansion =
            listOf(
                ExtraLearnset(636, ExtraLearnset.EGG_MOVES, listOf(37, 106, 193, 428)),
                ExtraLearnset(636, ExtraLearnset.MOVE_LEARNER_TOOLS, listOf(15)),
            )

        RetailMerge.extra(listOf(retail), expansion) shouldBe
            listOf(
                ExtraLearnset(636, ExtraLearnset.EGG_MOVES, listOf(106, 193, 234, 37, 428)),
                ExtraLearnset(636, ExtraLearnset.MOVE_LEARNER_TOOLS, listOf(15)),
            )
      }

      test("a retail detail record takes only the Expansion's egg groups and still encodes") {
        val retail = SpeciesDetail(35, flags = SpeciesDetail.HELD_ITEMS, heldItems = listOf(5545))
        val expansion =
            SpeciesDetail(
                35,
                flags = SpeciesDetail.EGG_GROUPS or SpeciesDetail.FLAG_200,
                eggGroups = 15 to 15,
            )

        val merged = RetailMerge.details(listOf(retail), listOf(expansion))

        merged shouldBe
            listOf(
                retail.copy(
                    flags = SpeciesDetail.HELD_ITEMS or SpeciesDetail.EGG_GROUPS,
                    eggGroups = 15 to 15,
                ))
        // The flag word decides which payloads are read, so it has to agree with the fields.
        SpeciesDetailCodec.decode(SpeciesDetailCodec.encode(merged)) shouldBe merged
      }

      test("a retail detail record gains evolutions and form entries after its own, nothing else") {
        val royal = formEntry(1, 6, 0, 0)
        val retail =
            SpeciesDetail(
                6,
                flags = SpeciesDetail.SPECIAL_VARIANTS or SpeciesDetail.HELD_ITEMS,
                heldItems = listOf(5545),
                specialVariants = listOf(1) + royal)
        val megaX = formEntry(2, 1215, 0, 151215)
        val expansion =
            SpeciesDetail(
                6,
                flags =
                    SpeciesDetail.SPECIAL_VARIANTS or
                        SpeciesDetail.EVOLUTIONS or
                        SpeciesDetail.STATS or
                        SpeciesDetail.ROM_SCALARS,
                stats = List(6) { 1 },
                romScalars = RomScalars(0, 1, 1, 1),
                specialVariants = listOf(1) + megaX,
                evolutions = listOf(ClientEvolution(4, 99, 700)),
            )

        val merged = RetailMerge.details(listOf(retail), listOf(expansion)).single()

        merged shouldBe
            retail.copy(
                flags = retail.flags or SpeciesDetail.EVOLUTIONS,
                specialVariants = listOf(2) + royal + megaX,
                evolutions = listOf(ClientEvolution(4, 99, 700)),
            )
        SpeciesDetailCodec.decode(SpeciesDetailCodec.encode(listOf(merged))) shouldBe listOf(merged)
      }
    })
