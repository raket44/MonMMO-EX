package pirated.server.game.services.command

import pirated.common.CharacterPermissions
import pirated.server.game.developer.DeveloperTools
import pirated.server.game.services.NdsLookahead
import javax.inject.Inject
import javax.inject.Singleton

/** The player's line-of-sight stops (NdsLookahead): list them, clear them, or switch them off for this session. */
@Singleton
class DeveloperLookaheadCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val lookahead: NdsLookahead,
) : ChatCommand {
  override val name = "devlookahead"
  override val usage = "/devlookahead [list|clear|on|off]"
  override val description = "shows, clears or toggles the temp stops placed ahead of the player on DS maps"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val state = ctx.state
    when (ctx.args.firstOrNull()?.lowercase() ?: "list") {
      "list" -> ctx.reply("Lookahead ${if (state.lookaheadEnabled) "on" else "off"}, ${state.lookaheadWalls.size} stop(s) standing: ${state.lookaheadWalls.values}")
      "clear" -> { lookahead.clear(ctx.session, state, "/devlookahead clear"); ctx.reply("Stops cleared.") }
      "on" -> { state.lookaheadEnabled = true; ctx.reply("Lookahead on.") }
      "off" -> { state.lookaheadEnabled = false; lookahead.clear(ctx.session, state, "/devlookahead off"); ctx.reply("Lookahead off for this session.") }
      else -> ctx.reply(usage)
    }
  }
}
