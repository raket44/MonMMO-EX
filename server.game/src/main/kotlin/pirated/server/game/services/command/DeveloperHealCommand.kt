package pirated.server.game.services.command

import pirated.common.CharacterPermissions
import pirated.server.game.developer.DeveloperTools
import pirated.server.game.services.StoryPlayerService
import javax.inject.Inject
import javax.inject.Singleton

/** Full party heal, because a fainted lead otherwise needs a Pokecenter that may be far away. */
@Singleton
class DeveloperHealCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val storyPlayer: StoryPlayerService,
) : ChatCommand {
  override val name = "heal"
  override val usage = "/heal"
  override val description = "fully heals your party"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    storyPlayer.healParty(ctx.session, ctx.state)
    ctx.reply("Party healed.")
  }
}
