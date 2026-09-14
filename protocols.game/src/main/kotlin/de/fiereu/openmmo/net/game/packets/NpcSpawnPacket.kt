package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*
import de.fiereu.openmmo.net.game.codecs.DefaultSkinSetCodec
import de.fiereu.openmmo.net.game.codecs.SkinSet

/**
 * A player-model look for an npc (client f/p01 trailer bit 8192 -> f/iw1(gender, skin set)):
 * the npc is drawn as a trainer wearing [skins] instead of a ROM sprite, which is how the
 * client's own custom npcs (the ferry captain in his pirate outfit) are dressed.
 */
data class NpcLook(val gender: Byte, val skins: SkinSet)

data class NpcSpawnPacket(
    val entityId: Long,
    /** Which region's sprite table [graphicsId] is looked up in. */
    val spriteRegionId: Int,
    val graphicsId: Int,
    val unk3: Int,
    val unk4: Int,
    val regionId: Int,
    val bankId: Int,
    val mapId: Int,
    val x: Int,
    val y: Int,
    val facing: Int,
    val unk5: Int,
    /**
     * f/p01's option bits: 1 (short+2 bytes), 2, 4 (2 bytes), 8, 16, 32, 64, 128, 256 (short),
     * 512 (f/Prn record), 1024 (list), 2048, 4096 (float), 8192 ([look]). Only 8 and 8192 are used.
     */
    val unk6: Int,
    val look: NpcLook? = null,
    /**
     * Option bit 4: the sprite's width and height overrides (f/o85.Uu0/Hp0 -> f/dw2.vg/Br0, read by
     * dw2.QX()/oy()). Without the bit both stay 0, which a ROM sprite is fine with but a region-10
     * file sprite takes as a zero size and is drawn a tile right and two down of its entity
     * (Crystal Onix, 2026-09-14). -1 means "no override": the client's own sprite table decides.
     */
    val spriteSize: Pair<Byte, Byte>? = null,
)

object NpcSpawnPacketCodec : PacketCodec<NpcSpawnPacket>() {
  override fun CodecScope<NpcSpawnPacket>.body(): NpcSpawnPacket {
    val entityId = field(S64LE) { it.entityId }
    val spriteRegionId = field(U8) { it.spriteRegionId }
    val graphicsId = field(U16LE) { it.graphicsId }
    val unk3 = field(U16LE) { it.unk3 }
    val unk4 = field(U16LE) { it.unk4 }
    val regionId = field(U8) { it.regionId }
    val bankId = field(U8) { it.bankId }
    val mapId = field(U8) { it.mapId }
    val x = field(U16LE) { it.x }
    val y = field(U16LE) { it.y }
    val unk5 = field(U8) { it.unk5 }
    val facing = field(U8) { it.facing }
    val flags =
        field(U16LE) {
          (it.unk6 and (LOOK_BIT or SIZE_BIT).inv()) or
              (if (it.look != null) LOOK_BIT else 0) or
              (if (it.spriteSize != null) SIZE_BIT else 0)
        }
    // Option payloads follow the flags in bit order; bit 1 (a short and two bytes) is never sent,
    // so the size bytes come first.
    val spriteSize =
        if (flags and SIZE_BIT != 0) {
          field(S8) { it.spriteSize!!.first } to field(S8) { it.spriteSize!!.second }
        } else null
    val look =
        if (flags and LOOK_BIT != 0) {
          val gender = field(S8) { it.look!!.gender }
          val skins = field(DefaultSkinSetCodec) { it.look!!.skins }
          NpcLook(gender, skins)
        } else null
    return NpcSpawnPacket(
        entityId,
        spriteRegionId,
        graphicsId,
        unk3,
        unk4,
        regionId,
        bankId,
        mapId,
        x,
        y,
        facing,
        unk5,
        flags and (LOOK_BIT or SIZE_BIT).inv(),
        look,
        spriteSize)
  }

  private const val LOOK_BIT = 8192
  private const val SIZE_BIT = 4
}
