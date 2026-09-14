package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.server.game.world.interest.InterestManager
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TransformEmitterTest :
    FunSpec({
      val emitter = BattlePacketEmitter(InterestManager())

      test("no stages pack to all sixes") {
        emitter.packStages(emptyMap()) shouldBe 0x66666666
      }

      // r59 orders hp, attack, defense, speed, sp. attack, sp. defense, accuracy, evasion.
      test("attack sits in the second nibble and speed in the fourth") {
        emitter.packStages(mapOf(BattleStat.ATTACK to 2, BattleStat.SPEED to -1)) shouldBe 0x66665686
      }
    })
