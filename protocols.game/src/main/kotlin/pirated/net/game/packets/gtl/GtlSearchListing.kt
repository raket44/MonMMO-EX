package pirated.net.game.packets.gtl

import pirated.bytecodec.Codec
import pirated.bytecodec.ReadBuffer
import pirated.bytecodec.S16LE
import pirated.bytecodec.S32LE
import pirated.bytecodec.S64LE
import pirated.bytecodec.S8
import pirated.bytecodec.U8
import pirated.bytecodec.WriteBuffer
import pirated.net.game.codecs.PokemonCodec

sealed interface GtlSearchListing {
  val listingId: Long
  val price: Int
  val listedAt: Int
  val expiresAt: Int
  val quantity: Short
}

// Every row names its own kind, so an own listings page can mix monsters and items.
// TODO(gtl): decode an own listings row. The old scraper reads three more bytes after the
//  quantity and then an item id even for a monster row, but no captured page has one.
internal fun gtlSearchListingCodec(pageKind: GtlListKind): Codec<GtlSearchListing> =
    object : Codec<GtlSearchListing> {
      override fun read(buf: ReadBuffer): GtlSearchListing {
        require(pageKind != GtlListKind.OWN_LISTINGS) { "own listing rows are not decoded yet" }
        val listingId = S64LE.read(buf)
        val rowKind = GtlListKindCodec.read(buf)
        val price = S32LE.read(buf)
        val listedAt = S32LE.read(buf)
        val expiresAt = S32LE.read(buf)
        val quantity = S16LE.read(buf)
        if (rowKind == GtlListKind.ITEM) {
          val itemId = S16LE.read(buf)
          val itemState = S8.read(buf)
          return GtlItemListing(listingId, price, listedAt, expiresAt, quantity, itemId, itemState)
        }
        val hasRecord = U8.read(buf) == 1
        val pokemon = if (hasRecord) PokemonCodec.read(buf) else null
        val stats = if (hasRecord) List(6) { S16LE.read(buf) } else emptyList()
        return GtlPokemonListing(listingId, price, listedAt, expiresAt, quantity, pokemon, stats)
      }

      override fun write(buf: WriteBuffer, value: GtlSearchListing) {
        S64LE.write(buf, value.listingId)
        GtlListKindCodec.write(
            buf,
            if (value is GtlItemListing) GtlListKind.ITEM else GtlListKind.POKEMON,
        )
        S32LE.write(buf, value.price)
        S32LE.write(buf, value.listedAt)
        S32LE.write(buf, value.expiresAt)
        S16LE.write(buf, value.quantity)
        when (value) {
          is GtlItemListing -> {
            S16LE.write(buf, value.itemId)
            S8.write(buf, value.itemState)
          }
          is GtlPokemonListing -> {
            U8.write(buf, if (value.pokemon == null) 0 else 1)
            value.pokemon?.let { PokemonCodec.write(buf, it) }
            value.stats.forEach { S16LE.write(buf, it) }
          }
        }
      }
    }
