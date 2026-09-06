package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grants or clears Donator Status on an account. The client shows "Donator Status active. <time>
 * left." in its menu header (f/gz, string 1150) while the expiry the join response carries lies
 * in the future, so the status is per login account and read at join: the player sees it after
 * their next login.
 */
@Singleton
class DonatorCommand
@Inject
constructor(
    private val characters: CharacterStore,
    private val sessions: SessionRegistry,
) : ChatCommand {
  override val name = "donator"
  override val usage = "/donator <days|off> [character name]"
  override val description = "grants Donator Status to your account or an online player's"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    val argument = ctx.args.getOrNull(0)?.lowercase()
    val days = argument?.toIntOrNull()
    if (argument == null || (days == null && argument != "off") || (days != null && days <= 0)) {
      ctx.reply(usage)
      return
    }
    val targetName = ctx.args.drop(1).joinToString(" ").trim()
    val (userId, shownName) =
        if (targetName.isEmpty()) ctx.state.userId to ctx.character.info.name
        else {
          val id =
              sessions.onlineCharacterIds().firstOrNull { id ->
                characters.getCharacter(id)?.info?.name.equals(targetName, ignoreCase = true)
              }
          val session = id?.let(sessions::getByCharacterId)
          val userId = session?.attributes?.get(PLAYER_STATE)?.userId
          if (userId == null) {
            ctx.reply("No player called '$targetName' is online.")
            return
          }
          userId to (characters.getCharacter(id)?.info?.name ?: targetName)
        }
    if (days == null) {
      characters.setDonatorUntil(userId, null)
      ctx.reply("Donator Status cleared for $shownName.")
      return
    }
    val until = System.currentTimeMillis() / 1000 + days.toLong() * 86_400
    characters.setDonatorUntil(userId, until)
    ctx.reply("Donator Status for $shownName: $days day(s), shown after the next login.")
  }
}
