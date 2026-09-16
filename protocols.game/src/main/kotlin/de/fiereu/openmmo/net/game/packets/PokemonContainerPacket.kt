package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.chunkByWireSize
import de.fiereu.openmmo.net.game.codecs.PokemonCodec

data class PokemonContainerPacket(
    val container: PokemonContainer,
    val hasChange: Boolean,
    val delete: Boolean,
    val pokemon: List<Pokemon>,
) {
  init {
    if (delete)
        require(hasChange) {
          "Pokemon container cannot be deleted if the packet doesn't change anything"
        }
    if (delete)
        require(pokemon.isEmpty()) { "Deleting a Pokemon container shouldn't have any pokemon" }
  }
}

private val PokemonListPrefixedU8: Codec<List<Pokemon>> =
    object : Codec<List<Pokemon>> {
      override fun read(buf: ReadBuffer): List<Pokemon> {
        val n = U8.read(buf)
        return List(n) { PokemonCodec.read(buf) }
      }

      override fun write(buf: WriteBuffer, value: List<Pokemon>) {
        U8.write(buf, value.size)
        value.forEach { PokemonCodec.write(buf, it) }
      }
    }

object PokemonContainerPacketCodec : PacketCodec<PokemonContainerPacket>() {
  override fun CodecScope<PokemonContainerPacket>.body(): PokemonContainerPacket {
    val container = PokemonContainer.entries[field(U8) { it.container.ordinal }]
    val flags =
        field(U8) {
          var f = 0
          if (it.hasChange) f = f or 1
          if (it.delete) f = f or 2
          f
        }
    val hasChange = (flags and 1) != 0
    val delete = (flags and 2) != 0
    if (delete) {
      return PokemonContainerPacket(container, hasChange, true, emptyList())
    }
    val pokemon = field(PokemonListPrefixedU8, PokemonContainerPacket::pokemon)
    return PokemonContainerPacket(container, hasChange, false, pokemon)
  }
}

/**
 * A whole container as packets the client can inflate: the first carries the change flag (f/r99.c00
 * registers a fresh container and files its monsters), the rest arrive without it and are filed
 * into that container one by one - the same path a boxed catch uses. The list count is a byte, so
 * a run never exceeds 255 monsters either.
 */
fun containerPackets(container: PokemonContainer, pokemon: List<Pokemon>): List<PokemonContainerPacket> {
  val runs = chunkByWireSize(pokemon, PokemonCodec, maxCount = 255)
  if (runs.isEmpty()) return listOf(PokemonContainerPacket(container, hasChange = true, delete = false, pokemon = emptyList()))
  return runs.mapIndexed { i, run ->
    PokemonContainerPacket(container, hasChange = i == 0, delete = false, pokemon = run)
  }
}
