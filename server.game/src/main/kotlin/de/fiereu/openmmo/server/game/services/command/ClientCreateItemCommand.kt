package de.fiereu.openmmo.server.game.services.command

import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.server.game.developer.DeveloperTools
import de.fiereu.openmmo.server.game.services.CosmeticsRegistry
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The GM Search window's ADD button for items (client f.oq1): it sends the chat line
 * `//createitem<PlayerName> <itemId> <quantity>` with the name glued to the command - the
 * dispatcher's glued-prefix fallback peels it off into the first argument. Grants by raw id, so
 * every catalog row works: server-registered items and client-generated cosmetics alike.
 */
@Singleton
class ClientCreateItemCommand
@Inject
constructor(
    private val tools: DeveloperTools,
    private val items: ItemRegistry,
    private val storyPlayer: StoryPlayerService,
) : ChatCommand {
  override val name = "createitem"
  override val aliases = emptyList<String>()
  override val usage = "//createitem <player> <itemId> [quantity]"
  override val description = "GM Search ADD: grants an item by id"
  override val permission = CharacterPermissions.DEVELOPER

  override suspend fun run(ctx: CommandContext) {
    if (!tools.enabled) {
      ctx.reply("Developer tools are disabled in the server configuration.")
      return
    }
    // Arguments may or may not lead with the player name; the numbers are what matter. Grants
    // always land on the sender - a name is accepted but only the sender's own.
    val numbers = ctx.args.mapNotNull { it.toIntOrNull() }
    val itemId = numbers.firstOrNull()
    if (itemId == null || itemId !in 1..65535) {
      ctx.reply(usage)
      return
    }
    val quantity = (numbers.getOrNull(1) ?: 1).coerceIn(1, 999)
    if (!storyPlayer.giveItemById(ctx.session, ctx.state, itemId, quantity)) {
      ctx.reply("Could not add item $itemId to the bag.")
      return
    }
    val name =
        items.get(itemId)?.name
            ?: CosmeticsRegistry.byItemId(itemId)?.let { "${it.name} (${it.slot} cosmetic)" }
            ?: "item $itemId"
    log.info { "[CreateItem] character=${ctx.characterId} item=$itemId quantity=$quantity" }
    ctx.reply("Added $quantity x $name.")
  }
}
