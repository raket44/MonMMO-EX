package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.NpcSpawnPacket
import de.fiereu.openmmo.net.game.packets.NpcSpawnPacketCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Option bit 4 of the npc spawn (client f/o85): two bytes, the sprite width and height overrides,
 * read right after the flags word when bit 1 is clear.
 */
class NpcSpawnSpriteSizeTest :
    FunSpec({
      fun spawn(size: Pair<Byte, Byte>?) =
          NpcSpawnPacket(
              entityId = 42,
              spriteRegionId = 10,
              graphicsId = 1000,
              unk3 = 0x4002,
              unk4 = 0,
              regionId = 0,
              bankId = 1,
              mapId = 74,
              x = 24,
              y = 9,
              facing = 0,
              unk5 = 2,
              unk6 = 8,
              spriteSize = size,
          )

      // entity 8, sprite region 1, graphics 2, unk3 2, unk4 2, region/bank/map 3, x 2, y 2, unk5 1, facing 1
      val flagsOffset = 8 + 1 + 2 + 2 + 2 + 3 + 2 + 2 + 1 + 1

      test("a sprite size sets bit 4 and follows the flags as two signed bytes") {
        val bytes = NpcSpawnPacketCodec.encodeToBytes(spawn((-1).toByte() to (-1).toByte()))

        bytes.size shouldBe flagsOffset + 2 + 2
        bytes[flagsOffset] shouldBe 0x0C.toByte()
        bytes[flagsOffset + 1] shouldBe 0x00.toByte()
        bytes[flagsOffset + 2] shouldBe (-1).toByte()
        bytes[flagsOffset + 3] shouldBe (-1).toByte()
        NpcSpawnPacketCodec.decodeBytes(bytes) shouldBe spawn((-1).toByte() to (-1).toByte())
      }

      test("an npc without a sprite size is unchanged: flags 8 and nothing after") {
        val bytes = NpcSpawnPacketCodec.encodeToBytes(spawn(null))

        bytes.size shouldBe flagsOffset + 2
        bytes[flagsOffset] shouldBe 0x08.toByte()
        NpcSpawnPacketCodec.decodeBytes(bytes) shouldBe spawn(null)
      }
    })
