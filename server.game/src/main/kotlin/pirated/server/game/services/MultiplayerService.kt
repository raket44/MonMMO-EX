package pirated.server.game.services

import pirated.server.game.session.SessionRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiplayerService
@Inject
constructor(
    private val sessionRegistry: SessionRegistry,
) {

  fun broadcastMessage(packet: Any) {
    for (characterId in sessionRegistry.onlineCharacterIds()) {
      val ctx = sessionRegistry.getByCharacterId(characterId) ?: continue
      if (ctx.channel.isActive) {
        ctx.send(packet)
      }
    }
  }
}
