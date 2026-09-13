package de.fiereu.openmmo.net.game

import de.fiereu.openmmo.common.enums.Platform
import de.fiereu.openmmo.common.test.decodeBytes
import de.fiereu.openmmo.common.test.encodeToBytes
import de.fiereu.openmmo.net.game.packets.JoinPacketCodec
import de.fiereu.openmmo.net.game.packets.NewAuthData
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream

/**
 * The join packet ends differently on the two clients.
 *
 * Desktop 31914 closes with a fixed 32-byte block after `unk1`. Android r32645 stops at `unk1`,
 * whose 32 bytes are the SHA-256 of the app's own signing certificate - the anti-tamper report the
 * client sends at join. Reading that trailing block unconditionally overran the frame by exactly
 * its own width, so the server dropped every Android join and the phone sat on "Loading, please
 * wait" with nothing in the login log, because the failure is after login.
 *
 * Field offsets below were taken from a real dropped frame (1149 bytes, logged by
 * ProtocolHandler): clientRevision and installationRevision both read 32645, the ROM list decodes
 * as BPRE/BPEE/IPGE/IRBO and platform reads 0x04.
 */
private const val ANDROID_REVISION = 32645

/** Stand-in for the signing-certificate hash the real client puts here. */
private val CERT_HASH = ByteArray(32) { (it + 1).toByte() }

private val TRAILING_BLOCK = ByteArray(32) { 0x5A }

private class Frame {
  private val out = ByteArrayOutputStream()

  fun u8(value: Int) = apply { out.write(value and 0xFF) }

  fun le16(value: Int) = apply {
    u8(value)
    u8(value shr 8)
  }

  fun le32(value: Int) = apply {
    le16(value)
    le16(value shr 16)
  }

  fun bytes(value: ByteArray) = apply { out.write(value) }

  fun utf16(value: String) = apply {
    value.forEach { le16(it.code) }
    le16(0)
  }

  fun build(): ByteArray = out.toByteArray()
}

/** A join body (no opcode byte), optionally carrying the desktop-only trailing block. */
private fun joinFrame(withTrailingBlock: Boolean): ByteArray {
  val frame =
      Frame()
          .u8(0x00) // auth tag: new session
          .le32(1) // userId
          .u8(4)
          .bytes(byteArrayOf(1, 2, 3, 4)) // session key
          .bytes(ByteArray(6) { 0xFF.toByte() }) // mac
          .le32(ANDROID_REVISION)
          .le32(ANDROID_REVISION)
          .u8(0) // current chat language
          .le16(96) // chat languages
          .le16(0) // matchmaking languages
          .u8(0x1F) // rom mask
          .u8(1)
          .utf16("BPRE")
          .u8(0)
          .u8(0) // one rom
          .u8(1)
          .u8(0)
          .utf16("Qualcomm") // one client-info entry
          .u8(0x04) // platform: Android
          .u8(0) // arch
          .u8(0) // bitness
          .u8(32)
          .bytes(CERT_HASH) // unk1
  if (withTrailingBlock) frame.bytes(TRAILING_BLOCK)
  return frame.build()
}

class JoinPacketTest :
    FunSpec({
      test("an Android join ends after unk1 and decodes with no trailing block") {
        val bytes = joinFrame(withTrailingBlock = false)
        val decoded = JoinPacketCodec.decodeBytes(bytes)

        decoded.unk2 shouldBe null
        decoded.clientRevision shouldBe ANDROID_REVISION
        decoded.installationRevision shouldBe ANDROID_REVISION
        decoded.platform shouldBe Platform.ANDROID
        decoded.unk1.toList() shouldBe CERT_HASH.toList()
        (decoded.authData as NewAuthData).userId shouldBe 1
        decoded.roms.single().code shouldBe "BPRE"

        // Re-encoding must not invent the field the client never sent.
        JoinPacketCodec.encodeToBytes(decoded).toList() shouldBe bytes.toList()
      }

      test("a desktop join still carries the trailing block and round trips") {
        val bytes = joinFrame(withTrailingBlock = true)
        val decoded = JoinPacketCodec.decodeBytes(bytes)

        decoded.unk2?.toList() shouldBe TRAILING_BLOCK.toList()
        decoded.unk1.toList() shouldBe CERT_HASH.toList()

        JoinPacketCodec.encodeToBytes(decoded).toList() shouldBe bytes.toList()
      }
    })
