package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/** Adds (or with a negative amount removes) money on the developer's own character. */
@Singleton
class GiveMoneyCommand @Inject constructor(private val characters: CharacterStore) : ChatCommand {
  override val name = "givemoney"
  override val usage = "/givemoney <amount>"
  override val description = "adds money to your character (negative to remove)"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val amount = ctx.args.getOrNull(0)?.toIntOrNull()
    if (amount == null || amount == 0) {
      ctx.reply(usage)
      return
    }
    val before = characters.getCharacter(ctx.characterId)?.info?.money ?: 0
    if (before + amount < 0) {
      ctx.reply("That would leave a negative balance ($before now).")
      return
    }
    if (!characters.addMoney(ctx.characterId, amount)) {
      ctx.reply("Could not change the balance.")
      return
    }
    val after = characters.getCharacter(ctx.characterId)?.info?.money ?: (before + amount)
    ctx.session.send(LocalCharacterDeltaPacket(money = after))
    ctx.reply("Money: $before -> $after")
  }
}
