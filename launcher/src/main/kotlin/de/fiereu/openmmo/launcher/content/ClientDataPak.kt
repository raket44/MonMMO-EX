package de.fiereu.openmmo.launcher.content

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * The client's data.pak, modelled rather than patched.
 *
 * Every section this file knows how to change is fully decoded, edited as a list of records, and
 * re-encoded. A section we cannot decode is carried through untouched, byte for byte. That is the
 * whole point: a misunderstood record layout cannot reach the client, because [SectionCodec.verify]
 * re-encodes the stock section first and refuses to continue unless the bytes come back identical.
 *
 * The alternative - appending raw bytes to a section and hoping the layout was right - is how a
 * wrong move-record header once corrupted the file and stopped the client booting.
 */
class ClientDataPak private constructor(val version: Int, private val sections: List<Section>) {

  data class Section(val type: Int, val payload: ByteArray) {
    override fun equals(other: Any?) =
        other is Section && other.type == type && other.payload.contentEquals(payload)

    override fun hashCode() = 31 * type + payload.contentHashCode()
  }

  fun payloadOf(type: Int): ByteArray? = sections.firstOrNull { it.type == type }?.payload

  /** Replaces one section's payload, keeping section order and everything else untouched. */
  fun with(type: Int, payload: ByteArray): ClientDataPak =
      ClientDataPak(
          version,
          sections.map { if (it.type == type) Section(type, payload) else it },
      )

  /**
   * Decodes one section, hands its records to [edit], and re-encodes the result.
   *
   * The stock payload is round-tripped before anything is changed, so a codec that has the layout
   * wrong fails here rather than in the client.
   */
  fun <T> edit(codec: SectionCodec<T>, edit: (List<T>) -> List<T>): ClientDataPak {
    val payload =
        checkNotNull(payloadOf(codec.type)) {
          "This data.pak has no ${codec.name} section (${codec.type})"
        }
    codec.verify(payload)
    return with(codec.type, codec.encode(edit(codec.decode(payload))))
  }

  fun compress(): ByteArray {
    val body =
        Writer()
            .apply {
              bytes(MAGIC)
              int(version)
              byte(sections.size)
              sections.forEach { section ->
                byte(section.type)
                int(section.payload.size)
                bytes(section.payload)
              }
            }
            .bytes()
    return ByteArrayOutputStream()
        .also { out -> GZIPOutputStream(out).use { it.write(body) } }
        .toByteArray()
  }

  companion object {
    private val MAGIC = byteArrayOf(7, 0x50, 0x4f, 0x4b, 0x45, 0x4d, 0x4d, 0x4f)
    const val SUPPORTED_VERSION = 135

    fun parse(compressed: ByteArray): ClientDataPak {
      val raw = GZIPInputStream(ByteArrayInputStream(compressed)).readBytes()
      val reader = Reader(raw)
      check(reader.bytes(MAGIC.size).contentEquals(MAGIC)) { "Not a PokeMMO data.pak" }
      val version = reader.int()
      check(version == SUPPORTED_VERSION) {
        "Unsupported data.pak version $version; expected $SUPPORTED_VERSION"
      }
      val sections = List(reader.byte()) { Section(reader.byte(), reader.bytes(reader.int())) }
      check(reader.exhausted()) { "Trailing bytes after the last data.pak section" }
      return ClientDataPak(version, sections)
    }
  }
}

/**
 * A section whose record layout is understood well enough to rewrite.
 *
 * [verify] is the safety property: decoding the stock payload and re-encoding it must reproduce the
 * original bytes exactly. Anything less means the layout is wrong somewhere, and the build stops
 * before the client ever sees it.
 */
interface SectionCodec<T> {
  val type: Int
  val name: String

  fun decode(payload: ByteArray): List<T>

  fun encode(records: List<T>): ByteArray

  fun verify(payload: ByteArray) {
    val decoded = decode(payload)
    val reencoded = encode(decoded)
    check(reencoded.contentEquals(payload)) {
      "$name (section $type) does not round-trip: decoded ${decoded.size} records, " +
          "re-encoded ${reencoded.size} bytes against ${payload.size} original"
    }
  }
}

internal class Reader(private val data: ByteArray) {
  var offset = 0
    private set

  fun byte(): Int = data[offset++].toInt() and 0xff

  fun short(): Int = byte() or (byte() shl 8)

  fun int(): Int = short() or (short() shl 16)

  fun long(): Long = (int().toLong() and 0xffffffffL) or (int().toLong() shl 32)

  /** Null-terminated UTF-16LE, the client's own string form. */
  fun utf16z(): String {
    val text = StringBuilder()
    while (true) {
      val c = short()
      if (c == 0) return text.toString()
      text.append(c.toChar())
    }
  }

  fun bytes(size: Int): ByteArray = data.copyOfRange(offset, offset + size).also { offset += size }

  fun exhausted(): Boolean = offset == data.size

  fun remaining(): Int = data.size - offset
}

internal class Writer {
  private val output = ByteArrayOutputStream()

  fun byte(value: Int) = output.write(value and 0xff)

  fun short(value: Int) {
    byte(value)
    byte(value ushr 8)
  }

  fun int(value: Int) {
    short(value)
    short(value ushr 16)
  }

  fun long(value: Long) {
    int(value.toInt())
    int((value ushr 32).toInt())
  }

  fun utf16z(value: String) {
    value.forEach { short(it.code) }
    short(0)
  }

  fun bytes(value: ByteArray) = output.write(value)

  fun bytes(): ByteArray = output.toByteArray()
}
