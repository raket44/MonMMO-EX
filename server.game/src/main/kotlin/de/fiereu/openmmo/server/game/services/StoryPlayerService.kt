package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.DEFAULT_MOVE_PP
import de.fiereu.openmmo.common.MAX_PARTY_SIZE
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.items.ItemDef
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.SocialListEntryAddPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleAddPokemon
import de.fiereu.openmmo.net.game.packets.battle.BattleSideAddPokemonPacket
import de.fiereu.openmmo.net.game.packets.battle.ItemStack
import de.fiereu.openmmo.net.game.packets.battle.itemStacksPacket
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.StatCalculator
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.battle.acquiredMonsterDelta
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/** Party, healing, and bag operations used by ROM-derived overworld scripts. */
@Singleton
class StoryPlayerService
@Inject
constructor(
    private val characters: CharacterStore,
    private val pokemonFactory: WildMonFactory,
    private val species: SpeciesRegistry,
    private val moves: MoveRegistry,
    private val items: ItemRegistry,
    private val dexProgress: DexProgressService,
    private val worldState: WorldStateService,
) {

  /** Gives a story Pokemon and syncs it. */
  suspend fun givePokemon(
      session: SessionContext,
      state: PlayerState,
      dexId: Int,
      level: Int,
      /** Null keeps the level-up moves the roll produced (gift monsters). */
      moveIds: List<Int>?,
      isShiny: Boolean = false,
  ): Pokemon? {
    val characterId = state.characterId ?: return null
    val stored = characters.getCharacter(characterId) ?: return null
    if (stored.pokemon.size >= MAX_PARTY_SIZE) return null
    val rolled = pokemonFactory.create(dexId, level, BattleRng()) ?: return null
    val pokemon =
        rolled.copy(
            ownerId = characterId,
            container = PokemonContainer.PARTY,
            containerSlot = stored.pokemon.size.toShort(),
            ot = stored.info.name,
            moves = moveIds?.let(::paddedMoves) ?: rolled.moves,
            isShiny = isShiny,
        )
    // Only tell the client about it once the database has it.
    if (!characters.addPokemon(characterId, pokemon)) return null
    // Send the granted Pokemon's full record.
    session.send(SocialListEntryAddPacket(pokemon))
    species.get(dexId)?.let { session.send(acquiredMonsterDelta(pokemon, it)) }
    session.send(
        PokemonContainerPacket(
            container = PokemonContainer.PARTY,
            hasChange = true,
            delete = false,
            pokemon = characters.getCharacter(characterId)?.pokemon ?: listOf(pokemon),
        ))
    // A gift is both seen and owned; push the refreshed tiers with the new monster.
    dexProgress.refresh(session, characterId)
    return pokemon
  }

  fun healParty(session: SessionContext, state: PlayerState) {
    val characterId = state.characterId ?: return
    val stored = characters.getCharacter(characterId) ?: return
    val healed =
        stored.pokemon.map { pokemon ->
          val definition = species.get(pokemon.dexId) ?: return@map pokemon
          pokemon.copy(
              hp = StatCalculator.computeAll(definition, pokemon).hp.toShort(),
              status = de.fiereu.openmmo.common.StatusCondition.NONE,
              moves =
                  pokemon.moves.map { move ->
                    val maxPp = moves.get(move.id.toInt())?.pp ?: move.pp.toInt()
                    PokemonMove(move.id, maxPp.toByte())
                  },
          )
        }
    healed.forEach { characters.updatePokemon(characterId, it) }
    // The spot that healed last is where a whiteout returns the player.
    val info = stored.info
    characters.setStoryVar(
        characterId,
        RespawnPoint.MAP_KEY,
        (info.positionRegionId.toInt() shl 16) or
            ((info.positionBankId.toInt() and 0xFF) shl 8) or
            (info.positionMapId.toInt() and 0xFF))
    characters.setStoryVar(
        characterId, RespawnPoint.XY_KEY, (info.positionX.toInt() shl 12) or info.positionY.toInt())
    session.send(
        PokemonContainerPacket(
            container = PokemonContainer.PARTY,
            hasChange = true,
            delete = false,
            pokemon = healed,
        ))
  }

  suspend fun giveItem(
      session: SessionContext,
      state: PlayerState,
      item: ItemDef,
      quantity: Int
  ): Boolean = giveItemById(session, state, items.idOf(item), quantity)

  /**
   * Grant by raw item id - works for server-registered items and the client-generated cosmetic
   * catalog alike. Re-sends the bag (the customization dialog lists from it) AND the local player
   * state (the only carrier of the per-item unlock flags), so a granted cosmetic is usable
   * immediately, no relog.
   */
  suspend fun giveItemById(
      session: SessionContext,
      state: PlayerState,
      itemId: Int,
      quantity: Int
  ): Boolean {
    val characterId = state.characterId ?: return false
    // Variant alts are chosen through the base item's variant window - as bag items they list
    // as bogus standalone cosmetics (and login would reclaim them anyway).
    if (itemId in CosmeticsRegistry.variantAltItems) return false
    if (!characters.addItem(characterId, itemId, quantity)) return false
    val stored = characters.getCharacter(characterId) ?: return false
    session.send(storyItemStacksPacket(stored.items))
    worldState.refreshUnlocks(session, stored)
    return true
  }

  /** How many of [item] the character carries, across every id the item is registered under. */
  fun itemCount(state: PlayerState, item: ItemDef): Int {
    val characterId = state.characterId ?: return 0
    val bag = characters.getCharacter(characterId)?.items ?: return 0
    return items.idsOf(item).sumOf { bag[it] ?: 0 }
  }

  fun itemByScriptConstant(token: String): ItemDef? = items.byScriptConstant(token)

  private fun paddedMoves(moveIds: List<Int>): List<PokemonMove> =
      moveIds.take(MAX_MOVES).map { id ->
        PokemonMove(id.toShort(), (moves.get(id)?.pp ?: DEFAULT_MOVE_PP).toByte())
      } + List((MAX_MOVES - moveIds.size).coerceAtLeast(0)) { PokemonMove(0, 0) }

  private companion object {
    const val MAX_MOVES = 4
  }
}

/** Story-var keys recording where a whiteout returns the player (packed map and tile). */
object RespawnPoint {
  const val MAP_KEY = "respawn/map"
  const val XY_KEY = "respawn/xy"
}

/** Builds a stable full bag snapshot. */
fun storyItemStacksPacket(items: Map<Int, Int>) =
    itemStacksPacket(
        items.entries
            .sortedBy { it.key }
            .map { (itemId, quantity) ->
              ItemStack(
                  objectId = (itemId.toLong() shl 16) or ITEM_ENTITY_TAG,
                  itemId = itemId.toShort(),
                  quantity = quantity.toShort(),
              )
            })

/**
 * A bag stack, not a monster. The open shop window only refreshes its count when the update arrives
 * as this single stack rather than as a whole new bag.
 */
fun itemStackUpdatePacket(itemId: Int, quantity: Int) =
    BattleSideAddPokemonPacket(
        side = 1,
        pokemon =
            BattleAddPokemon(
                entityId = (itemId.toLong() shl 16) or ITEM_ENTITY_TAG,
                frontSpriteId = itemId.toShort(),
                backSpriteId = quantity.toShort(),
                side = 1,
                slot = 0,
                partyIndex = -1,
                statusEffect = null,
            ),
    )

private const val ITEM_ENTITY_TAG = 0x5000L
