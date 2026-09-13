package de.fiereu.openmmo.net.game

import de.fiereu.bytecodec.ByteArrayReadBuffer
import de.fiereu.bytecodec.GrowableWriteBuffer
import de.fiereu.network.SessionAttribute
import de.fiereu.network.SessionAttributes
import de.fiereu.network.WireContext
import de.fiereu.openmmo.common.test.fixtureBuffer
import de.fiereu.openmmo.net.game.codecs.PokemonCodec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private class SlotTestAttributes(private val map: MutableMap<String, Any> = mutableMapOf()) :
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

private const val FIXTURE = "game/s2c/14/monster_record_32710.bin"

/**
 * The ability slot rides the byte straight after the IV word (client f/tK0.wG -> k91.WJ0). The
 * summary shows the species ability in that slot, so a slot that does not survive the record is a
 * monster that shows one ability and battles with another.
 */
class AbilitySlotRecordTest :
    FunSpec({
      val record = PokemonCodec.read(fixtureBuffer(FIXTURE))

      fun written(slot: Int, revision: Int?): ByteArray =
          WireContext.with(revision?.let { r -> SlotTestAttributes().also { it[CLIENT_REVISION] = r } }) {
            GrowableWriteBuffer().also { PokemonCodec.write(it, record.copy(abilitySlot = slot)) }.toByteArray()
          }

      for (revision in listOf(null, DESKTOP_REVISION_31914, 32645)) {
        test("slots 0, 1 and 2 survive the record for revision ${revision ?: "none"}") {
          for (slot in 0..2) {
            val bytes = written(slot, revision)
            WireContext.with(revision?.let { r -> SlotTestAttributes().also { it[CLIENT_REVISION] = r } }) {
              PokemonCodec.read(ByteArrayReadBuffer(bytes)).abilitySlot shouldBe slot
            }
          }
        }
      }

      test("the form byte survives the record and changes exactly one byte") {
        val base = GrowableWriteBuffer().also { PokemonCodec.write(it, record.copy(form = 0)) }.toByteArray()
        val formed = GrowableWriteBuffer().also { PokemonCodec.write(it, record.copy(form = 3)) }.toByteArray()
        base.size shouldBe formed.size
        base.indices.count { base[it] != formed[it] } shouldBe 1
        PokemonCodec.read(ByteArrayReadBuffer(formed)).form shouldBe 3
      }

      test("the slot is the byte after the IV word and nothing else moves") {
        val zero = written(0, null)
        val two = written(2, null)
        zero.size shouldBe two.size
        val differing = zero.indices.filter { zero[it] != two[it] }
        differing.size shouldBe 1
        two[differing.single()] shouldBe 2.toByte()
      }
    })
