package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.enums.Ability
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec

/**
 * Reports what the server drops from the Expansion catalogue. Species metadata imports cleanly, but
 * abilities and moves are resolved against the server's own enums, and anything it does not know is
 * silently degraded: an unknown ability becomes NONE, an unknown move is filtered out of the
 * starting moveset. This puts numbers on that so it is visible rather than assumed.
 */
class ExpansionCoverageTest :
    FunSpec({
      val expansion = ExpansionSpeciesRegistry()
      val moves = MoveRegistry()
      val known = Ability.entries.map { it.name }.toSet()

      test("ability and move coverage across the species the client now receives") {
        val staged = expansion.all().filter { it.isNewToClient && it.clientContentCompatible }

        val missingAbilities =
            staged
                .flatMap { it.abilitySymbols }
                .filterNot { it == "ABILITY_NONE" }
                .map { it.removePrefix("ABILITY_") }
                .filterNot(known::contains)
                .toSortedSet()
        val speciesLosingAbility =
            staged.count { entry ->
              entry.abilitySymbols.any {
                it != "ABILITY_NONE" && it.removePrefix("ABILITY_") !in known
              }
            }

        val learnset = staged.flatMap { it.levelUpLearnset }
        val droppedMoves =
            learnset.map { it.originalMoveId }.filter { moves.get(it) == null }.toSortedSet()
        val speciesLosingMoves =
            staged.count { entry ->
              entry.levelUpLearnset.any { moves.get(it.originalMoveId) == null }
            }

        println("[coverage] staged species: ${staged.size}")
        println(
            "[coverage] abilities: server knows ${Ability.entries.size}; " +
                "${missingAbilities.size} distinct Expansion abilities are unknown, " +
                "affecting $speciesLosingAbility species")
        println("[coverage] first unknown abilities: ${missingAbilities.take(12)}")
        println(
            "[coverage] moves: server knows ${moves.size()}; " +
                "${droppedMoves.size} distinct learnset moves are unknown, " +
                "affecting $speciesLosingMoves species " +
                "(${learnset.count { moves.get(it.originalMoveId) == null }} of ${learnset.size} entries)")
      }
    })
