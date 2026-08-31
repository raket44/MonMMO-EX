package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.script.ScriptRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeveloperScriptCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val scripts: ScriptRegistry,
) : ChatCommand {
  override val name = "devscript"
  override val usage = "/devscript [on|off|kotlin|auto|list|verbose] [script-or-setting]"
  override val description = "controls local interpreted-script testing"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }

    when (ctx.args.firstOrNull()?.lowercase()) {
      "on" -> changeOverride(ctx, enabled = true)
      "off" -> changeOverride(ctx, enabled = false)
      "kotlin" -> forceKotlin(ctx)
      "auto" -> clearOverride(ctx)
      "list" -> {
        val interpreted = tools.overrides().sorted().map { "INTERPRETER $it" }
        val kotlin = tools.kotlinOverrides().sorted().map { "KOTLIN $it" }
        val active = interpreted + kotlin
        ctx.reply(
            if (active.isEmpty()) "No interpreted overrides are active." else active.joinToString())
      }
      "verbose" -> changeVerbose(ctx)
      else -> ctx.reply(usage)
    }
  }

  private fun forceKotlin(ctx: CommandContext) {
    val target = ctx.args.getOrNull(1)
    if (target == null) {
      ctx.reply(usage)
      return
    }
    val changed = tools.enableKotlinOverride(target)
    ctx.reply(
        if (changed) "Kotlin override enabled for $target."
        else "Kotlin override was already enabled for $target.")
  }

  private fun clearOverride(ctx: CommandContext) {
    val target = ctx.args.getOrNull(1)
    if (target == null) {
      ctx.reply(usage)
      return
    }
    val changed = tools.clearScriptOverride(target)
    ctx.reply(
        if (changed) "Automatic script routing restored for $target."
        else "No override was active for $target.")
  }

  private fun changeOverride(ctx: CommandContext, enabled: Boolean) {
    val target = ctx.args.getOrNull(1)
    if (target == null) {
      ctx.reply(usage)
      return
    }
    if (enabled && !scripts.hasInterpreted(target)) {
      ctx.reply("No interpreted script is registered as $target.")
      return
    }
    val changed = if (enabled) tools.enableOverride(target) else tools.disableOverride(target)
    val action = if (enabled) "enabled" else "disabled"
    ctx.reply(
        if (changed) "Interpreted override $action for $target."
        else "Interpreted override was already $action for $target.")
  }

  private fun changeVerbose(ctx: CommandContext) {
    val enabled =
        when (ctx.args.getOrNull(1)?.lowercase()) {
          "on" -> true
          "off" -> false
          else -> {
            ctx.reply(usage)
            return
          }
        }
    tools.setInterpreterVerbose(enabled)
    ctx.reply("Interpreter verbose logging ${if (enabled) "enabled" else "disabled"}.")
  }
}
