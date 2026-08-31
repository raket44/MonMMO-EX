package de.fiereu.openmmo.common.enums

enum class MoveTarget(val mask: Int) {
  SELECTED(0),
  DEPENDS(1 shl 0),
  USER_OR_SELECTED(1 shl 1),
  RANDOM(1 shl 2),
  BOTH(1 shl 3),
  USER(1 shl 4),
  FOES_AND_ALLY(1 shl 5),
  OPPONENTS_FIELD(1 shl 6),
  // Targets the Expansion uses that the Emerald decomp did not. The mask values continue the
  // sequence; nothing reads them today, so they exist only to keep each target distinct.
  FIELD(1 shl 7),
  ALL_BATTLERS(1 shl 8),
  ALLY(1 shl 9),
  USER_AND_ALLY(1 shl 10),
  USER_OR_ALLY(1 shl 11),
  SMART(1 shl 12),
  OPPONENT(1 shl 13),
}
