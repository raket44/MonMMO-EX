package de.fiereu.openmmo.server.game.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuildNamesTest :
    FunSpec({
      test("team names are 2-16 letters, single spaces between words allowed") {
        GuildNames.validName("LostKnights") shouldBe true
        GuildNames.validName("Lost Knights") shouldBe true
        GuildNames.validName("Ab") shouldBe true
        GuildNames.validName("A") shouldBe false
        GuildNames.validName("Knights2") shouldBe false
        GuildNames.validName("Lost  Knights") shouldBe false
        GuildNames.validName(" Knights") shouldBe false
        GuildNames.validName("SeventeenLetters!") shouldBe false
        GuildNames.validName("AbcdefghijklmnopQ") shouldBe false
      }

      test("team tags are 2-4 letters") {
        GuildNames.validTag("LK") shouldBe true
        GuildNames.validTag("KNTS") shouldBe true
        GuildNames.validTag("K") shouldBe false
        GuildNames.validTag("KNTSX") shouldBe false
        GuildNames.validTag("K1") shouldBe false
        GuildNames.validTag("L K") shouldBe false
      }
    })
