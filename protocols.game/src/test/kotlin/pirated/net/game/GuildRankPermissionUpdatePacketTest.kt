package pirated.net.game

import pirated.common.enums.GuildPermission
import pirated.common.enums.GuildRank
import pirated.common.test.decodeBytes
import pirated.common.test.encodeToBytes
import pirated.net.game.packets.guild.GuildRankPermissionUpdatePacket
import pirated.net.game.packets.guild.GuildRankPermissionUpdatePacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class GuildRankPermissionUpdatePacketTest :
    FunSpec({
      test("editable ranks are ordered high-to-low: Executive is the first short") {
        val pkt =
            GuildRankPermissionUpdatePacket(
                mapOf(GuildRank.EXECUTIVE to setOf(GuildPermission.INVITE)))
        GuildRankPermissionUpdatePacketCodec.encodeToBytes(pkt) shouldBe
            byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0)
      }

      test("roundtrips the default permission matrix") {
        val invMsg = setOf(GuildPermission.INVITE, GuildPermission.TEAM_MESSAGES)
        val pkt =
            GuildRankPermissionUpdatePacket(
                mapOf(
                    GuildRank.GRUNT to emptySet(),
                    GuildRank.MEMBER to emptySet(),
                    GuildRank.OFFICER to invMsg,
                    GuildRank.COMMANDER to invMsg,
                    GuildRank.EXECUTIVE to invMsg,
                ),
            )
        GuildRankPermissionUpdatePacketCodec.decodeBytes(
            GuildRankPermissionUpdatePacketCodec.encodeToBytes(pkt)) shouldBe pkt
      }
    })
