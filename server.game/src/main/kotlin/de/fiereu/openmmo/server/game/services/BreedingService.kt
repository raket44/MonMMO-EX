package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.net.game.packets.AssignBreedingSlotPacket
import de.fiereu.openmmo.net.game.packets.BreedingForecastPacket
import de.fiereu.openmmo.net.game.packets.SubmitBreedingPartyPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

private val log = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

/**
 * The daycare breeding exchange, wire-decoded from the client (see the breed-window trace): the
 * window sends AssignBreedingSlot(uid1, uid2, genderPref) whenever the pair or the offspring gender
 * preference changes and then WAITS on the BreedingForecast reply - the forecast is the baby
 * preview in the middle and the cost column. The Breed button sends SubmitBreedingParty.
 *
 * v1 scope: the forecast answers with the non-Ditto parent's species (the server holds no evolution
 * chains yet, so no devolving to the base stage), empty stat/shiny/value columns and zero cost; the
 * submit only logs - egg creation is the next system.
 */
@Singleton
class BreedingService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry,
) {

  fun onAssignSlot(event: PacketEvent<AssignBreedingSlotPacket>) {
    val session = event.session
    val p = event.packet
    val charId = session.attributes[PLAYER_STATE]?.characterId ?: return
    val stored = characterStore.getCharacter(charId) ?: return
    val owned = stored.pokemon + stored.pcStorage
    val first = owned.firstOrNull { it.id == p.ownPokemonEntityId }
    val second = owned.firstOrNull { it.id == p.partnerPokemonEntityId }
    log.info {
      "Breeding assign: char=$charId a=${p.ownPokemonEntityId}(${first?.dexId}) " +
          "b=${p.partnerPokemonEntityId}(${second?.dexId}) genderPref=${p.slotIndex}"
    }
    if (first == null || second == null) {
      session.send(emptyForecast(p.ownPokemonEntityId, p.partnerPokemonEntityId))
      return
    }
    val offspringDex = if (first.dexId == DITTO) second.dexId else first.dexId
    // The nature line: an EMPTY possibleNatures list renders "???" (a random roll - the earlier
    // crash blamed on this list was really the statEntries indexing). An Everstone holder pins
    // their nature; both holding one is a 50/50 between theirs.
    val pinnedNatures = buildList {
      if (first.heldItem in EVERSTONES) add(first.nature.ordinal.toByte())
      if (second.heldItem in EVERSTONES) add(second.nature.ordinal.toByte())
    }
    session.send(
        BreedingForecastPacket(
            parentA = p.ownPokemonEntityId,
            parentB = p.partnerPokemonEntityId,
            hasPreview = true,
            species = clientSpeciesId(offspringDex).toShort(),
            form = 0,
            // The renderer walks its six stat constants and indexes THIS array by stat id
            // directly (pM1.Cm0 line 99: statEntries[stat.Df0]) - it must always hold one
            // entry per stat, in RC0.Df0 order: hp, atk, def, spAtk, spDef, SPEED. The
            // inheritance model behind the rows is documented on forecastStatEntries.
            statEntries = forecastStatEntries(first, second),
            possibleNatures = pinnedNatures.distinct(),
            valueIds = emptyList(),
            valueSources = emptyList(),
            gender = p.slotIndex,
            // Capture-mislabeled TWICE over: this short is the offspring's APPEARANCE FLAGS
            // bitfield (k91.Zl1 - bit 0 shiny, bit 3 secret, more for alpha; -1 rendered a
            // secret shiny alpha, operator-verified). 0 = plain until shininess rules exist.
            nature = 0,
            // Capture-mislabeled: this boolean is the GENDER-CHOOSER toggle, not shininess.
            // The renderer (Cm0 param 11) sets the gender buttons' visibility from it and
            // resets the preference to "any" when false - the section only exists while true.
            shiny = true,
            // The two cost ints are the PER-GENDER prices rendered in the chooser: cost = male,
            // secondaryCost = female (operator-verified when only male showed 5000).
            cost = GENDER_CHOICE_COST,
            secondaryCost = GENDER_CHOICE_COST,
        ))
  }

  fun onSubmit(event: PacketEvent<SubmitBreedingPartyPacket>) {
    val p = event.packet
    log.info {
      "Breeding submit: session=${p.sessionId} mons=${p.pokemonEntityIds} " +
          "gender=${p.slotIndex} valueId=${p.stateFlag} - egg creation not modeled yet"
    }
    sendNotice(event.session, "Breeding is coming soon - your pair was noted.")
  }

  /**
   * The inheritance model follows the CLIENT'S OWN headers (operator-confirmed as the spec): base
   * is string 2547 "3 IVs will be passed. 3 IVs will average."; with a shiny/secret parent the
   * client itself switches to string 2548 "4 IVs will be passed. 2 are always from the better
   * parent." (the header choice is k91.z3() - rarity bits 0|9 - so the math here must use the same
   * condition). The passed stats are chosen at random, a pass is 50/50 which parent (except the
   * shiny-forced ones, always the higher value), and a NON-passed stat rolls uniformly across the
   * whole min-to-max range - every value in between is possible, endpoints included. A held Power
   * brace pins its stat to the holder's value and occupies one of the pass slots.
   *
   * The forecast shows the per-stat marginals of that process as three tooltip lines: "High pass" /
   * "Low pass" (strings 2541/2542) for the pass shares and "Average" (2544) carrying the roll share
   * at the range midpoint - enumerating every rollable value cluttered the window. The renderer's
   * TreeSet of contribution values renders the "min - max" range, so the endpoints keep it honest.
   *
   * ROW ORDER: the renderer indexes statEntries[RC0.Df0] and the enum's Df0 bytes are hp 0, atk 1,
   * def 2, spAtk 3, spDef 4, SPEED 5 (bytecode: the SPEED constant is built with 5) - the same
   * order as the IV word's 5-bit groups. NOT the GBA hp/atk/def/speed order: sending that rotated
   * the last three rows, and the Speed brace showed Sp.Def's value.
   */
  private fun forecastStatEntries(
      first: de.fiereu.openmmo.common.Pokemon,
      second: de.fiereu.openmmo.common.Pokemon,
  ): List<de.fiereu.openmmo.net.game.packets.BreedingStatEntry> {
    val a = with(first.iVs) { listOf(hp, atk, def, spAtk, spDef, spd) }
    val b = with(second.iVs) { listOf(hp, atk, def, spAtk, spDef, spd) }
    val bracedA = POWER_BRACES[first.heldItem]
    val bracedB = POWER_BRACES[second.heldItem]
    val bracedStats = setOfNotNull(bracedA, bracedB)
    val shinyPair = first.isShiny || first.isSecret || second.isShiny || second.isSecret
    val passSlots = ((if (shinyPair) 4 else 3) - bracedStats.size).coerceAtLeast(0)
    val forcedHigh = if (shinyPair) minOf(2, passSlots) else 0
    val freeStats = 6 - bracedStats.size
    val pPass = if (freeStats > 0) passSlots.toFloat() / freeStats else 0f
    val pForcedHigh = if (freeStats > 0) forcedHigh.toFloat() / freeStats else 0f
    val pNormalPass = pPass - pForcedHigh
    val pRoll = 1f - pPass

    fun contribution(value: Int, percent: Float, label: Int) =
        de.fiereu.openmmo.net.game.packets.BreedingStatContribution(
            value = value.toByte(),
            // Already percent-scaled - the client suffixes "%" without multiplying.
            percent = percent,
            labelStringId = label,
        )

    return List(6) { stat ->
      val braced = stat in bracedStats
      val hi = maxOf(a[stat], b[stat])
      val lo = minOf(a[stat], b[stat])
      val contributions =
          when {
            braced -> {
              // Pinned to the brace holder's value; both parents bracing the same stat is a
              // 50/50 between their values (usually the same item, possibly different IVs).
              val holders = buildList {
                if (stat == bracedA) add(a[stat])
                if (stat == bracedB) add(b[stat])
              }
              holders.map { contribution(it, 100.0f / holders.size, 0) }
            }
            hi == lo -> listOf(contribution(hi, 100.0f, 0))
            else ->
                buildList {
                  if (pForcedHigh + pNormalPass / 2 > 0f)
                      add(contribution(hi, (pForcedHigh + pNormalPass / 2) * 100f, HIGH_PASS))
                  if (pNormalPass > 0f) add(contribution(lo, pNormalPass / 2 * 100f, LOW_PASS))
                  // One line stands in for the whole uniform roll across lo..hi - the actual
                  // roll can land on any value in the range, endpoints included.
                  if (pRoll > 0f) add(contribution((lo + hi) / 2, pRoll * 100f, AVERAGE))
                }
          }
      de.fiereu.openmmo.net.game.packets.BreedingStatEntry(
          guaranteed = braced,
          // The item CAUSING the guarantee - any nonzero value renders "Guaranteed inheritance
          // due to {item}", so unbraced rows must send 0 (stat indices here showed as Poke
          // Balls: index 5 = the Safari Ball).
          braceItemId =
              when {
                stat == bracedA -> first.heldItem.toShort()
                stat == bracedB -> second.heldItem.toShort()
                else -> 0
              },
          contributions = contributions,
      )
    }
  }

  private fun emptyForecast(a: Long, b: Long) =
      BreedingForecastPacket(
          parentA = a,
          parentB = b,
          hasPreview = false,
          species = 0,
          form = 0,
          statEntries = emptyList(),
          possibleNatures = emptyList(),
          valueIds = emptyList(),
          valueSources = emptyList(),
          gender = 0,
          nature = 0,
          shiny = false,
          cost = 0,
          secondaryCost = 0,
      )

  private fun sendNotice(session: SessionContext, message: String) = session.send(notice(message))

  private companion object {
    const val DITTO = 132
    /** Retail's price for pinning the offspring's gender. */
    const val GENDER_CHOICE_COST = 5000

    /** Client string ids for the pass-outcome tooltip labels. */
    const val HIGH_PASS = 2541
    const val LOW_PASS = 2542
    const val AVERAGE = 2544

    /** Everstone ids in both held-item catalogs - the holder's nature is pinned on the baby. */
    val EVERSTONES = setOf(5229, 6229)

    /**
     * Client item id of each Power brace to the wire stat index it pins (hp, atk, def, spd, spAtk,
     * spDef). Ids are the 5xxx held-item catalog; the 6xxx duplicates map identically. The
     * Everstone (5229/6229) braces the NATURE, which the forecast does not display yet.
     */
    val POWER_BRACES =
        mapOf(
            // Indices are RC0.Df0 order: hp, atk, def, spAtk, spDef, SPEED.
            5294 to 0,
            6294 to 0, // Power Weight - HP
            5289 to 1,
            6289 to 1, // Power Bracer - Attack
            5290 to 2,
            6290 to 2, // Power Belt - Defense
            5291 to 3,
            6291 to 3, // Power Lens - Sp. Attack
            5292 to 4,
            6292 to 4, // Power Band - Sp. Defense
            5293 to 5,
            6293 to 5, // Power Anklet - Speed
        )
  }
}
