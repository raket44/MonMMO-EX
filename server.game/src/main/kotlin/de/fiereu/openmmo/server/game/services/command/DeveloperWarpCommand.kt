package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.CustomWarps
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-game warp authoring - the tool that turns black-screen doors in Johto, Sinnoh and Unova into
 * working ones without anyone touching code:
 * 1. stand on the door or gate tile and type `/warp from`
 * 2. travel to where it should lead - walk, /tp, /tp raw, anything
 * 3. type `/warp to` - the link is live and saved immediately
 * 4. `/warp both` while still standing there also creates the return trip
 *
 * `/warp list` shows everything placed, `/warp delete` removes the warp on the tile you stand on.
 */
@Singleton
class DeveloperWarpCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val customWarps: CustomWarps,
) : ChatCommand {
  override val name = "link"
  override val usage = "/link from | to | both | list | delete"
  override val description = "places custom warps by standing on tiles"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    val state = ctx.state
    when (ctx.args.firstOrNull()) {
      "tour" -> ctx.reply("The tour moved to its own command: /tour auto 2 100 1.")
      "from" -> {
        state.pendingWarpSource =
            intArrayOf(state.regionId, state.bankId, state.mapId, state.x.toInt(), state.y.toInt())
        ctx.reply(
            "Warp source marked at ${state.regionId}:${state.bankId}:${state.mapId} " +
                "(${state.x}, ${state.y}). Travel to the destination and /warp to.")
      }
      "to" -> {
        val source = state.pendingWarpSource
        if (source == null) {
          ctx.reply("No source marked. Stand on the entry tile and /warp from first.")
          return
        }
        customWarps.add(
            CustomWarps.Warp(
                source[0],
                source[1],
                source[2],
                source[3],
                source[4],
                state.regionId,
                state.bankId,
                state.mapId,
                state.x.toInt(),
                state.y.toInt()))
        ctx.reply(
            "Warp live: ${source[0]}:${source[1]}:${source[2]} (${source[3]}, ${source[4]}) now " +
                "leads here. /warp both to also create the return trip from this tile.")
      }
      "both" -> {
        val source = state.pendingWarpSource
        if (source == null) {
          ctx.reply("No source marked; /warp both follows a /warp from ... /warp to sequence.")
          return
        }
        // The return trip: standing here leads back to one tile below the original entry, so
        // arriving does not immediately re-trigger the entry tile itself.
        customWarps.add(
            CustomWarps.Warp(
                state.regionId,
                state.bankId,
                state.mapId,
                state.x.toInt(),
                state.y.toInt(),
                source[0],
                source[1],
                source[2],
                source[3],
                source[4] + 1))
        state.pendingWarpSource = null
        ctx.reply("Return warp live: this tile now leads back beside the original entry.")
      }
      "list" -> {
        val all = customWarps.all()
        ctx.reply(
            if (all.isEmpty()) "No custom warps yet."
            else
                "${all.size} custom warps: " +
                    all.take(10).joinToString("; ") {
                      "${it.region}:${it.bank}:${it.map}(${it.x},${it.y})->" +
                          "${it.destRegion}:${it.destBank}:${it.destMap}"
                    } +
                    (if (all.size > 10) " ..." else ""))
      }
      "delete" -> {
        val removed =
            customWarps.remove(
                state.regionId, state.bankId, state.mapId, state.x.toInt(), state.y.toInt())
        ctx.reply(
            if (removed) "Deleted the custom warp on this tile."
            else "No custom warp on this tile. Stand exactly on its entry tile.")
      }
      else -> ctx.reply(usage)
    }
  }
}
