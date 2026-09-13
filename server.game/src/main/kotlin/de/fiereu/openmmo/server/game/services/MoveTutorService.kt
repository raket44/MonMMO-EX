package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.server.game.battle.BattlePacketEmitter
import de.fiereu.openmmo.common.MAX_MOVE_SLOTS
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnPromptPacket
import de.fiereu.openmmo.server.game.services.TutorCompatibility.Companion.BLAST_BURN
import de.fiereu.openmmo.server.game.services.TutorCompatibility.Companion.FRENZY_PLANT
import de.fiereu.openmmo.server.game.services.TutorCompatibility.Companion.HYDRO_CANNON
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
 * Compatibility comes from each ROM's tutor_learnsets.h for retail species and from the Expansion's
 * own taught lists for Expansion species - see [TutorCompatibility].
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
  private val compatibility: TutorCompatibility by lazy { TutorCompatibility.load() }

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
    val region = Region.byId(state.regionId)
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
        !compatibility.canLearn(region, mon.dexId, moveId) ->
            dialog.showAndWait(session, state, CANNOT_TEACH_TEXT, NPC_BOX, npcEntityId)
        else -> return teach(session, charId, mon.id, moveId)
      }
    }
  }

  /** [teach] for a script that can wait on the forget dialog. */
  private suspend fun teach(session: SessionContext, charId: Long, monId: Long, moveId: Int): Boolean {
    val answer = CompletableDeferred<Boolean>()
    if (!teach(session, charId, monId, moveId) { answer.complete(it) }) return false
    return withTimeoutOrNull(FORGET_DIALOG_TIMEOUT_MILLIS) { answer.await() } ?: false
  }

  /**
   * Teaches [moveId] to the party monster [monId]: a free slot takes it at once, a full moveset
   * opens the client's forget dialog. [onResult] gets true once the move sits in a slot (right
   * away, or from the dialog's answer) and false when the player gave up. Returns false without
   * calling back when the monster or move does not exist. Shared by the tutors and the bag's TMs
   * and HMs.
   */
  fun teach(session: SessionContext, charId: Long, monId: Long, moveId: Int, onResult: (Boolean) -> Unit): Boolean {
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
      log.info { "char=$charId taught move $moveId to $monId in slot $taken" }
      onResult(true)
      return true
    }
    battles.offerMove(session, charId, monId, moveId.toShort()) { taught ->
      log.info { "char=$charId move $moveId for $monId: ${if (taught) "replaced a move" else "declined"}" }
      onResult(taught)
    }
    return true
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

    /** pokefirered MOVETUTOR_* order (include/constants/moves.h) -> move id. */
    val KANTO_TUTOR_MOVES =
        listOf(5, 14, 25, 34, 38, 68, 69, 102, 118, 135, 138, 86, 153, 157, 164, FRENZY_PLANT, BLAST_BURN, HYDRO_CANNON)

    /** pokeemerald TUTOR_MOVE_* order (include/constants/party_menu.h) -> move id. */
    val HOENN_TUTOR_MOVES =
        listOf(5, 14, 25, 34, 38, 68, 69, 102, 118, 135, 138, 86, 153, 157, 164, 223, 205, 244, 173, 196, 203, 189, 8, 207, 214, 129, 111, 9, 7, 210)
  }
}
