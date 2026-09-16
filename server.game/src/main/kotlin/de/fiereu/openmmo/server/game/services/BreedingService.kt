package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.EggGroup
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.PokemonRarityFlag
import de.fiereu.openmmo.net.game.packets.AssignBreedingSlotPacket
import de.fiereu.openmmo.net.game.packets.BreedingForecastPacket
import de.fiereu.openmmo.net.game.packets.SubmitBreedingPartyPacket
import de.fiereu.openmmo.server.game.battle.Gender
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
 * Pairing rules (operator-specified): the FEMALE determines the offspring's family and the baby is
 * the family's youngest form; a Ditto substitutes for either parent; males just need a shared egg
 * group; shinies only breed with shinies, while an alpha MAY pair with a non-alpha (the client allows
 * it and warns the baby will not be an alpha). OT attribution: shiny babies
 * carry your OT only from a mother with your OT (else Unknown OT); non-shiny babies always carry
 * your name, starred when the species will not register in the Pokedex. The Breed button CONSUMES
 * both parents and files the egg the forecast previewed into the incubator container.
 */
@Singleton
class BreedingService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry,
    private val wildMons: de.fiereu.openmmo.server.game.battle.WildMonFactory,
    private val dialog: DialogService,
) {

  /** The last assigned pair per character, so held-item changes can refresh the open window. */
  private val activePairs = java.util.concurrent.ConcurrentHashMap<Long, AssignBreedingSlotPacket>()

  fun onAssignSlot(event: PacketEvent<AssignBreedingSlotPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    activePairs[charId] = event.packet
    sendForecast(event.session, charId, event.packet)
  }

  /**
   * Re-sends the current forecast when [monId]'s held item changed while its breed window is open -
   * the window only repaints on a forecast, so without this a removed brace stayed on screen
   * (operator-reported). A stale re-send after the window closed is harmless: the client's handler
   * no-ops without an open window.
   */
  fun refreshAfterHeldItemChange(session: SessionContext, charId: Long, monId: Long) {
    val pair = activePairs[charId] ?: return
    if (monId != pair.ownPokemonEntityId && monId != pair.partnerPokemonEntityId) return
    sendForecast(session, charId, pair)
  }

  private fun sendForecast(session: SessionContext, charId: Long, p: AssignBreedingSlotPacket) {
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
    val defA = speciesRegistry.forMonster(first)
    val defB = speciesRegistry.forMonster(second)
    val incompatible = incompatibilityReason(first, second, defA, defB)
    if (defA == null || defB == null || incompatible != null) {
      log.info {
        "Breeding pair rejected: char=$charId reason=${incompatible ?: "unknown species"}"
      }
      session.send(emptyForecast(p.ownPokemonEntityId, p.partnerPokemonEntityId))
      return
    }
    // The FEMALE determines the offspring's family (with a Ditto, the non-Ditto parent does,
    // whatever its gender), and the baby is always the family's YOUNGEST form.
    val mother =
        when {
          isDitto(first) -> second
          isDitto(second) -> first
          genderOf(second, defB) == FEMALE -> second
          else -> first
        }
    val offspringWire = EvolutionTable.baseForm(clientSpeciesId(mother.dexId))
    // The OT line (e30 via the 'gender' byte: 0 = your name in green, 1 = your name with a *,
    // 2 = Unknown OT). Shiny pairs only carry your OT when the mother is yours - otherwise the
    // baby hatches with an Unknown OT (no laundering a family you never shiny-hunted). Non-shiny
    // babies always get your name; the * marks a species that will NOT register in the Pokedex
    // (not caught-as-OT and not bred down from a mother with your OT).
    val playerName = stored.info.name
    val caughtAsOt =
        owned.asSequence().filter { it.ot == playerName }.map { clientSpeciesId(it.dexId) }.toSet()
    val otByte: Byte =
        if (first.isShiny || first.isSecret) {
          if (mother.ot == playerName) OT_SELF else OT_UNKNOWN
        } else {
          if (offspringWire in caughtAsOt || mother.ot == playerName) OT_SELF else OT_STARRED
        }
    // The nature line: an EMPTY possibleNatures list renders "???" (a random roll - the earlier
    // crash blamed on this list was really the statEntries indexing). An Everstone holder pins
    // their nature; both holding one is a 50/50 between theirs.
    val pinnedNatures = buildList {
      if (first.heldItem in EVERSTONES) add(first.nature.ordinal.toByte())
      if (second.heldItem in EVERSTONES) add(second.nature.ordinal.toByte())
    }
    // The offspring's appearance flags, in the record's own bits (PokemonRarityFlag / client
    // dl6.D20). The window builds a baby record from these and compares it against BOTH parents:
    // whatever it finds set on a parent but clear on the child it lists as a trait that will not
    // be inherited (strings 2521-2524, gathered by f/yb3.run). Sending a flat 0 made it announce
    // "It will not be an Alpha because one of the parents is not an Alpha" for a pair of alphas.
    // The client's own wording gives the rules: an Alpha needs BOTH parents Alpha, a Hidden
    // Ability comes from the MAIN parent only (2521), and Fateful Encounter never passes (2523).
    val offspringFlags = offspringRarityFlags(first, second, mother)
    session.send(
        BreedingForecastPacket(
            parentA = p.ownPokemonEntityId,
            parentB = p.partnerPokemonEntityId,
            hasPreview = true,
            species = offspringWire.toShort(),
            form = 0,
            // The renderer walks its six stat constants and indexes THIS array by stat id
            // directly (pM1.Cm0 line 99: statEntries[stat.Df0]) - it must always hold one
            // entry per stat, in RC0.Df0 order: hp, atk, def, SPEED, spAtk, spDef. The
            // inheritance model behind the rows is documented on forecastStatEntries.
            statEntries = forecastStatEntries(first, second),
            possibleNatures = pinnedNatures.distinct(),
            valueIds = emptyList(),
            valueSources = emptyList(),
            // This byte is the OT-attribution enum (e30), NOT a gender - the old genderPref echo
            // is what rendered "Unknown OT" for everyone (pref -1 indexed past e30.Mf).
            gender = otByte,
            // Capture-mislabeled TWICE over: this short is the offspring's APPEARANCE FLAGS
            // bitfield (k91.Zl1 - bit 0 shiny, bit 3 secret, more for alpha; -1 rendered a
            // secret shiny alpha, operator-verified). See offspringFlags above.
            nature = offspringFlags,
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

  /**
   * The Breed button. Owner's design (2026-09-16): the two parents are CONSUMED - the window warns
   * "You wont get these Pokemon back" - and the egg the forecast previewed is placed into the
   * incubator container exactly as shown. A baby that is not already shiny then takes one roll at
   * the server's egg shiny rate (see [eggShinyDenominator]).
   */
  suspend fun onSubmit(event: PacketEvent<SubmitBreedingPartyPacket>) {
    val session = event.session
    val p = event.packet
    val charId = session.attributes[PLAYER_STATE]?.characterId ?: return
    activePairs.remove(charId)
    // The window closed through THIS packet, not a dialog answer, so the daycare script is still
    // suspended on its choice. Release it first, before any early return below, or the player is
    // left standing in scripted state with movement locked.
    dialog.completePendingChoice(session)
    val stored = characterStore.getCharacter(charId) ?: return
    val owned = stored.pokemon + stored.pcStorage
    val ids = p.pokemonEntityIds.distinct()
    val first = ids.getOrNull(0)?.let { id -> owned.firstOrNull { it.id == id } }
    val second = ids.getOrNull(1)?.let { id -> owned.firstOrNull { it.id == id } }
    if (ids.size != 2 || first == null || second == null) {
      log.info { "Breeding submit with unknown parents: char=$charId mons=${p.pokemonEntityIds}" }
      return
    }
    val defA = speciesRegistry.forMonster(first)
    val defB = speciesRegistry.forMonster(second)
    val incompatible = incompatibilityReason(first, second, defA, defB)
    if (defA == null || defB == null || incompatible != null) {
      log.info { "Breeding submit rejected: char=$charId reason=${incompatible ?: "unknown species"}" }
      sendNotice(session, "These two cannot be bred together.")
      return
    }
    // Never file an egg past the slots the character has actually unlocked - the client paints the
    // rest "not yet unlocked" and the egg would be invisible in one of them.
    val unlocked = Incubators.unlockedSlots(stored.storyFlags)
    val eggs = stored.incubator
    val freeSlot = (0 until unlocked).firstOrNull { slot -> eggs.none { it.containerSlot.toInt() == slot } }
    if (freeSlot == null) {
      log.info { "Breeding submit with no free incubator: char=$charId unlocked=$unlocked eggs=${eggs.size}" }
      sendNotice(session, if (unlocked == 0) "You have no egg incubators yet." else "All your egg incubators are full.")
      return
    }

    val mother =
        when {
          isDitto(first) -> second
          isDitto(second) -> first
          genderOf(second, defB) == FEMALE -> second
          else -> first
        }
    val offspringWire = EvolutionTable.baseForm(clientSpeciesId(mother.dexId))
    // EvolutionTable speaks the client's wire ids; the factory wants a server id. For every retail
    // family those are the same number, and an Expansion-only base form is the offset copy.
    val babyServerId =
        listOf(offspringWire, de.fiereu.openmmo.common.EXPANSION_SERVER_SPECIES_BASE + offspringWire)
            .firstOrNull { speciesRegistry.get(it) != null && clientSpeciesId(it) == offspringWire }
            ?: offspringWire

    val flags = offspringRarityFlags(first, second, mother).toInt()
    val inheritsShiny = PokemonRarityFlag.SHINY.isSet(flags)
    val rng = de.fiereu.openmmo.server.game.battle.BattleRng()
    // A baby that did not inherit shininess still gets its own roll, at the egg rate.
    val donatorActive = (characterStore.donatorUntil(stored.info.userId) ?: 0L) > System.currentTimeMillis() / 1000
    val shinyDenominator = if (inheritsShiny) 0 else eggShinyDenominator(stored)
    val hints =
        de.fiereu.openmmo.server.game.battle.WildRollHints(
            nature = pinnedNature(first, second),
            gender = chosenGender(p.slotIndex),
        )
    val base = wildMons.create(babyServerId, EGG_LEVEL, rng, shinyDenominator, hints)
    if (base == null) {
      log.error { "Breeding submit could not build species $babyServerId for char=$charId" }
      sendNotice(session, "Something went wrong with that pair.")
      return
    }
    val playerName = stored.info.name
    val caughtAsOt =
        owned.asSequence().filter { it.ot == playerName }.map { clientSpeciesId(it.dexId) }.toSet()
    val unknownOt =
        (first.isShiny || first.isSecret) && mother.ot != playerName
    val egg =
        base.copy(
            ownerId = charId,
            container = PokemonContainer.INCUBATOR,
            containerSlot = freeSlot.toShort(),
            ot = if (unknownOt) "" else playerName,
            iVs = rollOffspringIVs(first, second, rng),
            isEgg = true,
            isShiny = base.isShiny || inheritsShiny,
            isSecret = PokemonRarityFlag.SECRET_SHINY.isSet(flags),
            isAlpha = PokemonRarityFlag.ALPHA.isSet(flags),
            hasHiddenAbility = PokemonRarityFlag.HIDDEN_ABILITY.isSet(flags),
        )

    // The parents go first: if one of them cannot be removed, no egg is created, so a failure can
    // never mint a monster out of nothing.
    if (!characterStore.removeAnyPokemon(charId, first.id)) {
      log.error { "Breeding could not consume parent ${first.id} for char=$charId" }
      sendNotice(session, "Something went wrong with that pair.")
      return
    }
    if (!characterStore.removeAnyPokemon(charId, second.id)) {
      log.error { "Breeding consumed ${first.id} but not ${second.id} for char=$charId" }
      sendNotice(session, "Something went wrong with that pair.")
      return
    }
    if (!characterStore.addPokemon(charId, egg)) {
      log.error { "Breeding consumed both parents but could not store the egg for char=$charId" }
      sendNotice(session, "Something went wrong with that pair.")
      return
    }
    // Eggs hatch on a TIMER, not on steps (see Incubators): a Flame Body or Magma Armor sitting in
    // the incubator's own slot and Donator Status both shorten it, and the two stack. The slot
    // itself is not modelled yet, so that term is false here.
    val helper = stored.hatchHelper?.let { speciesRegistry.forMonster(it) }
    val helperSpeedsHatching =
        helper != null && (helper.ability1 in Incubators.HATCH_ABILITIES || helper.ability2 in Incubators.HATCH_ABILITIES)
    val hatchIn =
        Incubators.hatchSeconds(
            eggCycles = speciesRegistry.get(babyServerId)?.eggCycles ?: DEFAULT_EGG_CYCLES,
            flameBody = helperSpeedsHatching,
            donator = donatorActive,
        )
    // Egg timers run on PLAY TIME so they pause while the player is offline (owner, 2026-09-16).
    characterStore.bankPlayTime(charId)
    val playTime = characterStore.getCharacter(charId)?.info?.playTimeSeconds ?: stored.info.playTimeSeconds
    characterStore.setStoryVar(charId, Incubators.hatchVarKey(freeSlot), playTime + hatchIn)
    characterStore.flushCharacterAsync(charId)
    log.info {
      "Bred char=$charId ${first.dexId}+${second.dexId} -> egg ${egg.dexId} slot=$freeSlot " +
          "shiny=${egg.isShiny}${if (!inheritsShiny && egg.isShiny) " (rolled 1 in $shinyDenominator)" else ""} " +
          "alpha=${egg.isAlpha} ha=${egg.hasHiddenAbility} ivs=${egg.iVs.total} hatchIn=${hatchIn}s"
    }
    resendContainers(session, charId)
    if (!inheritsShiny && egg.isShiny) sendNotice(session, "The egg is shining...")
  }

  /** Party, PC and incubators after a breed: two parents left and an egg arrived. */
  private fun resendContainers(session: SessionContext, charId: Long) {
    val after = characterStore.getCharacter(charId) ?: return
    de.fiereu.openmmo.net.game.packets
        .containerPackets(PokemonContainer.PARTY, after.pokemon)
        .forEach { session.send(it) }
    de.fiereu.openmmo.net.game.packets
        .containerPackets(PokemonContainer.PC, after.boxed)
        .forEach { session.send(it) }
    de.fiereu.openmmo.net.game.packets
        .containerPackets(PokemonContainer.INCUBATOR, after.incubator)
        .forEach { session.send(it) }
  }

  /** An Everstone holder pins the baby's nature; both holding one is a coin flip between them. */
  private fun pinnedNature(
      first: de.fiereu.openmmo.common.Pokemon,
      second: de.fiereu.openmmo.common.Pokemon,
  ): de.fiereu.openmmo.common.enums.PokemonNature? {
    val holders = buildList {
      if (first.heldItem in EVERSTONES) add(first.nature)
      if (second.heldItem in EVERSTONES) add(second.nature)
    }
    return holders.randomOrNull()
  }

  /** The window's gender chooser: the forecast prices male and female separately. */
  private fun chosenGender(preference: Byte): Byte? =
      when (preference.toInt()) {
        MALE -> MALE.toByte()
        FEMALE -> FEMALE.toByte()
        else -> null
      }

  /**
   * The egg's own shiny odds, 1 in this many, for a baby that did not inherit shininess.
   *
   * Base rate is [eggShinyRate] (override with -Dmonmmo.eggShinyRate; 0 turns egg shinies off).
   * Donator Status improves it by the same 10% the client advertises in its own donator blurb
   * (string 4001), and a running Shiny Charm adds [SHINY_CHARM_BONUS] - the charm is a timed Boost
   * Item (see [Boosts]), consumed on use and active for an hour, NOT something merely held.
   */
  private suspend fun eggShinyDenominator(stored: de.fiereu.openmmo.server.game.storage.StoredCharacter): Int {
    val base = eggShinyRate
    if (base <= 0) return 0
    var multiplier = 1.0
    val donatorUntil = characterStore.donatorUntil(stored.info.userId) ?: 0L
    if (donatorUntil > System.currentTimeMillis() / 1000) multiplier += DONATOR_SHINY_BONUS
    if (Boosts.isActive(stored, Boosts.Kind.SHINY)) multiplier += SHINY_CHARM_BONUS
    return (base / multiplier).toInt().coerceAtLeast(1)
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
   * ROW ORDER: the renderer indexes statEntries[RC0.Df0], and Df0 is the ENUM order hp 0, atk 1,
   * def 2, SPEED 3, spAtk 4, spDef 5 - the GBA order, play-verified twice via which row the Anklet
   * brace landed on. (RC0 carries a SECOND index, nw, with Speed LAST - the on-screen row order -
   * which was briefly mistaken for Df0.) The IV word's 5-bit groups follow the same Df0 order.
   */
  private fun forecastStatEntries(
      first: de.fiereu.openmmo.common.Pokemon,
      second: de.fiereu.openmmo.common.Pokemon,
  ): List<de.fiereu.openmmo.net.game.packets.BreedingStatEntry> {
    val a = with(first.iVs) { listOf(hp, atk, def, spd, spAtk, spDef) }
    val b = with(second.iVs) { listOf(hp, atk, def, spd, spAtk, spDef) }
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

  /**
   * The pairing rules (operator-specified): shinies only breed with shinies, alphas only with
   * alphas, a Ditto pairs with anything breedable except another Ditto, and otherwise the pair
   * needs one female and one male sharing an egg group. Returns a log-worthy reason, or null when
   * the pair can breed.
   */
  private fun incompatibilityReason(
      a: de.fiereu.openmmo.common.Pokemon,
      b: de.fiereu.openmmo.common.Pokemon,
      defA: de.fiereu.openmmo.pokemon.SpeciesDef?,
      defB: de.fiereu.openmmo.pokemon.SpeciesDef?,
  ): String? {
    if (defA == null || defB == null) return "unknown species"
    val aDitto = isDitto(a)
    val bDitto = isDitto(b)
    if (aDitto && bDitto) return "two Dittos"
    if ((a.isShiny || a.isSecret) != (b.isShiny || b.isSecret)) return "shiny with non-shiny"
    // Alpha with non-alpha is LEGAL to the client: it shows string 2522 ("It will not be an Alpha
    // ... because one of the parents is not an Alpha") as a warning and still lets the pair breed.
    // We used to refuse it, so the window offered a pair we rejected (owner agreed 2026-09-16).
    fun groups(def: de.fiereu.openmmo.pokemon.SpeciesDef) =
        setOf(def.eggGroup1, def.eggGroup2) - EggGroup.NONE
    if (!aDitto && EggGroup.NO_EGGS_DISCOVERED in groups(defA)) return "${defA.name} cannot breed"
    if (!bDitto && EggGroup.NO_EGGS_DISCOVERED in groups(defB)) return "${defB.name} cannot breed"
    if (aDitto || bDitto) return null
    val genders = setOf(genderOf(a, defA), genderOf(b, defB))
    if (genders != setOf(FEMALE, MALE)) return "no female+male pair (genders $genders)"
    if ((groups(defA) intersect groups(defB)).isEmpty()) return "no shared egg group"
    return null
  }

  /** The client's gender derivation, shared with battles through [Gender]. */
  private fun genderOf(
      mon: de.fiereu.openmmo.common.Pokemon,
      def: de.fiereu.openmmo.pokemon.SpeciesDef
  ): Int =
      when (Gender.of(def.genderRatio, mon.seed)) {
        Gender.FEMALE -> FEMALE
        Gender.GENDERLESS -> GENDERLESS
        else -> MALE
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

  companion object {
    const val DITTO = 132

    /**
     * Ditto by CLIENT WIRE id - expansion-imported monsters carry offset server dex ids (an
     * imported Ditto arrived as 65668 = 0x10000 + 132), so the raw dexId compare missed them and
     * the Ditto fell through to the genderless no-pair rejection.
     */
    fun isDitto(mon: de.fiereu.openmmo.common.Pokemon): Boolean =
        de.fiereu.openmmo.common.clientSpeciesId(mon.dexId) == DITTO

    /** Retail's price for pinning the offspring's gender. */
    const val GENDER_CHOICE_COST = 5000

    /** Eggs hatch at level 1. */
    const val EGG_LEVEL = 1

    /** Fallback when a species record carries no egg cycles; the common value in the data. */
    const val DEFAULT_EGG_CYCLES = 20

    /** Base egg shiny odds, 1 in this many; -Dmonmmo.eggShinyRate overrides, 0 turns them off. */
    val eggShinyRate: Int =
        System.getProperty("monmmo.eggShinyRate")?.toIntOrNull()?.coerceAtLeast(0) ?: 30_000

    /** Donator Status, in the client's own words (string 4001): +10% shiny chance. */
    const val DONATOR_SHINY_BONUS = 0.10

    /** A running Shiny Charm, the 5% the client's own item text quotes for it. */
    const val SHINY_CHARM_BONUS = 0.05

    /** Client string ids for the pass-outcome tooltip labels. */
    const val HIGH_PASS = 2541
    const val LOW_PASS = 2542
    const val AVERAGE = 2544

    /** Everstone ids in both held-item catalogs - the holder's nature is pinned on the baby. */
    val EVERSTONES = setOf(5229, 6229)

    /** f/gT0.Ug1 gender values. */
    const val MALE = 0
    const val FEMALE = 1
    const val GENDERLESS = -1

    /** e30 OT-attribution bytes (Mf indices; anything else renders "Unknown OT"). */
    const val OT_SELF: Byte = 0
    const val OT_STARRED: Byte = 1
    const val OT_UNKNOWN: Byte = 2

    /**
     * Client item id of each Power brace to the wire stat index it pins (hp, atk, def, spd, spAtk,
     * spDef). Ids are the 5xxx held-item catalog; the 6xxx duplicates map identically. The
     * Everstone (5229/6229) braces the NATURE, which the forecast does not display yet.
     */
    val POWER_BRACES =
        mapOf(
            // Indices are RC0.Df0 order: hp, atk, def, SPEED, spAtk, spDef.
            5294 to 0,
            6294 to 0, // Power Weight - HP
            5289 to 1,
            6289 to 1, // Power Bracer - Attack
            5290 to 2,
            6290 to 2, // Power Belt - Defense
            5293 to 3,
            6293 to 3, // Power Anklet - Speed
            5291 to 4,
            6291 to 4, // Power Lens - Sp. Attack
            5292 to 5,
            6292 to 5, // Power Band - Sp. Defense
        )
  }
}

/**
 * The offspring's appearance flags for the breed window, in the record's own bits
 * ([PokemonRarityFlag], client `dl6.D20`).
 *
 * The window builds a baby record from this short and compares it against BOTH parents: every trait
 * set on a parent but clear on the child is listed as one that will not be inherited (client
 * strings 2521-2524, gathered in f/yb3.run), under "Are you sure you want to continue?". Sending a
 * flat zero therefore told a pair of alphas "It will not be an Alpha because one of the parents is
 * not an Alpha" (owner-reported 2026-09-16).
 *
 * The rules come from the client's own wording:
 * - Alpha passes only when BOTH parents are alphas (2522).
 * - A Hidden Ability comes from the MAIN parent alone - the one whose family the baby takes (2521).
 * - Fateful Encounter never passes (2523), and neither does the raid mark.
 * - Shininess has no "will not inherit" string, and the pairing rules already require both parents
 *   to be shiny, so a shiny pair breeds a shiny; the rarer secret variant needs both parents secret.
 */
internal fun offspringRarityFlags(
    first: de.fiereu.openmmo.common.Pokemon,
    second: de.fiereu.openmmo.common.Pokemon,
    mother: de.fiereu.openmmo.common.Pokemon,
): Short {
  val bothShiny = (first.isShiny || first.isSecret) && (second.isShiny || second.isSecret)
  return ((if (bothShiny) PokemonRarityFlag.SHINY.mask else 0) or
          (if (first.isSecret && second.isSecret) PokemonRarityFlag.SECRET_SHINY.mask else 0) or
          (if (first.isAlpha && second.isAlpha) PokemonRarityFlag.ALPHA.mask else 0) or
          (if (mother.hasHiddenAbility) PokemonRarityFlag.HIDDEN_ABILITY.mask else 0))
      .toShort()
}

/**
 * The baby's actual IVs, rolling the model the forecast describes (see
 * `BreedingService.forecastStatEntries`): a Power brace pins its stat to the holder's value and
 * uses up a pass slot, a shiny pair passes four stats instead of three with two of them forced to
 * the better parent, every other pass is a coin flip between the parents, and a stat that is not
 * passed rolls anywhere in the parents' min-to-max range, endpoints included.
 *
 * Stat order is RC0.Df0, the order the forecast rows use: hp, atk, def, SPEED, spAtk, spDef.
 */
internal fun rollOffspringIVs(
    first: de.fiereu.openmmo.common.Pokemon,
    second: de.fiereu.openmmo.common.Pokemon,
    rng: de.fiereu.openmmo.server.game.battle.BattleRng,
): de.fiereu.openmmo.common.enums.IVs {
  val a = with(first.iVs) { listOf(hp, atk, def, spd, spAtk, spDef) }
  val b = with(second.iVs) { listOf(hp, atk, def, spd, spAtk, spDef) }
  val bracedA = BreedingService.POWER_BRACES[first.heldItem]
  val bracedB = BreedingService.POWER_BRACES[second.heldItem]
  val bracedStats = setOfNotNull(bracedA, bracedB)
  val shinyPair = first.isShiny || first.isSecret || second.isShiny || second.isSecret
  val passSlots = ((if (shinyPair) 4 else 3) - bracedStats.size).coerceAtLeast(0)
  val forcedHigh = if (shinyPair) minOf(2, passSlots) else 0
  val free = (0 until 6).filter { it !in bracedStats }.shuffled()
  val passed = free.take(passSlots).toSet()
  val forced = free.take(forcedHigh).toSet()
  val values = IntArray(6)
  for (stat in 0 until 6) {
    val hi = maxOf(a[stat], b[stat])
    val lo = minOf(a[stat], b[stat])
    values[stat] =
        when {
          stat in bracedStats -> {
            val holders = buildList {
              if (stat == bracedA) add(a[stat])
              if (stat == bracedB) add(b[stat])
            }
            holders[rng.pick(holders.size)]
          }
          stat in forced -> hi
          stat in passed -> if (rng.coinFlip()) a[stat] else b[stat]
          else -> lo + rng.pick(hi - lo + 1)
        }
  }
  return de.fiereu.openmmo.common.enums.IVs().apply {
    hp = values[0]
    atk = values[1]
    def = values[2]
    spd = values[3]
    spAtk = values[4]
    spDef = values[5]
  }
}
