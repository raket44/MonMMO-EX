package de.fiereu.openmmo.server.game.script

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.DynamicWarp
import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ItemDef
import de.fiereu.openmmo.maps.MapManager
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
    dialog.showAndWait(session, state, line.textId, SIGN, -1)
  }

  /** Show [line] from the interacted entity and wait for the player to go on. */
  suspend fun say(line: DialogLine) {
    holdScriptedFacing()
    dialog.showAndWait(session, state, line.textId, NPC, entityId)
  }

  /** Begin a pret `message`; the following wait command owns the client acknowledgement. */
  internal fun showMessage(line: DialogLine) {
    holdScriptedFacing()
    val sign = state.dialogMessageMode == DialogMessageMode.SIGN
    dialog.show(
        session,
        state,
        line.textId,
        if (sign) SIGN else NPC,
        if (sign) -1 else entityId,
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
    return dialog.askYesNo(session, state, line.textId, entityId)
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

  /** checkpartymove: the party slot of the first monster knowing [moveId], PARTY_SIZE (6) if none. */
  fun partyIndexWithMove(moveId: Int): Int {
    val party = characterId?.let { characters?.getCharacter(it)?.pokemon }.orEmpty()
    val index = party.indexOfFirst { mon -> mon.moves.any { it.id.toInt() == moveId } }
    return if (index < 0) de.fiereu.openmmo.common.MAX_PARTY_SIZE else index
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
  ): BattleResult =
      checkNotNull(battles) { "Battle service is unavailable" }
          .startTrainerBattle(session, trainer, defeatTextId)

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
