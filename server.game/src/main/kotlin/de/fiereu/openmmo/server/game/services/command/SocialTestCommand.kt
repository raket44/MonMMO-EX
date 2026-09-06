package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.net.game.packets.DuelInvitePacket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reverse-engineering aid: shows the social request prompt (s2c 0x50) on your own client with
 * a chosen type and flags byte, to learn which code means trade, link, duel or team.
 */
@Singleton
class SocialTestCommand @Inject constructor() : ChatCommand {
  override val name = "socialtest"
  override val usage = "/socialtest <type 0-15> [flags 0-3] [name]"
  override val description = "shows the social request prompt with the given type byte"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val type = ctx.args.getOrNull(0)?.toIntOrNull()
    if (type == null) {
      ctx.reply(usage)
      return
    }
    val flags = ctx.args.getOrNull(1)?.toIntOrNull() ?: 0
    val name = ctx.args.getOrNull(2) ?: "Tester"
    ctx.session.send(DuelInvitePacket(flags.toByte(), type.toByte(), name))
    ctx.reply("Sent prompt type=$type flags=$flags name=$name")
  }
}
