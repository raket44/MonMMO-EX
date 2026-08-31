package de.fiereu.openmmo.server.game.services.command

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.hasPermission
import de.fiereu.openmmo.server.game.services.notice
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

private const val PREFIX = '/'

@Singleton
class ChatCommandService
@Inject
constructor(
    private val characterStore: CharacterStore,
    registered: Set<@JvmSuppressWildcards ChatCommand>,
    private val developerTools: de.fiereu.openmmo.server.game.developer.DeveloperTools,
) {

  private val commands =
      registered
          .flatMap { command ->
            (listOf(command.name) + command.aliases).map { it.lowercase() to command }
          }
          .toMap()

  /** Handles a chat line. Returns false when it is ordinary chat the caller should broadcast. */
  suspend fun tryHandle(session: SessionContext, message: String): Boolean {
    val text = message.trim()
    if (!text.startsWith(PREFIX)) return false

    val parts = tokenizeCommandLine(text.drop(1).removePrefix(PREFIX.toString()))
    val name = parts.firstOrNull()?.lowercase()
    if (name == null) {
      session.send(notice("Type /help to see what you can run."))
      return true
    }

    val state = session.attributes[PLAYER_STATE]
    val character = state?.characterId?.let(characterStore::getCharacter)
    if (state == null || character == null) {
      session.send(notice("You are not in the world yet."))
      return true
    }

    // On a dev-tools server every character is an admin; permission bits matter once the
    // server is opened up and OPENMMO_DEV_TOOLS is off.
    fun allowed(command: ChatCommand): Boolean =
        developerTools.enabled || character.info.hasPermission(command.permission)

    var command = commands[name]
    var args = parts.drop(1)
    if (command == null) {
      // The client's own GM dialogs glue the player name straight onto the command
      // ("//createitem<Name> <id> <qty>" from the Search window's ADD buttons). Split the
      // longest registered command off the front and pass the remainder as the first argument.
      val glued = commands.keys.filter { name.startsWith(it) }.maxByOrNull { it.length }
      if (glued != null) {
        command = commands[glued]
        val remainder = parts.first().drop(glued.length)
        args = (if (remainder.isBlank()) emptyList() else listOf(remainder)) + parts.drop(1)
      }
    }
    if (command == null || !allowed(command)) {
      if (command != null) {
        log.info { "char=${character.info.id} may not run /$name" }
      }
      session.send(notice("Unknown command: $name. Try /help."))
      return true
    }

    val ctx =
        CommandContext(
            session = session,
            state = state,
            character = character,
            args = args,
            // Aliases map to the same instance, so the visible list is de-duplicated by identity.
            commands = commands.values.distinct().filter(::allowed),
        )
    try {
      command.run(ctx)
    } catch (e: Exception) {
      log.error(e) { "/$name failed for char=${character.info.id}" }
      ctx.reply("/$name failed.")
    }
    return true
  }
}
