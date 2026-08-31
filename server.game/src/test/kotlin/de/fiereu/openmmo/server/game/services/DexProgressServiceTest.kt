package de.fiereu.openmmo.server.game.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.io.DataInputStream
import java.util.BitSet
import java.util.zip.InflaterInputStream

/**
 * The client's decoded reader is the contract: `zlib(s16 byteCount, bytes, u8)`, bitset in
 * `BitSet.valueOf` order, bit index = wire id. This decodes exactly the way `f/Sd1.xx0` does.
 */
class DexProgressServiceTest :
    FunSpec({
      fun decode(blob: ByteArray): Pair<Int, BitSet> =
          DataInputStream(InflaterInputStream(blob.inputStream())).use { data ->
            val bytes = ByteArray(data.readShort().toInt())
            data.readFully(bytes)
            data.readByte()
            bytes.size to BitSet.valueOf(bytes)
          }

      test("a tier round-trips through the client's own decoding") {
        val blob = DexProgressGroups.encode(setOf(1, 495, 716, 1078))
        val (size, bits) = decode(blob)
        // Sized for the whole roster even when the high bits are clear. The roster grew when the
        // expansion import landed, so this tracks the imported highest id, not the retail 1078.
        size shouldBe 201
        (0..1078).filter(bits::get) shouldContainExactly listOf(1, 495, 716, 1078)
      }

      test("the captured retail blob decodes under the same rules, proving the format") {
        // The hex dump this server used to replay at every login: Snivy(495), Tepig(498),
        // Oshawott(501), Patrat(504) and Lillipup(506) seen - one specific early play session.
        val captured =
            "789c637060a00434a8b0320000133900ea"
                .chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        val (size, bits) = decode(captured)
        size shouldBe 64
        (0..520).filter(bits::get) shouldContainExactly listOf(495, 498, 501, 504, 506)
      }
    })
