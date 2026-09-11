package de.fiereu.openmmo.server.game.script

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.MapEntryScripts
import de.fiereu.openmmo.server.game.services.ScriptMovementService
import de.fiereu.openmmo.server.game.services.ScriptWarpService
import de.fiereu.openmmo.server.game.services.ShopService
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.session.SCRIPT_SNAPSHOT
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

/**
 * Launches a [Script] on the connection's own coroutine scope so it can wait on a dialog without
 * blocking packet handling. Shared by every script trigger (npc/sign interactions and map entry).
 * Script ownership stops a second trigger from starting in parallel; dialog visibility and the
 * lifecycle lock are tracked separately and are always cleaned up when the script ends.
 */
@Singleton
class ScriptRunner
@Inject
constructor(
    private val dialogService: DialogService,
    private val storyService: StoryService,
    private val movementService: ScriptMovementService,
    private val warpService: ScriptWarpService,
    private val storyPlayerService: StoryPlayerService,
    private val battleService: BattleService,
    private val characterStore: CharacterStore,
    private val mapManager: MapManager,
    private val entryScripts: MapEntryScripts,
    private val shopService: ShopService,
    private val developerTools: DeveloperTools? = null,
    private val moveRegistry: de.fiereu.openmmo.moves.MoveRegistry? = null,
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry? = null,
    private val layoutVariants: de.fiereu.openmmo.server.game.services.LayoutVariants? = null,
    private val banners: de.fiereu.openmmo.server.game.services.FieldMoveBanners? = null,
    private val moveTutor: de.fiereu.openmmo.server.game.services.MoveTutorService? = null,
    private val safariService: de.fiereu.openmmo.server.game.services.SafariService? = null,
) {
  fun run(session: SessionContext, state: PlayerState, script: Script, entityId: Long) =
      runAll(session, state, listOf(script), entityId)

  /**
   * Undoes what a script wrote when it does not reach its end. Scenes advance their story var last,
   * so leaving the earlier writes in place would let the next login replay the scene on top of
   * them, granting its rewards twice.
   */
  fun rollBack(session: SessionContext, state: PlayerState, entityId: Long) {
    val snapshot = session.attributes.remove(SCRIPT_SNAPSHOT) ?: return
    val charId = state.characterId ?: return
    log.info { "Rolling back the unfinished script for entity $entityId on character $charId" }
    characterStore.restoreProgress(charId, snapshot)
  }

  /**
   * Runs [scripts] in order on one coroutine so they share the dialog lock, for example a map's
   * on-transition script followed by its matching on-frame cutscene.
   */
  fun runAll(
      session: SessionContext,
      state: PlayerState,
      scripts: List<Script>,
      entityId: Long,
  ) {
    if (scripts.isEmpty()) return
    val scope =
        session.attributes.getOrPut(SCRIPT_SCOPE) {
          CoroutineScope(SupervisorJob() + Dispatchers.Default)
        }
    // A cancelled scope cannot launch, so bail before claiming script ownership.
    if (!scope.isActive) return
    // Check-and-claim under the state's lock: two interaction packets from a mashed A button land
    // on two workers at once, and both passed a bare check before either had claimed the flag -
    // an item ball then paid out twice (two Quick Claws, 2026-09-10).
    synchronized(state) {
      if (state.scriptRunning) return
      state.scriptRunning = true
    }
    // Existing Kotlin scripts rely on the runner's historical whole-script player lock.
    state.lockLocal(entityId)
    // Seize the client's movement controller IMMEDIATELY, on the packet thread - this is what
    // stops the player dead when a coord event (Oak's walk-up, the gym guide) fires mid-stride.
    // The hold is renewed by every dialog and scripted move; the finally below releases it.
    movementService.holdPlayer(session, state)
    val snapshot = state.characterId?.let(characterStore::getCharacter)
    if (snapshot != null) session.attributes[SCRIPT_SNAPSHOT] = snapshot
    val ctx =
        ScriptContext(
            session,
            state,
            entityId,
            dialogService,
            storyService,
            movementService,
            warpService,
            storyPlayerService,
            battleService,
            characterStore,
            mapManager,
            entryScripts,
            shopService,
            developerTools,
            moveRegistry,
            speciesRegistry,
            layoutVariants,
            banners,
            moveTutor,
            safariService,
        )
    scope.launch {
      var finished = false
      var cancelled = false
      try {
        for (script in scripts) {
          val description = describe(script)
          developerTools?.trace { "Script started: $description" }
          try {
            script.run(ctx)
          } finally {
            developerTools?.trace { "Script ended: $description" }
          }
        }
        finished = true
      } catch (e: CancellationException) {
        // The disconnect that cancelled this rolls back itself, in order with the battle flush
        // and the unload. Doing it here as well would race that and lose to the eviction.
        cancelled = true
        throw e
      } catch (e: NotImplementedError) {
        log.info { "Script not ported yet for entity $entityId" }
        // A stub wrote nothing, so there is nothing to undo.
        finished = true
      } catch (e: Exception) {
        developerTools?.trace { "Script error for entity $entityId: ${e.message}" }
        log.error(e) { "Script failed for entity $entityId" }
      } finally {
        if (!cancelled) {
          if (!finished) rollBack(session, state, entityId)
          session.attributes.remove(SCRIPT_SNAPSHOT)
        }
        dialogService.close(session, state)
        state.releaseScriptLock()
        state.scriptRunning = false
        // A script that faded to black and never faded back (or died in between) must not leave
        // the player staring at nothing; a warp in the script owns the screen from then on.
        if (state.screenFaded) {
          state.screenFaded = false
          session.send(de.fiereu.openmmo.net.game.packets.RenderScreenPacket(true))
        }
        // Let a still-animating scripted walk finish before clearing the queue - the clear
        // snaps the player to the endpoint of whatever it interrupts (the lab pull-back
        // "poof"). Hold delays carry no such risk; clearing them is the point.
        runCatching { movementService.awaitSelfActions(state) }
        movementService.releasePlayerHold(session, state)
        runDeferredTrigger(session, state)
      }
    }
  }

  /**
   * The coordinate trigger of a tile the player stepped onto while this script was still
   * finishing, see [PlayerState.deferredTrigger]. Only while they still stand on that tile of
   * that map - a warp or a step away since means the trigger was never entered.
   */
  private fun runDeferredTrigger(session: SessionContext, state: PlayerState) {
    val pending = state.deferredTrigger ?: return
    state.deferredTrigger = null
    if (state.regionId != pending.regionId || state.bankId != pending.bankId || state.mapId != pending.mapId) return
    if (state.x.toInt() != pending.x || state.y.toInt() != pending.y) return
    if (state.blocksNewScript) return
    val charId = state.characterId ?: return
    val map = mapManager.getMap(pending.regionId, pending.bankId, pending.mapId) ?: return
    val script = entryScripts.atCoordinate(charId, map, pending.x, pending.y) ?: return
    log.info { "Coordinate trigger at (${pending.x}, ${pending.y}) deferred behind the previous script fires now" }
    run(session, state, script, entityId = -1)
  }

  private fun describe(script: Script): String =
      if (script is InterpretedScript) "interpreted ${script.program.id.stable}"
      else "Kotlin ${script::class.simpleName ?: script.javaClass.name}"
}
