package de.fiereu.openmmo.moves

import de.fiereu.openmmo.moves.generated.GeneratedMoves
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoveRegistry @Inject constructor() {

  private val moves = ConcurrentHashMap<Int, MoveDef>()

  init {
    GeneratedMoves.loadInto(this)
    // Moves the client already has under its own ids (project owner, 2026-09-13: reuse retail).
    // Learnsets name the client's id, so the Expansion's mechanics answer to it as well.
    RETAIL_ALIASES.forEach { (expansionId, clientId) ->
      val move = moves[expansionId] ?: return@forEach
      moves.putIfAbsent(clientId, move.copy(id = clientId))
    }
  }

  fun register(move: MoveDef) {
    moves[move.id] = move
  }

  fun get(id: Int): MoveDef? = moves[id]

  fun all(): Collection<MoveDef> = moves.values

  fun size(): Int = moves.size

  private companion object {
    /** Expansion move id to the client's own id for the same move (Trick-or-Treat, Bouncy Bubble). */
    val RETAIL_ALIASES = mapOf(567 to 1000, 680 to 1019)
  }
}
