package de.fiereu.openmmo.launcher.content

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * New items as section 3 clone records, and the no-duplicates rule for TMs (project owner,
 * 2026-09-13). The byte layout is what f/fi7 :pswitch_6fc reads, in flag-bit order.
 */
class ClientItemRecordsTest :
    FunSpec({
      fun bytes(vararg values: Int) = values.map { it and 0xff }

      test("a TM clone carries its donor and its taught move") {
        ClientItemRecords.encode(ClientItemRecords.Record(20400, 5351, taughtMove = 850))
            .map { it.toInt() and 0xff } shouldBe
            bytes(0xb0, 0x4f, 0x40, 0x02, 0, 0, 0xe7, 0x14, 0x52, 0x03)
      }

      test("an item clone carries its name, description and its own icon") {
        ClientItemRecords.encode(
                ClientItemRecords.Record(21000, 5084, nameString = 710000, descriptionString = 730000))
            .map { it.toInt() and 0xff } shouldBe
            bytes(
                0x08, 0x52, // id 21000
                0x40, 0, 0, 0x08, // CLONE | TEXT_AND_ICON
                0xdc, 0x13, // donor 5084
                0x70, 0xd5, 0x0a, 0, // name 710000
                0x90, 0x23, 0x0b, 0, // description 730000
                0x08, 0x52, // icon = its own id
            )
      }

      test("retail's records stay byte for byte and only the count grows") {
        val stock = bytes(2, 0, 0xaa, 0xbb, 0xcc).map(Int::toByte).toByteArray()
        val record = ClientItemRecords.Record(21000, 5084, nameString = 710000)
        val appended = ClientItemRecords.append(stock, listOf(record))
        appended.take(2).map { it.toInt() } shouldBe listOf(3, 0)
        appended.copyOfRange(2, 5).toList() shouldBe stock.copyOfRange(2, 5).toList()
        appended.copyOfRange(5, appended.size).toList() shouldBe ClientItemRecords.encode(record).toList()
      }

      test("a new item must clone one of the client's own and live in the new block") {
        shouldThrow<IllegalArgumentException> { ClientItemRecords.encode(ClientItemRecords.Record(5500, 5084)) }
        shouldThrow<IllegalArgumentException> { ClientItemRecords.encode(ClientItemRecords.Record(21000, 21001)) }
      }

      test("no TM is created for a move a retail tool already teaches") {
        // Thunderbolt (85) is TM24 = 5351; Mega Punch (5) is the client's 1710, never its 7710 copy.
        TmPlan.retailToolFor(85) shouldBe 5351
        TmPlan.retailToolFor(5) shouldBe 1710
        val retailMoves = TmPlan.retailTools.values.toSet()
        retailMoves shouldContain 85
        retailMoves shouldNotContain 0
      }
    })
