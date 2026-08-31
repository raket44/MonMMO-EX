package de.fiereu.openmmo.launcher.content

/**
 * The move table: section 4, read by `f/uh.eF` case 4 into `f/sC`.
 *
 * Flag-driven like the species detail section - `u16 moveId, u8 category, u16 flags`, then one
 * payload per set bit in bit order - and then, unconditionally, a list of effects. The effects are
 * what makes a move do anything: an id and a parameter list, which is how PokeMMO scripts a move
 * rather than hard-coding it.
 *
 * The stock table holds 842 records with ids running past 559 to 3557, so the client is already
 * prepared for moves the Gen 5 ROMs never had. That is the room a new move goes into.
 */
data class MoveRecord(
    val moveId: Int,
    val category: Int,
    val flags: Int,
    val field001: Int? = null,
    val field002: Int? = null,
    val field004: Int? = null,
    /** Read through `f/rK1.wo0`, the same type enum species use. */
    val type: Int? = null,
    val field010: Int? = null,
    val field020: Int? = null,
    val field040: Int? = null,
    val field100: Int? = null,
    val contest: ContestBlock? = null,
    val field400: Int? = null,
    val effects: List<MoveEffect> = emptyList(),
) {
  companion object {
    const val FIELD_001 = 0x001
    const val FIELD_002 = 0x002
    const val FIELD_004 = 0x004
    const val TYPE = 0x008
    const val FIELD_010 = 0x010
    const val FIELD_020 = 0x020
    const val FIELD_040 = 0x040
    /** Carries no payload: the bit itself is the value. */
    const val FLAG_080 = 0x080
    const val FIELD_100 = 0x100
    const val CONTEST = 0x200
    const val FIELD_400 = 0x400
  }
}

/** The payload behind bit 0x200: two lists of single-byte enum values and four loose bytes. */
data class ContestBlock(
    val first: List<Int>,
    val afterFirst: Int,
    val second: List<Int>,
    val afterSecond: Int,
    val flag: Int,
    val trailing: Int,
)

/** One scripted effect: an id and the parameters it takes. */
data class MoveEffect(val effectId: Int, val params: List<MoveEffectParam>)

/**
 * One effect parameter, read by `f/tK0.fa1`.
 *
 * The second byte selects the payload shape, and its high bit means a qualifier byte follows before
 * the payload. Only [SHORTS] appears in the stock table; the rest are implemented from the bytecode
 * so that a section carrying them still round-trips.
 */
data class MoveEffectParam(
    val slot: Int,
    val kind: Int,
    val qualifier: Int? = null,
    val shorts: List<Int> = emptyList(),
    val int: Int? = null,
    val long: Long? = null,
    val text: String? = null,
) {
  companion object {
    const val EMPTY = 28
    const val LONG = 30
    val INTS = setOf(9, 10, 17)
    val TEXTS = setOf(5, 18)
    /** Anything not named above: a counted list of shorts. */
    const val SHORTS = 3
  }
}

object MoveCodec : SectionCodec<MoveRecord> {
  override val type = 4
  override val name = "move table"

  override fun decode(payload: ByteArray): List<MoveRecord> {
    val reader = Reader(payload)
    val records =
        List(reader.short()) {
          val moveId = reader.short()
          val category = reader.byte()
          val flags = reader.short()
          MoveRecord(
              moveId = moveId,
              category = category,
              flags = flags,
              field001 = if (flags and MoveRecord.FIELD_001 != 0) reader.byte() else null,
              field002 = if (flags and MoveRecord.FIELD_002 != 0) reader.short() else null,
              field004 = if (flags and MoveRecord.FIELD_004 != 0) reader.byte() else null,
              type = if (flags and MoveRecord.TYPE != 0) reader.byte() else null,
              field010 = if (flags and MoveRecord.FIELD_010 != 0) reader.int() else null,
              field020 = if (flags and MoveRecord.FIELD_020 != 0) reader.byte() else null,
              field040 = if (flags and MoveRecord.FIELD_040 != 0) reader.byte() else null,
              field100 = if (flags and MoveRecord.FIELD_100 != 0) reader.byte() else null,
              contest =
                  if (flags and MoveRecord.CONTEST != 0)
                      ContestBlock(
                          first = List(reader.byte()) { reader.byte() },
                          afterFirst = reader.byte(),
                          second = List(reader.byte()) { reader.byte() },
                          afterSecond = reader.byte(),
                          flag = reader.byte(),
                          trailing = reader.byte(),
                      )
                  else null,
              field400 = if (flags and MoveRecord.FIELD_400 != 0) reader.byte() else null,
              effects =
                  List(reader.byte()) {
                    MoveEffect(reader.int(), List(reader.byte()) { param(reader) })
                  },
          )
        }
    check(reader.exhausted()) { "move table has ${reader.remaining()} trailing bytes" }
    return records
  }

  override fun encode(records: List<MoveRecord>): ByteArray =
      Writer()
          .apply {
            short(records.size)
            records.forEach { record ->
              short(record.moveId)
              byte(record.category)
              short(record.flags)
              record.field001?.let(::byte)
              record.field002?.let(::short)
              record.field004?.let(::byte)
              record.type?.let(::byte)
              record.field010?.let(::int)
              record.field020?.let(::byte)
              record.field040?.let(::byte)
              record.field100?.let(::byte)
              record.contest?.let { contest ->
                byte(contest.first.size)
                contest.first.forEach(::byte)
                byte(contest.afterFirst)
                byte(contest.second.size)
                contest.second.forEach(::byte)
                byte(contest.afterSecond)
                byte(contest.flag)
                byte(contest.trailing)
              }
              record.field400?.let(::byte)
              byte(record.effects.size)
              record.effects.forEach { effect ->
                int(effect.effectId)
                byte(effect.params.size)
                effect.params.forEach { write(this, it) }
              }
            }
          }
          .bytes()

  /**
   * The kind byte is masked to seven bits before the switch, so a qualifier and a payload shape are
   * independent. Reading and writing both have to treat them that way or the high bit is lost.
   */
  private fun param(reader: Reader): MoveEffectParam {
    val slot = reader.byte()
    val raw = reader.byte()
    val kind = raw and 0x7f
    val qualifier = if (raw and 0x80 != 0) reader.byte() else null
    return when (kind) {
      MoveEffectParam.EMPTY -> MoveEffectParam(slot, kind, qualifier)
      MoveEffectParam.LONG -> MoveEffectParam(slot, kind, qualifier, long = reader.long())
      in MoveEffectParam.INTS -> MoveEffectParam(slot, kind, qualifier, int = reader.int())
      in MoveEffectParam.TEXTS -> MoveEffectParam(slot, kind, qualifier, text = reader.utf16z())
      else ->
          MoveEffectParam(slot, kind, qualifier, shorts = List(reader.byte()) { reader.short() })
    }
  }

  private fun write(writer: Writer, param: MoveEffectParam) {
    writer.byte(param.slot)
    writer.byte(if (param.qualifier != null) param.kind or 0x80 else param.kind)
    param.qualifier?.let(writer::byte)
    when (param.kind) {
      MoveEffectParam.EMPTY -> Unit
      MoveEffectParam.LONG -> writer.long(checkNotNull(param.long))
      in MoveEffectParam.INTS -> writer.int(checkNotNull(param.int))
      in MoveEffectParam.TEXTS -> writer.utf16z(checkNotNull(param.text))
      else -> {
        writer.byte(param.shorts.size)
        param.shorts.forEach(writer::short)
      }
    }
  }
}
