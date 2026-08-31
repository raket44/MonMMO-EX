package de.fiereu.openmmo.launcher.content

/**
 * Per-move extra data: section 12, one record for each of the client's 559 moves, listed descending
 * from the highest id the way section 10 descends from 1052.
 *
 * This is not the move table - that is section 4, which carries type, power, accuracy and PP. What
 * lives here is a cost and a tier that track each other (10000 goes with 4, 8000 with 3, 6000 and
 * 7000 with 2, 3000 to 5000 with 1) and, on 33 moves, a short list of id and quantity pairs whose
 * ids all fall in the 5000s where items sit. A teaching price, on the evidence, though nothing here
 * depends on that reading.
 */
data class MoveExtra(
    val moveId: Int,
    val cost: Int,
    val tier: Int,
    /** Id and quantity pairs. Empty on all but 33 stock moves, which carry exactly two. */
    val pairs: List<Pair<Int, Int>> = emptyList(),
)

object MoveExtraCodec : SectionCodec<MoveExtra> {
  override val type = 12
  override val name = "move extra data"

  override fun decode(payload: ByteArray): List<MoveExtra> {
    val reader = Reader(payload)
    val records =
        List(reader.short()) {
          MoveExtra(
              moveId = reader.short(),
              cost = reader.short(),
              tier = reader.short(),
              pairs = List(reader.byte()) { reader.short() to reader.short() },
          )
        }
    check(reader.exhausted()) { "move extra section has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<MoveExtra>): ByteArray =
      Writer()
          .apply {
            short(records.size)
            records.forEach { record ->
              short(record.moveId)
              short(record.cost)
              short(record.tier)
              byte(record.pairs.size)
              record.pairs.forEach { (id, quantity) ->
                short(id)
                short(quantity)
              }
            }
          }
          .bytes()
}
