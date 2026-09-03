package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.ContainerActionPacket
import de.fiereu.openmmo.net.game.packets.EntitySpriteChangePacket
import de.fiereu.openmmo.net.game.packets.EntityTransportationPacket
import de.fiereu.openmmo.net.game.packets.PartyReorderPacket
import de.fiereu.openmmo.net.game.packets.PokedexSpeciesUnlockPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.battle.StatCalculator
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/** The plain Bicycle (FRLG ITEM_BICYCLE = 360), granted at login by LoginService. */
private const val BICYCLE_ITEM_ID = 360

/** Transportation byte with bit 1 (f.ti.U7) set - the client's mounted-frames gate. */
private const val RIDING_TRANSPORTATION: Byte = 0x02

/** Client-generated cosmetic item ids (2000 + slot * 256 + addon, plus HAT's 4320+ range). */
private val COSMETIC_ITEM_BAND = 2000..4887

/**
 * Bag items used on party monsters, and party drags - the two container actions decoded from live
 * clicks so far.
 *
 * The use-item frame is `u16 itemCode, u64 monsterId, u16 quantity, u16 tail`. The item code is not
 * the item id: a real Ice Stone click sent the id minus 2048. That offset is treated as a
 * hypothesis and validated against the bag - the resolved id must be an item the character actually
 * holds - so a wrong guess turns into a log line instead of a wrong item.
 */
@Singleton
class InventoryActionService
@Inject
constructor(
    private val characters: CharacterStore,
    private val items: ItemRegistry,
    private val species: SpeciesRegistry,
    private val expansion: ExpansionSpeciesRegistry,
    private val dexProgress: DexProgressService,
    private val breedingService: BreedingService,
) {

  suspend fun onContainerAction(event: PacketEvent<ContainerActionPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val packet = event.packet
    if (packet.payload.size < 10) {
      log.info { "[UseItem] unknown container action shape: $packet" }
      return
    }
    // The codec's four-byte action head swallowed the item code and the monster id's low half.
    val itemCode = packet.action and 0xFFFF
    val monsterIdLow = (packet.action ushr 16) and 0xFFFF
    var monsterHigh = 0L
    for (index in 5 downTo 0) {
      monsterHigh = (monsterHigh shl 8) or (packet.payload[index].toLong() and 0xFF)
    }
    val monsterId = (monsterHigh shl 16) or monsterIdLow.toLong()
    val quantity =
        (packet.payload[6].toInt() and 0xFF) or ((packet.payload[7].toInt() and 0xFF) shl 8)

    val stored = characters.getCharacter(charId) ?: return
    // Cosmetic addon items (client-generated ids 2000..4883) are worn via the customization
    // dialog, never used from the bag - and the +2048 guess must not resolve to one: with bike
    // skins granted, the Bicycle's code 360 once resolved to cosmetic 2408 and the bike died.
    val itemId =
        listOf(itemCode + ITEM_CODE_OFFSET, itemCode).firstOrNull {
          it in stored.items && it !in COSMETIC_ITEM_BAND
        }
    log.info {
      "[UseItem] char=$charId code=$itemCode resolved=$itemId monster=$monsterId qty=$quantity"
    }
    if (itemId == null) {
      ctx.reply("That item could not be matched to anything in the bag (code $itemCode).")
      return
    }
    // monsterId 0 is a FIELD use - the item aimed at the world, not a party member. The bike is
    // the first decoded one: riding is the BIKE skin slot (SkinSlot.BIKE), so using the Bicycle
    // toggles that slot and re-announces the appearance via EntitySpriteChange (0x90).
    if (monsterId == 0L) {
      if (itemId == BICYCLE_ITEM_ID) {
        // THE BIKE, decoded end to end from the client: the drawn bike is the BIKE skin slot
        // (type 0 = Red Bicycle, per the 31000+ string block), and whether the player renders
        // the mounted frame set is transportation bit 1 (f.ti.aU1; f.F90.aX returns the
        // standing pose whenever f.ti.U7 - that bit - is false). Packet 0x28 (f.OJ0 ->
        // f.tS1.D40) overwrites that byte on the LIVE entity, so the toggle is instant - D40
        // even plays the region's bike bell on the not-riding -> riding edge.
        if (stored.skins[SkinSlot.BIKE] == null) {
          // No bike chosen yet - default to the Red Bicycle so there is something to draw.
          characters.setSkin(charId, SkinSlot.BIKE, Skin(SkinSlot.BIKE, 0u, 0u))
          characters.flushCharacterAsync(charId)
        }
        state.riding = !state.riding
        if (state.riding) {
          // Mounting re-announces the STORED skin set on the live entity before the ride bit
          // flips, so the drawn bike is always whatever the customization menu last picked -
          // the menu writes the skin, the bike script points at it.
          val current = characters.getCharacter(charId) ?: return
          // false routes the set into IL0.v4 - the DISPLAYED set (true stages into JQ1,
          // which nothing draws; that bool cost a whole night of invisible skin updates).
          ctx.send(
              EntitySpriteChangePacket(
                  entityId = charId,
                  facingFront = false,
                  appearance = SkinSet(current.info.skinRegionSelectionIndex, current.skins),
                  direction = 0,
              ))
        }
        ctx.send(EntityTransportationPacket(charId, if (state.riding) RIDING_TRANSPORTATION else 0))
        log.info {
          "[UseItem] BIKE riding=${state.riding} skin=${characters.getCharacter(charId)?.skins?.get(SkinSlot.BIKE)?.type} char=$charId"
        }
      } else {
        ctx.reply(
            "Using ${items.get(itemId)?.name ?: "item $itemId"} from the bag is not wired up yet.")
      }
      return
    }
    val target = stored.pokemon.firstOrNull { it.id == monsterId }
    if (target == null) {
      ctx.reply("That is not a monster in the party.")
      return
    }
    val itemName = items.get(itemId)?.name ?: "Item $itemId"

    val currentWire = clientSpeciesId(target.dexId)
    val evolvedWire = EvolutionTable.itemEvolution(currentWire, itemId)
    if (evolvedWire != null) {
      if (target.heldItem in BreedingService.EVERSTONES) {
        ctx.reply("It would have no effect.")
        return
      }
      if (monsterId in state.pendingEvolutions) return
      // The client plays its evolution cinematic off the prompt; the species changes and the
      // stone is consumed only when it confirms the sequence finished (EvolutionService).
      promptEvolution(ctx, state, target, evolvedWire, consumeItemId = itemId)
      log.info {
        "[UseItem] EVOLUTION OFFERED char=$charId monster=$monsterId wire $currentWire -> $evolvedWire item=$itemId"
      }
      return
    }

    if (itemId in HEAL_ITEMS) {
      val healAmount = HEAL_ITEMS.getValue(itemId)
      if (target.hp <= 0) {
        ctx.reply("$itemName cannot revive a fainted monster.")
        return
      }
      val definition = species.get(target.dexId)
      val maxHp = definition?.let { StatCalculator.computeAll(it, target).hp } ?: target.hp.toInt()
      if (target.hp >= maxHp) {
        ctx.reply("It would have no effect.")
        return
      }
      val healed = target.copy(hp = (target.hp + healAmount).coerceAtMost(maxHp).toShort())
      characters.updatePokemon(charId, healed)
      characters.addItem(charId, itemId, -1)
      sendStack(ctx, charId, itemId)
      sendParty(ctx, charId)
      ctx.reply("$itemName restored ${healed.hp - target.hp} HP.")
      return
    }

    ctx.reply("$itemName cannot be used that way yet.")
  }

  /**
   * The bag's Give flow: after picking an item and a target monster the client sends c2s 0x0F with
   * the monster's uid, a short and a byte. The short is treated as the item id (validated against
   * the bag like the use-item flow - a non-bag value turns into a log line, not a wrong item), 0 as
   * a take-back. The held item itself rides the monster record's eE0 short, so the refreshed party
   * container is what makes it show up. Every packet is logged raw until the fields are
   * capture-confirmed.
   */
  suspend fun onGiveHeldItem(
      event: PacketEvent<de.fiereu.openmmo.net.game.packets.PokemonListAddPacket>
  ) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val p = event.packet
    val stored = characters.getCharacter(charId) ?: return
    val target = (stored.pokemon + stored.pcStorage).firstOrNull { it.id == p.entityId }
    val code = p.slot.toInt()
    log.info {
      "[GiveItem] char=$charId monster=${p.entityId}(${target?.dexId}) code=$code " +
          "listType=${p.listType} held=${target?.heldItem}"
    }
    if (target == null) {
      ctx.reply("That is not one of your monsters.")
      return
    }
    if (code <= 0) {
      takeHeldItem(ctx, charId, target)
      return
    }
    val itemId =
        listOf(code, code + ITEM_CODE_OFFSET).firstOrNull {
          it in stored.items && it !in COSMETIC_ITEM_BAND
        }
    if (itemId == null) {
      ctx.reply("That item could not be matched to anything in the bag (code $code).")
      return
    }
    val previous = target.heldItem
    characters.updatePokemon(charId, target.copy(heldItem = itemId))
    characters.addItem(charId, itemId, -1)
    if (previous != 0) characters.addItem(charId, previous, 1)
    sendStack(ctx, charId, itemId)
    if (previous != 0) sendStack(ctx, charId, previous)
    // Deliberately NO full container resend: that replaces the client's k91 instances, orphaning
    // the references an open breed window captured at pair-select time - its "Held Item" lines
    // then keep rendering the old objects. The delta mutates the records IN PLACE instead.
    sendHeldItemDelta(ctx, target.id, itemId)
    breedingService.refreshAfterHeldItemChange(ctx, charId, target.id)
    val itemName = items.get(itemId)?.name ?: "Item $itemId"
    ctx.reply(
        if (previous != 0) "$itemName was given; the old held item went back to the bag."
        else "$itemName is now being held.")
  }

  private suspend fun takeHeldItem(
      ctx: de.fiereu.network.SessionContext,
      charId: Long,
      target: Pokemon,
  ) {
    if (target.heldItem == 0) {
      ctx.reply("It isn't holding anything.")
      return
    }
    val taken = target.heldItem
    characters.updatePokemon(charId, target.copy(heldItem = 0))
    characters.addItem(charId, taken, 1)
    sendStack(ctx, charId, taken)
    // No full container resend here either - see the give path.
    sendHeldItemDelta(ctx, target.id, 0)
    breedingService.refreshAfterHeldItemChange(ctx, charId, target.id)
    ctx.reply("${items.get(taken)?.name ?: "Item $taken"} was taken back.")
  }

  /**
   * A drag between two party slots swaps them. Slots are zero-based on the wire: the one-based
   * reading survived one test session as an everything-lands-one-off rearrange.
   */
  fun onPartyReorder(event: PacketEvent<PartyReorderPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val packet = event.packet
    log.info { "[PartyReorder] char=$charId $packet" }
    var changed = false
    for (move in packet.moves) {
      // Only party-to-party drags for now; PC boxes ride another container id.
      if (move.fromContainer != PARTY_CONTAINER || move.toContainer != PARTY_CONTAINER) {
        log.info { "[PartyReorder] unsupported containers: $move" }
        continue
      }
      if (characters.swapPartySlots(charId, move.fromSlot, move.toSlot)) changed = true
      else log.info { "[PartyReorder] rejected: $move" }
    }
    if (changed) sendParty(ctx, charId)
  }

  /**
   * The mid-session bag update is a per-stack delta, the same packet the shop flow uses; the full
   * snapshot only lands at login, and the client ignores it afterwards.
   */
  private fun sendStack(ctx: de.fiereu.network.SessionContext, charId: Long, itemId: Int) {
    val quantity = characters.getCharacter(charId)?.items?.get(itemId) ?: 0
    ctx.send(itemStackUpdatePacket(itemId, quantity))
  }

  /**
   * Updates the held item on the client's LIVE record: delta bit 0x100 writes k91.eE0 in place and
   * marks the container UI dirty, so the party/PC item icon and any open window showing it repaint
   * without waiting for a full container packet (which open windows keep stale references across).
   */
  private fun sendHeldItemDelta(ctx: de.fiereu.network.SessionContext, monId: Long, itemId: Int) {
    ctx.send(
        de.fiereu.openmmo.net.game.packets.battle.BattleEntityDeltaPacket(
            entityId = monId,
            heldItem = itemId.toShort(),
        ))
  }

  private fun sendParty(ctx: de.fiereu.network.SessionContext, charId: Long) {
    val party = characters.getCharacter(charId)?.pokemon ?: return
    ctx.send(
        PokemonContainerPacket(
            container = PokemonContainer.PARTY,
            hasChange = true,
            delete = false,
            pokemon = party,
        ))
  }

  private fun de.fiereu.network.SessionContext.reply(message: String) {
    send(notice(message))
  }

  private companion object {
    /** Measured: the Ice Stone (id 21000) arrived as 18952. Validated against the bag per use. */
    const val ITEM_CODE_OFFSET = 2048

    /** The client's container ordinal (f/Cy) for the party, as its drag packet writes it. */
    const val PARTY_CONTAINER = 1

    /** Client item id to HP restored. */
    val HEAL_ITEMS =
        mapOf(
            5017 to 20, // Potion
            5026 to 50, // Super Potion
            5025 to 200, // Hyper Potion
            5024 to 9999, // Max Potion
        )
  }
}

/**
 * Item evolutions, from the same calibrated table the client's overlay applies - wire id, ROM
 * method, item parameter (before the client's +5000 shift), target wire id per line.
 */
object EvolutionTable {
  private data class Entry(val from: Int, val method: Int, val param: Int, val to: Int)

  /** ROM methods where the player uses an item on the monster directly. */
  private val ITEM_METHODS = setOf(8, 16, 17)

  /** The client evolution-method enum (f/kx) by ordinal - the words the dump uses. */
  private val METHOD_WORDS =
      listOf(
          "BREEDING_ONLY",
          "HAPPINESS",
          "HAPPINESS_DAY",
          "HAPPINESS_NIGHT",
          "LEVEL",
          "TRADE",
          "TRADE_WITH_ITEM",
          "TRADE_FOR_OPPOSITE",
          "ITEM",
          "ATK_GREATER_THAN_DEF",
          "ATK_EQUAL_TO_DEF",
          "ATK_LESS_THAN_DEF",
          "PERSONALITY_HIGH",
          "PERSONALITY_LOW",
          "ALLOW_MONSTER_CREATION",
          "CREATE_EXTRA_MONSTER",
          "MAX_BEAUTY",
          "ITEM_MALE",
          "ITEM_FEMALE",
          "LEVEL_ITEM_DAY",
          "LEVEL_ITEM_NIGHT",
          "LEVEL_WITH_SKILL",
          "LEVEL_WITH_MONSTER",
          "LEVEL_MALE",
          "LEVEL_FEMALE",
          "LEVEL_LOCATION_1",
          "LEVEL_LOCATION_2",
          "LEVEL_LOCATION_3",
      )

  /**
   * Every species' evolutions, sourced from monsters.json - the operator-designated authority on
   * evolution rules, covering the retail chains the old expansion-only csv never had (which is why
   * a Squirtle sat unevolved at 16 and a Pikachu's forecast never devolved to Pichu). The csv still
   * contributes any line the json lacks. Item-method values in the json are CLIENT item ids
   * already; csv item params carry the pre-shift ROM value.
   */
  private val entries: List<Entry> by lazy {
    val fromJson =
        de.fiereu.openmmo.pokemon.retail.RetailMonsterData.all().flatMap { monster ->
          monster.evolutions.mapNotNull { evo ->
            val method = METHOD_WORDS.indexOf(evo.method)
            if (method < 0) return@mapNotNull null
            val param =
                if (method in ITEM_METHODS && evo.value > 5000) evo.value - 5000 else evo.value
            Entry(monster.id, method, param, evo.toId)
          }
        }
    val known = fromJson.map { Triple(it.from, it.method, it.to) }.toHashSet()
    val fromCsv =
        (EvolutionTable::class
                .java
                .getResourceAsStream("/monmmo/evolutions.csv")
                ?.bufferedReader()
                ?.useLines { lines ->
                  lines
                      .mapNotNull { line ->
                        val parts = line.split(':')
                        if (parts.size != 4) return@mapNotNull null
                        val numbers = parts.map { it.toIntOrNull() ?: return@mapNotNull null }
                        Entry(numbers[0], numbers[1], numbers[2], numbers[3])
                      }
                      .toList()
                } ?: emptyList())
            .filter { Triple(it.from, it.method, it.to) !in known }
    fromJson + fromCsv
  }

  private val preEvolution: Map<Int, Int> by lazy { entries.associate { it.to to it.from } }

  /**
   * Walks the evolution chain DOWN to the family's youngest form (wire ids) - breeding offspring
   * always hatch as the base stage (Pichu from a Pikachu line, operator-specified).
   */
  fun baseForm(wire: Int): Int {
    var current = wire
    val visited = mutableSetOf<Int>()
    while (visited.add(current)) {
      current = preEvolution[current] ?: return current
    }
    return wire
  }

  /** Facts about the monster that level-driven evolution conditions read. */
  data class LevelContext(
      val level: Int,
      val attack: Int,
      val defense: Int,
      val seed: Long,
      val female: Boolean,
      val heldItem: Int,
      val friendship: Int = 0,
      val daytime: Boolean = true,
  )

  /** The cartridge happiness threshold for the friendship evolutions. */
  const val FRIENDSHIP_EVOLUTION = 220

  /**
   * The species [fromWire] becomes on reaching [context], or null. Covers the level-driven methods:
   * plain level, gender-gated level, the Hitmon attack/defense split, the Wurmple personality split
   * ((seed shr 16) % 10, the cartridge rule), and held-item level methods. Happiness, trade, beauty
   * and location methods need systems that do not exist yet and never match here.
   */
  fun levelEvolution(fromWire: Int, context: LevelContext): Int? =
      entries
          .firstOrNull { entry ->
            entry.from == fromWire &&
                when (entry.method) {
                  1 -> context.friendship >= FRIENDSHIP_EVOLUTION // HAPPINESS
                  2 -> context.friendship >= FRIENDSHIP_EVOLUTION && context.daytime
                  3 -> context.friendship >= FRIENDSHIP_EVOLUTION && !context.daytime
                  4 -> context.level >= entry.param // LEVEL
                  9 -> context.level >= entry.param && context.attack > context.defense
                  10 -> context.level >= entry.param && context.attack == context.defense
                  11 -> context.level >= entry.param && context.attack < context.defense
                  12 -> context.level >= entry.param && (context.seed shr 16) % 10 >= 5
                  13 -> context.level >= entry.param && (context.seed shr 16) % 10 < 5
                  19, // LEVEL_ITEM_DAY and NIGHT: the held item is the condition; the server
                  20 -> // has no day cycle, so either time works.
                  context.heldItem != 0 &&
                          (context.heldItem == entry.param ||
                              context.heldItem == entry.param + 5000)
                  23 -> context.level >= entry.param && !context.female // LEVEL_MALE
                  24 -> context.level >= entry.param && context.female // LEVEL_FEMALE
                  else -> false
                }
          }
          ?.to

  /** The wire id this species becomes when [clientItemId] is used on it, or null. */
  fun itemEvolution(fromWire: Int, clientItemId: Int): Int? =
      entries
          .firstOrNull {
            it.from == fromWire &&
                it.method in ITEM_METHODS &&
                (it.param + 5000 == clientItemId || it.param == clientItemId)
          }
          ?.to
}
