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
    // shininessTypes was labeled correctly by the captures (operator-confirmed): its bytes are
    // f/ns0 SHININESS VARIANT ids - the enum's entries carry color pairs and particle refs
    // (normal/shiny/secret each with a tint and sparkle), and retail lists the pairing's
    // possible outcomes here (shiny x shiny etc). The renderer indexes [0] unconditionally,
    // so an empty list crashed it; until shininess rules exist, the one outcome is variant 0.
    session.send(
        BreedingForecastPacket(
            parentA = p.ownPokemonEntityId,
            parentB = p.partnerPokemonEntityId,
            hasPreview = true,
            species = clientSpeciesId(offspringDex).toShort(),
            form = 0,
            // The renderer walks its six stat constants and indexes THIS array by stat id
            // directly (pM1.Cm0 line 99: statEntries[stat.Df0]) - it must always hold one
            // entry per stat. Inheritance contributions come with the real breeding rules.
            statEntries =
                List(6) {
                  de.fiereu.openmmo.net.game.packets.BreedingStatEntry(
                      guaranteed = false,
                      statId = it.toShort(),
                      contributions = emptyList(),
                  )
                },
            shininessTypes = listOf(0),
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

  private fun emptyForecast(a: Long, b: Long) =
      BreedingForecastPacket(
          parentA = a,
          parentB = b,
          hasPreview = false,
          species = 0,
          form = 0,
          statEntries = emptyList(),
          shininessTypes = emptyList(),
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
  }
}
