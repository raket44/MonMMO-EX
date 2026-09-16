package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapDef
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.EntityInteractPacket
import de.fiereu.openmmo.net.game.packets.TileInteractPacket
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRegistry
import de.fiereu.openmmo.server.game.script.ScriptResolutionException
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.script.gbaScriptSource
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class InteractionService
@Inject
constructor(
    private val npcService: NpcService,
    private val ferryPlacements: FerryPlacements,
    private val ferryTravel: FerryTravel,
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
    private val scriptRegistry: ScriptRegistry,
    private val scriptRunner: ScriptRunner,
    private val scriptMovement: ScriptMovementService,
    private val ocarinas: OcarinaService,
    private val banners: FieldMoveBanners,
    private val raidPlacement: CrystalOnixRaidPlacement,
    private val raid: CrystalOnixRaidService,
) {

  /**
   * The party menu's field-move use: the client sends c2s 0x14 with the monster's entity id and
   * the MOVE id as the action type (Flash arrived as actionType 148 on a party member,
   * 2026-09-08, and went unhandled - only the ocarina route reached [useFieldMove]). The
   * monster must be in the party and know the move; the rest is what pressing A would do.
   */
  fun onFieldMoveRequest(event: PacketEvent<de.fiereu.openmmo.net.game.packets.EntityActionRequestPacket>) {
    val session = event.session
    val state = session.attributes[PLAYER_STATE] ?: return
    val stored = currentCharacter(state) ?: return
    val moveId = event.packet.actionType.toInt()
    if (moveId !in FieldMoves.MOVE_IDS) {
      log.info { "[FieldMove] char=${state.characterId} action $moveId on ${event.packet.targetEntityId} is not a field move" }
      return
    }
    val mon = stored.pokemon.firstOrNull { it.id == event.packet.targetEntityId }
    if (mon == null || mon.moves.none { it.id.toInt() == moveId }) {
      log.info { "[FieldMove] char=${state.characterId} move $moveId: monster ${event.packet.targetEntityId} not in party or does not know it" }
      return
    }
    log.info { "[FieldMove] char=${state.characterId} uses move $moveId from monster ${mon.id}" }
    useFieldMove(session, state, moveId)
  }

  /**
   * A DS-map npc: the ROM's own event table gives its script index, the client's bank/map pair is
   * the ROM map header id, and the DS corpus binds the two as `NDS_<header>_<index>`.
   */
  private fun onNdsEntityInteract(session: SessionContext, state: PlayerState, npcEntityId: Long) {
    val regionId = state.regionId
    val bankId = state.bankId
    val mapId = state.mapId
    val npc = npcService.ndsNpcForEntity(regionId, bankId, mapId, npcEntityId) ?: return
    scriptMovement.facePlayer(session, npcEntityId, state.facingDirection)
    // Ids from 2000 up are Platinum's shared script chunks (common, signposts, trainers...).
    val label =
        if (npc.script >= 2000) "NDS_CHUNK_${npc.script}" else "NDS_${(mapId shl 8) or bankId}_${npc.script}"
    val script =
        try {
          scriptRegistry.forLabel(label, gbaScriptSource(regionId))
        } catch (e: ScriptResolutionException) {
          log.info { "DS npc idx=${npc.index} $label: ${e.message}" }
          null
        }
    if (script != null) {
      runScript(session, state, script, npcEntityId)
    } else {
      log.info { "DS npc idx=${npc.index} script=${npc.script} ($label) has no wired dialog" }
    }
  }

  /** The player pressed the action button on a specific entity, that is an npc. */
  fun onEntityInteract(event: PacketEvent<EntityInteractPacket>) {
    val session = event.session
    val state = session.attributes[PLAYER_STATE] ?: return
    if (state.blocksNewScript) {
      log.debug { "Interaction ignored: a dialog or script is already active" }
      return
    }
    val stored = currentCharacter(state) ?: return
    val npcEntityId = event.packet.entityId

    val currentMap =
        mapManager.getMap(
            stored.info.positionRegionId,
            stored.info.positionBankId,
            stored.info.positionMapId,
        )
    if (currentMap == null) {
      if (state.regionId in 2..4) {
        onNdsEntityInteract(session, state, npcEntityId)
        return
      }
      log.warn { "Interaction on unloaded map ${state.regionId}:${state.bankId}:${state.mapId}" }
      return
    }
    val regionId = stored.info.positionRegionId.toInt()
    val bankId = stored.info.positionBankId.toInt()
    val mapId = stored.info.positionMapId.toInt()

    ferryPlacements.at(regionId, bankId, mapId)?.let { p ->
      if (npcService.getNpcEntityId(regionId, bankId, mapId, FerryPlacements.LOCAL_ID) == npcEntityId) {
        scriptMovement.facePlayer(session, npcEntityId, state.facingDirection)
        runScript(session, state, ferryTravel.script(stored, p), npcEntityId)
        return
      }
    }
    if (raidPlacement.isHere(regionId, bankId, mapId) &&
        npcService.getNpcEntityId(regionId, bankId, mapId, CrystalOnixRaidPlacement.LOCAL_ID) == npcEntityId) {
      runScript(session, state, raid.script(stored), npcEntityId)
      return
    }

    for (npc in currentMap.npcs) {
      if (npcService.getNpcEntityId(regionId, bankId, mapId, npc.entityIdx) == npcEntityId) {
        val script =
            try {
              scriptRegistry.forLabel(npc.script, gbaScriptSource(state.regionId))
            } catch (e: ScriptResolutionException) {
              // An unresolvable script is a content gap, never a session error - letting it
              // propagate used to DISCONNECT the player mid-conversation.
              log.info { "NPC entityIdx=${npc.entityIdx} script=${npc.script}: ${e.message}" }
              null
            }
        // Only a script's own faceplayer turns an object: cut trees, dolls and item balls never
        // turn on the cartridge (a blanket turn here had them face the player, 2026-09-12). The
        // hand-written Kotlin ports dropped their faceplayer lines, so those still get the turn.
        if (script !is de.fiereu.openmmo.server.game.script.interpreter.InterpretedScript) {
          scriptMovement.facePlayer(session, npcEntityId, state.facingDirection)
        }
        if (script != null) {
          runScript(session, state, script, npcEntityId)
        } else {
          log.info { "NPC entityIdx=${npc.entityIdx} script=${npc.script} has no wired dialog" }
        }
        return
      }
    }
    log.info { "Entity interaction for entity $npcEntityId not found on current map" }
  }

  /** The player pressed the action button on the tile they face, a sign or a piece of furniture. */
  fun onTileInteract(event: PacketEvent<TileInteractPacket>) {
    val session = event.session
    val state = session.attributes[PLAYER_STATE] ?: return
    if (state.blocksNewScript) return
    val stored = currentCharacter(state) ?: return

    val currentMap =
        mapManager.getMap(
            stored.info.positionRegionId,
            stored.info.positionBankId,
            stored.info.positionMapId,
        ) ?: return

    val facingX =
        when (state.facingDirection) {
          Direction.RIGHT -> stored.info.positionX.toInt() + 1
          Direction.LEFT -> stored.info.positionX.toInt() - 1
          else -> stored.info.positionX.toInt()
        }
    val facingY =
        when (state.facingDirection) {
          Direction.UP -> stored.info.positionY.toInt() - 1
          Direction.DOWN -> stored.info.positionY.toInt() + 1
          else -> stored.info.positionY.toInt()
        }
    val bgEvent =
        currentMap.bgEvents.find {
          it.x == facingX && it.y == facingY && facingDirOk(it.facingDir, state.facingDirection)
        }
    if (bgEvent == null) {
      // Dive: A while surfing over deep water goes under; A on the sea floor surfaces.
      if (divePrompt(session, state, stored, currentMap)) return
      // Pokecenter PCs are engine tiles (MB_PC), not bg events - the behavior is the trigger.
      val behavior = currentMap.tileAt(facingX, facingY)?.behavior
      if (behavior == de.fiereu.openmmo.common.enums.TileBehavior.PC) {
        openPcStorage(session, state, stored)
        return
      }
      // Facing water on foot is the ROM's Surf prompt (field script, not a map event). Without
      // the badge, the move or its ocarina the cartridge stays silent; retail says a line.
      if (behavior?.isSurfable == true && !state.surfing) {
        if (!FieldMoves.canUse(stored, state.regionId, FieldMoves.SURF)) {
          session.send(notice("The water is dyed a deep blue\u2026"))
          return
        }
        runFieldScript(session, state, "EventScript_UseSurf", entityId = -1)
        return
      }
      // Surfing and facing north into a waterfall: the same prompt the step gives.
      if (state.surfing && behavior == de.fiereu.openmmo.common.enums.TileBehavior.WATERFALL && state.facingDirection == Direction.UP) {
        waterfallPrompt(session, state, stored)
        return
      }
      log.debug { "Tile interaction at ($facingX, $facingY) has no bg event" }
      return
    }
    // House PCs are bg events whose ROM script drives the GBA storage engine; this server's
    // storage is the client's own UI, so any PC script routes there instead.
    if (bgEvent.script.endsWith("_EventScript_PC")) {
      openPcStorage(session, state, stored)
      return
    }
    val script =
        try {
          scriptRegistry.forLabel(bgEvent.script, gbaScriptSource(state.regionId))
        } catch (e: ScriptResolutionException) {
          log.info { "Bg event at ($facingX, $facingY) script=${bgEvent.script}: ${e.message}" }
          null
        }
    if (script != null) {
      runScript(session, state, script, entityId = -1)
    } else {
      log.info { "Bg event at ($facingX, $facingY) script=${bgEvent.script} has no wired dialog" }
    }
  }

  /**
   * The vanilla PC beat: "{PLAYER} booted up the PC.", the client's own built-in PC menu ("{01}'s
   * PC" / Global Trade Link / Mail / Cancel) over the ROM's which-PC question, then the storage
   * window for the PC choice. GTL and Mail notice until their windows can be opened server-side.
   */
  private fun openPcStorage(session: SessionContext, state: PlayerState, stored: StoredCharacter) {
    val kanto = state.regionId != HOENN_REGION_ID
    val boot = lineOf(if (kanto) KANTO_BOOTED_PC else HOENN_BOOTED_PC)
    val question = lineOf(if (kanto) KANTO_WHICH_PC else HOENN_WHICH_PC)
    val opened = lineOf(if (kanto) KANTO_STORAGE_OPENED else HOENN_STORAGE_OPENED)
    runScript(
        session,
        state,
        Script { ctx ->
          ctx.sign(boot)
          val choice = ctx.builtinMenu(question, PC_MENU_SET)
          log.info { "PC menu choice=$choice (${stored.pcStorage.size} boxed pokemon)" }
          when (choice) {
            1 -> {
              ctx.sign(opened)
              // Fresh contents first, then the toggle that shows the window.
              de.fiereu.openmmo.net.game.packets.containerPackets(de.fiereu.openmmo.common.enums.PokemonContainer.PC, stored.pcStorage)
                  .forEach { p -> session.send(p) }
              session.send(de.fiereu.openmmo.net.game.packets.battle.PcTogglePacket(shown = true))
              stored.info.id.let { ocarinas.refill(session, it) }
            }
            2 ->
                session.send(notice("The Global Trade Link is opened from the Trade menu for now."))
            3 -> session.send(notice("Mail is not implemented yet."))
            else -> Unit
          }
        },
        entityId = -1,
    )
  }

  private fun lineOf(id: Int) =
      object : de.fiereu.openmmo.common.dialog.DialogLine {
        override val textId = id
      }

  /** A shared ROM field script for this region's game; unresolvable ones log and do nothing. */
  fun runFieldScript(session: SessionContext, state: PlayerState, label: String, entityId: Long) {
    val script =
        try {
          scriptRegistry.forLabel(label, gbaScriptSource(state.regionId))
        } catch (e: ScriptResolutionException) {
          log.info { "Field script $label unavailable: ${e.message}" }
          null
        } ?: return
    runScript(session, state, script, entityId)
  }

  /**
   * The Dotted Hole door (field_specials.c CutMoveOpenDottedHoleDoor): the Cut banner, the flag
   * the map's ON_LOAD reads on later visits, and the open door in place of the locked one - a
   * script's setmetatile keeps the ROM tile's elevation, so the lock lifts and the door is a warp.
   */
  private fun openDottedHole(session: SessionContext, state: PlayerState, stored: StoredCharacter) {
    if (MapEntryPolish.DOTTED_HOLE_CUT_FLAG in stored.storyFlags) {
      session.send(notice("The door is already open."))
      return
    }
    banners.send(session, stored, state.regionId, FieldMoves.CUT)
    runScript(
        session,
        state,
        Script { ctx ->
          ctx.setFlag(MapEntryPolish.DOTTED_HOLE_CUT_FLAG)
          ctx.setMetatile(MapEntryPolish.DOTTED_HOLE_DOOR_X, MapEntryPolish.DOTTED_HOLE_DOOR_Y, MapEntryPolish.DOTTED_HOLE_DOOR_OPEN_METATILE, impassable = false)
        },
        entityId = -1,
    )
    log.info { "Dotted Hole opened by Cut for char=${stored.info.id}" }
  }

  /** The waterfall prompt, or the engine's "can't" line when the badge is missing. */
  fun waterfallPrompt(session: SessionContext, state: PlayerState, stored: StoredCharacter) {
    val hoenn = Region.byId(state.regionId) == Region.HOENN
    val label =
        if (FieldMoves.badgeHeld(stored, state.regionId, FieldMoves.WATERFALL)) {
          if (hoenn) "EventScript_UseWaterfall" else "EventScript_Waterfall"
        } else if (hoenn) "EventScript_CannotUseWaterfall" else "EventScript_CantUseWaterfall"
    runFieldScript(session, state, label, entityId = -1)
  }

  /**
   * A field move used from the bag's ocarina (the party-menu route is not decoded yet): exactly
   * what pressing A would do for that move, so the ROM script does the asking and the checks.
   */
  fun useFieldMove(session: SessionContext, state: PlayerState, moveId: Int) {
    if (state.blocksNewScript) return
    val stored = currentCharacter(state) ?: return
    val map =
        mapManager.getMap(stored.info.positionRegionId, stored.info.positionBankId, stored.info.positionMapId)
            ?: return
    val region = stored.info.positionRegionId.toInt()
    val bank = stored.info.positionBankId.toInt()
    val mapId = stored.info.positionMapId.toInt()
    val hoenn = Region.byId(state.regionId) == Region.HOENN
    val fx = stored.info.positionX.toInt() + state.facingDirection.dx
    val fy = stored.info.positionY.toInt() + state.facingDirection.dy
    val facing = map.tileAt(fx, fy)?.behavior
    // fldeff_cut.c CutMoveRuinValleyCheck: Cut facing the Dotted Hole's braille door opens it.
    if (moveId == FieldMoves.CUT && map.sourceName == MapEntryPolish.RUIN_VALLEY_MAP && fx == MapEntryPolish.DOTTED_HOLE_DOOR_X && fy == MapEntryPolish.DOTTED_HOLE_DOOR_Y) {
      openDottedHole(session, state, stored)
      return
    }
    when (moveId) {
      FieldMoves.CUT, FieldMoves.ROCK_SMASH, FieldMoves.STRENGTH -> {
        val wanted =
            when (moveId) {
              FieldMoves.CUT -> "EventScript_CutTree"
              FieldMoves.ROCK_SMASH -> "EventScript_RockSmash"
              else -> "EventScript_StrengthBoulder"
            }
        val target =
            map.npcs
                .map { npcService.effectiveNpc(region, bank, mapId, it, stored.storyFlags, stored.storyVars) }
                .firstOrNull { it.x == fx && it.y == fy && it.script == wanted && (it.hideFlag.isEmpty() || it.hideFlag !in stored.storyFlags) }
        if (target == null) {
          session.send(notice("There is nothing here to use that on."))
          return
        }
        val entityId = npcService.getNpcEntityId(region, bank, mapId, target.entityIdx) ?: -1L
        runFieldScript(session, state, target.script, entityId)
      }
      FieldMoves.SURF ->
          if (!state.surfing && facing?.isSurfable == true) runFieldScript(session, state, "EventScript_UseSurf", -1)
          else session.send(notice("There is no water to surf on here."))
      FieldMoves.WATERFALL ->
          if (state.surfing && state.facingDirection == Direction.UP && facing == de.fiereu.openmmo.common.enums.TileBehavior.WATERFALL) waterfallPrompt(session, state, stored)
          else session.send(notice("Face a waterfall while surfing to use that."))
      FieldMoves.DIVE -> if (!divePrompt(session, state, stored, map)) session.send(notice("There is nowhere to dive here."))
      FieldMoves.FLASH -> {
        // fldeff_flash.c: only in a dark cave, once per stay (the flag), and the flag is what
        // keeps the next floor lit and what going outdoors clears (MapScriptService).
        val flashFlag = FieldMoves.flashActiveFlag(state.regionId)
        when {
          map.lighting == de.fiereu.openmmo.common.enums.Lighting.REGULAR -> session.send(notice("It is not dark here."))
          flashFlag in stored.storyFlags -> session.send(notice("Flash is already lighting the way."))
          else -> {
            state.characterId?.let { characterStore.setStoryFlag(it, flashFlag) }
            // FireRed's EventScript_FldEffFlash has no dofieldeffect - on the cartridge the field
            // effect itself shows the monster - so the banner is sent here; Emerald's UseFlash
            // reaches it through dofieldeffect.
            if (!hoenn) banners.send(session, stored, state.regionId, FieldMoves.FLASH)
            runFieldScript(session, state, if (hoenn) "EventScript_UseFlash" else "EventScript_FldEffFlash", -1)
          }
        }
      }
      FieldMoves.FLY -> session.send(notice("Fly is not available on this server yet."))
      else -> log.info { "Field move $moveId has no overworld use" }
    }
  }

  /** Dive down from deep water or surface from the sea floor; false when neither applies. */
  private fun divePrompt(session: SessionContext, state: PlayerState, stored: StoredCharacter, map: MapDef): Boolean {
    if (!state.surfing) return false
    val standing = map.tileAt(stored.info.positionX.toInt(), stored.info.positionY.toInt())?.behavior
    val hoenn = Region.byId(state.regionId) == Region.HOENN
    val label =
        when {
          state.underwater -> if (hoenn) "EventScript_UseDiveUnderwater" else "EventScript_TrySurface"
          standing == de.fiereu.openmmo.common.enums.TileBehavior.DEEP_WATER -> if (hoenn) "EventScript_UseDive" else "EventScript_DeepWater"
          else -> return false
        }
    runFieldScript(session, state, label, entityId = -1)
    return true
  }

  private fun currentCharacter(state: PlayerState): StoredCharacter? {
    val charId = state.characterId ?: return null
    return characterStore.getCharacter(charId)
  }

  private fun facingDirOk(eventDir: String, facing: Direction): Boolean =
      eventDir == "BG_EVENT_PLAYER_FACING_ANY" ||
          (eventDir == "BG_EVENT_PLAYER_FACING_NORTH" && facing == Direction.UP) ||
          (eventDir == "BG_EVENT_PLAYER_FACING_SOUTH" && facing == Direction.DOWN) ||
          (eventDir == "BG_EVENT_PLAYER_FACING_WEST" && facing == Direction.LEFT) ||
          (eventDir == "BG_EVENT_PLAYER_FACING_EAST" && facing == Direction.RIGHT)

  private fun runScript(
      session: SessionContext,
      state: PlayerState,
      script: Script,
      entityId: Long,
  ) = scriptRunner.run(session, state, script, entityId)
}

private const val KANTO_STORAGE_OPENED = 1724606
private const val HOENN_STORAGE_OPENED = 271001251
private const val HOENN_REGION_ID = 1

/** The client's built-in menu registry set 3: "{01}'s PC" / Global Trade Link / Mail / Cancel. */
private const val PC_MENU_SET = 3
private const val KANTO_BOOTED_PC = 1724533
private const val KANTO_WHICH_PC = 1724554
private const val HOENN_BOOTED_PC = 271001178
private const val HOENN_WHICH_PC = 271001199
