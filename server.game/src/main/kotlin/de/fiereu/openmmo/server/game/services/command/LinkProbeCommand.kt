package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.services.ChatLinkService
import javax.inject.Inject
import javax.inject.Singleton

/** Picks which candidate reply a chat monster-link click gets (see ChatLinkService). Server-wide. */
@Singleton
class LinkProbeCommand @Inject constructor(private val chatLinks: ChatLinkService) : ChatCommand {
  override val name = "linkprobe"
  override val usage = "/linkprobe <0-5> (0 = 0xB1 summary, the real path)"
  override val description = "chooses the reply variant for chat monster-link clicks (0 summary window 0xB1, 1 GTL, 2 event, 3 rental, 4 shared-view box, 5 record book)"
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
