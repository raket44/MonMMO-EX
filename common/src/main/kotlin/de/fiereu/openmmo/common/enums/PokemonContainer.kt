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
  UNKNOWN_13,
  UNKNOWN_14,

  /**
   * Client f/Cy.Vg0 (byte 15): the "shared monster" slot. A monster delivered here is not the
   * player's; f/NA0.dI0 opens the box window for it in read-only summary mode (f/ur1.m3) when the
   * 0x27 toggle follows. Chat monster links are viewed through it (2026-09-11).
   */
  SHARED_VIEW
}
