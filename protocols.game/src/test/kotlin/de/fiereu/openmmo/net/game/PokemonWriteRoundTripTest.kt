package de.fiereu.openmmo.net.game

import de.fiereu.bytecodec.GrowableWriteBuffer
import de.fiereu.bytecodec.U8
import de.fiereu.openmmo.common.test.fixture
import de.fiereu.openmmo.common.test.fixtureBuffer
import de.fiereu.openmmo.net.game.codecs.CharacterInfoCodecShort
import de.fiereu.openmmo.net.game.codecs.DefaultSkinSetCodec
import de.fiereu.openmmo.net.game.codecs.PokemonCodec
import de.fiereu.openmmo.net.game.codecs.SkinSetCodecNoLeading
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The character list is the packet the client fails to parse when a party is present, so what the
 * server produces for it is worth pinning against the one real capture we have.
 *
 * Only the width is asserted. Several fields the codecs write as fixed zero bytes hold real values
 * in the capture (byte 49 of the capture is 25, the server writes 0), and the monster record's
 * "unknown" trailer differs between the two captures we hold, so an exact match is not achievable
 * until those fields are identified. Width is still worth guarding: a size change means a field was
 * added or dropped, which is what actually desynchronises a reader.
 */
class PokemonWriteRoundTripTest :
    FunSpec({
      test("re-writing the captured character list keeps its exact width") {
        val original = fixture("game/s2c/02/character_list_32710.bin")
        val buf = fixtureBuffer("game/s2c/02/character_list_32710.bin")
        val out = GrowableWriteBuffer()

        val count = U8.read(buf)
        U8.write(out, count)
        repeat(count) {
          CharacterInfoCodecShort.write(out, CharacterInfoCodecShort.read(buf))
          DefaultSkinSetCodec.write(out, DefaultSkinSetCodec.read(buf))
          SkinSetCodecNoLeading.write(out, SkinSetCodecNoLeading.read(buf))
          out.writeByte(buf.readByte())
          val party = U8.read(buf)
          U8.write(out, party)
          repeat(party) { PokemonCodec.write(out, PokemonCodec.read(buf)) }
        }
        buf.remaining() shouldBe 0
        out.toByteArray().size shouldBe original.size
      }

      test("re-writing a captured monster record keeps its exact width") {
        val original = fixture("game/s2c/14/monster_record_32710.bin")
        val out = GrowableWriteBuffer()
        PokemonCodec.write(
            out, PokemonCodec.read(fixtureBuffer("game/s2c/14/monster_record_32710.bin")))
        out.toByteArray().size shouldBe original.size
      }
    })
