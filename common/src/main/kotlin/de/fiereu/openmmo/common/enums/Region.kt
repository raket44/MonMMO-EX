package de.fiereu.openmmo.common.enums

// The five regions the five supported ROMs provide, numbered as the retail client renders them:
// region 2 draws Unova and 4 draws Johto, verified in-game. The old entries called 2 "Johto" and
// invented a "Galar" at 3 - a region no supported ROM contains.
enum class Region(val wireValue: Byte) {
  KANTO(0),
  HOENN(1),
  UNOVA(2),
  SINNOH(3),
  JOHTO(4);

  val displayName: String = name.lowercase().replaceFirstChar { it.uppercase() }

  companion object {
    fun byId(id: Int): Region? = entries.find { it.wireValue.toInt() == id }

    fun byWireValue(wireValue: Byte): Region? = entries.find { it.wireValue == wireValue }
  }
}
