package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.EvolutionPromptPacket
import de.fiereu.openmmo.net.game.packets.EvolutionPromptResponsePacket
import de.fiereu.openmmo.net.game.packets.PokedexSpeciesUnlockPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.battle.StatCalculator
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PendingEvolution
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Offers an evolution to the client: the s2c 0x18 prompt makes the client queue its own evolution
 * cinematic (sprite morph, cry, the cancellable return button) for that party monster - after the
 * current battle's remaining events, or right away out of battle. Nothing changes server-side until
 * the client answers; see [EvolutionService.onResponse].
 */
fun promptEvolution(
    ctx: SessionContext,
    state: PlayerState,
    mon: Pokemon,
    targetWire: Int,
    consumeItemId: Int = 0,
) {
  state.pendingEvolutions[mon.id] = PendingEvolution(targetWire, consumeItemId)
  ctx.send(EvolutionPromptPacket(mon.id, targetWire.toShort(), cancellable = true))
}

/**
 * The answer to an evolution prompt. The client sends c2s 0x0B (uid, accepted) from its evolution
 * scene: accepted=true when the morph sequence completes, accepted=false when the player pressed
 * the return button and confirmed the cancel. Only then does the species change - the same order
 * the cartridges use, where B during the animation keeps the monster as it was.
 */
@Singleton
class EvolutionService
@Inject
constructor(
    private val characters: CharacterStore,
    private val species: SpeciesRegistry,
    private val expansion: ExpansionSpeciesRegistry,
    private val dexProgress: DexProgressService,
) {

  suspend fun onResponse(event: PacketEvent<EvolutionPromptResponsePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val monId = event.packet.pokemonEntityId
    val pending = state.pendingEvolutions.remove(monId)
    if (pending == null) {
      log.info { "char=$charId answered an evolution prompt for monster=$monId with none pending" }
      return
    }
    val mon = characters.getCharacter(charId)?.pokemon?.firstOrNull { it.id == monId } ?: return
    val fromName = expansion.getByClientWireId(clientSpeciesId(mon.dexId))?.displayName
        ?: species.get(mon.dexId)?.name ?: "The monster"
    if (!event.packet.accepted) {
      log.info { "char=$charId cancelled the evolution of $fromName (monster=$monId)" }
      ctx.send(notice("Huh? $fromName stopped evolving!"))
      return
    }
    val newDexId = expansion.getByClientWireId(pending.targetWire)?.serverId ?: pending.targetWire
    val evolvedDef = species.get(newDexId)
    val toName =
        expansion.getByClientWireId(pending.targetWire)?.displayName ?: evolvedDef?.name ?: "something new"
    var evolved = mon.copy(dexId = newDexId)
    if (evolvedDef != null) {
      // Max HP rises with the new base stats; current HP carries over, capped at the new max.
      val maxHp = StatCalculator.computeAll(evolvedDef, evolved).hp
      evolved = evolved.copy(hp = mon.hp.toInt().coerceAtMost(maxHp).toShort())
    }
    characters.updatePokemon(charId, evolved)
    if (pending.consumeItemId > 0) {
      characters.addItem(charId, pending.consumeItemId, -1)
      val left = characters.getCharacter(charId)?.items?.get(pending.consumeItemId) ?: 0
      ctx.send(itemStackUpdatePacket(pending.consumeItemId, left))
    }
    characters.getCharacter(charId)?.pokemon?.let { party ->
      ctx.send(
          PokemonContainerPacket(
              container = PokemonContainer.PARTY,
              hasChange = true,
              delete = false,
              pokemon = party,
          ))
    }
    ctx.send(PokedexSpeciesUnlockPacket(pending.targetWire.toShort()))
    dexProgress.refresh(ctx, charId)
    characters.flushCharacterAsync(charId)
    ctx.send(notice("$fromName evolved into $toName!"))
    log.info {
      "char=$charId $fromName (monster=$monId) evolved into $toName (wire ${pending.targetWire})" +
          if (pending.consumeItemId > 0) " using item ${pending.consumeItemId}" else ""
    }
  }
}
