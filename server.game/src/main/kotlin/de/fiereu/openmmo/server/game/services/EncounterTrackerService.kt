package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.EncounterTrackerEntry
import de.fiereu.openmmo.net.game.packets.EncounterTrackerHit
import de.fiereu.openmmo.net.game.packets.EncounterTrackerPinPacket
import de.fiereu.openmmo.net.game.packets.EncounterTrackerStatePacket
import de.fiereu.openmmo.net.game.packets.EncounterTrackerUpdatePacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The server side of the client's Encounter Tracker (see EncounterTrackerPackets): the client
 * displays what it is sent and adds each 0x8E to its kinds itself, so the server mirrors the
 * same arithmetic into the character's story vars and replays the sets at login. Counts live
 * per character under `tracker/<kind>/...` keys (ints; last-seen is stored in seconds).
 */
@Singleton
class EncounterTrackerService @Inject constructor(private val characterStore: CharacterStore) {

  /** A wild encounter on the field: one monster, no alpha. */
  fun onWildEncounter(session: SessionContext, charId: Long, dexId: Int, type: Int = TYPE_WILD, alpha: Boolean = false) {
    // Rows are keyed by the client's species id (the pin packet answers with it).
    val species = de.fiereu.openmmo.common.clientSpeciesId(dexId)
    val kinds = kindsFor(type, alpha)
    val now = (System.currentTimeMillis() / 1000L).toInt()
    for (kind in kinds) {
      bump(charId, key(kind, "total"))
      bump(charId, key(kind, "all"))
      bump(charId, key(kind, species, "run"))
      bump(charId, key(kind, species, "all"))
      characterStore.setStoryVar(charId, key(kind, species, "last"), now)
    }
    characterStore.flushCharacterAsync(charId)
    session.send(EncounterTrackerUpdatePacket(type.toByte(), listOf(EncounterTrackerHit(species.toShort(), alpha)), 1))
  }

  /**
   * Every kind's set at login: the client's update packets look each kind up by id and an unsent
   * kind is a missing map entry, so all eleven go out, empty ones included.
   */
  fun sendState(session: SessionContext, stored: StoredCharacter) {
    val vars = stored.storyVars
    for (kind in 0..LAST_KIND) {
      val prefix = "$PREFIX/$kind/"
      val species = HashSet<Int>()
      for (k in vars.keys) {
        if (!k.startsWith(prefix)) continue
        val rest = k.removePrefix(prefix)
        val slash = rest.indexOf('/')
        if (slash > 0) rest.substring(0, slash).toIntOrNull()?.let(species::add)
      }
      val entries =
          species.sorted().map { dex ->
            EncounterTrackerEntry(
                species = dex.toShort(),
                count = vars[key(kind, dex, "run")] ?: 0,
                allTime = vars[key(kind, dex, "all")] ?: 0,
                lastMillis = (vars[key(kind, dex, "last")] ?: 0) * 1000L,
                pinned = (vars[key(kind, dex, "pin")] ?: 0) != 0,
            )
          }
      session.send(
          EncounterTrackerStatePacket(
              kind = kind.toByte(),
              reset = true,
              // Never a redraw here: the HUD may not exist yet (f/NA0.uv null -> the client's handler dies).
              refresh = false,
              total = vars[key(kind, "total")] ?: 0,
              allTime = vars[key(kind, "all")] ?: 0,
              entries = entries,
          ))
    }
  }

  fun onPin(event: PacketEvent<EncounterTrackerPinPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val p = event.packet
    characterStore.setStoryVar(charId, key(p.kind.toInt(), p.species.toInt(), "pin"), if (p.pinned) 1 else 0)
    characterStore.flushCharacterAsync(charId)
    log.info { "Encounter tracker: char=$charId kind=${p.kind} species=${p.species} pinned=${p.pinned}" }
  }

  private fun bump(charId: Long, key: String) {
    val current = characterStore.getCharacter(charId)?.storyVars?.get(key) ?: 0
    characterStore.setStoryVar(charId, key, current + 1)
  }

  companion object {
    const val PREFIX = "tracker"
    const val LAST_KIND = 10

    /** f/CE0 encounter types. */
    const val TYPE_WILD = 0
    const val TYPE_SWEET_SCENT = 1
    const val TYPE_RAID = 2
    const val TYPE_FOSSIL = 3
    const val TYPE_MYSTERIOUS = 4
    const val TYPE_EGG = 5

    /** f/Qm0 kinds. */
    const val KIND_LAST_SHINY = 0
    const val KIND_ROLLOVER = 1
    const val KIND_WILD = 2
    const val KIND_ALPHA = 3
    const val KIND_RAID = 4
    const val KIND_MYSTERIOUS = 5
    const val KIND_EGG = 6
    const val KIND_FOSSIL = 7
    const val KIND_TRIP = 8
    const val KIND_SWEET_SCENT = 9
    const val KIND_WILD_OTHER = 10

    /** f/Qm0.vQ1: counted on every encounter. */
    private val ALWAYS = listOf(KIND_LAST_SHINY, KIND_ROLLOVER, KIND_TRIP)

    /** f/CE0.lU0: the kinds each encounter type counts into. */
    private val BY_TYPE =
        mapOf(
            TYPE_WILD to listOf(KIND_WILD, KIND_WILD_OTHER),
            TYPE_SWEET_SCENT to listOf(KIND_SWEET_SCENT),
            TYPE_RAID to listOf(KIND_RAID),
            TYPE_FOSSIL to listOf(KIND_FOSSIL),
            TYPE_MYSTERIOUS to listOf(KIND_MYSTERIOUS),
            TYPE_EGG to listOf(KIND_EGG),
        )

    fun kindsFor(type: Int, alpha: Boolean): List<Int> =
        ALWAYS + BY_TYPE[type].orEmpty() + (if (alpha) listOf(KIND_ALPHA) else emptyList())

    fun key(kind: Int, field: String) = "$PREFIX/$kind/$field"

    fun key(kind: Int, dex: Int, field: String) = "$PREFIX/$kind/$dex/$field"
  }
}
