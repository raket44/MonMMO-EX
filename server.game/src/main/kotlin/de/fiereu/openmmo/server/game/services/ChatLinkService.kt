package de.fiereu.openmmo.server.game.services

import de.fiereu.bytecodec.GrowableWriteBuffer
import de.fiereu.bytecodec.U8
import de.fiereu.bytecodec.Utf16LeNullTerminated
import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.codecs.PokemonCodec
import de.fiereu.openmmo.net.game.packets.ChatLinkInspectRequestPacket
import de.fiereu.openmmo.net.game.packets.MonsterRecordBookPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The reply to a chat monster-link click (c2s 0x2E). The client only ever builds its summary
 * window from a monster in one of its own containers, and the packet the retail server answers
 * with is not identified yet, so the reply is a switchable probe (/linkprobe N) until one of the
 * candidates opens the window in play (2026-09-11):
 *
 * 0-3 the monster delivered in a container the client shows summaries for (GTL listing, event,
 *     rental party, trade); 4 the dialog kind that carries monster records (wire 59: owner name,
 *     count, records); 5 the loose-record packet alone.
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
    log.info { "Chat link: char=${state.characterId} inspects ${mon.id} (${mon.dexId}) of ${request.ownerId}, probe variant $variant" }
    when (variant) {
      0 -> session.send(inContainer(mon, PokemonContainer.GTS))
      1 -> session.send(inContainer(mon, PokemonContainer.EVENT))
      2 -> session.send(inContainer(mon, PokemonContainer.RENTAL_PARTY))
      3 -> session.send(inContainer(mon, PokemonContainer.TRADE))
      4 -> {
        val out = GrowableWriteBuffer()
        Utf16LeNullTerminated.write(out, owner.info.name)
        U8.write(out, 1)
        PokemonCodec.write(out, mon)
        val seq = state.dialogSeqId
        state.dialogSeqId = seq + 1
        session.send(
            DialogActionPacket(
                flags = seq.toByte(),
                actionType = RECORD_LIST_KIND,
                textId = 0,
                entityId = mon.id,
                contextValue = 0,
                messageArgs = emptyList(),
                detail = out.toByteArray(),
            ))
      }
      else -> session.send(MonsterRecordBookPacket(listOf(mon)))
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
    /** Dialog wire 59 (f/qM1 Cu0): parse case 8 = a string, u8 count, then monster records. */
    const val RECORD_LIST_KIND: Byte = 59
  }
}
