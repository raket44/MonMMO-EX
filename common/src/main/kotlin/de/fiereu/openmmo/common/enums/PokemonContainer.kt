package de.fiereu.openmmo.common.enums

enum class PokemonContainer {
  PC,
  PARTY,
  TRADE,
  DAYCARE,
  RENTAL_PARTY,
  MAIL,
  AUCTION,
  VOID,
  DELETED,
  EVENT,
  BATTLE_BOX_1,
  BATTLE_BOX_2,
  GTS,
  /**
   * The egg incubators (client `f/xe1.ma`, wire byte 13, capacity FIFTEEN - the eight permanent
   * slots plus the seven temporary ones). The incubator page (f/fb6) lists this container and the
   * PC side by side, which is what its fill-all / empty-to-PC buttons move eggs between
   * (bytecode-walked 2026-09-16; see server Incubators).
   */
  INCUBATOR,
  UNKNOWN_14,

  /**
   * Client f/Cy.Vg0 (byte 15): the "shared monster" slot. A monster delivered here is not the
   * player's; f/NA0.dI0 opens the box window for it in read-only summary mode (f/ur1.m3) when the
   * 0x27 toggle follows. Chat monster links are viewed through it (2026-09-11).
   */
  SHARED_VIEW
}
