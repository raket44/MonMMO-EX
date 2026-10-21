package pirated.server.game.services.command

import pirated.common.CharacterPermissions
import pirated.net.game.packets.WorldActionDispatchPacket
import pirated.server.game.developer.DeveloperTools
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A raw s2c 0xB6 world action, for probing a map's client handler without a script: `/devworld
 * 6 0 384 0 1` hands the loaded map's gimmick handler [0x180, 0, 1] (Nimbasa Gym: flip track 0
 * on), `/devworld 6 0 608 15` its restore mask. Action 6 = gimmick, 7 = door under the player,
 * 9 = client scene by id, 32 = Hall of Fame (see the client actions notes).
 */
@Singleton
class DeveloperWorldCommand
@Inject
constructor(
    private val tools: DeveloperTools,
) : ChatCommand {
  override val name = "devworld"
  override val usage = "/devworld <action> <subject> [short ...]"
  override val description = "sends a raw world action (0xB6) to your own client"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val numbers = ctx.args.map { it.toIntOrNull() }
    if (numbers.size < 2 || numbers.any { it == null }) {
      ctx.reply(usage)
      return
    }
    val values = numbers.filterNotNull()
    val packet = WorldActionDispatchPacket(values[0].toByte(), values[1].toByte(), values.drop(2).map { it.toShort() })
    ctx.session.send(packet)
    ctx.reply("Sent world action ${values[0]} subject ${values[1]} args ${values.drop(2)}")
  }
}
