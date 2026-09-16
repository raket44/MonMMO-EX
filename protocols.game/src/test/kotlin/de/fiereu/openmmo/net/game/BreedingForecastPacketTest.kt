package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.BreedingForecastPacket
import de.fiereu.openmmo.net.game.packets.BreedingForecastPacketCodec
import de.fiereu.openmmo.net.game.packets.BreedingStatContribution
import de.fiereu.openmmo.net.game.packets.BreedingStatEntry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private fun row(value: Byte) =
    BreedingStatEntry(
        guaranteed = false,
        braceItemId = 0,
        contributions = listOf(BreedingStatContribution(value, 50.0f, 2541)),
    )

private fun forecast() =
    BreedingForecastPacket(
        parentA = 0x1122334455667788L,
        parentB = 0x0102030405060708L,
        hasPreview = true,
        species = 172,
        form = 0,
        statEntries = List(6) { row((it + 10).toByte()) },
        possibleNatures = emptyList(),
        valueIds = emptyList(),
        valueSources = emptyList(),
        gender = 0,
        nature = 0,
        shiny = true,
        cost = 5000,
        secondaryCost = 5000,
    )

/**
 * The r32645 reader is f/bx8.Rl0. It reads, in order: two uids, a U8 preview flag, then either the
 * preview body or (flag clear) an int plus a U8-counted list. The preview body ends with the two
 * cost ints AND one more U8 presence flag for an optional trailing block. Leaving that last byte
 * off made the client throw "Buffer underflow for Ij0 [R] 0x73 bx8:315", drop the packet, and draw
 * no forecast at all in the breed window (2026-09-16).
 */
class BreedingForecastPacketTest :
    FunSpec({
      test("the preview body ends with the client's trailing presence flag") {
        val bytes = BreedingForecastPacketCodec.encodeToBytes(forecast())
        bytes.last() shouldBe 0.toByte()
        // uids 16 + flag 1 + species 2 + form 1 + statCount 1 + six rows (1+2+1+9 each) + natures 1
        // + values 1 + gender 1 + nature 2 + chooser 1 + cost 4 + cost 4 + trailing 1.
        bytes.size shouldBe 16 + 1 + 2 + 1 + 1 + 6 * 13 + 1 + 1 + 1 + 2 + 1 + 4 + 4 + 1
        BreedingForecastPacketCodec.decodeBytes(bytes) shouldBe forecast()
      }

      test("a rejected pair still carries the no-preview int and list count") {
        val empty =
            BreedingForecastPacket(
                parentA = 7,
                parentB = 9,
                hasPreview = false,
                species = 0,
                form = 0,
                statEntries = emptyList(),
                possibleNatures = emptyList(),
                valueIds = emptyList(),
                valueSources = emptyList(),
                gender = 0,
                nature = 0,
                shiny = false,
                cost = 0,
                secondaryCost = 0,
            )
        val bytes = BreedingForecastPacketCodec.encodeToBytes(empty)
        // uids 16 + flag 1 + bx8.yO int 4 + bx8.VJ1 count 1.
        bytes.size shouldBe 22
        BreedingForecastPacketCodec.decodeBytes(bytes) shouldBe empty
      }
    })
