package de.fiereu.openmmo.launcher.content

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

/**
 * The records written for moves the client lacks, checked the way r32645 `f/fi7` case 4 reads them:
 * `u16 id, u8 category, u16 flags`, then one payload per set bit in bit order - 0x1 accuracy (u8),
 * 0x2 power (u16), 0x4 PP (u8), 0x8 type (u8), 0x10 (i32), 0x20 priority (signed u8), ...
 */
class ExpansionMoveRecordTest :
    FunSpec({
      val root = Path.of("../../pokeemerald-expansion")
      val moves = MoveText.parse(root, MoveText.ids(root)).associateBy { it.id }

      /** The record's bytes as the client sees them, without the section's leading count. */
      fun bytes(record: MoveRecord): ByteArray = MoveCodec.encode(listOf(record)).let {
        it.copyOfRange(2, it.size)
      }

      test("Water Shuriken's generation-gated category resolves to special") {
        val shuriken = moves.getValue(594)
        shuriken.name shouldBe "Water Shuriken"
        shuriken.category shouldBe "DAMAGE_CATEGORY_SPECIAL"
        moveRecord(shuriken).category shouldBe 1
        bytes(moveRecord(shuriken))[2].toInt() shouldBe 1
      }

      test("a positive priority move carries bit 0x20 and a signed byte after the type") {
        val upperHand = moves.getValue(846)
        upperHand.name shouldBe "Upper Hand"
        upperHand.priority shouldBe 3
        val record = moveRecord(upperHand)
        record.flags shouldBe 0x2f
        record.category shouldBe 0
        val raw = bytes(record)
        // id(2) category(1) flags(2) accuracy(1) power(2) pp(1) type(1) -> priority at offset 10.
        (raw[3].toInt() and 0xff or (raw[4].toInt() and 0xff shl 8)) shouldBe 0x2f
        raw[9].toInt() shouldBe 1 // TYPE_FIGHTING
        raw[10].toInt() shouldBe 3
      }

      test("a negative priority is written as a signed byte") {
        val beakBlast = moves.getValue(653)
        beakBlast.name shouldBe "Beak Blast"
        val raw = bytes(moveRecord(beakBlast))
        raw[10].toInt() shouldBe -3
        MoveCodec.decode(MoveCodec.encode(listOf(moveRecord(beakBlast)))).single().field020!!.toByte()
            .toInt() shouldBe -3
      }

      test("a priority-0 move keeps the original layout") {
        val flyingPress = moves.getValue(560)
        flyingPress.priority shouldBe 0
        val record = moveRecord(flyingPress)
        record.flags shouldBe 0x0f
        record.field020 shouldBe null
      }

      test("every staged move id's priority and category match the Expansion") {
        // The moves diff.csv flagged: priority was 0 for all of them before the fix.
        mapOf(569 to 1, 578 to 3, 588 to 4, 594 to 1, 596 to 4, 600 to 1, 608 to 1, 623 to 2,
                624 to 4, 634 to 3, 653 to -3, 658 to -3, 663 to 1, 720 to 4, 780 to 4, 785 to 1,
                836 to 4, 837 to 1, 846 to 3)
            .forEach { (id, priority) ->
              val record = moveRecord(moves.getValue(id))
              record.field020 shouldBe priority
              (record.flags and MoveRecord.FIELD_020) shouldBe MoveRecord.FIELD_020
            }
      }

      test("retail move records round-trip byte for byte") {
        val stock = stockDataPak() ?: return@test
        val payload = ClientDataPak.parse(Files.readAllBytes(stock)).payloadOf(MoveCodec.type)!!
        MoveCodec.encode(MoveCodec.decode(payload)).contentEquals(payload) shouldBe true
      }

      // Retail moves staged as Fairy (project owner, 2026-09-13: "client's numbers but our move
      // retyping"): retail's own record, bit 0x8 and type 19, every other byte kept.

      test("the Expansion's Fairy retypes of ROM-numbered moves are exactly the approved three") {
        RetailMerge.fairyMoveRetypes(moves.mapValues { it.value.type }, 559) shouldBe setOf(186, 204, 236)
        moves.getValue(186).name shouldBe "Sweet Kiss"
        moves.getValue(204).name shouldBe "Charm"
        moves.getValue(236).name shouldBe "Moonlight"
      }

      test("the raw retype decoder finds the inserted type byte and rejects any other change") {
        // id 204, category 2, flags 0x0005, accuracy 100, PP 20, no effects.
        val stock = byteArrayOf(0xcc.toByte(), 0, 2, 0x05, 0, 100, 20, 0)
        val retyped = byteArrayOf(0xcc.toByte(), 0, 2, 0x0d, 0, 100, 20, 19, 0)
        retypedTypeByte(stock, retyped) shouldBe 19
        // Category changed along with the type.
        retypedTypeByte(stock, byteArrayOf(0xcc.toByte(), 0, 1, 0x0d, 0, 100, 20, 19, 0)) shouldBe null
        // Type byte in the wrong place (after the effect count).
        retypedTypeByte(stock, byteArrayOf(0xcc.toByte(), 0, 2, 0x0d, 0, 100, 20, 0, 19)) shouldBe null
        // Flags gained more than 0x8.
        retypedTypeByte(stock, byteArrayOf(0xcc.toByte(), 0, 2, 0x2d, 0, 100, 20, 19, 0)) shouldBe null
      }

      test("synthetic retail records are retyped in place, never appended, and the rest stay byte for byte") {
        val other = MoveRecord(33, category = 0, flags = 0x7, field001 = 100, field002 = 40, field004 = 35)
        val bare = MoveRecord(186, category = 2, flags = 0)
        val flagged =
            MoveRecord(
                204,
                category = 2,
                flags = 0x145,
                field001 = 100,
                field004 = 20,
                field040 = 7,
                field100 = 1,
                effects = listOf(MoveEffect(12, listOf(MoveEffectParam(0, 3, shorts = listOf(1, 2))))),
            )
        val added = moveRecord(moves.getValue(560))
        val rebuilt =
            MoveCodec.decode(
                MoveCodec.encode(RetailMerge.moves(listOf(other, bare, flagged), setOf(186, 204), listOf(added))))

        rebuilt.map { it.moveId } shouldBe listOf(33, 186, 204, 560)
        bytes(rebuilt[0]).contentEquals(bytes(other)) shouldBe true
        rebuilt[1].type shouldBe 19
        retypedTypeByte(bytes(bare), bytes(rebuilt[1])) shouldBe 19
        rebuilt[2].type shouldBe 19
        retypedTypeByte(bytes(flagged), bytes(rebuilt[2])) shouldBe 19
        isApprovedFairyRetype(flagged, rebuilt[2]) shouldBe true
      }

      test("a retype needs a retail Normal record and an approved id; anything more is not approved") {
        val charm = MoveRecord(204, category = 2, flags = 0)
        shouldThrow<IllegalArgumentException> { RetailMerge.moves(emptyList(), setOf(204), emptyList()) }
        shouldThrow<IllegalArgumentException> {
          RetailMerge.fairyRetype(MoveRecord(204, 2, MoveRecord.TYPE, type = 11))
        }
        RetailMerge.fairyRetype(MoveRecord(204, 2, MoveRecord.TYPE, type = 0)).type shouldBe 19
        shouldThrow<IllegalArgumentException> {
          RetailMerge.moves(listOf(charm), emptyList<Int>().toSet(), listOf(MoveRecord(204, 2, 0)))
        }
        isApprovedFairyRetype(charm, RetailMerge.fairyRetype(charm)) shouldBe true
        isApprovedFairyRetype(charm, RetailMerge.fairyRetype(charm).copy(category = 0)) shouldBe false
        val tackle = MoveRecord(33, category = 0, flags = 0)
        isApprovedFairyRetype(tackle, tackle.copy(flags = MoveRecord.TYPE, type = 19)) shouldBe false
      }

      test("stock r32645 section 4 rebuilt: the three are Fairy, otherwise byte-identical, all else untouched") {
        val pak = r32645StockDataPak() ?: return@test
        val stockPak = ClientDataPak.parse(Files.readAllBytes(pak))
        val stock = MoveCodec.decode(stockPak.payloadOf(MoveCodec.type)!!)
        val rebuiltPak =
            stockPak.edit(MoveCodec) { RetailMerge.moves(it, RetailMerge.APPROVED_FAIRY_MOVE_RETYPES, emptyList()) }
        val rebuilt = MoveCodec.decode(ClientDataPak.parse(rebuiltPak.compress()).payloadOf(MoveCodec.type)!!)

        rebuilt.size shouldBe stock.size
        val retyped =
            stock.indices.filter { i ->
              val before = bytes(stock[i])
              val after = bytes(rebuilt[i])
              if (after.contentEquals(before)) return@filter false
              stock[i].moveId shouldBe rebuilt[i].moveId
              rebuilt[i].type shouldBe 19
              retypedTypeByte(before, after) shouldBe 19
              isApprovedFairyRetype(stock[i], rebuilt[i]) shouldBe true
              true
            }
        retyped.map { stock[it].moveId }.toSet() shouldBe setOf(186, 204, 236)
        retyped.size shouldBe 3
      }
    })

/**
 * The r32645 Android client's pristine data.pak: `MONMMO_R32645_DATA_PAK`, or where
 * package-android.cmd extracts it. The file is not in the repository, so the test stands down without it.
 */
internal fun r32645StockDataPak(): Path? =
    listOfNotNull(
            System.getenv("MONMMO_R32645_DATA_PAK"),
            System.getenv("TEMP")?.let { "$it/monmmo-android-build/stock-root/data/data.pak" },
        )
        .map(Path::of)
        .firstOrNull(Files::isRegularFile)

/**
 * Reads a record the way r32645 `f/fi7` case 4 lays it out - `u16 id, u8 category, u16 flags`, then
 * accuracy (0x1, u8), power (0x2, u16), PP (0x4, u8), and the type byte (0x8) next - independently
 * of [MoveCodec]. Returns the type byte when [staged] is exactly [stock] with bit 0x8 set and that one
 * byte written (inserted, or overwritten where retail already flagged a type), otherwise null.
 */
internal fun retypedTypeByte(stock: ByteArray, staged: ByteArray): Int? {
  fun u16(bytes: ByteArray) = (bytes[3].toInt() and 0xff) or ((bytes[4].toInt() and 0xff) shl 8)
  if (stock.size < 5 || staged.size < 5) return null
  val flags = u16(stock)
  if (u16(staged) != flags or MoveRecord.TYPE) return null
  val offset =
      5 + (if (flags and 0x1 != 0) 1 else 0) + (if (flags and 0x2 != 0) 2 else 0) + (if (flags and 0x4 != 0) 1 else 0)
  if (staged.size <= offset) return null
  val withoutType =
      if (flags and MoveRecord.TYPE != 0) staged.copyOf().also { it[offset] = stock[offset] }
      else staged.copyOfRange(0, offset) + staged.copyOfRange(offset + 1, staged.size)
  if (withoutType.size != stock.size) return null
  withoutType[3] = stock[3]
  withoutType[4] = stock[4]
  return if (withoutType.contentEquals(stock)) staged[offset].toInt() and 0xff else null
}
