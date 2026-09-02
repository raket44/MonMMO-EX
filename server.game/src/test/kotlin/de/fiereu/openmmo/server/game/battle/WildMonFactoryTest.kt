package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.LearnsetRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.storage.EntityIdService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class WildMonFactoryTest :
    FunSpec({
      val factory =
          WildMonFactory(SpeciesRegistry(), MoveRegistry(), LearnsetRegistry(), EntityIdService())

      test("the same seed rolls the same monster") {
        val a = factory.create(19, 3, BattleRng(seed = 42))!!
        val b = factory.create(19, 3, BattleRng(seed = 42))!!
        a.seed shouldBe b.seed
        a.iVs shouldBe b.iVs
        a.nature shouldBe b.nature
      }

      test("IVs stay in the legal range and hp equals the computed maximum") {
        val species = SpeciesRegistry()
        repeat(50) { i ->
          val mon = factory.create(19, 5, BattleRng(seed = i.toLong()))!!
          mon.iVs.values.forEach { it.toInt() shouldBeInRange 0..31 }
          val stats = StatCalculator.computeAll(species.get(19)!!, mon)
          mon.hp shouldBe stats.hp.toShort()
        }
      }

      // MECHANICS tests on a synthetic species with a pinned learnset (id 55000 resolves through
      // no live data source), so the assertions survive the expansion/retail/decomp precedence.
      val pinnedSpecies = SpeciesRegistry()
      val pinnedLearnsets = LearnsetRegistry()
      pinnedSpecies.register(pinnedSpecies.get(19)!!.copy(id = 55_000))
      pinnedLearnsets.register(
          55_000,
          listOf(
              de.fiereu.openmmo.pokemon.LevelUpMove(1, 33),
              de.fiereu.openmmo.pokemon.LevelUpMove(4, 45),
              de.fiereu.openmmo.pokemon.LevelUpMove(21, 230),
              de.fiereu.openmmo.pokemon.LevelUpMove(25, 74),
              de.fiereu.openmmo.pokemon.LevelUpMove(29, 235),
              de.fiereu.openmmo.pokemon.LevelUpMove(33, 76),
          ))
      val pinnedFactory =
          WildMonFactory(pinnedSpecies, MoveRegistry(), pinnedLearnsets, EntityIdService())

      test("the moveset is the level up moveset with registry pp") {
        val mon = pinnedFactory.create(55_000, 5, BattleRng(seed = 7))!!
        mon.moves.map { it.id.toInt() } shouldBe listOf(33, 45, 0, 0)
        mon.moves[0].pp shouldBe MoveRegistry().get(33)!!.pp.toByte()
        mon.moves[1].pp shouldBe MoveRegistry().get(45)!!.pp.toByte()
      }

      test("a high level monster keeps only the last four moves") {
        val mon = pinnedFactory.create(55_000, 100, BattleRng(seed = 7))!!
        mon.moves.map { it.id.toInt() } shouldBe listOf(230, 74, 235, 76)
      }

      test("xp matches the species growth curve at the rolled level") {
        val mon = factory.create(19, 7, BattleRng(seed = 1))!!
        mon.xp shouldBe ExpCurves.totalXpFor(SpeciesRegistry().get(19)!!.growthRate, 7)
      }

      test("an unknown species returns null") {
        factory.create(9999, 5, BattleRng(seed = 1)).shouldBeNull()
      }

      test("Expansion species can be constructed without changing the Pokemon model") {
        val sylveon = factory.create(0x10000 + 700, 25, BattleRng(seed = 9))
        val sprigatito = factory.create(0x10000 + 1289, 10, BattleRng(seed = 10))

        sylveon.shouldNotBeNull()
        sylveon.dexId shouldBe 0x10000 + 700
        sprigatito.shouldNotBeNull()
        sprigatito.dexId shouldBe 0x10000 + 1289
      }

      test("mapped Gen 5 Expansion control uses the established client id") {
        factory.create(495, 10, BattleRng(seed = 11))!!.dexId shouldBe 495
      }

      test("the wild id carries the monster tag") {
        val mon = factory.create(19, 3, BattleRng(seed = 3))!!
        mon.shouldNotBeNull()
        (mon.id and 0xFFFF) shouldBe 0xC000L
      }
    })
