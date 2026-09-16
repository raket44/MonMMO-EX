package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.net.game.packets.LocalPlayerStatePacket
import de.fiereu.openmmo.net.game.packets.PokemonContainerPacket
import de.fiereu.openmmo.net.game.packets.containerPackets
import de.fiereu.openmmo.net.game.packets.StoryFlagUpdatePacket
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the client its story, party and bag state. Story vars have no incremental packet, so
 * anything that rewrites them has to send this whole block again.
 */
@Singleton
class WorldStateService @Inject constructor(
    private val dexProgress: DexProgressService,
    private val tracker: EncounterTrackerService? = null,
) {

  /**
   * Set [fullVars] when this is a resync rather than a login, so vars that dropped back to 0 are
   * sent as 0 instead of being left off and read as their old value.
   */
  fun send(ctx: SessionContext, stored: StoredCharacter, fullVars: Boolean = false) {
    // The table must land before any monster or follower is built. Without it the table stays null
    // and the client crashes constructing a party monster that reads a flag.
    // The "world flag table" turned out to be the Pokedex seen/owned/OT tiers; a captured hex
    // dump of one early session used to be replayed here for every character. Real progress now.
    ctx.send(dexProgress.resetPacket(stored))
    ctx.send(localPlayerState(stored, fullVars))
    val setFlags = StoryClientState.flags(stored.info.positionRegionId, stored.storyFlags)
    if (fullVars) {
      // A full re-sync must also CLEAR what the client already mirrors: /story reset used to
      // send only the set flags, so the badges a wiped save no longer held stayed lit
      // client-side (badge HUD, level cap) until relog. Explicit zeroes first, truth after.
      val region = stored.info.positionRegionId.toInt()
      val keep = setFlags.map { it.flagId }.toSet()
      ClientStoryWhitelist.ids(region)
          .filter { it !in keep }
          .sorted()
          .forEach {
            ctx.send(StoryFlagUpdatePacket(stored.info.positionRegionId, it, enabled = false))
          }
    }
    setFlags.forEach { ctx.send(it) }
    // A Hall of Fame entry keeps the encounter counter unlocked on every later login (HallOfFame),
    // and carries the five permanent egg incubators it unlocked (Incubators).
    if (HallOfFame.FLAG in stored.storyFlags) {
      ctx.send(HallOfFame.encounterCounterPacket())
      ctx.send(Incubators.firstChampionPacket())
    }
    if (Incubators.MET_DAYCARE_MAN in stored.storyFlags) ctx.send(Incubators.daycareManPacket())

    val containers =
        mapOf(
            PokemonContainer.PARTY to stored.pokemon,
            PokemonContainer.PC to stored.boxed,
            PokemonContainer.BATTLE_BOX_1 to emptyList(),
            PokemonContainer.BATTLE_BOX_2 to emptyList(),
            PokemonContainer.DAYCARE to emptyList(),
            PokemonContainer.INCUBATOR to stored.incubator,
            PokemonContainer.UNKNOWN_14 to emptyList(),
        )
    // Split under the client's inflate buffer (containerPackets): a big PC in one packet garbles
    // every compressed packet after it, bag included.
    for ((container, pokemon) in containers) {
      containerPackets(container, pokemon).forEach { p -> ctx.send(p) }
    }

    // The real server sends the bag stacks interleaved with the containers, so the client has the
    // items before entering the world.
    storyItemStacksPackets(stored.items).forEach { p -> ctx.send(p) }
  }

  /**
   * Re-push just the local-player state, mid-session. The client re-applies it wholesale
   * (f.Za0.X91), which is the only channel that updates the per-item unlock flags (Gc0.tv0) -
   * without this, a cosmetic granted mid-session sits in the bag but stays locked in the
   * customization dialog until relog.
   */
  fun refreshUnlocks(ctx: SessionContext, stored: StoredCharacter) {
    ctx.send(localPlayerState(stored, fullVars = false))
  }

  // Missing it leaves player state uninitialised and the client crashes reading it, for example
  // when opening the battle bag.
  private fun localPlayerState(
      stored: StoredCharacter,
      fullVars: Boolean,
  ): LocalPlayerStatePacket {
    val info = stored.info
    val partyDex = stored.pokemon.map { clientSpeciesId(it.dexId).toShort() }
    return LocalPlayerStatePacket(
        region = info.positionRegionId,
        mapId = info.positionMapId.toShort(),
        moveSpeed = 0.05f,
        x = info.positionX,
        y = info.positionY,
        z = 0,
        money = info.money,
        gender = info.rivalSex,
        skinTone = 0,
        hairColor = 0,
        playtime = 0.0,
        flags = 0,
        partyDex = partyDex,
        partyForms = partyDex.map { 0.toByte() },
        // f/Za0 keeps these as f/Ob1.Ls / r5: the counts the Pokedex and trainer card show.
        pokedexSeen = DexProgressService.seenWireIds(stored).sorted().map { it.toShort() },
        pokedexCaught = DexProgressService.ownedWireIds(stored).sorted().map { it.toShort() },
        badges = emptyList(),
        variables = StoryClientState.itemUnlocks(stored.items),
    )
  }
}
