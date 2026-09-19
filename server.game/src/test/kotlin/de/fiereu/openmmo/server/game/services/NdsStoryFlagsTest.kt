package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.story.generated.johto.JohtoFlags
import de.fiereu.openmmo.story.generated.sinnoh.SinnohFlags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class NdsStoryFlagsTest :
    FunSpec({
      test("a HeartGold npc's numeric hide flag maps to the named story key") {
        // FLAG_HIDE_NEW_BARK_RIVAL = 0x190 in pokeheartgold include/constants/flags.h.
        NdsStoryFlags.key(4, 0x190) shouldBe JohtoFlags.FLAG_HIDE_NEW_BARK_RIVAL
        NdsStoryFlags.isHidden(4, 0x190, setOf(JohtoFlags.FLAG_HIDE_NEW_BARK_RIVAL)) shouldBe true
        NdsStoryFlags.isHidden(4, 0x190, emptySet()) shouldBe false
        NdsStoryFlags.isHidden(4, 0, setOf(JohtoFlags.FLAG_HIDE_NEW_BARK_RIVAL)) shouldBe false
      }

      test("White's flags are numeric keys") {
        NdsStoryFlags.key(2, 657) shouldBe "unova/FLAG_657"
        NdsStoryFlags.isHidden(2, 657, setOf("unova/FLAG_657")) shouldBe true
      }

      test("the DS games' new-game init scripts fill the initial flag sets") {
        JohtoFlags.initiallySet.size shouldBe 143
        JohtoFlags.initiallySet shouldContain JohtoFlags.FLAG_HIDE_NEW_BARK_FRIEND
        SinnohFlags.initiallySet.size shouldBe 112
        SinnohFlags.initiallySet shouldContain SinnohFlags.FLAG_HIDE_TWINLEAF_TOWN_PLAYER_HOUSE_2F_RIVAL
      }
    })
