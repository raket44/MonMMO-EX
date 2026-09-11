package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.ChatLinkInspectRequestPacket
import de.fiereu.openmmo.net.game.packets.MonsterRecordBookPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.SocialProfileDialogOpenPacket
import de.fiereu.openmmo.net.game.packets.battle.PcTogglePacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The reply to a chat monster-link click (c2s 0x2E): s2c 0xB1 (f/WV0) = status byte + monster.
 * Status 0 carries the record and the client opens the standalone summary window (f/NA0.A0 ->
 * f/a, mode f/p40.W11) for it, no container involved. Status 1 shows string 5998 ("Could not
 * find the link you clicked on ... owner may be offline"), status 2 string 5997 ("The owner of
 * that link has blocked you"). Worked out from the bytecode 2026-09-11.
 *
 * `/linkprobe N` (developer) can still answer with the rejected candidates for comparison:
 * 1-3 the monster in a GTL/event/rental container (nothing), 4 the shared-view container + 0x27
 * (opens the box viewer on it), 5 the loose record-book packet (nothing).
 */
@Singleton
class ChatLinkService @Inject constructor(private val characterStore: CharacterStore) {

  @Volatile var probeVariant: Int = 0

  suspend fun onInspect(event: PacketEvent<ChatLinkInspectRequestPacket>) {
    val session = event.session
    val state = session.attributes[PLAYER_STATE] ?: return
    val request = event.packet
    val owner = characterStore.getOrLoadCharacter(request.ownerId)
    val mon = owner?.let { (it.pokemon + it.pcStorage).firstOrNull { p -> p.id == request.monsterId } }
    if (mon == null) {
      log.info { "Chat link: monster ${request.monsterId} of ${request.ownerId} not found for char=${state.characterId}" }
      session.send(SocialProfileDialogOpenPacket(status = STATUS_NOT_FOUND, content = null))
      return
    }
    val variant = probeVariant
    log.info { "Chat link: char=${state.characterId} views ${mon.id} (${mon.dexId}) of ${request.ownerId}, variant $variant" }
    when (variant) {
      1 -> session.send(inContainer(mon, PokemonContainer.GTS))
      2 -> session.send(inContainer(mon, PokemonContainer.EVENT))
      3 -> session.send(inContainer(mon, PokemonContainer.RENTAL_PARTY))
      4 -> {
        session.send(inContainer(mon, PokemonContainer.SHARED_VIEW))
        session.send(PcTogglePacket(shown = true))
      }
      5 -> session.send(MonsterRecordBookPacket(listOf(mon)))
      else -> session.send(SocialProfileDialogOpenPacket(status = STATUS_OK, content = mon))
    }
  }

  private fun inContainer(mon: Pokemon, container: PokemonContainer) =
      PokemonContainerPacket(
          container = container,
          hasChange = true,
          delete = false,
          pokemon = listOf(mon.copy(container = container, containerSlot = 0)),
      )

  private companion object {
    const val STATUS_OK: Byte = 0
    const val STATUS_NOT_FOUND: Byte = 1
  }
}
