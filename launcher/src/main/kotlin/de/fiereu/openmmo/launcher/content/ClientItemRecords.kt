package de.fiereu.openmmo.launcher.content

/**
 * New items, made the way the client makes its own copies (project owner, 2026-09-13: no duplicates,
 * expand the working code).
 *
 * data.pak section 3 is the client's item table: `u16 count`, then per record `u16 itemId, i32 flags`
 * and one payload per set bit, in bit order (f/fi7 :pswitch_6fc). Retail uses flag 0x40 to clone an
 * item from a donor - its whole 6000 band and its 7600 tools are made that way - and every item here
 * is made the same way: a real retail item of the right kind is copied (a stone for a stone, a TM for
 * a TM), then given its own identity. Retail's records are kept byte for byte; ours are appended.
 */
object ClientItemRecords {
  /** Clone a donor: u16 donor id. The clone pass after the table (fi7 :cond_9bb) builds the item. */
  const val CLONE = 0x40

  /** The move a tool teaches: u16. The clone pass carries it onto the clone (MonMMO-EX client code). */
  const val TAUGHT_MOVE = 0x200

  /**
   * MonMMO-EX client code: i32 name string, i32 description string, u16 icon id (the file
   * sprites/itemicons/<icon>.png). Without it a clone wears its donor's name, text and icon.
   */
  const val TEXT_AND_ICON = 0x8000000

  /** Every id below this is the client's own (ROM bands up to 9536, PokeMMO's 1000-7999). */
  const val FIRST_NEW_ITEM_ID = 20000

  data class Record(
      val itemId: Int,
      val donorId: Int,
      val taughtMove: Int? = null,
      val nameString: Int? = null,
      val descriptionString: Int = 0,
  )

  fun encode(record: Record): ByteArray {
    require(record.itemId in FIRST_NEW_ITEM_ID..0x7FFF) {
      "Item ${record.itemId} is not in the block for new items (the client keys items by a short)"
    }
    require(record.donorId in 1 until FIRST_NEW_ITEM_ID) {
      "Item ${record.itemId} must clone one of the client's own items, not ${record.donorId}"
    }
    val text = record.nameString != null
    return Writer()
        .apply {
          short(record.itemId)
          int(
              CLONE or
                  (if (record.taughtMove != null) TAUGHT_MOVE else 0) or
                  (if (text) TEXT_AND_ICON else 0))
          short(record.donorId)
          record.taughtMove?.let(::short)
          if (text) {
            int(checkNotNull(record.nameString))
            int(record.descriptionString)
            short(record.itemId)
          }
        }
        .bytes()
  }

  /** Retail's table with [records] after it: retail's bytes unchanged, only the count grows. */
  fun append(stock: ByteArray, records: List<Record>): ByteArray {
    val count = Reader(stock).short()
    require(count + records.size <= 0xFFFF) { "Section 3 cannot hold ${count + records.size} records" }
    require(records.map { it.itemId }.toSet().size == records.size) { "An item id is created twice" }
    return Writer()
        .apply {
          short(count + records.size)
          bytes(stock.copyOfRange(2, stock.size))
          records.forEach { bytes(encode(it)) }
        }
        .bytes()
  }
}
