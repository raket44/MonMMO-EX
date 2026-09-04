package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.LearnsetRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.storage.EntityIdService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

class WildAbilitySlotTest :
    FunSpec({
      test("wild Diglett rolls both ability slots") {
        val species = SpeciesRegistry()
        val def = species.get(50)!!
        println("DIAG Diglett ability1=${def.ability1} ability2=${def.ability2}")
        val factory = WildMonFactory(species, MoveRegistry(), LearnsetRegistry(), EntityIdService())
        val rolled = (1..40).map { Abilities.of(def, factory.create(50, 18, BattleRng())!!) }.toSet()
        println("DIAG rolled=$rolled")
        rolled shouldContain Ability.ARENA_TRAP
      }
    })
