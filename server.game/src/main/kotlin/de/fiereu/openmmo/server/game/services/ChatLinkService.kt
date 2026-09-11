package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.ChatLinkInspectRequestPacket
import de.fiereu.openmmo.net.game.packets.MonsterRecordBookPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.battle.PcTogglePacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The reply to a chat monster-link click (c2s 0x2E). The client (f/yT0 = 0x13 container packet
 * handler) keeps one container object per container byte; byte 15 (f/Cy.Vg0) is the "shared
 * monster" slot, built as f/po, and f/ln1.fN makes it the selected container ahead of the PC.
 * The 0x27 toggle (f/ka0) then calls f/NA0.CK1 -> dI0, which opens the box window for the
 * selected container, in read-only summary mode (f/ur1.m3, fresh stats) when it is byte 15.
 * So the reply is: the monster delivered into container 15, then 0x27 with `shown`.
 *
 * `/linkprobe N` (developer) can still answer with the older candidates for comparison:
 * 1-3 the monster in a GTL/event/rental container, 5 the loose record-book packet.
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
      return
    }
    val variant = probeVariant
    log.info { "Chat link: char=${state.characterId} views ${mon.id} (${mon.dexId}) of ${request.ownerId}, variant $variant" }
    when (variant) {
      1 -> session.send(inContainer(mon, PokemonContainer.GTS))
      2 -> session.send(inContainer(mon, PokemonContainer.EVENT))
      3 -> session.send(inContainer(mon, PokemonContainer.RENTAL_PARTY))
      5 -> session.send(MonsterRecordBookPacket(listOf(mon)))
      else -> {
        session.send(inContainer(mon, PokemonContainer.SHARED_VIEW))
        session.send(PcTogglePacket(shown = true))
      }
    }
  }

  private fun inContainer(mon: Pokemon, container: PokemonContainer) =
      PokemonContainerPacket(
          container = container,
          hasChange = true,
          delete = false,
          pokemon = listOf(mon.copy(container = container, containerSlot = 0)),
      )
}
