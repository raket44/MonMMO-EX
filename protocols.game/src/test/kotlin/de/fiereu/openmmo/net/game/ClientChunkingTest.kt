package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacketCodec
import de.fiereu.openmmo.net.game.packets.battle.BattleSidePartyPacketCodec
import de.fiereu.openmmo.net.game.packets.battle.ItemStack
import de.fiereu.openmmo.net.game.packets.battle.itemStacksPackets
import de.fiereu.openmmo.net.game.packets.containerPackets
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private fun boxed(slot: Int) =
    Pokemon(
        id = slot.toLong() shl 16 or 0xC000L,
        ownerId = 0x9000L,
        container = PokemonContainer.PC,
        containerSlot = slot.toShort(),
        dexId = 1 + slot % 150,
        seed = slot,
        ot = "Argeno",
        nickname = "Meowscarada",
        level = 50,
        hp = 100,
        xp = 100000,
        eVs = EVs(),
        iVs = IVs(),
        moves = List(4) { PokemonMove(0, 0) },
        isShiny = false,
        hasHiddenAbility = false,
        isAlpha = false,
        isSecret = false,
        isFatefulEncounter = false,
        isRaidEncounter = false,
        caughtAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0),
    )

// The r32645 client inflates each compressed packet into a fixed 30000-byte buffer (f/od4.oP);
// a 217-monster PC blew through it and garbled the bag at login (2026-09-16).
class ClientChunkingTest :
    FunSpec({
      test("a big PC goes out as several packets that each fit the client's inflate buffer") {
        val pc = List(217) { boxed(it) }
        val whole = PokemonContainerPacketCodec.encodeToBytes(
            de.fiereu.openmmo.net.game.packets.PokemonContainerPacket(PokemonContainer.PC, true, false, pc))
        whole.size shouldBeGreaterThan CLIENT_INFLATE_BUFFER

        val packets = containerPackets(PokemonContainer.PC, pc)
        packets.size shouldBeGreaterThan 1
        packets.first().hasChange.shouldBeTrue()
        packets.drop(1).forEach { it.hasChange.shouldBeFalse() }
        packets.forEach { PokemonContainerPacketCodec.encodeToBytes(it).size shouldBeLessThanOrEqual CLIENT_CHUNK_BUDGET + 8 }
        packets.flatMap { it.pokemon } shouldBe pc
      }

      test("a small container and an empty one stay single packets with the change flag") {
        val one = containerPackets(PokemonContainer.PARTY, listOf(boxed(0)))
        one.size shouldBe 1
        one.single().hasChange.shouldBeTrue()
        val none = containerPackets(PokemonContainer.DAYCARE, emptyList())
        none.single().hasChange.shouldBeTrue()
        none.single().pokemon.shouldBeEmpty()
      }

      test("a huge bag is split, the first packet replacing and the rest adding") {
        val stacks = List(3000) { ItemStack(objectId = (it.toLong() shl 16) or 0x5000L, itemId = (it % 900).toShort(), quantity = 1) }
        val packets = itemStacksPackets(stacks)
        packets.size shouldBeGreaterThan 1
        packets.first().replace.shouldBeTrue()
        packets.drop(1).forEach { it.replace.shouldBeFalse() }
        packets.forEach { BattleSidePartyPacketCodec.encodeToBytes(it).size shouldBeLessThanOrEqual CLIENT_CHUNK_BUDGET + 8 }
        packets.sumOf { it.pokemon.size } shouldBe stacks.size
        itemStacksPackets(emptyList()).single().replace.shouldBeTrue()
      }
    })
