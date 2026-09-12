package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.services.CosmeticAnimations
import javax.inject.Inject
import javax.inject.Singleton

/** Plays an entity animation byte on the player (`/anim N`) or pins the cosmetic-click one (`/anim set N`). */
@Singleton
class AnimProbeCommand @Inject constructor(private val animations: CosmeticAnimations) : ChatCommand {
  override val name = "anim"
  override val usage = "/anim <0-255> | /anim set <0-255>"
  override val description = "plays entity animation N on you for everyone nearby; 'set N' makes cosmetic clicks play N"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val args = ctx.args
    if (args.firstOrNull() == "set") {
      val n = args.getOrNull(1)?.toIntOrNull()
      if (n == null || n !in 0..255) {
        ctx.reply(usage)
        return
      }
      animations.useAnimation = n
      ctx.reply("Cosmetic clicks now play animation $n.")
      return
    }
    val n = args.firstOrNull()?.toIntOrNull()
    if (n == null || n !in 0..255) {
      ctx.reply(usage)
      ctx.reply("Cosmetic clicks play animation ${animations.useAnimation}.")
      return
    }
    animations.play(ctx.session, ctx.characterId, n)
    ctx.reply("Playing animation $n.")
  }
}
