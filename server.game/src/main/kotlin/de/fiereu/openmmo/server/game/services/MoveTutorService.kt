package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.server.game.battle.BattlePacketEmitter
import de.fiereu.openmmo.common.MAX_MOVE_SLOTS
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacket
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private val log = KotlinLogging.logger {}

/**
 * The ROM's move tutors (`special ChooseMonForMoveTutor`, pokefirered/pokeemerald party_menu.c):
 * the script has asked and been told yes, set VAR_0x8005 to the tutor's move index, and now wants
 * the party menu opened so the player picks the monster to teach. The GBA answers VAR_RESULT = 1
 * once the move sits in a slot, 0 when the player backs out or gives up on forgetting.
 *
 * The client has no GBA party menu, so the pick runs through its species-list window (the one the
 * Hoenn starter choice uses) over the party's species, and the "which move to forget" step is the
 * client's own forget dialog - the same one level-up moves open, answered through the same reply
 * packet (see [BattleService.offerMove]).
 *
 * Compatibility comes from each ROM's tutor_learnsets.h (`monmmo/tutor-learnsets-<region>.csv`:
 * national dex id, then the move ids that ROM's tutors can teach it). The three starter moves
 * (Frenzy Plant, Blast Burn, Hydro Cannon) are gated on the final evolutions like party_menu.c.
 */
@Singleton
class MoveTutorService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val moveRegistry: MoveRegistry,
    private val dialog: DialogService,
    private val battles: BattleService,
    private val emitter: BattlePacketEmitter,
) {
  private val kantoLearnsets: Map<Int, Set<Int>> by lazy { load("/monmmo/tutor-learnsets-kanto.csv") }
  private val hoennLearnsets: Map<Int, Set<Int>> by lazy { load("/monmmo/tutor-learnsets-hoenn.csv") }

  /** The move a tutor index names in [region]'s ROM, or null when the index is unknown there. */
  fun moveFor(regionId: Int, tutorIndex: Int): Int? =
      when (Region.byId(regionId)) {
        Region.HOENN -> HOENN_TUTOR_MOVES.getOrNull(tutorIndex)
        else -> KANTO_TUTOR_MOVES.getOrNull(tutorIndex)
      }

  /**
   * Runs the whole pick-and-teach exchange for the player of [state] at the tutor [npcEntityId].
   * True once the move is in a slot; false when the player closed the picker or declined to
   * forget a move.
   */
  suspend fun tutor(session: SessionContext, state: PlayerState, npcEntityId: Long, tutorIndex: Int): Boolean {
    val charId = state.characterId ?: return false
    val moveId = moveFor(state.regionId, tutorIndex)
    if (moveId == null) {
      log.warn { "char=$charId: no tutor move for index $tutorIndex in region ${state.regionId}" }
      return false
    }
    val learnsets = if (Region.byId(state.regionId) == Region.HOENN) hoennLearnsets else kantoLearnsets
    while (true) {
      val party = characterStore.getCharacter(charId)?.pokemon ?: return false
      if (party.isEmpty()) return false
      val pick =
          dialog.chooseFromSpecies(
              session, state, WHICH_MON_TEXT, party.map { if (it.isEgg) 0 else clientSpeciesId(it.dexId) })
      val mon = party.getOrNull(pick - 1) ?: return false
      when {
        mon.isEgg -> dialog.showAndWait(session, state, EGG_TEXT, NPC_BOX, npcEntityId)
        mon.moves.any { it.id.toInt() == moveId } ->
            dialog.showAndWait(session, state, CANNOT_TEACH_TEXT, NPC_BOX, npcEntityId)
        !canLearn(learnsets, mon.dexId, moveId) ->
            dialog.showAndWait(session, state, CANNOT_TEACH_TEXT, NPC_BOX, npcEntityId)
        else -> return teach(session, charId, mon.id, moveId)
      }
    }
  }

  private fun canLearn(learnsets: Map<Int, Set<Int>>, dexId: Int, moveId: Int): Boolean =
      when (moveId) {
        FRENZY_PLANT -> dexId == VENUSAUR
        BLAST_BURN -> dexId == CHARIZARD
        HYDRO_CANNON -> dexId == BLASTOISE
        else -> learnsets[dexId]?.contains(moveId) == true
      }

  /** A free slot takes the move at once; a full moveset goes through the client's forget dialog. */
  private suspend fun teach(session: SessionContext, charId: Long, monId: Long, moveId: Int): Boolean {
    val def = moveRegistry.get(moveId) ?: return false
    val stored = characterStore.getCharacter(charId)?.pokemon?.firstOrNull { it.id == monId } ?: return false
    val moves = stored.moves.toMutableList()
    val slot = moves.indexOfFirst { it.id.toInt() == 0 }
    if (slot >= 0 || moves.size < MAX_MOVE_SLOTS) {
      val move = PokemonMove(moveId.toShort(), def.pp.toByte())
      if (slot >= 0) moves[slot] = move else moves += move
      characterStore.updatePokemon(charId, stored.copy(moves = moves))
      characterStore.flushCharacterAsync(charId)
      // The client prints "{mon} learned {move}!" for a slot of 0-3 and refreshes the moveset
      // from the delta, exactly as after a level-up.
      val taken = moves.indexOfFirst { it.id.toInt() == moveId }
      session.send(MoveLearnPromptPacket(monId, taken.toByte(), moveId.toShort()))
      session.send(emitter.moveSlotsDelta(monId, moves.map { it.id to it.pp }, 0))
      log.info { "char=$charId tutor taught move $moveId to $monId in slot $taken" }
      return true
    }
    val answer = CompletableDeferred<Boolean>()
    battles.offerMove(session, charId, monId, moveId.toShort()) { answer.complete(it) }
    val taught = withTimeoutOrNull(FORGET_DIALOG_TIMEOUT_MILLIS) { answer.await() } ?: false
    log.info { "char=$charId tutor move $moveId for $monId: ${if (taught) "replaced a move" else "declined"}" }
    return taught
  }

  private fun load(resource: String): Map<Int, Set<Int>> {
    val stream = MoveTutorService::class.java.getResourceAsStream(resource)
    if (stream == null) {
      log.warn { "missing tutor learnset table $resource" }
      return emptyMap()
    }
    return stream.bufferedReader().useLines { lines ->
      lines
          .filter { it.isNotBlank() && !it.startsWith("#") }
          .map { line -> line.split(',').map { it.trim().toInt() } }
          .associate { it.first() to it.drop(1).toSet() }
    }
  }

  private companion object {
    /** Client string "Which {mon} should be tutored?" - the retail tutor's own prompt. */
    const val WHICH_MON_TEXT = 16779003
    /** Client string "Not even a Move Master can teach an Egg!". */
    const val EGG_TEXT = 16779006
    /** Client string "Sorry, I don't have anything I can teach that {mon}!". */
    const val CANNOT_TEACH_TEXT = 16779007
    /** Dialog action type of an npc speech box. */
    const val NPC_BOX = 4
    const val FORGET_DIALOG_TIMEOUT_MILLIS = 180_000L

    const val FRENZY_PLANT = 338
    const val BLAST_BURN = 307
    const val HYDRO_CANNON = 308
    const val VENUSAUR = 3
    const val CHARIZARD = 6
    const val BLASTOISE = 9

    /** pokefirered MOVETUTOR_* order (include/constants/moves.h) -> move id. */
    val KANTO_TUTOR_MOVES =
        listOf(5, 14, 25, 34, 38, 68, 69, 102, 118, 135, 138, 86, 153, 157, 164, FRENZY_PLANT, BLAST_BURN, HYDRO_CANNON)

    /** pokeemerald TUTOR_MOVE_* order (include/constants/party_menu.h) -> move id. */
    val HOENN_TUTOR_MOVES =
        listOf(5, 14, 25, 34, 38, 68, 69, 102, 118, 135, 138, 86, 153, 157, 164, 223, 205, 244, 173, 196, 203, 189, 8, 207, 214, 129, 111, 9, 7, 210)
  }
}
