package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.ServerMessageArg
import de.fiereu.openmmo.net.game.packets.ServerMessagePacket
import de.fiereu.openmmo.net.game.packets.WorldActionDispatchPacket
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The client's HM banner ("{mon} used Cut!" sliding in with the Pokemon, plus the player's
 * field-move pose): s2c 0xB6 case 2 (f/Ty0) with the move id as subject and two shorts - the
 * party slot and 0 for a party member, or a fresh key and the stand-in species encoded negative
 * (f/kC1.YL1 reads a negative second value as species in its low 12 bits) for an ocarina. The
 * client remembers the last (key, value) pair per key and skips a repeat (f/ln1.lPt7), hence the
 * running key. The grey box is client string 6068 "{00} used its {01}!", the one Sweet Scent
 * shows. Used by the ROM scripts' dofieldeffect and by Fly; callers wait [HOLD_MILLIS] before the
 * move lands so the pose and the banner's slide finish first.
 */
@Singleton
class FieldMoveBanners
@Inject
constructor(
    private val moves: de.fiereu.openmmo.moves.MoveRegistry,
    private val species: de.fiereu.openmmo.pokemon.SpeciesRegistry,
) {
  private val serial = AtomicInteger(1)

  /** Sends the banner and the grey box; false when the character cannot use the move. */
  fun send(session: SessionContext, stored: StoredCharacter, regionId: Int, moveId: Int): Boolean {
    val slot = stored.pokemon.indexOfFirst { mon -> mon.moves.any { it.id.toInt() == moveId } }
    val ocarina = slot < 0
    if (ocarina && !FieldMoves.ocarinaOwned(stored, moveId)) return false
    val standIn = OCARINA_STAND_INS[regionId to moveId] ?: SWEET_SCENT_STAND_INS[regionId] ?: 71
    // Party member: 0 - the client finds the party Pokemon by the move itself (a slot number
    // here drew a blank banner whenever the mover was not in slot 0).
    val first = if (ocarina) serial.incrementAndGet() and 0x0FFF else 0
    val second = if (ocarina) standIn - 4096 else 0
    session.send(WorldActionDispatchPacket(2, moveId.toByte(), listOf(first.toShort(), second.toShort())))
    val who =
        if (!ocarina) stored.pokemon[slot].let { it.nickname.ifEmpty { species.get(it.dexId)?.name ?: "" } }
        else "${stored.info.name}'s summoned ${species.get(standIn)?.name ?: "Pokemon"}"
    session.send(
        ServerMessagePacket(
            USED_MOVE_STRING,
            listOf(
                ServerMessageArg(0, RAW_STRING, false, 0, null, null, who, null),
                ServerMessageArg(1, RAW_STRING, false, 0, null, null, moves.get(moveId)?.name ?: "", null)),
            showOnMap = true,
            mode = null))
    return true
  }

  companion object {
    /** The pose (900 ms) plus the banner's slide in, hold and slide out on the client. */
    const val HOLD_MILLIS = 5000L
    /** Client string "{00} used its {01}!". */
    const val USED_MOVE_STRING = 6068
    /** Message argument kind for a raw string (client f/RO0 kind 5). */
    const val RAW_STRING = 5

    /** The client's own Sweet Scent stand-in per region (f/ZB1.mf1), the fallback. */
    val SWEET_SCENT_STAND_INS = mapOf(0 to 71, 1 to 357, 2 to 549, 3 to 415, 4 to 216)

    /**
     * The Pokemon an HM ocarina summons, by (region, move): a native of that region's own
     * generation that can learn the move, no species twice in a region. Regions: 0 Kanto,
     * 1 Hoenn, 2 Unova, 3 Sinnoh, 4 Johto. Retail's list is server-side and unknown; this is ours.
     */
    val OCARINA_STAND_INS: Map<Pair<Int, Int>, Int> =
        mapOf(
            // Kanto: Farfetch'd, Pidgeot, Lapras, Machoke, Voltorb, Geodude, Goldeen, Tentacruel, Abra
            (0 to 15) to 83, (0 to 19) to 18, (0 to 57) to 131, (0 to 70) to 67, (0 to 148) to 100,
            (0 to 249) to 74, (0 to 127) to 118, (0 to 291) to 73, (0 to 100) to 63,
            // Hoenn: Zigzagoon, Swellow, Wailmer, Makuhita, Volbeat, Nosepass, Barboach, Relicanth, Ralts
            (1 to 15) to 263, (1 to 19) to 277, (1 to 57) to 320, (1 to 70) to 296, (1 to 148) to 313,
            (1 to 249) to 299, (1 to 127) to 339, (1 to 291) to 369, (1 to 100) to 280,
            // Unova: Patrat, Unfezant, Basculin, Timburr, Watchog, Roggenrola, Alomomola, Frillish, Elgyem
            (2 to 15) to 504, (2 to 19) to 521, (2 to 57) to 550, (2 to 70) to 532, (2 to 148) to 505,
            (2 to 249) to 524, (2 to 127) to 594, (2 to 291) to 592, (2 to 100) to 605,
            // Sinnoh: Bidoof, Staravia, Buizel, Monferno, Chingling, Shieldon, Bibarel, Finneon
            (3 to 15) to 399, (3 to 19) to 397, (3 to 57) to 418, (3 to 70) to 391, (3 to 148) to 433,
            (3 to 249) to 410, (3 to 127) to 400, (3 to 291) to 456,
            // Johto: Sentret, Noctowl, Quagsire, Sudowoodo, Ledian, Phanpy, Chinchou, Mantine, Natu
            (4 to 15) to 161, (4 to 19) to 164, (4 to 57) to 195, (4 to 70) to 185, (4 to 148) to 166,
            (4 to 249) to 231, (4 to 127) to 170, (4 to 291) to 226, (4 to 100) to 177,
        )
  }
}
