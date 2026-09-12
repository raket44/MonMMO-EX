package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.server.game.services.CosmeticAnimations
import javax.inject.Inject
import javax.inject.Singleton

/** Plays a hat variant on the player for a few seconds (`/anim N`) or pins the cosmetic-click one (`/anim set N`). */
@Singleton
class AnimProbeCommand @Inject constructor(private val animations: CosmeticAnimations) : ChatCommand {
  override val name = "anim"
  override val usage = "/anim <0-255> | /anim set <0-255>"
  override val description = "swaps your worn hat to variant N for a few seconds, for everyone nearby; 'set N' makes cosmetic clicks use N"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val args = ctx.args
    if (args.firstOrNull() == "set") {
      val n = args.getOrNull(1)?.toIntOrNull()
      if (n == null || n !in 0..255) {
        ctx.reply(usage)
        return
      }
      animations.useVariant = n
      ctx.reply("Cosmetic clicks now use variant $n.")
      return
    }
    val n = args.firstOrNull()?.toIntOrNull()
    if (n == null || n !in 0..255) {
      ctx.reply(usage)
      ctx.reply("Cosmetic clicks use variant ${animations.useVariant}.")
      return
    }
    animations.play(ctx.session, ctx.characterId, SkinSlot.HAT, n)
    ctx.reply("Hat variant $n for a few seconds.")
  }
}
