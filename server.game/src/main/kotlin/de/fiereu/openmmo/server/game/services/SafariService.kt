package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.SafariStatus
import de.fiereu.openmmo.server.game.battle.BattleInstance
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.script.interpreter.InterpretedScripts
import de.fiereu.openmmo.server.game.session.PlayerState
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.story.generated.kanto.KantoFlags
import de.fiereu.openmmo.story.generated.kanto.KantoVars
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The Safari Game, on the client's own safari protocol: the character carries remaining steps
 * (s16) and Safari Balls (s8) in its info block, the local-character delta (mask bit 2) updates
 * both live (client f/cd1 -> f/ZZ.Iq1 / FJ1, then refreshes the battle panel f/h60.vI0), the client
 * counts its own steps down per tile (f/ln1.x00), and a battle opened in mode 1 (f/my.cN, the byte
 * after the format in the 0x30 header) shows Ball / Bait / Rock (f/h60 "battle-ball", "battle-rock",
 * "battle-bait", action kinds 5, 6, 7 of f/sV) and "N Safari Balls" from the ball count.
 *
 * The rules are FireRed's src/safari_zone.c: EnterSafariMode sets FLAG_SYS_SAFARI_MODE, 30 balls and
 * 600 steps; every step counts down and 0 runs SafariZone_EventScript_TimesUp; ExitSafariMode
 * clears everything; a battle that ends with no balls left walks the player out (CB2_EndSafariBattle).
 */
@Singleton
class SafariService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val story: StoryService,
    private val mapManager: MapManager,
    private val scriptRunner: Provider<ScriptRunner>,
    private val items: de.fiereu.openmmo.items.ItemRegistry,
) {
  private val safariBallItemId: Int by lazy { items.idOf(de.fiereu.openmmo.items.generated.Items.SAFARI_BALL) }
  fun isActive(charId: Long): Boolean = story.isFlagSet(charId, KantoFlags.FLAG_SYS_SAFARI_MODE)

  /** special EnterSafariMode: the flag, 30 Safari Balls into the bag, the counters told. */
  suspend fun enter(session: SessionContext, charId: Long) {
    story.setFlag(charId, KantoFlags.FLAG_SYS_SAFARI_MODE)
    characterStore.addItem(charId, safariBallItemId, BALLS)
    characterStore.getCharacter(charId)?.let { session.send(storyItemStacksPacket(it.items)) }
    set(session, charId, STEPS, BALLS)
    log.info { "[safari] char=$charId enters: $BALLS balls, $STEPS steps" }
  }

  /** special ExitSafariMode: the flag off, the Safari Balls taken back, the counters zeroed. */
  suspend fun exit(session: SessionContext, charId: Long) {
    story.clearFlag(charId, KantoFlags.FLAG_SYS_SAFARI_MODE)
    val left = characterStore.getCharacter(charId)?.items?.get(safariBallItemId) ?: 0
    if (left > 0) {
      characterStore.addItem(charId, safariBallItemId, -left)
      characterStore.getCharacter(charId)?.let { session.send(storyItemStacksPacket(it.items)) }
    }
    set(session, charId, 0, 0)
    log.info { "[safari] char=$charId leaves" }
  }

  fun ballsLeft(charId: Long): Int = characterStore.getCharacter(charId)?.info?.remainingSafariBalls?.toInt() ?: 0

  /** A Safari Ball thrown (the bag item leaves in BattleService.throwBall): the counter follows. */
  fun consumeBall(session: SessionContext, charId: Long): Int {
    val info = characterStore.getCharacter(charId)?.info ?: return 0
    val balls = (info.remainingSafariBalls - 1).coerceAtLeast(0)
    set(session, charId, info.remainingSafariSteps.toInt(), balls)
    return balls
  }

  /**
   * One step inside the Safari Game (SafariZoneTakeStep): the counter falls, and at 0 the PA calls
   * time. True when the times-up script took the player, so the step's encounter roll is skipped.
   */
  fun onStep(session: SessionContext, state: PlayerState): Boolean {
    val charId = state.characterId ?: return false
    if (!isActive(charId)) return false
    val info = characterStore.getCharacter(charId)?.info ?: return false
    if (info.remainingSafariSteps <= 0) return false
    val steps = info.remainingSafariSteps - 1
    characterStore.updateCharacter(info.copy(remainingSafariSteps = steps.toShort()))
    characterStore.flushCharacterAsync(charId)
    if (steps % 100 == 0) log.info { "[safari] char=$charId has $steps steps left" }
    if (steps > 0) return false
    val timesUp = InterpretedScripts.byBareLabel[TIMES_UP]
    if (timesUp == null) {
      log.error { "[safari] char=$charId is out of steps but $TIMES_UP is not registered" }
      return false
    }
    log.info { "[safari] char= is out of steps" }
    scriptRunner.get().run(session, state, timesUp, entityId = -1)
    return true
  }

  /**
   * CB2_EndSafariBattle: with balls left the field simply returns; out of balls, a catch shows the
   * PA's out-of-balls line and leaves through the gate, and a miss walks straight out to the gate
   * (SafariZone_EventScript_OutOfBallsMidBattle: scene 3, ExitSafariMode, the gate's warp).
   */
  fun afterBattle(session: SessionContext, state: PlayerState, battle: BattleInstance, result: BattleResult) {
    val safari = battle.safari ?: return
    if (safari.balls > 0 || result == BattleResult.DISCONNECTED) return
    val charId = battle.charId
    if (result == BattleResult.CAUGHT) {
      InterpretedScripts.byBareLabel[OUT_OF_BALLS]?.let { scriptRunner.get().run(session, state, it, entityId = -1) }
      return
    }
    val gate = mapManager.getMapsByName(GATE_MAP).firstOrNull { it.regionId.toInt() == 0 }
    if (gate == null) {
      log.warn { "[safari] gate map $GATE_MAP unknown; char=$charId stays put" }
      return
    }
    scriptRunner
        .get()
        .run(
            session,
            state,
            Script { ctx ->
              ctx.setVar(KantoVars.VAR_MAP_SCENE_FUCHSIA_CITY_SAFARI_ZONE_ENTRANCE, GATE_SCENE_OUT_OF_BALLS)
              ctx.exitSafari()
              ctx.warp(0, gate.bankId.toInt(), gate.mapId.toInt(), GATE_X, GATE_Y, de.fiereu.openmmo.common.enums.Direction.DOWN)
            },
            entityId = -1)
  }

  private fun set(session: SessionContext, charId: Long, steps: Int, balls: Int) {
    val info = characterStore.getCharacter(charId)?.info ?: return
    characterStore.updateCharacter(info.copy(remainingSafariSteps = steps.toShort(), remainingSafariBalls = balls.toByte()))
    characterStore.flushCharacterAsync(charId)
    session.send(LocalCharacterDeltaPacket(safari = SafariStatus(steps.toShort(), balls.toByte())))
  }

  companion object {
    const val STEPS = 600
    const val BALLS = 30
    /** The 0x30 header's mode byte for a Safari battle (client f/my.cN). */
    const val BATTLE_MODE: Byte = 1
    private const val TIMES_UP = "SafariZone_EventScript_TimesUp"
    private const val OUT_OF_BALLS = "SafariZone_EventScript_OutOfBalls"
    private const val GATE_MAP = "FuchsiaCity_SafariZone_Entrance"
    /** SafariZone_EventScript_Exit: `warp MAP_FUCHSIA_CITY_SAFARI_ZONE_ENTRANCE, 4, 1`. */
    private const val GATE_X = 4
    private const val GATE_Y = 1
    private const val GATE_SCENE_OUT_OF_BALLS = 3
  }
}
