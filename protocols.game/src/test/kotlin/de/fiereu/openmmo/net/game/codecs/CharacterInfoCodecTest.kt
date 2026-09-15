package de.fiereu.openmmo.net.game.codecs

import de.fiereu.openmmo.common.CharacterInfo
import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

class CharacterInfoCodecTest :
    FunSpec({
      test("encodes the captured position block alignment and NDS global coordinates") {
        val timestamp = LocalDateTime.of(2024, 1, 2, 3, 4, 5)
        val info =
            CharacterInfo(
                id = 123,
                name = "Uno",
                userId = 2,
                rivalSex = 1,
                lastLogin = timestamp,
                createdAt = timestamp,
                money = 30000,
                permissions = 0,
                sweetScentPp = 8,
                remainingSafariSteps = 0,
                remainingSafariBalls = 0,
                pcExtraSlots = 0,
                battleBoxExtraSlots = 0,
                templateAmount = 0,
                positionRegionId = 2,
                positionBankId = 133.toByte(),
                positionMapId = 1,
                positionX = 782,
                positionY = 749,
                repelLeft = 0,
                repelItemId = 0,
                lureLeft = 0,
                lureItemId = 0,
            )

        val bytes = CharacterInfoCodecShort.encodeToBytes(info)
        val tail = bytes.copyOfRange(bytes.size - 68, bytes.size)

        tail.copyOfRange(45, 56) shouldBe
            byteArrayOf(
                0x02,
                0xff.toByte(),
                0x02,
                0x02,
                0x85.toByte(),
                0x01,
                0x00,
                0x0e,
                0x03,
                0xed.toByte(),
                0x02,
            )
        CharacterInfoCodecShort.decodeBytes(bytes) shouldBe info
      }

      // f/ih6.lQ reads the int after the creation date into eu6.zz, which the trainer card shows as
      // zz / 3600 hours ("Time played: {00} hour(s)"); it was always 0 before 2026-09-14.
      test("time played rides in seconds right after the creation date") {
        val timestamp = LocalDateTime.of(2024, 1, 2, 3, 4, 5)
        val info =
            CharacterInfo(
                id = 123, name = "Uno", userId = 2, rivalSex = 1, lastLogin = timestamp, createdAt = timestamp,
                playTimeSeconds = 7_200, money = 30000, permissions = 0, remainingSafariSteps = 0,
                remainingSafariBalls = 0, pcExtraSlots = 0, battleBoxExtraSlots = 0, templateAmount = 0,
                positionRegionId = 2, positionBankId = 133.toByte(), positionMapId = 1, positionX = 782,
                positionY = 749, repelLeft = 0, repelItemId = 0, lureLeft = 0, lureItemId = 0)

        val bytes = CharacterInfoCodecShort.encodeToBytes(info)

        // id 8, "Uno" + terminator 8, empty prefix 2, user 4, rival sex 1, last login 4, created 4.
        bytes.copyOfRange(31, 35) shouldBe byteArrayOf(0x20, 0x1c, 0x00, 0x00)
        CharacterInfoCodecShort.decodeBytes(bytes).playTimeSeconds shouldBe 7_200
      }

      test("permission bits above the low byte are not sent") {
        val timestamp = LocalDateTime.of(2024, 1, 2, 3, 4, 5)
        val info =
            CharacterInfo(
                id = 123,
                name = "Uno",
                userId = 2,
                rivalSex = 1,
                lastLogin = timestamp,
                createdAt = timestamp,
                money = 30000,
                permissions = CharacterPermissions.DEVELOPER or 8,
                sweetScentPp = 8,
                remainingSafariSteps = 0,
                remainingSafariBalls = 0,
                pcExtraSlots = 0,
                battleBoxExtraSlots = 0,
                templateAmount = 0,
                positionRegionId = 2,
                positionBankId = 133.toByte(),
                positionMapId = 1,
                positionX = 782,
                positionY = 749,
                repelLeft = 0,
                repelItemId = 0,
                lureLeft = 0,
                lureItemId = 0,
            )

        val decoded =
            CharacterInfoCodecShort.decodeBytes(CharacterInfoCodecShort.encodeToBytes(info))

        decoded.permissions shouldBe 0
        decoded.sweetScentPp shouldBe 8
      }
    })
