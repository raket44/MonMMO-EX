package de.fiereu.openmmo.moves

import de.fiereu.openmmo.common.enums.DamageCategory
import de.fiereu.openmmo.common.enums.PokemonType
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
    applyClientStats()
  }

  fun register(move: MoveDef) {
    moves[move.id] = move
  }

  fun get(id: Int): MoveDef? = moves[id]

  fun all(): Collection<MoveDef> = moves.values

  fun size(): Int = moves.size

  /**
   * The numbers the client shows are the numbers the battle uses (project owner, 2026-09-13:
   * "client's numbers but our move retyping"): category, accuracy, PP and priority come from the
   * client's own move table, power too wherever both sides give a real number. Our Fairy retypes
   * stay, and the client's 0/1 power markers never overwrite a status or variable-power move -
   * the engine keys those on their power.
   */
  private fun applyClientStats() {
    val stream = MoveRegistry::class.java.getResourceAsStream(CLIENT_STATS) ?: return
    stream.bufferedReader().useLines { lines ->
      for (line in lines) {
        if (line.isBlank() || line.startsWith("#") || line.startsWith("id,")) continue
        val cols = line.split(",")
        val id = cols[0].toIntOrNull() ?: continue
        val move = moves[id] ?: continue
        val clientType = runCatching { PokemonType.valueOf(cols[1]) }.getOrNull()
        val clientPower = cols[3].toIntOrNull() ?: move.power
        val clientAccuracy = cols[4].toIntOrNull() ?: move.accuracy
        moves[id] =
            move.copy(
                type = if (clientType == null || move.type == PokemonType.FAIRY) move.type else clientType,
                category = runCatching { DamageCategory.valueOf(cols[2]) }.getOrDefault(move.category),
                power = if (clientPower > 1 && move.power > 1) clientPower else move.power,
                accuracy = if (clientAccuracy >= NEVER_MISSES) 0 else clientAccuracy,
                pp = cols[5].toIntOrNull()?.takeIf { it > 0 } ?: move.pp,
                priority = cols[6].toIntOrNull() ?: move.priority,
            )
      }
    }
  }

  private companion object {
    /** Expansion move id to the client's own id for the same move (Trick-or-Treat, Bouncy Bubble). */
    val RETAIL_ALIASES = mapOf(567 to 1000, 680 to 1019)

    const val CLIENT_STATS = "/monmmo/client-move-stats.csv"
    /** The client's accuracy for a move that never misses; ours is 0. */
    const val NEVER_MISSES = 101
  }
}
