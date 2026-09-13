package de.fiereu.openmmo.net.game

import de.fiereu.bytecodec.GrowableWriteBuffer
import de.fiereu.network.SessionAttribute
import de.fiereu.network.SessionAttributes
import de.fiereu.network.WireContext
import de.fiereu.openmmo.common.test.fixture
import de.fiereu.openmmo.common.test.fixtureBuffer
import de.fiereu.openmmo.net.game.codecs.PokemonCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The monster record is one byte longer for clients from 32710 on than for desktop 31914. The
 * width is chosen per connection from the revision declared at join; outside a session the captured
 * (long) form applies, which is what the 32710 fixture holds.
 */
private class FakeAttributes(private val map: MutableMap<String, Any> = mutableMapOf()) :
    SessionAttributes {
  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> get(key: SessionAttribute<T>): T? = map[key.name] as T?

  override fun <T : Any> set(key: SessionAttribute<T>, value: T) {
    map[key.name] = value
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> remove(key: SessionAttribute<T>): T? = map.remove(key.name) as T?

  @Suppress("UNCHECKED_CAST")
  override fun <T : Any> getOrPut(key: SessionAttribute<T>, default: () -> T): T =
      map.getOrPut(key.name, default) as T

  override fun contains(key: SessionAttribute<*>): Boolean = key.name in map
}

private fun sessionAt(revision: Int): SessionAttributes =
    FakeAttributes().also { it[CLIENT_REVISION] = revision }

private const val FIXTURE = "game/s2c/14/monster_record_32710.bin"

class MonsterRecordRevisionTest :
    FunSpec({
      val captured = fixture(FIXTURE)

      fun writtenWidth(attributes: SessionAttributes?): Int =
          WireContext.with(attributes) {
            val out = GrowableWriteBuffer()
            PokemonCodec.write(out, PokemonCodec.read(fixtureBuffer(FIXTURE)))
            out.toByteArray().size
          }

      test("with no session the captured 139-byte record round trips at its exact width") {
        captured.size shouldBe 139
        writtenWidth(null) shouldBe 139
      }

      test("a 32645 (Android) session gets the long record, same as the capture") {
        writtenWidth(sessionAt(32645)) shouldBe 139
      }

      test("a 32710 session gets the long record") {
        writtenWidth(sessionAt(32710)) shouldBe 139
      }

      test("a 31914 (desktop) session gets the record one byte shorter") {
        // The reader in this context consumes only the short trailer, so the fixture leaves its
        // extra byte unread; what matters is the width written for that client.
        WireContext.with(sessionAt(DESKTOP_REVISION_31914)) {
          val buf = fixtureBuffer(FIXTURE)
          val record = PokemonCodec.read(buf)
          buf.remaining() shouldBe 1
          val out = GrowableWriteBuffer()
          PokemonCodec.write(out, record)
          out.toByteArray().size shouldBe 138
        }
      }

      test("the context is restored after the block") {
        WireContext.with(sessionAt(31914)) { currentClientRevision() shouldBe 31914 }
        currentClientRevision() shouldBe null
      }
    })
