package de.fiereu.openmmo.launcher.content

/**
 * Alternate GBA block grids our mod ships as `world_map_footers/<region>-<id>.bin`, the same
 * mod-loader path (client f/EP) retail's resources.zip uses for its bank-43 maps. The server swaps
 * one in with s2c 0x2D (MapLayoutSwitchPacket) when a story flag calls for it; see the server's
 * LayoutVariants for which flag selects which id.
 *
 * File format (client f/zI1): int width, int height, two ints the client skips, byte borderWidth,
 * byte borderHeight, short skipped, then width*height little-endian map.bin block words, then the
 * border words. Ids 384-423 are retail's; ours start at 450.
 *
 * 0-450: Vermilion City Gym with the electric barrier down - the decomp layout with the ten
 * setmetatile edits of VermilionCity_Gym_EventScript_SetBeamsOff applied (elevation bits kept,
 * collision set where the script says impassable).
 */
object ClientMapFooters {
  private val NAMES = listOf("0-450.bin")

  fun entries(): List<Pair<String, ByteArray>> =
      NAMES.map { name ->
        val bytes =
            ClientMapFooters::class.java.getResourceAsStream("/monmmo/world_map_footers/$name")?.readBytes()
                ?: error("missing footer resource $name")
        name to bytes
      }
}
