package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.clientStaffLevel
import de.fiereu.openmmo.common.withClientStaffLevel
import de.fiereu.openmmo.net.game.packets.SelectedCharacterPacket
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets the staff level the client reads out of CharacterInfo, which is what unlocks its own
 * built-in GM Menu (`f.sb`: GM Menu / Player Search / Teleport / Profiler). The client gates that
 * window on `staffLevel >= 1` and other staff UI on up to 3.
 */
@Singleton
class DeveloperGmMenuCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val characters: CharacterStore,
) : ChatCommand {
  override val name = "gm"
  override val aliases = listOf("gmmenu", "staffmenu")
  override val usage = "/gm [on|off|<level 0-255>|status]"
  override val description = "unlocks the client's built-in GM Menu by setting its staff level"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val stored = characters.getCharacter(ctx.characterId)
    if (stored == null) {
      ctx.reply("Could not load your character.")
      return
    }

    val argument = ctx.args.firstOrNull()?.lowercase()
    val level =
        when (argument) {
          null,
          "on",
          "unlock" -> CharacterPermissions.CLIENT_STAFF_LEVEL_FULL
          "off",
          "lock" -> 0
          "status" -> {
            ctx.reply(
                "Client staff level is ${stored.info.clientStaffLevel} " +
                    "(0 none, 1 CM, 2-4 MOD, 5-6 GM, 7 SGM, 8 HGM, 9 DEV, 10 ADM).")
            return
          }
          else ->
              argument.toIntOrNull()?.takeIf { it in 0..255 }
                  ?: run {
                    ctx.reply(usage)
                    return
                  }
        }

    val info = stored.info.copy(permissions = withClientStaffLevel(stored.info.permissions, level))
    characters.updateCharacter(info)
    characters.flushCharacterAsync(ctx.characterId)
    // Push the character again so a client that re-reads it picks the level up without a relog.
    ctx.session.send(SelectedCharacterPacket(info))
    ctx.reply(
        if (level == 0) "Client staff level cleared. Relog to hide the GM Menu."
        else
            "Client staff level set to $level. If the GM Menu is not on screen yet, return to " +
                "character selection and log back in once.")
  }
}
