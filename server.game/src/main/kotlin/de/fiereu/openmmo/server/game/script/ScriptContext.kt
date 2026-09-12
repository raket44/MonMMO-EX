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
    private val safari: de.fiereu.openmmo.server.game.services.SafariService? = null,
    private val mapScripts: de.fiereu.openmmo.server.game.services.MapScriptService? = null,
) {
  private val characterId: Long?
    get() = state.characterId

  val facingDirection: Direction
    get() = state.facingDirection

  /**
   * The facing the player had as this script started. The cartridge writes gSpecialVar_Facing
   * at the input that starts a script (field_control_avatar.c) and never again while it runs,
   * so a scene's own turns do not change which VAR_FACING branch it takes - reading the live
   * facing made the National Dex scene walk the rival in three times (2026-09-12).
   */
  private val facingAtStart: Direction = state.facingDirection

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

  /** Set by GetInGameTradeSpeciesInfo so the next ChoosePartyMon window asks which monster to trade. */
  private var tradePickPending = false

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

  /**
   * A braille sign: the client's own braille dialog (kind 19, f/qM1.WN) reads the sign's
   * `.braille` bytes from the ROM at [line]'s id and draws them with its braille font - a
   * string argument is never looked at (a client-string id drew fourteen garbage cells,
   * 2026-09-12). The following waitbuttonpress owns the acknowledgement, like a message.
   */
  internal fun showBraille(line: DialogLine) {
    holdScriptedFacing()
    dialog.show(session, state, line.textId, BRAILLE, -1, DialogPresentation())
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

  /**
   * special GetElevatorFloor (src/field_specials.c): the floor index the dynamic warp names, for
   * every FireRed elevator - Silph Co and the Department Store nF = n + 3, Rocket Hideout BnF =
   * 4 - n (B1F 3, B2F 2, B4F 0), Trainer Tower floors 15 and its lobby 3 - or 4 anywhere else.
   */
  fun elevatorFloor(): Int {
    val name = dynamicWarpMapName() ?: return 4
    if (name.startsWith("TrainerTower_")) return if (name == "TrainerTower_Lobby") 3 else 15
    val floor = FLOOR_SUFFIX.find(name) ?: return 4
    val n = floor.groupValues[2].toInt()
    return if (floor.groupValues[1].isEmpty()) n + 3 else 4 - n
  }

  /**
   * specialvar InitElevatorFloorSelectMenuPos: the row of the elevator's own floor in its menu
   * (the ROM's scroll + cursor): Silph Co lists 11F..1F, the Department Store 5F..1F, the Rocket
   * Hideout B1F, B2F, B4F; anything else starts at the top.
   */
  fun elevatorMenuPosition(): Int {
    val name = dynamicWarpMapName() ?: return 0
    val n = FLOOR_SUFFIX.find(name)?.groupValues?.get(2)?.toInt() ?: return 0
    return when {
      name.startsWith("SilphCo_") -> 11 - n
      name.startsWith("CeladonCity_DepartmentStore_") -> 5 - n
      name.startsWith("RocketHideout_B") -> if (n == 4) 2 else n - 1
      else -> 0
    }
  }

  /**
   * The DS elevator floor of the dynamic warp's map: [floors] is the game's own table by header id
   * (bank | map << 8), [fallback] what the game answers for a map outside it.
   */
  fun dsDynamicWarpFloor(regionId: Int, floors: Map<String, Int>, fallback: Int): Int {
    val warp = characterId?.let { characters?.getCharacter(it)?.info?.dynamicWarp } ?: return fallback
    if (warp.regionId.toInt() != regionId) return fallback
    val header = (warp.bankId.toInt() and 0xFF) or ((warp.mapId.toInt() and 0xFF) shl 8)
    return floors[header.toString()] ?: fallback
  }

  /** special EnterSafariMode: the flag, 30 balls and 600 steps, the client's counters told. */
  suspend fun enterSafari() {
    characterId?.let { safari?.enter(session, it) }
  }

  /** special ExitSafariMode. */
  suspend fun exitSafari() {
    characterId?.let { safari?.exit(session, it) }
  }

  /** Flags parked in vars by `setvar VAR, FLAG_X` (FireRed stores the flag id; ours are named), keyed by the namespaced var. */
  private val flagsInVars = mutableMapOf<String, String>()

  fun rememberFlagInVar(namespacedVar: String, namespacedFlag: String) {
    flagsInVars[namespacedVar] = namespacedFlag
  }

  /** `special SetHiddenItemFlag`: FlagSet(gSpecialVar_0x8004) - sets the flag the var was loaded with. */
  fun setFlagRememberedInVar(namespacedVar: String) {
    val flag = flagsInVars[namespacedVar] ?: return
    setFlag(flag)
  }

  /** specialvar IsThereRoomInAnyBoxForMorePokemon. */
  fun pcHasRoom(): Boolean =
      (characterId?.let { characters?.getCharacter(it)?.pcStorage?.size } ?: 0) < de.fiereu.openmmo.server.game.storage.PC_CAPACITY

  /** specialvar DoesPlayerPartyContainSpecies: VAR_0x8004 names the species (MON_DATA_SPECIES_OR_EGG). */
  /**
   * specialvar GetInGameTradeSpeciesInfo: VAR_0x8004 names the trade; STR_VAR_1 = the species the
   * NPC asks for, STR_VAR_2 = the one they offer; the answer is the requested species.
   */
  fun inGameTradeInfo(index: Int): Int {
    val trade = de.fiereu.openmmo.server.game.services.InGameTrades.FIRERED.getOrNull(index) ?: return 0
    // The party pick that follows is a trade: its window says so instead of the tutor line.
    tradePickPending = true
    speciesName(trade.requestedDexId)?.let { bufferText(1, it) }
    speciesName(trade.dexId)?.let { bufferText(2, it) }
    return trade.requestedDexId
  }

  /** specialvar GetTradeSpecies: the species in party slot VAR_0x8005, SPECIES_NONE for an egg. */
  fun partySpecies(slot: Int): Int = checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.partySpecies(state, slot)

  /** special DoInGameTradeScene: party slot [slot] goes to the NPC, the NPC's monster takes its place. */
  suspend fun inGameTrade(index: Int, slot: Int): Boolean {
    val trade = de.fiereu.openmmo.server.game.services.InGameTrades.FIRERED.getOrNull(index) ?: return false
    return checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.tradeWithNpc(session, state, slot, trade)
  }

  fun partyContainsSpecies(species: Int): Boolean =
      characterId?.let { characters?.getCharacter(it)?.pokemon?.any { mon -> mon.dexId == species } } ?: false

  /**
   * special ChoosePartyMon: the party in the client's species-list picker (the stand-in for the
   * party menu); the 0-based slot picked, PARTY_SIZE when the window was closed.
   */
  suspend fun choosePartyMember(): Int {
    val party = characterId?.let { characters?.getCharacter(it)?.pokemon } ?: return de.fiereu.openmmo.common.MAX_PARTY_SIZE
    if (party.isEmpty()) return de.fiereu.openmmo.common.MAX_PARTY_SIZE
    holdScriptedFacing()
    val prompt = if (tradePickPending) WHICH_MON_TO_TRADE_TEXT else WHICH_MON_TEXT
    tradePickPending = false
    val pick =
        dialog.chooseFromSpecies(
            session,
            state,
            prompt,
            party.map { if (it.isEgg) 0 else de.fiereu.openmmo.common.clientSpeciesId(it.dexId) })
    return if (pick in 1..party.size) pick - 1 else de.fiereu.openmmo.common.MAX_PARTY_SIZE
  }

  /** special GetMagikarpSizeRecordInfo: STR_VAR_3 = the record's size, STR_VAR_1 = the species. */
  fun bufferMagikarpRecord() {
    bufferText(3, formatMonSize(monSize(MAGIKARP_HEIGHT, getVar(de.fiereu.openmmo.story.generated.kanto.KantoVars.VAR_MAGIKARP_SIZE_RECORD))))
    bufferText(1, speciesName(MAGIKARP) ?: "MAGIKARP")
  }

  /**
   * special CompareMagikarpSize (src/pokemon_size_record.c CompareMonSize): the party [slot] from
   * VAR_RESULT; 0 no pick, 1 not a Magikarp, 2 smaller than the record, 3 a new record (kept in
   * VAR_MAGIKARP_SIZE_RECORD as the size hash, 0 = the game's default), 4 a tie. STR_VAR_3 gets
   * the old size, STR_VAR_2 the new one.
   */
  fun compareMagikarpSize(slot: Int): Int {
    if (slot >= de.fiereu.openmmo.common.MAX_PARTY_SIZE) return 0
    val mon = characterId?.let { characters?.getCharacter(it)?.pokemon?.getOrNull(slot) } ?: return 0
    if (mon.isEgg || mon.dexId != MAGIKARP) return 1
    val hash = monSizeHash(mon)
    val newSize = monSize(MAGIKARP_HEIGHT, hash)
    val oldSize = monSize(MAGIKARP_HEIGHT, getVar(de.fiereu.openmmo.story.generated.kanto.KantoVars.VAR_MAGIKARP_SIZE_RECORD))
    bufferText(3, formatMonSize(oldSize))
    bufferText(2, formatMonSize(newSize))
    return when {
      newSize == oldSize -> 4
      newSize < oldSize -> 2
      else -> {
        setVar(de.fiereu.openmmo.story.generated.kanto.KantoVars.VAR_MAGIKARP_SIZE_RECORD, hash)
        3
      }
    }
  }

  private fun dynamicWarpMapName(): String? {
    val warp = characterId?.let { characters?.getCharacter(it)?.info?.dynamicWarp } ?: return null
    return maps?.getMap(warp.regionId, warp.bankId, warp.mapId)?.sourceName
  }

  /**
   * Shows DS-bank text buttons over the question [line]; returns the 0-based button index.
   *
   * Wire 49 is an UPDATE kind (f/h4.Mq1 = 3, with wires 47 and 48): the client's packet handler
   * (f/A11.IT0) never opens a dialog for it - it hands the packet to the dialog currently open
   * (f/cg.h80 builds the button window f/gl0 from it), and with nothing open it answers 0 to the
   * server at once. So the question goes out first as a plain message, unwaited, and the list
   * follows on the same stream; the buttons appear once the text has finished printing (f/cg.nG1).
   */
  internal suspend fun dsTextListMenu(line: DialogLine, list: de.fiereu.openmmo.server.game.script.interpreter.InterpreterSupport.DsTextList, preselected: Int): Int {
    showMessage(line)
    val pick = dialog.dsTextListMenu(session, state, list.region, list.bank, list.entries, preselected, list.args)
    // The client keeps the text-button list up after its answer; a script that loops back to the
    // same menu (the Trainer School's whiteboard topics) stacked one list per pick and the exit
    // closed only the topmost. The GBA's menu window closes on selection, so close it here.
    dialog.close(session, state)
    return pick
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
  fun getVar(key: String): Int {
    // The engine writes the player's facing into VAR_FACING as a script starts (script.c
    // SetUpFieldMove / event_object_movement.c), so the DIR_* branch ladders of a scene turn the
    // right way; it is never a stored variable.
    if (key.endsWith("/VAR_FACING")) return gbaFacingCode(facingAtStart)
    return characterId?.let { story.getVar(it, key) } ?: 0
  }

  /** The player's facing as the GBA DIR_* code (global.h: 1 south, 2 north, 3 west, 4 east). */
  fun gbaFacingCode(direction: Direction = state.facingDirection): Int =
      when (direction) {
        Direction.UP -> 2
        Direction.LEFT -> 3
        Direction.RIGHT -> 4
        else -> 1
      }

  fun setVar(key: String, value: Int) {
    characterId?.let { story.setVar(it, key, value) }
    // A scene variable that arms or disarms an elevation lock on this map (the badge gates) is
    // re-evaluated at once, so the guard's "go right ahead" lifts the row before the release.
    de.fiereu.openmmo.server.game.services.MapEntryPolish.onVarChanged(this, key)
  }

  /** The hosted map the player stands on, null on DS maps. */
  fun currentMap(): de.fiereu.openmmo.maps.MapDef? {
    val info = characterId?.let { characters?.getCharacter(it)?.info } ?: return null
    return maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
  }

  fun hasTileOverride(x: Int, y: Int): Boolean = state.tileOverrides.containsKey((x shl 16) or (y and 0xFFFF))

  /** Puts a tile back to what the map says (graphic, collision and elevation) for this player. */
  fun restoreMetatile(x: Int, y: Int) {
    val tile = currentMap()?.tileAt(x, y) ?: return
    val upper = tile.collision.toInt() and 0xFF
    setMetatile(x, y, tile.material.toInt() and 0xFFFF, impassable = (upper and 1) != 0, elevation = upper shr 2)
    state.tileOverrides.remove((x shl 16) or (y and 0xFFFF))
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

  /** The client's "You found a {00}!" toast for an item a script handed over. */
  fun announceItem(item: de.fiereu.openmmo.items.ItemDef, quantity: Int = 1) {
    fun stringArg(id: Int, text: String) =
        de.fiereu.openmmo.net.game.packets.ServerMessageArg(
            argId = id.toByte(),
            type = 5,
            hasExtra = false,
            extra = 0,
            longValue = null,
            intValue = null,
            stringValue = text,
            shortValues = null,
        )
    val packet =
        if (quantity == 1) {
          de.fiereu.openmmo.net.game.packets.ServerMessagePacket(
              FOUND_ITEM_STRING, listOf(stringArg(0, item.name)), true, null)
        } else {
          de.fiereu.openmmo.net.game.packets.ServerMessagePacket(
              FOUND_ITEMS_STRING,
              listOf(stringArg(0, quantity.toString()), stringArg(1, item.name)),
              true,
              null)
        }
    send(packet)
  }

  /** A ROM script item constant ("ITEM_TM39") resolved the way the interpreter resolves it. */
  fun itemByScriptConstant(token: String): ItemDef? = checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.itemByScriptConstant(token)

  /** Grant by raw client item id, for items the registry lists under several ids. */
  suspend fun giveItemById(itemId: Int, quantity: Int = 1): Boolean =
      checkNotNull(player) { STORY_PLAYER_UNAVAILABLE }.giveItemById(session, state, itemId, quantity)

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
  /** A hosted map by its decomp source name (OneIsland_Harbor) within [regionId]. */
  fun mapByName(name: String, regionId: Int): de.fiereu.openmmo.maps.MapDef? =
      maps?.getMapsByName(name)?.firstOrNull { it.regionId.toInt() == regionId }

  fun partySize(): Int = characterId?.let { characters?.getCharacter(it)?.pokemon?.size } ?: 0

  /** Party members that can fight: not an egg, HP above zero. */
  fun usablePartyCount(): Int =
      characterId?.let { characters?.getCharacter(it)?.pokemon?.count { mon -> !mon.isEgg && mon.hp > 0 } } ?: 0

  /**
   * PetalburgGymSetDoorMetatiles (field_specials.c): the sliding door of gym room [room] in its
   * open frame - two impassable tiles, the frame and the row below it. The slide animates on
   * the GBA; here the door is simply open.
   */
  fun petalburgGymOpenRoomDoors(room: Int) {
    val doors: List<Pair<Int, Int>> =
        when (room) {
          1 -> listOf(1 to 104, 7 to 104)
          2 -> listOf(1 to 78, 7 to 78)
          3 -> listOf(1 to 91, 7 to 91)
          4 -> listOf(7 to 39)
          5 -> listOf(1 to 52, 7 to 52)
          6 -> listOf(1 to 65)
          7 -> listOf(7 to 13)
          8 -> listOf(1 to 26)
          else -> emptyList()
        }
    for ((x, y) in doors) {
      setMetatile(x, y, PETALBURG_GYM_DOOR_OPEN, impassable = true)
      setMetatile(x, y + 1, PETALBURG_GYM_DOOR_OPEN + METATILE_ROW_WIDTH, impassable = true)
    }
  }

  /** hideplayer / showplayer: the player's own sprite, the client's set_invisible / set_visible. */
  fun hidePlayerSprite() = movement.hideSelf(session, state)

  fun showPlayerSprite() = movement.showSelf(session, state)

  /** setwildbattle + dowildbattle: a scripted wild encounter the script waits out. */
  internal suspend fun wildBattle(dexId: Int, level: Int): BattleResult =
      checkNotNull(battles) { "Battle service is unavailable" }.startScriptedWildBattle(session, dexId, level)

  /** special StartMarowakBattle: the scripted wild battle, but the ghost cannot be caught. */
  internal suspend fun ghostBattle(dexId: Int, level: Int): BattleResult =
      checkNotNull(battles) { "Battle service is unavailable" }.startScriptedWildBattle(session, dexId, level, catchable = false)

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
    val key = movement.npcXyOverrideKey(state, localId) ?: return
    val walked = movement.scriptedNpcPose(state, localId)
    val info = characterId?.let { characters?.getCharacter(it)?.info } ?: return
    val npc = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)?.npcs?.firstOrNull { it.entityIdx == localId } ?: return
    // An npc nobody walked by script may still have been moved by the engine - a Strength
    // boulder pushed onto Victory Road's floor switch - and that placement is the one to keep;
    // writing the template here snapped the boulder back off the switch (2026-09-11).
    val pushed = getVar(key).takeIf { it != 0 }?.let { (it shr 12) to (it and 0xFFF) }
    setNpcXyOverride(localId, walked?.x ?: pushed?.first ?: npc.x, walked?.y ?: pushed?.second ?: npc.y)
  }

  /**
   * special EnterHallOfFame: the client's own Hall of Fame screen (see [HallOfFame]) - record the
   * entry, unlock the encounter counter, show the screen, wait for the player to close it, then
   * wake them up in the bedroom the game started in. The ROM's hall-of-fame and credits never run.
   */
  suspend fun enterHallOfFame() {
    val regionId = state.regionId
    val id = characterId
    // post_battle_event_funcs.c EnterHallOfFame: the party is healed and FLAG_SYS_GAME_CLEAR set.
    // That flag is the client's ninth "badge" (ClientStoryWhitelist.badgeIds, Kanto 2092): with
    // it the level cap table (LG0.cU1) reaches its last row, 100.
    healParty()
    Region.byId(regionId)?.name?.lowercase()?.let { setFlag("$it/FLAG_SYS_GAME_CLEAR") }
    if (id != null) {
      characters?.setStoryFlag(id, de.fiereu.openmmo.server.game.services.HallOfFame.FLAG)
      characters?.flushCharacterAsync(id)
    }
    session.send(de.fiereu.openmmo.server.game.services.HallOfFame.encounterCounterPacket())
    val closed = dialog.expectAcknowledgement(session)
    session.send(de.fiereu.openmmo.server.game.services.HallOfFame.showPacket(regionId))
    closed.await()
    val home = de.fiereu.openmmo.server.game.services.HallOfFame.home(regionId, female = playerGender() != 0) ?: return
    val map = mapByName(home.mapName, regionId) ?: return
    warp(regionId, map.bankId.toInt() and 0xFF, map.mapId.toInt() and 0xFF, home.x, home.y, home.facing)
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
  fun setMetatile(x: Int, y: Int, metatileId: Int, impassable: Boolean, elevation: Int? = null) {
    val id = characterId ?: return
    val info = characters?.getCharacter(id)?.info ?: return
    // The block's upper byte is collision (bits 0-1) plus elevation (bits 2-5, stored +1). The
    // client compares elevations when walking and draws by them, so the existing tile's bits
    // stay and only the collision changes - a bare 0/1 here read as elevation -1: unwalkable and
    // drawn on the wrong layer.
    val key = (x shl 16) or (y and 0xFFFF)
    val map = maps?.getMap(info.positionRegionId, info.positionBankId, info.positionMapId)
    val existing = state.tileOverrides[key] ?: map?.tileAt(x, y)
    // [elevation] (GBA 0..15) replaces the tile's own: the client refuses steps between different
    // non-zero elevations before it even looks for a door, which is how a locked door stays a
    // door on screen and still stops the player (MapEntryPolish).
    // A script's own setmetatile keeps the ROM tile's elevation, never a lock's (MapEntryPolish
    // raises a locked door; the unlock script's setmetatile must put it back on the ground).
    val elevationBits = if (elevation != null) (elevation shl 2) else (((map?.tileAt(x, y) ?: existing)?.collision?.toInt() ?: 0x10) and 0xFC)
    val collision: Byte = (elevationBits or (if (impassable) 1 else 0)).toByte()
    // Behavior is a tileset attribute of the metatile, read from the map's tileset table (floor
    // over a staircase is no warp any more; the Pokemon League's opened exit door IS a warp door,
    // one the map never uses closed - keeping the closed door's behavior left the client fading
    // into a door the server never warped, 2026-09-11). Maps without the table fall back to any
    // tile built from the same metatile, then to the tile's previous behavior.
    val behavior =
        map?.metatileBehavior(metatileId)
            ?: map?.tiles?.firstOrNull { it.material == metatileId.toShort() }?.behavior
            ?: existing?.behavior
            ?: de.fiereu.openmmo.common.enums.TileBehavior.NORMAL
    state.tileOverrides[key] = de.fiereu.openmmo.common.Tile2D(metatileId.toShort(), collision, behavior)
    // The client's own setmetatile: one tile packet with the ROM's metatile id and the GBA upper
    // byte (collision bits + the tile's existing elevation), drawn from the ROM tileset.
    session.send(
        de.fiereu.openmmo.net.game.packets.MapTileSetPacket(
            info.positionRegionId, info.positionBankId, info.positionMapId,
            x.toShort(), y.toShort(), collision.toShort(), metatileId.toShort()))
  }

  /**
   * A ROM string variable (STR_VAR_n) for the next dialogs of this script: a raw text argument.
   * The client addresses ROM placeholders by their text code: {PLAYER} is slot 1, STR_VAR_1..3
   * are slots 2..4 (play-verified 2026-09-10 on the Cinnabar trade NPCs: a name in slot 2 showed
   * as STR_VAR_1 and slot 1 showed nowhere).
   */
  /** fadescreen: the client's render-screen packet, off for FADE_TO_*, on for FADE_FROM_*. */
  fun fadeScreen(toBlank: Boolean) {
    send(de.fiereu.openmmo.net.game.packets.RenderScreenPacket(!toBlank))
    state.screenFaded = toBlank
  }

  /** A ROM string variable (STR_VAR_n) for the next dialogs of this script: a raw text argument. */
  fun bufferText(variable: Int, text: String) {
    val slot = variable + STR_VAR_SLOT_OFFSET
    setMessageArg(slot, de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg(slot = slot.toByte(), kind = 5, text = text))
  }

  fun partyNickname(slot: Int): String? {
    val mon = characterId?.let { characters?.getCharacter(it)?.pokemon?.getOrNull(slot) } ?: return null
    return mon.nickname.ifEmpty { speciesName(mon.dexId) }
  }

  fun moveName(moveId: Int): String? = moves?.get(moveId)?.name

  fun speciesName(dexId: Int): String? = speciesRegistry?.get(dexId)?.name

  /**
   * Pokedex seen and owned counts, the way the client's dex tiers are built: owned is every
   * species held in the party or PC, seen is that plus every species marked seen. [kantoOnly]
   * limits both to the first 151, the ROM's Kanto dex.
   */
  fun dexCounts(kantoOnly: Boolean): Pair<Int, Int> {
    val stored = characterId?.let { characters?.getCharacter(it) } ?: return 0 to 0
    // Same tiers the Pokedex shows: caught = held or ever received, seen = caught + marked.
    val owned = de.fiereu.openmmo.server.game.services.DexProgressService.ownedWireIds(stored)
    val seen = de.fiereu.openmmo.server.game.services.DexProgressService.seenWireIds(stored)
    // One dex for the whole game: trades and the GTL bring species from every region, so the
    // Kanto-only count (VAR_0x8004 = 0) would penalise exactly the MMO play. Every species counts.
    return seen.size to owned.size
  }

  fun leadSpeciesName(): String? =
      characterId?.let { characters?.getCharacter(it)?.pokemon?.firstOrNull() }?.let { speciesName(it.dexId) }

  /** Flash (setflashlevel): lights the map the player stands in until its next load (s2c 0xC1). */
  fun lightMap(level: Int) {
    session.send(de.fiereu.openmmo.net.game.packets.MapLightingPacket.fromRomLevel(level))
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

  /**
   * The S.S. Anne leaving harbour, src/ss_anne.c DoSSAnneDepartureCutscene: after a short hold the
   * boat object slides west until it is off screen, then a beat before the script goes on. The ROM
   * moves the sprite a pixel every five frames for about 240 pixels; here the boat walks the same
   * distance, sixteen tiles, at npc walking speed. The wake and smoke sprites are ROM-drawn effects
   * the client cannot be asked for, and the horn has no server-side sound channel. The script's own
   * removeobject, player walk, scene var and warp follow as written (2026-09-08: the ship never
   * sailed because this special was unsupported, so the whole departure script was skipped).
   */
  suspend fun sailBoatAway(localId: Int) {
    kotlinx.coroutines.delay(50 * 1000L / 60)
    moveNpc(localId, *Array(16) { MovementStep.WALK_LEFT })
    kotlinx.coroutines.delay(40 * 1000L / 60)
  }

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
  /** The cartridge reloads the field's objects after a battle: an npc a script hid before it is gone. */
  fun despawnHiddenNpcs() = movement.despawnHiddenNpcs(session, state)

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
      // This coroutine owns the destination's entry scripts; the arrival's own player requests
      // must not run a second copy. The full arrival (temp flags/vars wiped, placements reset,
      // then ON_TRANSITION/ON_LOAD, the frame script, the landing trigger) runs here.
      val arrival = mapScripts
      if (arrival != null) {
        arrival.onScriptedArrival(this, destination)
        return
      }
      val scripts = entryScripts ?: return
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
    /** Dialog kind wire 19 (f/qM1.WN): the braille sign window, fed by a ROM braille text id. */
    const val BRAILLE = 19
    /** METATILE_PetalburgGym_SlidingDoor_Frame4 and the tileset row width the lower tile sits at. */
    const val PETALBURG_GYM_DOOR_OPEN = 0x21C
    const val METATILE_ROW_WIDTH = 8
    // Client string table (strings_en.xml): "You found a {00}!" / "You found {00} {01}(s)!"
    const val FOUND_ITEM_STRING = 6063
    const val FOUND_ITEMS_STRING = 6066
    const val FEMALE: Byte = 1
    const val STORY_PLAYER_UNAVAILABLE = "Story player service is unavailable"
  }
}

/** Transportation byte while surfing: bit 0x01, client f.ti.J10. */
/** A map name's floor: `SilphCo_11F`, `RocketHideout_B2F` (group 1 = the basement B, group 2 = n). */
private val FLOOR_SUFFIX = Regex("_(B?)(\\d+)F$")

private const val SURF_TRANSPORTATION = 0x01

/** The GBA wallet cap. */
private const val MAX_MONEY = 999_999

/** The tutor's "which one?" ROM line, the party picker's question (MoveTutorService). */
private const val WHICH_MON_TEXT = 16779003

/** Client string "Which one do you want to trade?
You won't get it back." */
private const val WHICH_MON_TO_TRADE_TEXT = 16805088

/** ROM text codes: {PLAYER} = 1, STR_VAR_1 = 2, STR_VAR_2 = 3, STR_VAR_3 = 4 - the slot the client substitutes. */
private const val STR_VAR_SLOT_OFFSET = 1

private const val MAGIKARP = 129
/** Magikarp's Pokedex height in decimeters (GetPokedexHeightWeight). */
private const val MAGIKARP_HEIGHT = 9

/** src/pokemon_size_record.c GetMonSizeHash: low IV nibbles and the personality's two bytes. */
private fun monSizeHash(mon: de.fiereu.openmmo.common.Pokemon): Int {
  val personality = mon.seed and 0xFFFF
  val hp = mon.iVs.hp and 0xF
  val atk = mon.iVs.atk and 0xF
  val def = mon.iVs.def and 0xF
  val spd = mon.iVs.spd and 0xF
  val spAtk = mon.iVs.spAtk and 0xF
  val spDef = mon.iVs.spDef and 0xF
  val hi = (((atk xor def) * hp) xor (personality and 0xFF)) and 0xFF
  val lo = (((spAtk xor spDef) * spd) xor (personality shr 8)) and 0xFF
  return (hi shl 8) + lo
}

/** sBigMonSizeTable: (unk0, unk2, unk4) rows. */
private val BIG_MON_SIZE_TABLE =
    listOf(
        Triple(290, 1, 0), Triple(300, 1, 10), Triple(400, 2, 110), Triple(500, 4, 310),
        Triple(600, 20, 710), Triple(700, 50, 2710), Triple(800, 100, 7710), Triple(900, 150, 17710),
        Triple(1000, 150, 32710), Triple(1100, 100, 47710), Triple(1200, 50, 57710), Triple(1300, 20, 62710),
        Triple(1400, 5, 64710), Triple(1500, 2, 65210), Triple(1600, 1, 65410), Triple(1700, 1, 65510))

/** src/pokemon_size_record.c GetMonSize, in millimetres. */
private fun monSize(height: Int, hash: Int): Int {
  val b = hash and 0xFFFF
  var index = 15
  for (i in 1 until 15) {
    if (b < BIG_MON_SIZE_TABLE[i].third) {
      index = i - 1
      break
    }
  }
  val (unk0, unk2, unk4) = BIG_MON_SIZE_TABLE[index]
  return height * (unk0 + (b - unk4) / unk2) / 10
}

/** FormatMonSizeRecord, the US game's inches (UNITS_IMPERIAL): "35.4". */
private fun formatMonSize(size: Int): String {
  val inches = size * 100 / 254
  return "${inches / 10}.${inches % 10}"
}
