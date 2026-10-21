package pirated.net.game.packets.gtl

import pirated.bytecodec.Codec
import pirated.bytecodec.ReadBuffer
import pirated.bytecodec.S16LE
import pirated.bytecodec.U16LE
import pirated.bytecodec.WriteBuffer

sealed interface GtlSearchFilter {
  val kind: GtlFilterKind
}

internal val GtlSearchFilterCodec: Codec<GtlSearchFilter> =
    object : Codec<GtlSearchFilter> {
      override fun read(buf: ReadBuffer): GtlSearchFilter {
        val kind = GtlFilterKindCodec.read(buf)
        if (kind == GtlFilterKind.SPECIES) {
          return GtlSpeciesFilter(List(U16LE.read(buf)) { S16LE.read(buf) })
        }
        val payload = ByteArray(kind.payloadSize)
        buf.readBytes(payload)
        return GtlRawFilter(kind, payload)
      }

      override fun write(buf: WriteBuffer, value: GtlSearchFilter) {
        GtlFilterKindCodec.write(buf, value.kind)
        when (value) {
          is GtlSpeciesFilter -> {
            U16LE.write(buf, value.speciesIds.size)
            value.speciesIds.forEach { S16LE.write(buf, it) }
          }
          is GtlRawFilter -> buf.writeBytes(value.payload)
        }
      }
    }
