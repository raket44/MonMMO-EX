package pirated.net.game

import pirated.common.Skin
import pirated.common.enums.SkinSlot
import pirated.common.test.decodeBytes
import pirated.common.test.encodeToBytes
import pirated.net.game.packets.PlayerHead
import pirated.net.game.packets.guild.GuildMemberEntry
import pirated.net.game.packets.guild.SyncGuildMembersPacket
import pirated.net.game.packets.guild.SyncGuildMembersPacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuildRosterPacketTest :
    FunSpec({
      // r32645 f/vo: replace byte, count byte, then per member entity id, rank, join date, the player
      // summary (f/ih6.bK1) and a trailing online byte (qd6.XD0).
      test("a roster row is entity, rank, join date, player summary, then the online flag last") {
        val head = PlayerHead(tone = 1, skins = mapOf(SkinSlot.HAIR to Skin(SkinSlot.HAIR, 3u, 0u)))
        val pkt =
            SyncGuildMembersPacket(
                replace = true,
                members = listOf(GuildMemberEntry(9L, 2, 0x11223344, "", 0x55667788, head, online = true)))
        val bytes = SyncGuildMembersPacketCodec.encodeToBytes(pkt)

        // replace 1, count 1, entity 8, rank 1, joined 4, name terminator 2 = 17.
        bytes.copyOfRange(17, bytes.size) shouldBe
            byteArrayOf(
                0x00, 0x88.toByte(), 0x77, 0x66, 0x55, // skipped byte, last seen
                0x01, 0x00, // tone, variants
                0x03, 0x00, // HAIR 3
                0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
                0x01, // online
            )
        SyncGuildMembersPacketCodec.decodeBytes(bytes) shouldBe pkt
      }
    })
