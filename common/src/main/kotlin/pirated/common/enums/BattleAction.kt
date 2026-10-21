package pirated.common.enums

/** A player's battle-turn choice. */
enum class BattleAction(val id: Byte) {
  MOVE(0),
  ITEM(1),
  SWITCH(2),
  RUN(3),
  /** The Safari Game's Ball, Bait and Rock (client f/sV ids 5, 6, 7); they carry no tail bytes. */
  BALL(5),
  BAIT(6),
  ROCK(7),
  /**
   * Triple battles' Shift (client kind 11, no tail bytes): an end Pokemon asks to move to the
   * centre, spending its turn. Seen 2026-09-26 as frame 32 00 0b from position 0 in format 5.
   */
  SHIFT(11);

  companion object {
    fun fromId(id: Byte): BattleAction? = entries.firstOrNull { it.id == id }
  }
}
