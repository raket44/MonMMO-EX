package pirated.net.game.codecs

import pirated.bytecodec.Codec
import pirated.bytecodec.U16LE
import pirated.bytecodec.imap
import pirated.common.Tile2D

val Tile2DCodec: Codec<Tile2D> =
    U16LE.imap(
        decode = { raw ->
          Tile2D(
              material = (raw and 0x3FF).toShort(),
              collision = ((raw shr 10) and 0x3F).toByte(),
          )
        },
        encode = { tile ->
          ((tile.collision.toInt() and 0x3F) shl 10) or (tile.material.toInt() and 0x3FF)
        },
    )
