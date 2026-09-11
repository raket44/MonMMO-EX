package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.services.ChatLinkService
import javax.inject.Inject
import javax.inject.Singleton

/** Picks which candidate reply a chat monster-link click gets (see ChatLinkService). Server-wide. */
@Singleton
class LinkProbeCommand @Inject constructor(private val chatLinks: ChatLinkService) : ChatCommand {
  override val name = "linkprobe"
  override val usage = "/linkprobe <0-5>"
  override val description = "chooses the reply variant for chat monster-link clicks (0 GTL container, 1 event, 2 rental, 3 trade, 4 record dialog, 5 record book)"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val n = ctx.args.firstOrNull()?.toIntOrNull()
    if (n == null || n !in 0..5) {
      ctx.reply(usage)
      ctx.reply("Now: variant ${chatLinks.probeVariant}")
      return
    }
    chatLinks.probeVariant = n
    ctx.reply("Chat link clicks now answer with variant $n.")
  }
}
