package de.fiereu.openmmo.server.game.session

import de.fiereu.network.SessionContext
import io.netty.channel.Channel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRegistry @Inject constructor() {
  private val sessionsByChannel = ConcurrentHashMap<Channel, SessionContext>()
  private val sessionsByCharacter = ConcurrentHashMap<Long, SessionContext>()

  fun register(ctx: SessionContext) {
    sessionsByChannel[ctx.channel] = ctx
  }

  fun unregister(ctx: SessionContext) {
    sessionsByChannel.remove(ctx.channel)
    val state = ctx.attributes[PLAYER_STATE]
    val charId = state?.characterId
    // Only if this session still owns the character: a stale session unregistering after a fresh
    // login for the same character must not drop the live session's binding.
    if (charId != null) sessionsByCharacter.remove(charId, ctx)
  }

  fun bindCharacter(ctx: SessionContext, characterId: Long) {
    sessionsByCharacter[characterId] = ctx
  }

  fun unbindCharacter(characterId: Long) {
    sessionsByCharacter.remove(characterId)
  }

  fun getByCharacterId(id: Long): SessionContext? = sessionsByCharacter[id]

  fun onlineCharacterIds(): Set<Long> = sessionsByCharacter.keys
}
