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
 * Footer 0-450 (Vermilion Gym, beams down) was the first use and is retired: the ROM script's
 * setmetatile renders directly once the tile packet's fields were in the right order.
 */
object ClientMapFooters {
  // Empty since the Vermilion Gym beams run on the ROM's own setmetatile (s2c 0x22); add a
  // "<region>-<id>.bin" here for a variant that has no ROM script.
  private val NAMES = emptyList<String>()

  fun entries(): List<Pair<String, ByteArray>> =
      NAMES.map { name ->
        val bytes =
            ClientMapFooters::class.java.getResourceAsStream("/monmmo/world_map_footers/$name")?.readBytes()
                ?: error("missing footer resource $name")
        name to bytes
      }
}
