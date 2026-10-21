package pirated.server.game.battle

import pirated.common.Pokemon
import pirated.net.game.packets.battle.BattleEntityDeltaPacket
import pirated.net.game.packets.battle.Experience
import pirated.pokemon.SpeciesDef

/** Tells the client about a monster the player just obtained. */
fun acquiredMonsterDelta(mon: Pokemon, species: SpeciesDef): BattleEntityDeltaPacket {
  val stats = StatCalculator.computeAll(species, mon)
  // The real server also sends a happiness here, which we do not model.
  return BattleEntityDeltaPacket(
      entityId = mon.id,
      experience = Experience(mon.level, mon.xp),
      statValues = stats.asWireList(),
      currentHp = mon.hp,
      faintFlag = 0,
  )
}
