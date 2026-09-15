package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.EntityAppearanceInfo
import de.fiereu.openmmo.net.game.packets.EntityGroupMember
import de.fiereu.openmmo.net.game.packets.EntityGroupSnapshotPacket
import de.fiereu.openmmo.net.game.packets.EntityGroupSnapshotPacketCodec
import de.fiereu.openmmo.net.game.packets.FriendListEntry
import de.fiereu.openmmo.net.game.packets.FriendListPacket
import de.fiereu.openmmo.net.game.packets.FriendListPacketCodec
import de.fiereu.openmmo.net.game.packets.GroupListFrame
import de.fiereu.openmmo.net.game.packets.GroupListFrameSet
import de.fiereu.openmmo.net.game.packets.PARTY_LIST
import de.fiereu.openmmo.net.game.packets.PlayerHead
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class FriendListPacketTest :
    FunSpec({
      val head =
          PlayerHead(
              tone = 3,
              skins =
                  mapOf(
                      SkinSlot.HAIR to Skin(SkinSlot.HAIR, 12u, 5u),
                      SkinSlot.HAT to Skin(SkinSlot.HAT, 700u, 2u, variant = 1u),
                  ))

      test("roundtrips entries with mixed online state, variable-length names and heads") {
        val pkt =
            FriendListPacket(
                mode = 0,
                entries =
                    listOf(
                        FriendListEntry(0x0102030405069000L, 1_600_000_000, false, "Alpha", 1_700_000_000, head),
                        FriendListEntry(0xA7L, 1_650_000_000, true, "BravoBravoBravo", 1_710_000_000),
                    ),
            )
        val decoded = FriendListPacketCodec.decodeBytes(FriendListPacketCodec.encodeToBytes(pkt))
        decoded shouldBe pkt
        decoded.entries.map { it.name } shouldBe listOf("Alpha", "BravoBravoBravo")
        decoded.entries.map { it.online } shouldBe listOf(false, true)
      }

      // f/ih6.bK1 after the name: skipped byte, last seen int, tone, 2-bit variants (HAT at bits 6-7),
      // then HAIR, FACIAL_HAIR, EYES, HAT as type | colour << 10, 0xFFFF for an empty slot.
      test("the head block is the client's summary layout: tone, variants, hair, facial hair, eyes, hat") {
        val pkt = FriendListPacket(0, listOf(FriendListEntry(1L, 0, false, "", 0x01020304, head)))
        val bytes = FriendListPacketCodec.encodeToBytes(pkt)

        // mode 1, count 1, player 8, since 4, online 1, empty name terminator 2 = 17.
        bytes.copyOfRange(17, bytes.size) shouldBe
            byteArrayOf(
                0x00, 0x04, 0x03, 0x02, 0x01, // skipped byte, last seen
                0x03, 0x40, // tone, HAT variant 1 at bits 6-7
                0x0c, 0x14, // HAIR 12 | 5 << 10 = 0x140C
                0xff.toByte(), 0xff.toByte(), // FACIAL_HAIR empty
                0xff.toByte(), 0xff.toByte(), // EYES empty
                0xbc.toByte(), 0x0a, // HAT 700 | 2 << 10 = 0x0ABC
            )
      }

      // r32645 f/ih6.OW: count, container byte, then entity id, species, form, gender, rarity bits.
      test("a link member carries its head and party icons as the client reads them") {
        val icon = GroupListFrame(monsterId = 0x9C00L, species = 25, form = 0, gender = 1, rarity = 1)
        val pkt =
            EntityGroupSnapshotPacket(
                true, 7L, listOf(EntityGroupMember(7L, EntityAppearanceInfo("Red", head), GroupListFrameSet(PARTY_LIST, listOf(icon)))))
        val bytes = EntityGroupSnapshotPacketCodec.encodeToBytes(pkt)

        bytes.copyOfRange(bytes.size - 16, bytes.size) shouldBe
            byteArrayOf(0x01, 0x01, 0x00, 0x9c.toByte(), 0, 0, 0, 0, 0, 0, 0x19, 0x00, 0x00, 0x01, 0x01, 0x00)
        EntityGroupSnapshotPacketCodec.decodeBytes(bytes) shouldBe pkt
      }
    })
