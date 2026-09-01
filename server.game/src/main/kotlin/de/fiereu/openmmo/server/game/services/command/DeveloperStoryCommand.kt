package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.StoryClientState
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.services.WorldStateService
import de.fiereu.openmmo.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperStoryCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val story: StoryService,
    private val characters: CharacterStore,
    private val worldState: WorldStateService,
) : ChatCommand {
  override val name = "devstory"
  override val usage = "/devstory <flag get|flag set|flag clear|var get|var set> <key> [value]"
  override val description = "reads or changes local story flags and variables"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val kind = ctx.args.getOrNull(0)?.lowercase()
    val action = ctx.args.getOrNull(1)?.lowercase()
    val rawKey = ctx.args.getOrNull(2)
    if (kind == null || action == null || rawKey == null) {
      ctx.reply(usage)
      return
    }
    val key = storyKey(ctx, rawKey) ?: return
    when (kind to action) {
      "flag" to "get" -> ctx.reply("$key = ${story.isFlagSet(ctx.characterId, key)}")
      "flag" to "set" -> changeFlag(ctx, key, enabled = true)
      "flag" to "clear" -> changeFlag(ctx, key, enabled = false)
      "var" to "get" -> ctx.reply("$key = ${story.getVar(ctx.characterId, key)}")
      "var" to "set" -> setVar(ctx, key)
      else -> ctx.reply(usage)
    }
  }

  private fun changeFlag(ctx: CommandContext, key: String, enabled: Boolean) {
    if (enabled) story.setFlag(ctx.characterId, key) else story.clearFlag(ctx.characterId, key)
    val update = StoryClientState.flagUpdate(ctx.state.regionId.toByte(), key, enabled)
    update?.let(ctx.session::send)
    // Manually granted badges get the same retail popup the script path sends.
    if (enabled && update != null) {
      characters.getCharacter(ctx.characterId)?.storyFlags?.let { flags ->
        StoryClientState.badgeAnnouncement(ctx.state.regionId.toByte(), update.flagId, flags)
            ?.let(ctx.session::send)
      }
    }
    characters.flushCharacterAsync(ctx.characterId)
    ctx.reply("$key = $enabled")
  }

  private fun setVar(ctx: CommandContext, key: String) {
    val value = ctx.args.getOrNull(3)?.toIntOrNull()
    if (value == null) {
      ctx.reply(usage)
      return
    }
    story.setVar(ctx.characterId, key, value)
    characters.getCharacter(ctx.characterId)?.let {
      worldState.send(ctx.session, it, fullVars = true)
    }
    characters.flushCharacterAsync(ctx.characterId)
    ctx.reply("$key = $value")
  }

  private fun storyKey(ctx: CommandContext, raw: String): String? {
    if ('/' in raw) return raw
    val region = Region.byWireValue(ctx.state.regionId.toByte())
    if (region == null) {
      ctx.reply("Current region ${ctx.state.regionId} has no story namespace.")
      return null
    }
    return "${region.name.lowercase()}/$raw"
  }
}
