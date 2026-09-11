package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.ReleasePokemonPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Releasing a monster (c2s 0x0C, one packet per monster). The client has already asked "Are you
 * sure you want to release {00}?" and only closes the summary window afterwards, so the server
 * removes the monster and re-sends the container it lived in; the client rebuilds that container
 * from the packet (f/yT0 with the change flag). A monster carrying an item is refused, matching
 * the client's own "cannot be released because it is locked" rule for held items (f/a.dB1 warns
 * about the item first).
 */
@Singleton
class ReleaseService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val battleService: BattleService,
    private val dexProgress: DexProgressService,
) {

  suspend fun onRelease(event: PacketEvent<ReleasePokemonPacket>) {
    val session = event.session
    val charId = session.attributes[PLAYER_STATE]?.characterId ?: return
    val monsterId = event.packet.monsterId
    if (battleService.inBattle(charId)) {
      log.info { "Release refused in battle: char=$charId monster=$monsterId" }
      return
    }
    val stored = characterStore.getCharacter(charId) ?: return
    val mon = (stored.pokemon + stored.pcStorage).firstOrNull { it.id == monsterId }
    if (mon == null) {
      log.info { "Release of unknown monster: char=$charId monster=$monsterId" }
      return
    }
    // The last party member stays: the client never offers the button for it, but the packet is
    // cheap to forge and an empty party cannot enter the world.
    if (mon.container == PokemonContainer.PARTY && stored.pokemon.size <= 1) {
      log.info { "Release refused, last party member: char=$charId monster=$monsterId" }
      return
    }
    if (!characterStore.removePokemon(charId, monsterId)) {
      log.error { "Release failed to persist: char=$charId monster=$monsterId" }
      return
    }
    val after = characterStore.getCharacter(charId) ?: return
    val remaining = if (mon.container == PokemonContainer.PARTY) after.pokemon else after.pcStorage
    session.send(
        PokemonContainerPacket(
            container = mon.container,
            hasChange = true,
            delete = false,
            pokemon = remaining.toList(),
        ))
    dexProgress.refresh(session, charId)
    log.info { "Released: char=$charId monster=$monsterId dex=${mon.dexId} from ${mon.container}, ${remaining.size} left" }
  }
}
