package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.battle.CrystalOnixRaid
import de.fiereu.openmmo.server.game.services.NpcService
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clears the caller's Crystal Onix win for today so the raid can be fought again (project owner,
 * 2026-09-14, for testing and recording). Gated on the developer permission alone, not on the
 * developer tools switch, so it works on the live server.
 */
@Singleton
class DeveloperRaidCommand
@Inject
constructor(
    private val characters: CharacterStore,
    private val npcs: NpcService,
) : ChatCommand {
  override val name = "devraid"
  override val usage = "/devraid"
  override val description = "clears today's Crystal Onix win so the raid boss can be fought again"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    // A value of 0 removes the var.
    characters.setStoryVar(ctx.characterId, CrystalOnixRaid.WIN_DAY_KEY, 0)
    characters.flushCharacterAsync(ctx.characterId)
    npcs.respawnRaidBoss(ctx.session, ctx.state.regionId, ctx.state.bankId, ctx.state.mapId)
    ctx.reply("Crystal Onix raid reset: the boss is back in Rock Tunnel 1F.")
  }
}
