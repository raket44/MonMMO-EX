package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.Bool
import de.fiereu.bytecodec.CodecScope
import de.fiereu.bytecodec.PacketCodec
import de.fiereu.bytecodec.S16LE
import de.fiereu.bytecodec.S64LE

/**
 * s2c 0x18 - tells the client one of the player's own monsters is about to evolve.
 *
 * Decoded from the client's reader `f/IK1` (registered for opcode 24 in the in-game phase): the
 * monster's uid, the species it becomes, and a flag byte. The handler finds the party monster by
 * uid, stores the target on it (`gT0.ON0`) and, when the species is at least 1, queues the evolution
 * cinematic (`f/re1` -> the battle scene's `vB` -> `f/rP0`, the sprite-morph sequence with the
 * "Do you wish to cancel the evolution?" return button) on the live battle - or on a fresh battle
 * context when none is open. A species of 0 just clears a stored target.
 *
 * The flag reaches the scene's return button: true lets the player cancel with B. When the sequence
 * finishes the client answers with [EvolutionPromptResponsePacket] (uid, accepted); a cancel sends
 * it with accepted=false. The server applies the species change only on that answer.
 *
 * Previously mis-imported as "SocialEntryPresencePacket" (playerId/status/online) - same bytes.
 */
data class EvolutionPromptPacket(
    val pokemonId: Long,
    val species: Short,
    val cancellable: Boolean,
)

object EvolutionPromptPacketCodec : PacketCodec<EvolutionPromptPacket>() {
  override fun CodecScope<EvolutionPromptPacket>.body(): EvolutionPromptPacket {
    val pokemonId = field(S64LE) { it.pokemonId }
    val species = field(S16LE) { it.species }
    val cancellable = field(Bool) { it.cancellable }
    return EvolutionPromptPacket(pokemonId, species, cancellable)
  }
}
