package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.net.game.packets.PlayerVariableEntry
import de.fiereu.openmmo.net.game.packets.StoryFlagUpdatePacket
import de.fiereu.openmmo.story.generated.hoenn.HoennFlags
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class StoryClientStateTest :
    FunSpec({
      test("maps persisted GBA flags to the captured region and numeric id") {
        StoryClientState.flags(
            regionId = 1,
            flags = setOf(HoennFlags.FLAG_VISITED_LITTLEROOT_TOWN),
        ) shouldBe listOf(StoryFlagUpdatePacket(1, 0x086f, enabled = true))
      }

      test("held items are enabled, unheld cosmetics zeroed, all sorted by id") {
        val entries = StoryClientState.itemUnlocks(mapOf(360 to 1, 21 to 3))

        entries shouldBe entries.sortedBy { it.key }
        entries.single { it.key.toInt() == 21 } shouldBe PlayerVariableEntry(21, 1)
        entries.single { it.key.toInt() == 360 } shouldBe PlayerVariableEntry(360, 1)
        // The twelve always-selectable bicycle colors are enabled without a bag item.
        (4816..4827).forEach { id ->
          entries.single { it.key.toInt() == id }.value shouldBe 1.toByte()
        }
        // Item-backed cosmetics not in the bag are sent as an explicit 0, overriding the
        // retail pak's default-unlocked leftovers.
        entries.any { it.value == 0.toByte() } shouldBe true
      }

      test("does not leak another region's story flags") {
        StoryClientState.flags(
            regionId = 0,
            flags = setOf(HoennFlags.FLAG_VISITED_LITTLEROOT_TOWN),
        ) shouldBe emptyList()
      }
    })
