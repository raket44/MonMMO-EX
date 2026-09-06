package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionPacket
import de.fiereu.openmmo.server.game.services.SocialRequestService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reverse-engineering aid: shows a social request prompt on your own client. `/socialtest
 * <kind> [name] [team]` with kind 16 challenge, 17 friend, 18 trade, 19 link, 20 team, or one of
 * those words. The prompt's answer is logged by SocialRequestService like a real one.
 */
@Singleton
class SocialTestCommand @Inject constructor(private val social: SocialRequestService) : ChatCommand {
  override val name = "socialtest"
  override val usage = "/socialtest <challenge|friend|trade|link|team|wire> [name] [team]"
  override val description = "shows a social request prompt on your own screen"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val kindArg = ctx.args.getOrNull(0)?.lowercase()
    val kind =
        SocialRequestService.Kind.entries.firstOrNull { it.name.lowercase() == kindArg }
            ?: kindArg?.toIntOrNull()?.let { w -> SocialRequestService.Kind.entries.firstOrNull { it.wire == w } }
    if (kind == null) {
      ctx.reply(usage)
      return
    }
    val name = ctx.args.getOrNull(1) ?: "Tester"
    val team = ctx.args.getOrNull(2) ?: "TestTeam"
    val id = social.selfPrompt(ctx.session, kind, name, team)
    ctx.reply("Sent ${kind.name} prompt (wire ${kind.wire}) id=$id from '$name'")
  }
}
