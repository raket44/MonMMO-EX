package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.IncubatorStateRequestPacket
import de.fiereu.openmmo.net.game.packets.containerPackets
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.PC_CAPACITY
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

/** How often due eggs are swept while players are online. */
private val HATCH_TICK = 15.seconds

/**
 * The incubator page's state request and the egg timer itself.
 *
 * c2s 0x70 is the page ASKING for its contents when it opens (client f/fb6.z61), not a button. Both
 * bulk buttons are computed client-side and move monsters with ordinary container drags, so neither
 * needs anything here: "Remove All" (string 1479) is `fb6.SB`, which checks the PC has room and
 * walks the occupied slots, and "Fill All" (1480) is `f/vg5` mode 6.
 *
 * Reading 0x70 as Remove All emptied a player's incubators every time they merely opened the page
 * (owner pressed "I", 2026-09-16).
 *
 * Hatching is a TIMER, not steps: each egg's due time is written as a play-time reading when it is
 * laid (see [Incubators] and BreedingService), so it pauses while the player is offline. A due egg
 * simply stops being an egg and stays in its slot as a hatched monster - the page has its own
 * "Hatched" state for exactly that (strings 1873, 1481), and the client owns the "Oh?" animation
 * with its Enabled / Only Shiny / Disabled setting.
 */
@Singleton
class IncubatorService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val sessions: SessionRegistry,
    scope: CoroutineScope,
) {
  private val tickJob = SupervisorJob()
  private val tickScope = CoroutineScope(scope.coroutineContext + tickJob)
  private var ticker: Job? = null

  fun start() {
    if (ticker != null) return
    ticker =
        tickScope.launch {
          while (isActive) {
            delay(HATCH_TICK)
            for (charId in sessions.onlineCharacterIds()) {
              runCatching { hatchDue(charId) }
                  .onFailure { log.error(it) { "Egg sweep failed for char=$charId" } }
            }
          }
        }
  }

  fun stop() {
    ticker?.cancel()
    ticker = null
  }

  /**
   * c2s 0x70: the page opened. It is a REQUEST, not a button - answer with the current contents and
   * sweep anything already due, so an egg that came due while the player was elsewhere is a baby by
   * the time the page draws.
   */
  suspend fun onStateRequest(event: PacketEvent<IncubatorStateRequestPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    // Sweep anything already due so the page draws babies, not eggs - but send NO container back.
    // The page builds itself from the containers it already holds and only then asks (f/fb6.z61
    // sends this at the end of its build), so a container packet here arrives AFTER the capture and
    // replaces the object the freshly opened page is drawing: its egg slots then ignore every later
    // delta, which is why they alone were not updating live while the PC and the hatch-helper slot
    // were (owner-reported 2026-09-16).
    hatchDue(charId)
  }

  suspend fun removeAll(session: SessionContext, charId: Long) {
    val stored = characterStore.getCharacter(charId) ?: return
    val held = stored.incubator
    if (held.isEmpty()) return
    val taken = stored.boxed.map { it.containerSlot.toInt() }.toMutableSet()
    var moved = 0
    for (mon in held) {
      val slot = (0 until PC_CAPACITY).firstOrNull { it !in taken }
      if (slot == null) {
        log.info { "Incubator remove-all stopped, PC full: char=$charId" }
        break
      }
      taken += slot
      if (!characterStore.moveBetweenContainers(
          charId, PokemonContainer.INCUBATOR, mon.containerSlot.toInt(), PokemonContainer.PC, slot)) {
        log.info { "Incubator remove-all could not move ${mon.id} for char=$charId" }
        continue
      }
      // The slot is free again, so its timer must not outlive the egg that owned it.
      characterStore.setStoryVar(charId, Incubators.hatchVarKey(mon.containerSlot.toInt()), 0)
      moved++
    }
    if (moved == 0) return
    characterStore.flushCharacterAsync(charId)
    log.info { "Incubator remove-all: char=$charId moved=$moved to the PC" }
    resend(session, charId)
  }

  /**
   * Hatches every egg whose play-time deadline has passed. Returns what hatched, so a caller can
   * announce it; the monster keeps its slot and simply stops being an egg.
   */
  suspend fun hatchDue(charId: Long): List<Pokemon> {
    characterStore.bankPlayTime(charId)
    val stored = characterStore.getCharacter(charId) ?: return emptyList()
    val playTime = stored.info.playTimeSeconds
    val hatched = ArrayList<Pokemon>()
    for (egg in stored.incubator) {
      if (!egg.isEgg) continue
      val slot = egg.containerSlot.toInt()
      val due = stored.storyVars[Incubators.hatchVarKey(slot)] ?: continue
      if (due <= 0 || playTime < due) continue
      val baby = egg.copy(isEgg = false)
      characterStore.updateStoredPokemon(charId, baby)
      characterStore.setStoryVar(charId, Incubators.hatchVarKey(slot), 0)
      hatched += baby
      log.info { "Egg hatched: char=$charId slot=$slot dex=${baby.dexId} shiny=${baby.isShiny} alpha=${baby.isAlpha}" }
    }
    if (hatched.isEmpty()) return emptyList()
    characterStore.flushCharacterAsync(charId)
    sessions.getByCharacterId(charId)?.let { resend(it, charId) }
    return hatched
  }

  /**
   * ONLY the incubator. Pushing the PC here replaced the container object an open box window had
   * captured, and every later drag out of the PC was then aimed at a stale slot - the server kept
   * rejecting moves from cells the client still believed were occupied (owner-reported, log
   * 2026-09-16 12:39).
   */
  private fun resend(session: SessionContext, charId: Long) {
    val after = characterStore.getCharacter(charId) ?: return
    containerPackets(PokemonContainer.INCUBATOR, after.incubator).forEach { session.send(it) }
  }
}
