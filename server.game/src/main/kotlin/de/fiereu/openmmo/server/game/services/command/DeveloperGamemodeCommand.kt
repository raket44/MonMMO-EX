package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creative mode for world builders: the server stops enforcing collision and rolling wild
 * encounters for this session, so an admin can walk straight to wherever a warp or fixture needs
 * placing. Purely server-side state - the client is untouched, so walls still LOOK solid; the
 * server simply lets the step through.
 */
@Singleton
class DeveloperGamemodeCommand
@Inject
constructor(
    private val tools: DeveloperTools,
) : ChatCommand {
  // /gm now belongs to the client's GM Menu; creative mode lives on /gamemode.
  override val name = "gamemode"
  override val aliases = listOf("gmode")
  override val usage = "/gamemode c|s"
  override val description = "toggles creative or survival gamemode"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    when (ctx.args.firstOrNull()) {
      "c",
      "creative",
      "1" -> {
        ctx.state.creative = true
        ctx.reply("Creative mode ON: no collision, no wild encounters. /gm s to return.")
      }
      "s",
      "survival",
      "0" -> {
        ctx.state.creative = false
        ctx.reply("Creative mode OFF: the world is solid again.")
      }
      else -> ctx.reply("Currently ${if (ctx.state.creative) "creative" else "survival"}. $usage")
    }
  }
}
