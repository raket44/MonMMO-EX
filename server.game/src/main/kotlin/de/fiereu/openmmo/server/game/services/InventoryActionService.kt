package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
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
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.Value16Group
import de.fiereu.openmmo.net.game.packets.PartyReorderPacket
import de.fiereu.openmmo.net.game.packets.PokedexSpeciesUnlockPacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.battle.StatCalculator
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/** The plain Bicycle (FRLG ITEM_BICYCLE = 360), granted at login by LoginService. */
private const val BICYCLE_ITEM_ID = 360

/** Transportation byte with bit 1 (f.ti.U7) set - the client's mounted-frames gate. */
private const val RIDING_TRANSPORTATION: Byte = 0x02

/** Client-generated cosmetic item ids (2000 + slot * 256 + addon, plus HAT's 4320+ range). */
private val COSMETIC_ITEM_BAND = 2000..4887

/** HAT addons 256..495 carry item ids 4576..4815 (f/J61.ZQ1: id + 4320). */
private const val HAT_HIGH_ITEM_BASE = 4320
private val HAT_HIGH_ITEMS = (256 + HAT_HIGH_ITEM_BASE)..(495 + HAT_HIGH_ITEM_BASE)

/** A cosmetic item id back to its addon slot and id. */
private fun cosmeticSlotAndId(itemCode: Int): Pair<SkinSlot, Int>? {
  if (itemCode in HAT_HIGH_ITEMS) return SkinSlot.HAT to (itemCode - HAT_HIGH_ITEM_BASE)
  val rel = itemCode - 2000
  val slot = SkinSlot.entries.getOrNull(rel / 256) ?: return null
  return slot to (rel % 256)
}

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
    private val presenceService: PresenceService,
    private val cosmeticAnimations: CosmeticAnimations,
    private val ocarinas: OcarinaService,
    private val moveTeacher: MoveTutorService,
    private val moves: de.fiereu.openmmo.moves.MoveRegistry,
    private val trades: TradeService,
    private val links: LinkService,
) {

  private val consumeScope =
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

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
    // A hotbar click on a cosmetic: a mount item rides that mount, a worn animated cosmetic
    // plays its animation (the Werewolf Masks' howl). Nothing else in the band does anything.
    if (itemId == null && itemCode in COSMETIC_ITEM_BAND && monsterId == 0L) {
      useCosmetic(ctx, state, charId, stored, itemCode)
      return
    }
    if (itemId == null) {
      ctx.reply("That item could not be matched to anything in the bag (code $itemCode).")
      return
    }
    // monsterId 0 is a FIELD use - the item aimed at the world, not a party member. The bike is
    // the first decoded one: riding is the BIKE skin slot (SkinSlot.BIKE), so using the Bicycle
    // toggles that slot and re-announces the appearance via EntitySpriteChange (0x90).
    if (monsterId == 0L) {
      if (ocarinas.isOcarina(itemId)) {
        ocarinas.use(ctx, state, charId, itemId)
        return
      }
      Boosts.Kind.byItem(itemId)?.let { kind ->
        useCharm(ctx, charId, stored, itemId, kind)
        return
      }
      repelSteps[itemId]?.let { steps ->
        useRepel(ctx, charId, stored, itemId, steps)
        return
      }
      if (itemId == BICYCLE_ITEM_ID) {
        rideBike(ctx, state, charId, stored, mountType = null)
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
      val definition = species.forMonster(target)
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

    // TMs and HMs: the client's own tools table says which move the item teaches (MachineMoves).
    // The client only offers the party members its learnsets allow, so the pick is trusted here.
    // A TM is used up once the move sits in a slot (also after the forget dialog); an HM never is.
    val machineMove =
        MachineMoves.moveFor(itemId, itemName) { name ->
          moves.all().firstOrNull { it.name.equals(name, ignoreCase = true) }?.id
        }
    if (machineMove != null) {
      when {
        target.isEgg -> ctx.reply("An Egg cannot learn a move.")
        target.moves.any { it.id.toInt() == machineMove } ->
            ctx.reply("${target.nickname} already knows ${moves.get(machineMove)?.name ?: "that move"}.")
        else -> {
          val consumable = !MachineMoves.isHm(itemId)
          val started =
              moveTeacher.teach(ctx, charId, target.id, machineMove) { learned ->
                if (learned && consumable) {
                  // The forget dialog answers on the packet thread; the store's item update suspends.
                  consumeScope.launch {
                    characters.addItem(charId, itemId, -1)
                    characters.flushCharacterAsync(charId)
                    sendStack(ctx, charId, itemId)
                  }
                }
              }
          log.info { "[UseItem] MACHINE char=$charId item=$itemId move=$machineMove monster=$monsterId started=$started" }
          if (!started) ctx.reply("$itemName could not be taught.")
        }
      }
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
    val before = placements(characters.getCharacter(charId) ?: return)
    var changed = false
    for (move in packet.moves) {
      // The trade window's party picker sends its choice as a drag into the client's TRADE
      // container (f/Cy 2), and taking it back as the reverse drag. Neither touches storage.
      if (move.toContainer == TRADE_CONTAINER || move.fromContainer == TRADE_CONTAINER) {
        trades.onPartyMove(ctx, charId, move.fromContainer, move.fromSlot, move.toContainer)
        continue
      }
      val from = clientContainer(move.fromContainer)
      val to = clientContainer(move.toContainer)
      if (from == null || to == null) {
        log.info { "[PartyReorder] unsupported containers: $move" }
        continue
      }
      if (characters.moveBetweenContainers(charId, from, move.fromSlot, to, move.toSlot))
          changed = true
      else log.info { "[PartyReorder] rejected: $move" }
    }
    if (!changed) return
    // Each moved monster gets the live-record delta's container+slot pair (client f/Y9, bit 0x40):
    // same container = slot update, another container = removed from the old one and added to
    // the new one, and both containers are marked dirty so open windows repaint in place. A full
    // PC container packet would instead replace the container object the open PC tab captured at
    // construction (k01.QE1), leaving the boxes stale until reopened.
    val after = placements(characters.getCharacter(charId) ?: return)
    for ((id, placement) in after) {
      if (before[id] == placement) continue
      ctx.send(
          de.fiereu.openmmo.net.game.packets.battle.BattleEntityDeltaPacket(
              entityId = id,
              listing =
                  de.fiereu.openmmo.net.game.packets.battle.Listing(
                      placement.first.ordinal.toByte(), placement.second),
          ))
    }
    // NOTHING is resent whole here, on purpose. Pushing a full container replaces the object the
    // open window captured at construction, so the window goes stale until it is reopened - the
    // comment above says so, and doing it anyway is exactly what made a monster dragged out of an
    // incubator vanish from the slot and never appear in the boxes (owner-reported 2026-09-16).
    // The delta is the only thing that repaints an open window in place.
    sendParty(ctx, charId)
    // The follower is a party monster: boxed, it must stop walking; a new lead takes over.
    presenceService.refreshFollower(ctx)
  }

  private fun placements(
      stored: de.fiereu.openmmo.server.game.storage.StoredCharacter
  ): Map<Long, Pair<PokemonContainer, Short>> =
      (stored.pokemon + stored.pcStorage).associate { it.id to (it.container to it.containerSlot) }

  /** The drag packet's container byte is the client's f/Cy ordinal; PC (0) and party (1) match ours. */
  private fun clientContainer(byte: Int): PokemonContainer? =
      when (byte) {
        PC_CONTAINER -> PokemonContainer.PC
        PARTY_CONTAINER -> PokemonContainer.PARTY
        // The incubator page drags into its own two containers: the egg slots and the single
        // hatch-helper slot that wants a Flame Body or Magma Armor (owner's Larvesta, 2026-09-16).
        INCUBATOR_CONTAINER -> PokemonContainer.INCUBATOR
        HATCH_HELPER_CONTAINER -> PokemonContainer.HATCH_HELPER
        else -> null
      }

  /**
   * The mid-session bag update is a per-stack delta, the same packet the shop flow uses; the full
   * snapshot only lands at login, and the client ignores it afterwards.
   */
  /** Repel / Super Repel / Max Repel: how many steps each keeps weaker wild monsters away (item.c). */
  private val repelSteps: Map<Int, Int> by lazy {
    mapOf(
        items.idOf(de.fiereu.openmmo.items.generated.Items.REPEL) to 100,
        items.idOf(de.fiereu.openmmo.items.generated.Items.SUPER_REPEL) to 200,
        items.idOf(de.fiereu.openmmo.items.generated.Items.MAX_REPEL) to 250,
    )
  }

  /**
   * A repel from the bag: the counter goes into the character (CharacterInfo.repelLeft/repelItemId,
   * the HUD's "{00} repel step(s)" line) and to the client at once through the local delta's 0x10
   * group (f/cd1 -> ZZ.J61 / ZZ.fS1). EncounterService burns a step per step and gates the rolls.
   */
  /**
   * A Boost Item from the bag: consumed on use, then it runs for an hour (see [Boosts]). Only one
   * charm of a type may run, which is the client's own rule - string 5991, quoted back here.
   */
  private suspend fun useCharm(
      ctx: SessionContext,
      charId: Long,
      stored: StoredCharacter,
      itemId: Int,
      kind: Boosts.Kind,
  ) {
    // The charm clock is PLAY TIME, so it pauses while the player is offline: bank the current
    // session first, then measure against it.
    characters.bankPlayTime(charId)
    val playTime = characters.getCharacter(charId)?.info?.playTimeSeconds ?: stored.info.playTimeSeconds
    if (Boosts.isActive(playTime, stored.storyVars, kind)) {
      ctx.reply("You already have a Charm of that type active.")
      return
    }
    characters.setStoryVar(charId, kind.key, Boosts.expiryFrom(playTime))
    characters.addItem(charId, itemId, -1)
    characters.flushCharacterAsync(charId)
    sendStack(ctx, charId, itemId)
    ctx.reply("Used the ${kind.label}. Its effect lasts one hour.")
    log.info { "[UseItem] CHARM char=$charId item=$itemId kind=${kind.name} for ${Boosts.DURATION_SECONDS}s" }
  }

  private suspend fun useRepel(ctx: SessionContext, charId: Long, stored: StoredCharacter, itemId: Int, steps: Int) {
    val name = items.get(itemId)?.name ?: "Repel"
    if (stored.info.repelLeft > 0) {
      ctx.reply("The effect of the previous $name still lingers.")
      return
    }
    characters.updateCharacter(stored.info.copy(repelLeft = steps.toShort(), repelItemId = itemId.toShort()))
    characters.addItem(charId, itemId, -1)
    characters.flushCharacterAsync(charId)
    sendStack(ctx, charId, itemId)
    ctx.send(LocalCharacterDeltaPacket(value16 = Value16Group(steps.toShort(), itemId.toShort())))
    ctx.reply("Used the $name. Weaker wild monsters will stay away for $steps steps.")
    log.info { "[UseItem] REPEL char=$charId item=$itemId steps=$steps" }
  }

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
    // A link overlay shows this party's icons: a rearrange or a withdrawal repaints them.
    links.onPartyChanged(charId)
  }

  private fun de.fiereu.network.SessionContext.reply(message: String) {
    send(notice(message))
  }

  private companion object {
    /** Measured: the Ice Stone (id 21000) arrived as 18952. Validated against the bag per use. */
    const val ITEM_CODE_OFFSET = 2048
    /** The client's trade container in its drag packets (f/Cy 2); the listing side uses 10. */
    const val TRADE_CONTAINER = 2

    /** The client's container ordinals (f/Cy) as its drag packet writes them. */
    const val INCUBATOR_CONTAINER = 13
    const val HATCH_HELPER_CONTAINER = 14
    const val PC_CONTAINER = 0
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

  /**
   * THE BIKE, decoded end to end from the client: the drawn bike is the BIKE skin slot (type 0 =
   * Red Bicycle, per the 31000+ string block), and whether the player renders the mounted frame
   * set is transportation bit 1 (f.ti.aU1; f.F90.aX returns the standing pose whenever f.ti.U7 -
   * that bit - is false). Packet 0x28 (f.OJ0 -> f.tS1.D40) overwrites that byte on the LIVE
   * entity, so the toggle is instant - D40 even plays the region's bike bell on the not-riding ->
   * riding edge. A mount item names the bike skin to ride ([mountType]); the Bicycle rides the
   * skin the customization menu last picked.
   */
  private fun rideBike(ctx: SessionContext, state: PlayerState, charId: Long, stored: StoredCharacter, mountType: Int?) {
    if (mountType != null) {
      // Riding a named mount: that skin from now on; a click on the mount already ridden dismounts.
      val worn = stored.skins[SkinSlot.BIKE]?.type?.toInt()
      if (worn != mountType) {
        characters.setSkin(charId, SkinSlot.BIKE, Skin(SkinSlot.BIKE, mountType.toUShort(), 0u))
        characters.flushCharacterAsync(charId)
        state.riding = false
      }
    } else if (stored.skins[SkinSlot.BIKE] == null) {
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
      presenceService.announce(
          ctx,
          EntitySpriteChangePacket(
              entityId = charId,
              staged = false,
              appearance = SkinSet(current.info.skinRegionSelectionIndex, current.skins),
              gender = current.info.rivalSex,
          ))
    }
    presenceService.announce(ctx, EntityTransportationPacket(charId, if (state.riding) RIDING_TRANSPORTATION else 0))
    log.info {
      "[UseItem] BIKE riding=${state.riding} skin=${characters.getCharacter(charId)?.skins?.get(SkinSlot.BIKE)?.type} char=$charId"
    }
  }

  /** A hotbar click on a cosmetic item the character owns. */
  private fun useCosmetic(ctx: SessionContext, state: PlayerState, charId: Long, stored: StoredCharacter, itemCode: Int) {
    if (itemCode !in stored.items) {
      log.info { "[UseItem] cosmetic $itemCode is not in the bag of char=$charId" }
      return
    }
    val (slot, addonId) = cosmeticSlotAndId(itemCode) ?: return
    if (slot == SkinSlot.BIKE) {
      rideBike(ctx, state, charId, stored, mountType = addonId)
      return
    }
    val worn = stored.skins[slot]?.type?.toInt()
    if (worn != addonId) {
      log.info { "[UseItem] cosmetic $itemCode ($slot $addonId) is not worn by char=$charId (worn $worn)" }
      return
    }
    cosmeticAnimations.play(ctx, charId, slot, addonId)
  }

}

/**
 * Item evolutions, from the same calibrated table the client's overlay applies - wire id, ROM
 * method, item parameter (before the client's +5000 shift), target wire id per line.
 */
object EvolutionTable {
  /** [time] is "day", "night" or null (any time) - a condition the method key does not carry. */
  private data class Entry(val from: Int, val method: Int, val param: Int, val to: Int, val time: String? = null)

  /** ROM methods where the player uses an item on the monster directly: ITEM, ITEM_MALE, ITEM_FEMALE. */
  private val ITEM_METHODS = setOf(8, 17, 18)

  /** The client's second copy of the 5000-band items, each at its 5000-band id + 1000. */
  private val MIRROR_ITEM_BAND = 6000..6999

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
                        if (parts.size !in 4..5) return@mapNotNull null
                        val numbers = parts.take(4).map { it.toIntOrNull() ?: return@mapNotNull null }
                        Entry(numbers[0], numbers[1], numbers[2], numbers[3], time = parts.getOrNull(4)?.ifBlank { null })
                      }
                      .toList()
                } ?: emptyList())
    // The csv's time of day also governs the json's row for the same evolution: a new species'
    // row sits in both, and only the csv knows Hisuian Decidueye is Dartrix's night branch.
    val timeByKey =
        fromCsv.filter { it.time != null }.associate { Triple(it.from, it.method, it.to) to it.time }
    fromJson.map { it.copy(time = timeByKey[Triple(it.from, it.method, it.to)]) } +
        fromCsv.filter { Triple(it.from, it.method, it.to) !in known }
  }

  /**
   * Of the entries whose conditions are met, one for the current time of day wins over one for any
   * time, and one for the other time never fires. That is how a split evolution decides (project
   * owner, 2026-09-13): a Thunder Stone makes Pikachu an Alolan Raichu at night, a Raichu by day.
   */
  private fun pick(candidates: List<Entry>, daytime: Boolean): Entry? {
    val now = if (daytime) "day" else "night"
    return candidates.firstOrNull { it.time == now } ?: candidates.firstOrNull { it.time == null }
  }

  /** Day on the in-game clock the player sees - the same check the level-up evolutions use. */
  fun isDaytime(): Boolean = WorldClock.isDaytime()

  private val preEvolution: Map<Int, Int> by lazy { entries.associate { it.to to it.from } }

  /**
   * Whether client item [itemId] (held or used) is the item an entry's [param] names. The json
   * carries client ids (5233 Metal Coat), the csv the pre-shift ROM value (233), and the client
   * lists every 5000-band item a second time at +1000 (6233 is also Metal Coat, name for name
   * across all 429 of them); an Onix traded holding THAT Metal Coat stayed an Onix (2026-09-08).
   */
  private fun itemMatches(itemId: Int, param: Int): Boolean {
    if (itemId == 0) return false
    val canonical = if (itemId in MIRROR_ITEM_BAND) itemId - 1000 else itemId
    return canonical == param || canonical == param + 5000 || canonical + 5000 == param
  }

  /**
   * The species [fromWire] becomes when it arrives by trade holding [heldItem] (client item id)
   * in exchange for [partnersWire] (the wires that went the other way), or null. ROM methods 5
   * TRADE, 6 TRADE_WITH_ITEM (Metal Coat, King's Rock, ...), 7 TRADE_FOR_OPPOSITE (Shelmet and
   * Karrablast for each other). Item values in the json may carry the client's +5000 shift.
   */
  fun tradeEvolution(fromWire: Int, heldItem: Int, partnersWire: Collection<Int>): Int? =
      entries
          .firstOrNull { entry ->
            entry.from == fromWire &&
                when (entry.method) {
                  5 -> true
                  6 -> itemMatches(heldItem, entry.param)
                  7 -> entry.param in partnersWire
                  else -> false
                }
          }
          ?.to

  /** True when the trade evolution [tradeEvolution] picks for these inputs is the held-item kind (method 6). */
  fun tradeEvolutionUsesHeldItem(fromWire: Int, heldItem: Int, partnersWire: Collection<Int>): Boolean =
      entries
          .firstOrNull { entry ->
            entry.from == fromWire &&
                when (entry.method) {
                  5 -> true
                  6 -> itemMatches(heldItem, entry.param)
                  7 -> entry.param in partnersWire
                  else -> false
                }
          }
          ?.method == 6

  /** True when [wire] has any evolution left, which is what Eviolite asks. */
  fun canEvolve(wire: Int): Boolean = entries.any { it.from == wire }

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

  private val evolutionsFrom: Map<Int, List<Int>> by lazy { entries.groupBy({ it.from }, { it.to }) }

  /** Every species in [wire]'s evolution family (wire ids): its base form and all that grows from it. */
  fun family(wire: Int): Set<Int> {
    val family = linkedSetOf<Int>()
    val queue = ArrayDeque(listOf(baseForm(wire)))
    while (queue.isNotEmpty()) {
      val next = queue.removeFirst()
      if (family.add(next)) queue.addAll(evolutionsFrom[next].orEmpty())
    }
    return family + wire
  }

  /** True when [a] and [b] share an evolution family (the Love Ball's "same species tree"). */
  fun sameFamily(a: Int, b: Int): Boolean = baseForm(a) == baseForm(b)

  /** True when any member of [wire]'s family evolves through friendship (HAPPINESS, _DAY, _NIGHT). */
  fun familyEvolvesByFriendship(wire: Int): Boolean {
    val members = family(wire)
    return entries.any { it.from in members && it.method in 1..3 }
  }

  /** True when any member of [wire]'s family evolves by using client item [itemId] on it. */
  fun familyEvolvesByItem(wire: Int, itemId: Int): Boolean {
    val members = family(wire)
    return entries.any { it.from in members && it.method in ITEM_METHODS && itemMatches(itemId, it.param) }
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
      /** Move ids the monster knows (LEVEL_WITH_SKILL). */
      val moves: Set<Int> = emptySet(),
      /** Client species ids in the party (LEVEL_WITH_MONSTER). */
      val partyWires: Set<Int> = emptySet(),
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
      pick(
          entries.filter { entry ->
            entry.from == fromWire &&
                when (entry.method) {
                  1 -> context.friendship >= FRIENDSHIP_EVOLUTION // HAPPINESS
                  2 -> context.friendship >= FRIENDSHIP_EVOLUTION && context.daytime
                  3 -> context.friendship >= FRIENDSHIP_EVOLUTION && !context.daytime
                  // LEVEL. A 0 is no level: the Expansion's special conditions used to arrive as
                  // level 0 and evolved on any level-up.
                  4 -> entry.param > 0 && context.level >= entry.param
                  21 -> entry.param in context.moves // LEVEL_WITH_SKILL (Tangela, Primeape)
                  22 -> entry.param in context.partyWires // LEVEL_WITH_MONSTER (Mantyke)
                  9 -> context.level >= entry.param && context.attack > context.defense
                  10 -> context.level >= entry.param && context.attack == context.defense
                  11 -> context.level >= entry.param && context.attack < context.defense
                  12 -> context.level >= entry.param && (context.seed shr 16) % 10 >= 5
                  13 -> context.level >= entry.param && (context.seed shr 16) % 10 < 5
                  19, // LEVEL_ITEM_DAY and NIGHT: the held item is the condition; the server
                  20 -> // has no day cycle, so either time works.
                  itemMatches(context.heldItem, entry.param)
                  23 -> context.level >= entry.param && !context.female // LEVEL_MALE
                  24 -> context.level >= entry.param && context.female // LEVEL_FEMALE
                  else -> false
                }
          },
          context.daytime,
      )?.to

  /**
   * The wire id this species becomes when [clientItemId] is used on it, or null. [daytime] decides a
   * split evolution (see [pick]).
   */
  fun itemEvolution(fromWire: Int, clientItemId: Int, daytime: Boolean = isDaytime()): Int? =
      pick(
          entries.filter {
            it.from == fromWire && it.method in ITEM_METHODS && itemMatches(clientItemId, it.param)
          },
          daytime,
      )?.to
}
