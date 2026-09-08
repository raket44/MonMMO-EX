package de.fiereu.openmmo.server.game.script

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.DynamicWarp
import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ItemDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.dialog.DialogMessageArg
import de.fiereu.openmmo.net.game.packets.dialog.TextPokemonSpeciesArg
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.DialogPresentation
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.MapEntryScripts
import de.fiereu.openmmo.server.game.services.ScriptMovementService
import de.fiereu.openmmo.server.game.services.ScriptWarpService
import de.fiereu.openmmo.server.game.services.ShopService
import de.fiereu.openmmo.server.game.services.StoryClientState
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.session.DialogMessageMode
import de.fiereu.openmmo.server.game.session.DialogTextColor
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.trainer.TrainerDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext

/** What a [Script] uses to talk to the player it interacted with and read or write story state. */
private val log = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

class ScriptContext
internal constructor(
    internal val session: SessionContext,
    internal val state: PlayerState,
    /** The npc the player talked to, or -1 for a sign. */
    val entityId: Long,
    internal val dialog: DialogService,
    internal val story: StoryService,
    private val movement: ScriptMovementService,
    private val warp: ScriptWarpService? = null,
    internal val player: StoryPlayerService? = null,
    private val battles: BattleService? = null,
    internal val characters: CharacterStore? = null,
    private val maps: MapManager? = null,
    private val entryScripts: MapEntryScripts? = null,
    private val shops: ShopService? = null,
    private val developerTools: DeveloperTools? = null,
    private val moves: de.fiereu.openmmo.moves.MoveRegistry? = null,
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry? = null,
    private val layoutVariants: de.fiereu.openmmo.server.game.services.LayoutVariants? = null,
    private val banners: de.fiereu.openmmo.server.game.services.FieldMoveBanners? = null,
    private val tutor: de.fiereu.openmmo.server.game.services.MoveTutorService? = null,
) {
  private val characterId: Long?
    get() = state.characterId

  val facingDirection: Direction
    get() = state.facingDirection

  val isFemale: Boolean
    get() = movement.playerGender(state) == FEMALE

  internal val playerEntityId: Long
    get() = checkNotNull(state.characterId) { "Scene has no selected character" }

  internal val playerName: String
    get() = state.characterId?.let { characters?.getCharacter(it)?.info?.name }.orEmpty()

  internal fun send(packet: Any) = session.send(packet)

  internal fun traceInterpreter(message: () -> String) = developerTools?.trace(message)

  /**
   * Between dialog boxes the client returns the movement controller and lets the player twirl in
   * place while the script still holds them. Every dialog a script shows re-takes the controller
   * first with the facing the script last gave the player, so the sprite snaps back and stays put
   * until the script releases.
   */
  private fun holdScriptedFacing() = movement.reassertScriptedFacing(session, state)

  /** Show [line] as a sign and wait for the player to close it. */
  suspend fun sign(line: DialogLine) {
    holdScriptedFacing()
    dialog.showAndWait(session, state, line.textId, SIGN, -1, presentation())
  }

  /** Show [line] from the interacted entity and wait for the player to go on. */
  suspend fun say(line: DialogLine) {
    holdScriptedFacing()
    dialog.showAndWait(session, state, line.textId, NPC, entityId, presentation())
  }

  /** [say] attributed to another npc entity - the second trainer of a double sighting speaks for itself. */
  suspend fun sayAs(speaker: Long, line: DialogLine) {
    holdScriptedFacing()
    dialog.showAndWait(session, state, line.textId, NPC, speaker, presentation())
  }

  /** Begin a pret `message`; the following wait command owns the client acknowledgement. */
  /**
   * Text placeholder arguments (`{0N}` in ROM text) the Gen 4 Buffer* commands set; every dialog
   * this context shows carries them, like the DS keeps its string buffers until overwritten.
   */
  private val messageArgs = java.util.TreeMap<Int, DialogMessageArg>()

  fun setMessageArg(slot: Int, arg: DialogMessageArg) {
    messageArgs[slot] = arg
  }

  private fun presentation() = DialogPresentation(messageArgs = messageArgs.values.toList())

  internal fun showMessage(line: DialogLine) {
    holdScriptedFacing()
    val sign = state.dialogMessageMode == DialogMessageMode.SIGN
    dialog.show(
        session,
        state,
        line.textId,
        if (sign) SIGN else NPC,
        if (sign) -1 else entityId,
        presentation(),
    )
  }

  internal fun setDialogMessageMode(mode: DialogMessageMode) {
    state.dialogMessageMode = mode
  }

  internal fun setDialogTextColor(color: DialogTextColor) {
    state.dialogTextColor = color
  }

  /** Show [line] from a cutscene npc addressed by its decomp local id. */
  suspend fun sayNpc(localId: Int, line: DialogLine) {
    holdScriptedFacing()
    dialog.showAndWait(
        session,
        state,
        line.textId,
        NPC,
        movement.npcEntityId(state, localId) ?: -1,
    )
  }

  /** Shows dialog with a species-name variable. */
  suspend fun sayNpcWithSpeciesName(localId: Int, line: DialogLine, speciesId: Int) {
    holdScriptedFacing()
    dialog.showAndWait(
        session,
        state,
        line.textId,
        NPC,
        movement.npcEntityId(state, localId) ?: -1,
        DialogPresentation(
            messageArgs =
                listOf(
                    TextPokemonSpeciesArg(
                        partySlot = 1,
                        stringVariable = 1,
                        speciesId = speciesId.toShort(),
                    ))),
    )
  }

  /** Opens the Emerald starter picker. */
  suspend fun chooseHoennStarter(): Int {
    holdScriptedFacing()
    return dialog.chooseHoennStarter(session, state)
  }

  /** Shows a built-in client choice menu over [line]; 1-based pick, 0 = closed unanswered. */
  suspend fun builtinMenu(line: DialogLine, menuSet: Int): Int {
    holdScriptedFacing()
    return dialog.builtinMenu(session, state, line.textId, menuSet)
  }

  /** Opens the client's daycare breed-selection window over [line]; returns its response. */
  suspend fun breedSelection(line: DialogLine): Int {
    holdScriptedFacing()
    return dialog.breedSelection(session, state, line.textId)
  }

  /** Opens the client's storage window on the player's PC boxes (fresh contents first). */
  fun openPcStorageWindow() {
    val stored = characterId?.let { characters?.getCharacter(it) } ?: return
    session.send(
        de.fiereu.openmmo.net.game.packets.PokemonContainerPacket(
            container = de.fiereu.openmmo.common.enums.PokemonContainer.PC,
            hasChange = true,
            delete = false,
            pokemon = stored.pcStorage,
        ))
    session.send(de.fiereu.openmmo.net.game.packets.battle.PcTogglePacket(shown = true))
  }

  /** Ask a ROM-backed yes/no question from the interacted entity. */
  suspend fun askYesNo(line: DialogLine): Boolean {
    holdScriptedFacing()
    return dialog.askYesNo(session, state, line.textId, entityId, messageArgs.values.toList())
  }

  /** Ask a ROM-backed yes/no question from a cutscene npc. */
  suspend fun askYesNoNpc(localId: Int, line: DialogLine): Boolean {
    holdScriptedFacing()
    return dialog.askYesNo(
        session,
        state,
        line.textId,
        movement.npcEntityId(state, localId) ?: -1,
    )
  }

  /** True if the story [flag] is set. Keys come from the content layer, for example HoennFlags. */
  fun isFlagSet(flag: String): Boolean = characterId?.let { story.isFlagSet(it, flag) } ?: false

  fun setFlag(flag: String) {
    characterId?.let { charId ->
      story.setFlag(charId, flag)
      // A flag that selects a map variant swaps the client's block grid right away.
      layoutVariants?.onFlagSet(session, state, flag)
      val update = StoryClientState.flagUpdate(state.regionId.toByte(), flag, enabled = true)
      if (update != null) {
        session.send(update)
        // A badge flag also gets the retail grey popup ("Pokemon up to lv. N will now obey
        // you") - the visible badge-obtained moment for the first four regions.
        characters?.getCharacter(charId)?.storyFlags?.let { flags ->
          StoryClientState.badgeAnnouncement(state.regionId.toByte(), update.flagId, flags)
              ?.let(session::send)
        }
      }
    }
  }

  fun clearFlag(flag: String) {
    characterId?.let {
      story.clearFlag(it, flag)
      StoryClientState.flagUpdate(state.regionId.toByte(), flag, enabled = false)
          ?.let(session::send)
    }
  }

  /** The story var [key], or 0 if it was never set. */
  fun getVar(key: String): Int = characterId?.let { story.getVar(it, key) } ?: 0

  fun setVar(key: String, value: Int) {
    characterId?.let { story.setVar(it, key, value) }
  }

  /** Record the local player/interacted-object lock used by this script. */
  fun lock() {
    state.lockLocal(entityId)
    movement.holdPlayer(session, state)
  }

  /** Record the stronger all-object lifecycle lock used by cutscenes. */
  fun lockAll() {
    state.lockAll()
    movement.holdPlayer(session, state)
  }

  /** Release a local lifecycle lock and close any visible message, without ending the script. */
  fun release() {
    dialog.close(session, state)
    state.releaseScriptLock()
    // The queue clear is left to the runner: it waits out any still-animating player walk
    // first, or a release right after a scripted walk clears it mid-step (the lab "poof").
  }

  /** Release an all-object lifecycle lock and close any visible message. */
  fun releaseAll() {
    dialog.close(session, state)
    state.releaseScriptLock()
  }

  /** Close only the visible message. Script execution and lifecycle ownership continue. */
  fun closeMessage() = dialog.close(session, state)

  /** Wait for the current message operation, if it has not already completed. */
  suspend fun waitMessage() = dialog.waitForMessage(session)

  /** Wait for A/B on the active message using the client's dialog acknowledgement. */
  internal suspend fun waitButtonPress() = dialog.waitForButtonPress(session)

  suspend fun givePokemon(dexId: Int, level: Int, vararg moveIds: Int) =
      checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }
          .givePokemon(session, state, dexId, level, moveIds.toList())

  fun healParty() = checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.healParty(session, state)

  suspend fun giveItem(item: ItemDef, quantity: Int = 1): Boolean =
      checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.giveItem(session, state, item, quantity)

  /** Take an item back out of the bag, the decomp removeitem. False when the bag lacks it. */
  suspend fun takeItem(item: ItemDef, quantity: Int = 1): Boolean = giveItem(item, -quantity)

  fun resolveItem(constant: String): ItemDef? =
      checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.itemByScriptConstant(constant)

  /** An item by client wire id (region * 1000 + the game's own index), for DS var-valued items. */
  fun resolveItemWire(id: Int): ItemDef? = checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.itemByWireId(id)

  fun itemCount(item: ItemDef): Int =
      checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.itemCount(state, item)

  /** MALE is 0 and FEMALE 1, matching the GBA checkplayergender result. */
  fun playerGender(): Int = movement.playerGender(state)?.toInt() ?: 0

  fun playerXy(): Pair<Int, Int>? = movement.playerXy(state)

  /** The hide flag of the npc the player is talking to, for finditem's disappearing item ball. */
  fun interactingHideFlag(): String? = interactingLocalId()?.let { movement.npcHideFlag(state, it) }

  /**
   * Opens the mart window on [items], the decomp pokemart. It does not wait: the player shops while
   * the script ends, because nothing in the capture tells the server when the window closed.
   */
  fun pokemart(vararg items: ItemDef) =
      checkNotNull(shops) { "Shop service is unavailable" }.open(session, entityId, items.toList())

  /** Run a non-catchable, non-escapable story battle and wait for its result. */
  suspend fun battle(dexId: Int, level: Int, vararg moveIds: Int): BattleResult =
      checkNotNull(battles) { "Battle service is unavailable" }
          .startScriptedBattle(session, dexId, level, moveIds.toList())

  /** Fight the decomp trainer with this id, using the region the player is standing in. */
  suspend fun trainerBattle(trainerId: Int): BattleResult {
    val region =
        checkNotNull(Region.byWireValue(state.regionId.toByte())) {
          "Scene ran in unknown region ${state.regionId}"
        }
    return checkNotNull(battles) { "Battle service is unavailable" }
        .startTrainerBattle(session, region, trainerId)
  }

  /**
   * givemon: a gift monster into the party. Returns the ROM's MON_GIVEN_TO_PARTY (0) or
   * MON_CANT_GIVE (2); the PC fallback (1) is not modelled, a full party refuses the gift.
   */
  suspend fun giveMonster(dexId: Int, level: Int): Int {
    val given = player?.givePokemon(session, state, dexId, level, moveIds = null) ?: return 2
    return if (given != null) 0 else 2
  }

  /** IsPlayerLeftOfVermilionSailor: the player stands at a lower x than the npc's placement. */
  fun isPlayerLeftOfNpc(localId: Int): Boolean {
    val (px, _) = playerXy() ?: return false
    val info = characterId?.let { characters?.getCharacter(it)?.info } ?: return false
    val npc = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)?.npcs?.getOrNull(localId) ?: return false
    return px < npc.x
  }

  /** A client string shown as a server message on the map (the proven 0xF5 channel). */
  fun clientMessage(stringId: Int) {
    session.send(de.fiereu.openmmo.net.game.packets.ServerMessagePacket(stringId, emptyList(), showOnMap = true, mode = null))
  }

  /** The player's money, for checkmoney. */
  fun money(): Int = characterId?.let { characters?.getCharacter(it)?.info?.money } ?: 0

  /** addmoney / removemoney: the wallet moves and the client's own counter follows. */
  fun addMoney(delta: Int) {
    val id = characterId ?: return
    val store = characters ?: return
    val info = store.getCharacter(id)?.info ?: return
    val updated = info.copy(money = (info.money + delta).coerceIn(0, MAX_MONEY))
    store.updateCharacter(updated)
    store.flushCharacterAsync(id)
    session.send(de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket(money = updated.money))
  }

  /** getpartysize. */
  fun partySize(): Int = characterId?.let { characters?.getCharacter(it)?.pokemon?.size } ?: 0

  /** setwildbattle + dowildbattle: a scripted wild encounter the script waits out. */
  internal suspend fun wildBattle(dexId: Int, level: Int): BattleResult =
      checkNotNull(battles) { "Battle service is unavailable" }.startScriptedWildBattle(session, dexId, level)

  /**
   * copyobjectxytoperm: an npc's current tile becomes its permanent one. The live tile is the
   * override a script already wrote, else the map's own placement.
   */
  /**
   * copyobjectxytoperm: the tile the npc stands on NOW becomes its template for later spawns -
   * after the scripted walk that just moved it (Miguel beside the fossil he claimed), not the
   * tile the map data lists. Re-sending the template tile here snapped him back mid-walk.
   */
  fun copyNpcXyToPerm(localId: Int) {
    movement.npcXyOverrideKey(state, localId) ?: return
    val walked = movement.scriptedNpcPose(state, localId)
    val info = characterId?.let { characters?.getCharacter(it)?.info } ?: return
    val npc = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)?.npcs?.firstOrNull { it.entityIdx == localId } ?: return
    setNpcXyOverride(localId, walked?.x ?: npc.x, walked?.y ?: npc.y)
  }

  /**
   * special ChooseMonForMoveTutor: the player picks the party member to teach the move the tutor
   * index (VAR_0x8005) names; true once it sits in a slot, false when they back out.
   */
  suspend fun chooseMonForMoveTutor(tutorIndex: Int): Boolean {
    val service = tutor ?: return false
    holdScriptedFacing()
    return service.tutor(session, state, entityId, tutorIndex)
  }

  /** checkpartymove: the party slot of the first monster knowing [moveId], PARTY_SIZE (6) if none. */
  /**
   * checkpartymove, with the engine's badge gate folded in (the cartridge checks the badge before
   * it ever runs the script) and the client's ocarinas honoured: an owned ocarina for the move
   * stands in for a party member knowing it, as its own description promises, and answers slot 0.
   */
  /**
   * The client's HM banner ("{mon} used Cut!" with the party member, or the region's stand-in
   * for an ocarina): s2c 0xB6 case 2 (f/Ty0) - subject = move id, args = [ocarina item id or -1,
   * serial]. The client remembers the last (item, value) pair and skips a repeat (f/ln1.lPt7),
   * hence the serial. Sent at the script's dofieldeffect in place of the ROM's message box.
   */
  fun fieldMoveBanner(fieldEffect: String): Boolean {
    val fm = de.fiereu.openmmo.server.game.services.FieldMoves
    val moveId =
        when {
          !fieldEffect.startsWith("FLDEFF_USE_") && fieldEffect != "FLDEFF_FLASH" -> return false
          "CUT" in fieldEffect -> fm.CUT
          "ROCK_SMASH" in fieldEffect -> fm.ROCK_SMASH
          "STRENGTH" in fieldEffect -> fm.STRENGTH
          "SURF" in fieldEffect -> fm.SURF
          "WATERFALL" in fieldEffect -> fm.WATERFALL
          "DIVE" in fieldEffect -> fm.DIVE
          "FLASH" in fieldEffect -> fm.FLASH
          else -> return false
        }
    val stored = characterId?.let { characters?.getCharacter(it) } ?: return false
    return banners?.send(session, stored, state.regionId, moveId) ?: false
  }

  fun partyIndexWithMove(moveId: Int): Int {
    val stored = characterId?.let { characters?.getCharacter(it) } ?: return de.fiereu.openmmo.common.MAX_PARTY_SIZE
    if (!de.fiereu.openmmo.server.game.services.FieldMoves.badgeHeld(stored, state.regionId, moveId)) return de.fiereu.openmmo.common.MAX_PARTY_SIZE
    val index = stored.pokemon.indexOfFirst { mon -> mon.moves.any { it.id.toInt() == moveId } }
    if (index >= 0) return index
    return if (de.fiereu.openmmo.server.game.services.FieldMoves.ocarinaOwned(stored, moveId)) 0 else de.fiereu.openmmo.common.MAX_PARTY_SIZE
  }

  /**
   * FLDEFF_USE_SURF: the player mounts (transportation bit 0x01, client f.ti.J10) and takes the
   * step onto the water it faces. Land clears the bit again in MovementService.
   */
  suspend fun startSurfing() {
    state.riding = false
    state.surfing = true
    session.send(de.fiereu.openmmo.net.game.packets.EntityTransportationPacket(playerEntityId, SURF_TRANSPORTATION.toByte()))
    val step =
        when (state.facingDirection) {
          Direction.UP -> MovementStep.WALK_UP
          Direction.LEFT -> MovementStep.WALK_LEFT
          Direction.RIGHT -> MovementStep.WALK_RIGHT
          else -> MovementStep.WALK_DOWN
        }
    movement.moveSelf(session, state, listOf(step))
  }

  /**
   * Waterfall (FLDEFF_USE_WATERFALL): the surfer rides up every waterfall tile ahead and onto the
   * water above it, the forced climb the GBA's field effect performs.
   */
  suspend fun climbWaterfall() {
    val id = characterId ?: return
    val info = characters?.getCharacter(id)?.info ?: return
    val map = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val x = info.positionX.toInt()
    var y = info.positionY.toInt() - 1
    var falls = 0
    while (map.tileAt(x, y)?.behavior == de.fiereu.openmmo.common.enums.TileBehavior.WATERFALL) {
      falls++
      y--
    }
    if (falls == 0) return
    state.surfing = true
    movement.moveSelf(session, state, List(falls + 1) { MovementStep.WALK_UP })
  }

  /**
   * Dive (FLDEFF_USE_DIVE): the map's DIVE connection is the sea floor under this spot and its
   * EMERGE connection the surface above; the player keeps the tile. The client draws underwater
   * from the map kind on its own, and the surf flag stays so the floor counts as ridable.
   */
  fun dive() {
    val id = characterId ?: return
    val info = characters?.getCharacter(id)?.info ?: return
    val map = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId) ?: return
    val wanted = if (state.underwater) Direction.EMERGE else Direction.DIVE
    val link = map.connections.firstOrNull { it.direction == wanted }
    if (link == null) {
      log.info { "Dive: ${info.positionRegionId}:${info.positionBankId}:${info.positionMapId} has no $wanted connection" }
      return
    }
    val goingUnder = wanted == Direction.DIVE
    warp?.rawWarp(session, id, info.positionRegionId.toInt(), link.targetBank, link.targetMap, info.positionX.toInt(), info.positionY.toInt())
    state.surfing = true
    state.underwater = goingUnder
    log.info { "Dive: char=$id ${if (goingUnder) "went under to" else "surfaced to"} ${link.targetBank}:${link.targetMap}" }
  }


  /**
   * setmetatile: the tile the player's map shows at (x, y) becomes [metatileId] with the given
   * collision, for this player, until the map reloads (s2c 0x22). Movement reads the override.
   */
  fun setMetatile(x: Int, y: Int, metatileId: Int, impassable: Boolean) {
    val id = characterId ?: return
    val info = characters?.getCharacter(id)?.info ?: return
    // The block's upper byte is collision (bits 0-1) plus elevation (bits 2-5, stored +1). The
    // client compares elevations when walking and draws by them, so the existing tile's bits
    // stay and only the collision changes - a bare 0/1 here read as elevation -1: unwalkable and
    // drawn on the wrong layer.
    val key = (x shl 16) or (y and 0xFFFF)
    val map = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    val existing = state.tileOverrides[key] ?: map?.tileAt(x, y)
    val collision: Byte = (((existing?.collision?.toInt() ?: 0x10) and 0xFC) or (if (impassable) 1 else 0)).toByte()
    state.tileOverrides[key] =
        de.fiereu.openmmo.common.Tile2D(metatileId.toShort(), collision, existing?.behavior ?: de.fiereu.openmmo.common.enums.TileBehavior.NORMAL)
    // The client's own setmetatile: one tile packet with the ROM's metatile id and the GBA upper
    // byte (collision bits + the tile's existing elevation), drawn from the ROM tileset.
    session.send(
        de.fiereu.openmmo.net.game.packets.MapTileSetPacket(
            info.positionRegionId, info.positionBankId, info.positionMapId,
            x.toShort(), y.toShort(), collision.toShort(), metatileId.toShort()))
  }

  /** A ROM string variable (STR_VAR_n) for the next dialogs of this script: a raw text argument. */
  fun bufferText(variable: Int, text: String) {
    setMessageArg(variable, de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg(slot = variable.toByte(), kind = 5, text = text))
  }

  fun partyNickname(slot: Int): String? {
    val mon = characterId?.let { characters?.getCharacter(it)?.pokemon?.getOrNull(slot) } ?: return null
    return mon.nickname.ifEmpty { speciesName(mon.dexId) }
  }

  fun moveName(moveId: Int): String? = moves?.get(moveId)?.name

  fun speciesName(dexId: Int): String? = speciesRegistry?.get(dexId)?.name

  fun leadSpeciesName(): String? =
      characterId?.let { characters?.getCharacter(it)?.pokemon?.firstOrNull() }?.let { speciesName(it.dexId) }

  /** Flash (setflashlevel): lights the map the player stands in until its next load (s2c 0xC1). */
  fun lightMap(level: Int) {
    session.send(de.fiereu.openmmo.net.game.packets.MapLightingPacket(level.toByte(), lit = true))
  }

  /** How many party monsters can still fight - the vanilla double-battle entry gate reads it. */
  internal fun ablePartyCount(): Int =
      characterId?.let { id -> characters?.getCharacter(id)?.pokemon?.count { it.hp > 0 } } ?: 0

  internal fun resolveTrainer(constant: String): TrainerDef {
    val region =
        checkNotNull(Region.byWireValue(state.regionId.toByte())) {
          "Scene ran in unknown region ${state.regionId}"
        }
    return checkNotNull(battles) { "Battle service is unavailable" }
        .resolveTrainer(region, constant) ?: error("No $region trainer resolves from $constant")
  }

  internal fun resolveTrainerById(id: Int): TrainerDef {
    val region =
        checkNotNull(Region.byWireValue(state.regionId.toByte())) {
          "Scene ran in unknown region ${state.regionId}"
        }
    return checkNotNull(battles) { "Battle service is unavailable" }.resolveTrainer(region, id)
        ?: error("No $region trainer resolves from id $id")
  }

  internal suspend fun trainerBattle(
      trainer: TrainerDef,
      defeatTextId: Int? = null,
      whiteoutOnDefeat: Boolean = true,
  ): BattleResult {
    // A double sighting queued a second trainer: both fight at once, then the flag clears.
    val partner = state.pendingPartnerTrainer
    val partnerDefeat = state.pendingPartnerDefeatTextId
    state.pendingPartnerTrainer = null
    state.pendingPartnerDefeatTextId = null
    return checkNotNull(battles) { "Battle service is unavailable" }
        .startTrainerBattle(session, trainer, defeatTextId, whiteoutOnDefeat, partner, partnerDefeat)
  }

  /**
   * Walk the map npc with decomp local id [localId] (its entityIdx) through [steps] and wait for
   * the whole path to finish. This is applymovement plus waitmovement for an npc.
   */
  suspend fun moveNpc(localId: Int, vararg steps: MovementStep) =
      movement.moveNpc(session, state, localId, steps.toList())

  /** Starts concurrent NPC movement paths. */
  suspend fun moveNpcs(vararg paths: Pair<Int, List<MovementStep>>) =
      movement.moveNpcs(session, state, paths.toList())

  /** Starts player and NPC paths together. */
  suspend fun moveSelfAndNpcs(
      selfSteps: List<MovementStep>,
      vararg paths: Pair<Int, List<MovementStep>>,
  ) = movement.moveSelfAndNpcs(session, state, selfSteps, paths.toList())

  /** Walk the player's own avatar through [steps] and wait for it to finish. */
  suspend fun moveSelf(vararg steps: MovementStep) =
      movement.moveSelf(session, state, steps.toList())

  /** Starts an interpreted npc movement now; waitmovement awaits the returned operation later. */
  internal suspend fun applyNpcMovement(
      localId: Int,
      steps: List<MovementStep>,
  ): Deferred<Unit> {
    movement.requireNpc(state, localId)
    return CoroutineScope(currentCoroutineContext()).async(start = CoroutineStart.UNDISPATCHED) {
      movement.moveNpc(session, state, localId, steps)
    }
  }

  /**
   * Starts an interpreted player movement now; waitmovement awaits the returned operation later.
   */
  internal suspend fun applyPlayerMovement(steps: List<MovementStep>): Deferred<Unit> =
      CoroutineScope(currentCoroutineContext()).async(start = CoroutineStart.UNDISPATCHED) {
        movement.moveSelf(session, state, steps)
      }

  internal fun interactingLocalId(): Int? = movement.localIdForEntity(state, entityId)

  /** Authentic faceplayer: turn only the currently selected object toward the player. */
  internal fun facePlayer() = movement.facePlayer(session, entityId, facingDirection)

  /** Show a normally hidden map npc (its decomp local id) to this player, the decomp addobject. */
  /** Show a hidden npc (`addobject`). Clears its hide flag, mirroring the decomp command. */
  /** GBA addobject: spawn the npc as it is, leaving its hide flag alone. */
  fun addNpc(localId: Int) = movement.showNpc(session, state, localId)

  /** Turn a map npc toward the player (the ROM's VAR_FACING branch ladders). */
  fun npcFacePlayer(localId: Int) {
    movement.npcEntityId(state, localId)?.let { movement.facePlayer(session, it, facingDirection) }
  }

  fun showNpc(localId: Int) {
    movement.npcHideFlag(state, localId)?.let(::clearFlag)
    movement.showNpc(session, state, localId)
  }

  /**
   * Shows a hidden NPC at a new position, as a session visual only - scenes use it to display a
   * displaced copy while the hide flag keeps the template spawn suppressed.
   */
  fun showNpcAt(localId: Int, x: Int, y: Int) = movement.showNpcAt(session, state, localId, x, y)

  /** Repositions an existing NPC. */
  fun repositionNpc(localId: Int, x: Int, y: Int) =
      movement.repositionNpc(session, state, localId, x, y)

  /** Relocate the player's overworld entity as part of a cutscene. */
  fun repositionSelf(x: Int, y: Int, facing: Direction) =
      movement.repositionSelf(session, state, x, y, facing)

  /**
   * Remove an npc and its collision (`removeobject`). With [persist] the npc's hide flag is set
   * too, the way the decomp command does it, so the removal survives re-entering the map - the
   * departed lab rival, taken starter balls, collected item balls. Without it the removal is a
   * cutscene visual and the npc comes back with the map.
   */
  fun removeNpc(localId: Int, persist: Boolean = false) {
    movement.removeNpc(session, state, localId)
    if (persist) movement.npcHideFlag(state, localId)?.let(::setFlag)
  }

  /** GBA respawns a map npc when its hide flag clears; scripted clearflag mirrors that. */
  fun respawnNpcForClearedHideFlag(flag: String) =
      movement.respawnNpcByHideFlag(session, state, flag)

  /** Persist an npc's overridden tile (`setobjectxyperm`); every later spawn uses it. */
  /** setobjectmovementtype: pins an npc's movement type for this map visit and re-sends it. */
  fun setNpcMovementType(localId: Int, type: de.fiereu.openmmo.common.enums.MovementType) {
    val key = movement.npcMovementOverrideKey(state, localId) ?: return
    setVar(key, type.ordinal + 1)
    movement.showNpc(session, state, localId)
  }

  fun setNpcXyOverride(localId: Int, x: Int, y: Int) {
    val key = movement.npcXyOverrideKey(state, localId) ?: return
    setVar(key, (x shl 12) or y)
    repositionNpc(localId, x, y)
  }

  /** Set where a MAP_DYNAMIC warp sends this player (the decomp setdynamicwarp). */
  fun setDynamicWarp(regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int, facing: Direction) =
      movement.setDynamicWarp(
          state,
          DynamicWarp(
              regionId.toByte(), bankId.toByte(), mapId.toByte(), x.toShort(), y.toShort(), facing))

  /**
   * Warps the player without door movement, then runs the destination map's entry scripts on this
   * same coroutine, the way the decomp's warp continues into the new map's scripts.
   */
  /** A DS map by ROM header (bank = low byte, map = high byte), through the raw warp path. */
  fun rawWarp(regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int) {
    val warpService = checkNotNull(warp) { "Script warp service is unavailable" }
    val charId = checkNotNull(state.characterId) { "Scene has no selected character" }
    warpService.rawWarp(session, charId, regionId, bankId, mapId, x, y)
  }

  suspend fun warp(regionId: Int, bankId: Int, mapId: Int, x: Int, y: Int, facing: Direction) {
    val warpService = checkNotNull(warp) { "Script warp service is unavailable" }
    state.scriptOwnsMapEntry = true
    try {
      warpService.warp(
          session,
          state,
          DynamicWarp(
              regionId.toByte(),
              bankId.toByte(),
              mapId.toByte(),
              x.toShort(),
              y.toShort(),
              facing,
          ),
      )
      val destination = maps?.getMap(regionId, bankId, mapId) ?: return
      val scripts = entryScripts ?: return
      // This coroutine owns the destination's entry scripts; the arrival's own player requests
      // must not run a second copy.
      state.entryScriptsMapKey =
          de.fiereu.openmmo.server.game.services.MapScriptService.entryScriptsKey(destination)
      scripts.onEntry(state, destination).forEach { it.run(this) }
      state.characterId?.let { charId ->
        scripts.atCoordinate(charId, destination, state.x.toInt(), state.y.toInt())?.run(this)
      }
    } finally {
      state.scriptOwnsMapEntry = false
    }
  }

  private companion object {
    // Sign boxes have no speaker, npc boxes point at the entity.
    const val SIGN = 3
    const val NPC = 4
    const val FEMALE: Byte = 1
    const val STORY_PLAYER_UNAVAILABLE = "Story player service is unavailable"
  }
}

/** Transportation byte while surfing: bit 0x01, client f.ti.J10. */
private const val SURF_TRANSPORTATION = 0x01

/** The GBA wallet cap. */
private const val MAX_MONEY = 999_999
