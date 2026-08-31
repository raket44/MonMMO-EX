package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.Direction
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
    private val mapManager: MapManager,
    private val characterStore: CharacterStore,
    private val scriptRegistry: ScriptRegistry,
    private val scriptRunner: ScriptRunner,
    private val scriptMovement: ScriptMovementService,
) {

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
      log.warn { "Interaction on unloaded map ${state.regionId}:${state.bankId}:${state.mapId}" }
      return
    }
    val regionId = stored.info.positionRegionId.toInt()
    val bankId = stored.info.positionBankId.toInt()
    val mapId = stored.info.positionMapId.toInt()

    for (npc in currentMap.npcs) {
      if (npcService.getNpcEntityId(regionId, bankId, mapId, npc.entityIdx) == npcEntityId) {
        // Every talked-to npc turns to the player FIRST, script or not - vanilla's interaction
        // locks and faceplayers before the script body, and the hand-written ports dropped
        // their faceplayer lines. Facing must never depend on script resolution succeeding.
        scriptMovement.facePlayer(session, npcEntityId, state.facingDirection)
        val script =
            try {
              scriptRegistry.forLabel(npc.script, gbaScriptSource(state.regionId))
            } catch (e: ScriptResolutionException) {
              // An unresolvable script is a content gap, never a session error - letting it
              // propagate used to DISCONNECT the player mid-conversation.
              log.info { "NPC entityIdx=${npc.entityIdx} script=${npc.script}: ${e.message}" }
              null
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
      log.debug { "Tile interaction at ($facingX, $facingY) has no bg event" }
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
