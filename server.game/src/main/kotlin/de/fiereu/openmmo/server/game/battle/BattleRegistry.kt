package de.fiereu.openmmo.server.game.battle

import de.fiereu.network.SessionContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/** The running battles, one per character. */
@Singleton
class BattleRegistry @Inject constructor() {

  private val byChar = ConcurrentHashMap<Long, BattleInstance>()
  private val ids = AtomicLong(1)

  fun create(
      charId: Long,
      session: SessionContext,
      party: List<BattleMonState>,
      opponent: List<BattleMonState>,
      rng: BattleRng,
      rules: BattleRules = BattleRules(),
      format: de.fiereu.openmmo.net.game.packets.battle.BattleFormat = de.fiereu.openmmo.net.game.packets.battle.BattleFormat.SINGLES,
  ): BattleInstance {
    val battle =
        BattleInstance(
            ids.getAndIncrement(),
            charId,
            session,
            party,
            opponent,
            rng,
            rules.catchable,
            rules.escapable,
            rules.trainer,
            rules.defeatTextId,
            rules.whiteoutOnDefeat,
            rules.trainerRegion,
            format,
        )
    byChar[charId] = battle
    return battle
  }

  fun byChar(charId: Long): BattleInstance? = byChar[charId]

  fun remove(charId: Long): BattleInstance? = byChar.remove(charId)
}
