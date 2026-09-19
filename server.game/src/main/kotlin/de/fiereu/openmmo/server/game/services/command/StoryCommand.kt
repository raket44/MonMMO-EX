package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.maps.WarpTile
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import de.fiereu.openmmo.server.game.services.WarpService
import de.fiereu.openmmo.server.game.services.WorldStateService
import de.fiereu.openmmo.server.game.services.CLIENT_BICYCLE_ITEM
import de.fiereu.openmmo.server.game.services.DUPLICATE_BICYCLE_ITEM
import de.fiereu.openmmo.server.game.services.REGIONAL_BIKE_ITEMS
import de.fiereu.openmmo.server.game.services.itemStackUpdatePacket
import de.fiereu.openmmo.server.game.services.storyItemStacksPackets
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.NewGameStarts
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryCommand
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val worldStateService: WorldStateService,
    private val storyPlayerService: StoryPlayerService,
    private val warpService: WarpService,
    private val battleService: BattleService,
    private val items: ItemRegistry,
    private val mapManager: de.fiereu.openmmo.maps.MapManager,
) : ChatCommand {
  override val name = "story"
  override val usage = "/story [checkpoint|reset|reset keep]"

  private val ALL_BIKE_ITEMS = REGIONAL_BIKE_ITEMS + CLIENT_BICYCLE_ITEM + DUPLICATE_BICYCLE_ITEM
  override val description = "jumps to a story scene, or lists the scenes with no argument"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val wanted = ctx.args.firstOrNull()
    if (wanted == null) {
      KANTO_CHECKPOINTS.forEach { ctx.reply("${it.name} - ${it.description}") }
      ctx.reply("reset - starts the region's story over, keeping money and the counters")
      ctx.reply("reset keep - the same, but your party, PC and bag stay as they are")
      return
    }
    // A jump warps out from under a parked script or a running battle, and neither recovers.
    if (ctx.state.blocksNewScript) {
      ctx.reply("Finish what you are talking to first.")
      return
    }
    if (battleService.inBattle(ctx.characterId)) {
      ctx.reply("Finish the battle first.")
      return
    }
    if (wanted.equals("reset", ignoreCase = true)) {
      reset(ctx, keepBuild = ctx.args.getOrNull(1).equals("keep", ignoreCase = true))
      return
    }
    val checkpoint = KANTO_CHECKPOINTS.find { it.name.equals(wanted, ignoreCase = true) }
    if (checkpoint == null) {
      ctx.reply("No checkpoint called $wanted. Run /story for the list.")
      return
    }
    apply(ctx, checkpoint)
  }

  private fun reset(ctx: CommandContext, keepBuild: Boolean) {
    val charId = ctx.characterId
    val stored = characterStore.getCharacter(charId) ?: return
    val region = Region.byWireValue(stored.info.positionRegionId)
    if (region == null) {
      ctx.reply("That region has no known start, so there is nothing to reset to.")
      return
    }
    val female = stored.info.rivalSex == CharacterGender.FEMALE.wireValue
    val start = NewGameStarts.forRegion(region, female)
    val ns = NewGameStarts.namespace(region)

    // Every bike quest is reset with the flags, so every bike goes with them even when the bag is
    // kept: the login reclaim only runs at login, and a reset made mid-session left the Bicycle
    // in the bag until the next relog (2026-09-08).
    val keptItems = stored.items.filterKeys { it !in ALL_BIKE_ITEMS }
    val takenBikes = stored.items.keys.filter { it in ALL_BIKE_ITEMS }
    characterStore.replaceProgress(
        characterId = charId,
        party = if (keepBuild) stored.pokemon.toList() else emptyList(),
        items = if (keepBuild) keptItems else emptyMap(),
        // Only THIS region restarts: the other regions' progress (and their untouched opening
        // state) stays - a Johto reset must not wipe a finished Kanto (2026-09-19).
        storyFlags = stored.storyFlags.filterNot { it.startsWith(ns) }.toSet() + start.storyFlags,
        storyVars = stored.storyVars.filterKeys { !it.startsWith(ns) } + start.storyVars,
        pc = if (keepBuild) stored.pcStorage.toList() else emptyList(),
    )
    // Hoenn's opening reads the dynamic warp on its way out of the truck.
    characterStore.setDynamicWarp(charId, start.dynamicWarp)

    val refreshed = characterStore.getCharacter(charId) ?: return
    worldStateService.send(ctx.session, refreshed, fullVars = true)
    // The bag as it now is, then a zero stack for each bike so an open bag drops it at once.
    storyItemStacksPackets(refreshed.items).forEach { p -> ctx.session.send(p) }
    for (itemId in takenBikes) ctx.session.send(itemStackUpdatePacket(itemId, 0))
    if (mapManager.getMap(region.wireValue.toInt(), start.bankId.toInt() and 0xFF, start.mapId.toInt() and 0xFF) == null) {
      // A DS bedroom is ROM-rendered (no MapDef): the same raw warp the ferry uses to start the
      // region. Banks above 127 (Nuvema 135, Twinleaf 159) are stored as negative bytes.
      warpService.executeRawWarp(
          ctx.session, charId, region.wireValue.toInt(),
          start.bankId.toInt() and 0xFF, start.mapId.toInt() and 0xFF, start.x.toInt(), start.y.toInt())
    } else {
      warpService.executeWarp(
          ctx.session,
          charId,
          WarpTile(
              x = 0,
              y = 0,
              targetRegionId = region.wireValue,
              targetBankId = start.bankId,
              targetMapId = start.mapId,
              targetX = start.x.toInt(),
              targetY = start.y.toInt(),
              exitFacing = Direction.DOWN,
          ),
      )
    }
    characterStore.flushCharacterAsync(charId)
    ctx.reply(
        if (keepBuild) "Reset to the ${region.displayName} start. Party, PC and bag kept."
        else "Reset to the ${region.displayName} start. Your party, PC and bag are empty.")
  }

  private suspend fun apply(ctx: CommandContext, checkpoint: StoryCheckpoint) {
    val charId = ctx.characterId
    characterStore.replaceProgress(
        characterId = charId,
        party = emptyList(),
        items = checkpoint.items.mapKeys { items.idOf(it.key) },
        storyFlags = checkpoint.storyFlags,
        storyVars = checkpoint.storyVars,
    )
    checkpoint.party.forEach {
      storyPlayerService.givePokemon(ctx.session, ctx.state, it.dexId, it.level, it.moveIds)
    }

    // There is no packet for a single var, so the whole block goes again, and its ids resolve
    // against the region on the character, which the warp has not moved yet.
    val stored = characterStore.getCharacter(charId) ?: return
    val aimed = stored.copy(info = stored.info.copy(positionRegionId = checkpoint.region.wireValue))
    worldStateService.send(ctx.session, aimed, fullVars = true)

    // A warp rather than a move, so the destination runs its entry scripts and the scene fires.
    warpService.executeWarp(
        ctx.session,
        charId,
        WarpTile(
            x = 0,
            y = 0,
            targetRegionId = checkpoint.region.wireValue,
            targetBankId = checkpoint.bankId.toByte(),
            targetMapId = checkpoint.mapId.toByte(),
            targetX = checkpoint.x,
            targetY = checkpoint.y,
            exitFacing = checkpoint.facing,
        ),
    )
    characterStore.flushCharacterAsync(charId)
    ctx.reply("Jumped to ${checkpoint.name}.")
  }
}
