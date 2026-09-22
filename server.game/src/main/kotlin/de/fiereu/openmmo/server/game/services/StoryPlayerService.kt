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
import de.fiereu.openmmo.net.game.packets.battle.itemStacksPackets
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.BattleRng
import de.fiereu.openmmo.server.game.battle.StatCalculator
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.battle.acquiredMonsterDelta
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
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
    private val ocarinas: OcarinaService,
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
      /** The form number on [dexId] (retail forms are species + form, never separate species). */
      form: Int = 0,
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
            form = form,
        )
    // Only tell the client about it once the database has it.
    if (!characters.addPokemon(characterId, pokemon)) return null
    // Send the granted Pokemon's full record.
    session.send(SocialListEntryAddPacket(pokemon))
    species.forMonster(pokemon)?.let { session.send(acquiredMonsterDelta(pokemon, it)) }
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

  /** The species in party [slot], 0 for an egg or an empty slot (GetTradeSpecies). */
  fun partySpecies(state: PlayerState, slot: Int): Int {
    val mon = state.characterId?.let { characters.getCharacter(it)?.pokemon?.getOrNull(slot) } ?: return 0
    return if (mon.isEgg) 0 else mon.dexId
  }

  /**
   * An in-game trade: the monster in party [slot] goes to the NPC and the NPC's monster takes its
   * place at the same level, with the NPC as its original trainer, its table nickname, IVs,
   * personality and held item (CreateInGameTradePokemonInternal in src/trade_scene.c). False when
   * nothing was swapped.
   */
  suspend fun tradeWithNpc(session: SessionContext, state: PlayerState, slot: Int, trade: InGameTrade): Boolean {
    val characterId = state.characterId ?: return false
    val stored = characters.getCharacter(characterId) ?: return false
    val given = stored.pokemon.getOrNull(slot) ?: return false
    val definition = species.get(trade.dexId) ?: return false
    val rolled = pokemonFactory.create(trade.dexId, given.level.toInt(), BattleRng()) ?: return false
    val ivs = de.fiereu.openmmo.common.enums.IVs()
    ivs.hp = trade.ivs[0]
    ivs.atk = trade.ivs[1]
    ivs.def = trade.ivs[2]
    ivs.spd = trade.ivs[3]
    ivs.spAtk = trade.ivs[4]
    ivs.spDef = trade.ivs[5]
    val received =
        rolled.copy(
            ownerId = characterId,
            container = PokemonContainer.PARTY,
            containerSlot = slot.toShort(),
            ot = trade.otName,
            nickname = trade.nickname,
            seed = trade.personality,
            iVs = ivs,
            heldItem = trade.heldItem?.let { items.byScriptConstant(it) }?.let { items.idOf(it) } ?: 0,
            isShiny = false,
        )
    val healed = received.copy(hp = StatCalculator.computeAll(definition, received).hp.toShort())
    if (!characters.removePokemon(characterId, given.id)) return false
    if (!characters.addPokemon(characterId, healed)) {
      // Put the player's monster back rather than lose it.
      characters.addPokemon(characterId, given)
      return false
    }
    // addPokemon appends; move the newcomer back into the slot the traded monster held.
    val last = (characters.getCharacter(characterId)?.pokemon?.size ?: 1) - 1
    if (last != slot) characters.swapPartySlots(characterId, last, slot)
    characters.flushCharacterAsync(characterId)
    val party = characters.getCharacter(characterId)?.pokemon ?: return false
    val inParty = party.firstOrNull { it.id == healed.id } ?: healed
    session.send(SocialListEntryAddPacket(inParty))
    session.send(acquiredMonsterDelta(inParty, definition))
    session.send(PokemonContainerPacket(container = PokemonContainer.PARTY, hasChange = true, delete = false, pokemon = party))
    dexProgress.refresh(session, characterId)
    return true
  }

  /**
   * Heals the party. [recordRespawn] makes this spot the whiteout return point (a nurse); the
   * whiteout heal itself passes false - it used to record the spot the player fell on, so a
   * whiteout "returned" the player to the cave floor it happened on (2026-09-15).
   */
  fun healParty(session: SessionContext, state: PlayerState, recordRespawn: Boolean = true) {
    val characterId = state.characterId ?: return
    val stored = characters.getCharacter(characterId) ?: return
    val healed =
        stored.pokemon.map { pokemon ->
          val definition = species.forMonster(pokemon) ?: return@map pokemon
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
    ocarinas.refill(session, characterId)
    // The spot that healed last is where a whiteout returns the player.
    if (recordRespawn) {
      val info = stored.info
      characters.setStoryVar(
          characterId,
          RespawnPoint.MAP_KEY,
          (info.positionRegionId.toInt() shl 16) or
              ((info.positionBankId.toInt() and 0xFF) shl 8) or
              (info.positionMapId.toInt() and 0xFF))
      characters.setStoryVar(
          characterId, RespawnPoint.XY_KEY, (info.positionX.toInt() shl 12) or info.positionY.toInt())
    }
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
    // An HM is one item for the whole game, so the item cannot say which region handed it over;
    // the receipt flag does (FieldMoves.receiptFlag): it is the HM's bag page and the DS regions'
    // field-move gate. Kanto's and Hoenn's are the ROM's own flags, which their scripts set.
    if (quantity > 0 && BagRegions.isHm(itemId)) {
      val region = de.fiereu.openmmo.common.enums.Region.byId(state.regionId)
      val moveId = de.fiereu.openmmo.items.ClientTools.itemToMove[itemId]
      if (region != null && moveId != null && region != de.fiereu.openmmo.common.enums.Region.KANTO && region != de.fiereu.openmmo.common.enums.Region.HOENN) {
        FieldMoves.receiptFlag(region, moveId)?.let { characters.setStoryFlag(characterId, it) }
      }
    }
    // The bike quests: Hoenn's Mach/Acro Bike and the DS Bicycles are regional key items the
    // scripts check for, but riding is tied to the client's own Bicycle (360), so that comes
    // along with the first of them. There is no unconditional grant any more.
    if (quantity > 0 && itemId in REGIONAL_BIKE_ITEMS) {
      val bag = characters.getCharacter(characterId)?.items.orEmpty()
      if (CLIENT_BICYCLE_ITEM !in bag) characters.addItem(characterId, CLIENT_BICYCLE_ITEM, 1)
    }
    val stored = characters.getCharacter(characterId) ?: return false
    storyItemStacksPackets(stored).forEach { p -> session.send(p) }
    worldState.refreshUnlocks(session, stored)
    return true
  }

  /** How many of [item] the character carries, across every id the item is registered under. */
  fun itemCount(state: PlayerState, item: ItemDef): Int {
    val characterId = state.characterId ?: return 0
    val bag = characters.getCharacter(characterId)?.items ?: return 0
    return items.idsOf(item).sumOf { bag[it] ?: 0 }
  }

  /** [regionId] picks between namesakes across bands - a Kanto script gets Kanto's Super Rod. */
  fun itemByScriptConstant(token: String, regionId: Int? = null): ItemDef? = items.byScriptConstant(token, regionId)

  fun itemByWireId(id: Int): ItemDef? = items.get(id)

  /** The client wire id of [item] (the reverse of [itemByWireId]). */
  fun itemWireId(item: ItemDef): Int = items.idOf(item)

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
/**
 * The whole bag as stacks; the character's receipt flags decide which region's page an HM or a
 * shared Gen 3 key item is filed under (BagRegions).
 */
fun storyItemStacksPackets(stored: StoredCharacter): List<de.fiereu.openmmo.net.game.packets.battle.BattleSidePartyPacket> =
    itemStacksPackets(
        stored.items.entries
            .sortedBy { it.key }
            .flatMap { (itemId, quantity) ->
              if (itemId in dyeableGarments) {
                // One hidden stack per color: the wardrobe's picker (f/Te, in-game mode) enables
                // only the colors some bag stack of the addon carries in its color byte, and the
                // creation screen's free palette is not offered in game. The color rides in the
                // low byte of the stack id, so the click's stack reference names it.
                (0 until GARMENT_COLORS).map { color ->
                  ItemStack(
                      objectId = (itemId.toLong() shl 16) or ITEM_ENTITY_TAG or color.toLong(),
                      itemId = itemId.toShort(),
                      quantity = 1,
                      region = BagRegions.single(itemId),
                      color = color.toByte(),
                  )
                }
              } else {
                // A key item both GBA regions hand out is one stack per region that did; the
                // region byte doubles as the stack id's low byte so the two stacks stay distinct.
                val stacks = BagRegions.stacks(itemId, quantity, stored.storyFlags)
                stacks.map { (region, count) ->
                  val low = if (stacks.size > 1) region.toLong() else 0L
                  ItemStack(
                      objectId = (itemId.toLong() shl 16) or ITEM_ENTITY_TAG or low,
                      itemId = itemId.toShort(),
                      quantity = count.toShort(),
                      region = region,
                  )
                }
              }
            })

/** The starting garments, dyeable to any of the skin short's 64 colors. */
private val dyeableGarments: Set<Int>
  get() = de.fiereu.openmmo.server.game.services.CosmeticsRegistry.starterGarments.map { it.itemId }.toSet()

/** The skin short carries the color in six bits. */
const val GARMENT_COLORS = 64

/**
 * A bag stack, not a monster. The open shop window only refreshes its count when the update arrives
 * as this single stack rather than as a whole new bag.
 */
/**
 * A region id the player is never in, so a stack tagged with it is on no bag page. The wardrobe's
 * own stock - the bike colors and the starting garments - is granted as bag items because the
 * dialog lists from the bag, but retail never shows them in the bag (2026-09-08). Every other
 * stack's page is BagRegions' call.
 */
const val HIDDEN_BAG_REGION: Byte = 100

/**
 * One stack's new count. Items with a page of their own (a region's key items and HMs) keep it;
 * the Gen 3 ids both GBA regions hand out are never updated one at a time (nothing consumes or
 * sells an HM), so a single stack tagged for every page is the safe answer for them here.
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
                partyIndex = BagRegions.single(itemId),
                statusEffect = null,
            ),
    )

/** Low 16 bits of every bag stack uid the client is given (`itemId shl 16 or tag`); TradeService tells stacks from monsters by it. */
internal const val ITEM_ENTITY_TAG = 0x5000L

/** The client's Bicycle (FRLG ITEM_BICYCLE 360), the item its bike feature is tied to. */
const val CLIENT_BICYCLE_ITEM = 360

/** The Gen 5-numbered duplicate "Bicycle" the registry used to hand out; reclaimed on sight. */
const val DUPLICATE_BICYCLE_ITEM = 5450

/**
 * Hoenn's Mach and Acro Bike at their Gen 3 ids - the ones the client names and Rydel hands out.
 * They were 1259/1272 for a while (the registry's imagined "Hoenn table"), which on the client are
 * Super Carbos and Multi-Vitamin Pack: the login reclaim ate those from every character without a
 * bike flag (2026-09-22).
 */
val HOENN_BIKE_ITEMS = listOf(259, 272)

/** The DS games' Bicycle, region * 1000 + 433. */
val DS_BICYCLE_ITEMS = setOf(2433, 3433, 4433)

/** Every regional bike item; any of them brings the client's Bicycle along. */
val REGIONAL_BIKE_ITEMS = HOENN_BIKE_ITEMS.toSet() + DS_BICYCLE_ITEMS

/**
 * Kanto key items that resolved to the catalogue's Gen 5-numbered namesakes (same display name,
 * the name index picked the 5000-band entry) -> their Gen 3 ids. Black/White hands out none of
 * these five, so a held one is Kanto's. Fixed at login; the resolver no longer produces them.
 */
val CATALOGUE_KEY_ITEMS_TO_GBA =
    mapOf(5456 to 265, 5467 to 351, 5444 to 260, 5445 to 262, 5446 to 263)
