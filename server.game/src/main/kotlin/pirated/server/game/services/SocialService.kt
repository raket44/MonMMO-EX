package pirated.server.game.services

import pirated.network.PacketEvent
import pirated.network.SessionContext
import pirated.net.game.packets.AddFriendPacket
import pirated.net.game.packets.BlockPlayerPacket
import pirated.net.game.packets.CancelSocialInteractionPacket
import pirated.net.game.packets.FriendListEntry
import pirated.net.game.packets.FriendListPacket
import pirated.net.game.packets.FriendProfileRequestPacket
import pirated.net.game.packets.PartyMemberJoinPacket
import pirated.net.game.packets.PartyMemberLeavePacket
import pirated.net.game.packets.PlayerHead
import pirated.net.game.packets.RemoveFriendPacket
import pirated.net.game.packets.RequestSocialProfilePacket
import pirated.net.game.packets.UnblockPlayerPacket
import pirated.server.game.session.PLAYER_STATE
import pirated.server.game.session.SessionRegistry
import pirated.server.game.storage.CharacterStore
import pirated.server.game.storage.FriendProfile
import pirated.server.game.storage.SocialStore
import pirated.server.game.storage.StoredCharacter
import pirated.server.game.storage.profile
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class SocialService
@Inject
constructor(
    private val socialStore: SocialStore,
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
    private val socialRequests: Provider<SocialRequestService>,
) {

  fun sendFriendList(ctx: SessionContext) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    ctx.send(buildFriendList(state.userId))
  }

  fun onAddFriend(event: PacketEvent<AddFriendPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val name = event.packet.username
    if (name in socialStore.getFriends(state.userId)) return
    // Retail asks the other player first ("{00} has requested you as a friend"); an offline
    // target is added one-sidedly as before, since there is nobody to ask.
    if (socialRequests.get().request(ctx, SocialRequestService.Kind.FRIEND, name)) return
    socialStore.addFriend(state.userId, name)
    log.info { "AddFriend user=${state.userId} name='$name' (offline, one-sided)" }
    ctx.send(buildFriendList(state.userId))
  }

  /** A friend request was accepted: both users list each other and get their lists refreshed. */
  fun completeFriendship(userA: Int, nameA: String, ctxA: SessionContext?, userB: Int, nameB: String, ctxB: SessionContext?) {
    socialStore.addFriend(userA, nameB)
    socialStore.addFriend(userB, nameA)
    log.info { "Friends: user=$userA '$nameA' <-> user=$userB '$nameB'" }
    ctxA?.send(buildFriendList(userA))
    ctxB?.send(buildFriendList(userB))
  }

  fun onRemoveFriend(event: PacketEvent<RemoveFriendPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val name = event.packet.username
    val removed = socialStore.removeFriend(state.userId, name)
    log.info { "RemoveFriend user=${state.userId} name='$name' removed=$removed" }
    ctx.send(buildFriendList(state.userId))
  }

  fun onBlockPlayer(event: PacketEvent<BlockPlayerPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val packet = event.packet
    socialStore.block(state.userId, packet.username)
    log.info {
      "BlockPlayer user=${state.userId} name='${packet.username}' reason='${packet.reason}'"
    }
    ctx.send(
        PartyMemberJoinPacket(
            player = syntheticId(packet.username),
            name = packet.username,
            secondaryName = packet.reason,
            value = 0,
        ))
  }

  fun onUnblockPlayer(event: PacketEvent<UnblockPlayerPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val name = event.packet.username
    val removed = socialStore.unblock(state.userId, name)
    log.info { "UnblockPlayer user=${state.userId} name='$name' removed=$removed" }
    ctx.send(PartyMemberLeavePacket(memberId = syntheticId(name)))
  }

  fun onFriendProfileRequest(event: PacketEvent<FriendProfileRequestPacket>) {
    log.info { "FriendProfileRequest targetEntityId=${event.packet.targetEntityId}" }
  }

  fun onRequestSocialProfile(event: PacketEvent<RequestSocialProfilePacket>) {
    log.info { "RequestSocialProfile targetId=${event.packet.targetId}" }
  }

  fun onCancelSocialInteraction(event: PacketEvent<CancelSocialInteractionPacket>) {
    log.info { "CancelSocialInteraction from ${event.session.remoteAddress}" }
  }

  /**
   * The friend list: online friends from memory (their outfit may have changed since the last
   * save), the rest - last seen and head - from one query. [overrides], by lowercase name, carry a
   * friend who is leaving right now, whose save may not have landed yet.
   */
  private fun buildFriendList(userId: Int, overrides: Map<String, FriendProfile> = emptyMap()): FriendListPacket {
    val friends = socialStore.friendsSince(userId)
    val onlineByName =
        sessionRegistry.onlineCharacterIds()
            .mapNotNull(characterStore::getCharacter)
            .associateBy { it.info.name.lowercase() }
    val online = friends.keys.filter { it.lowercase() in onlineByName }.toSet()
    val offline = socialStore.profiles(friends.keys.filter { it !in online && it.lowercase() !in overrides })
    val now = epochSeconds(java.time.LocalDateTime.now())
    val entries =
        friends.map { (name, since) ->
          val live = onlineByName[name.lowercase()]
          val profile = overrides[name.lowercase()] ?: live?.profile() ?: offline[name.lowercase()]
          FriendListEntry(
              player = syntheticId(name),
              friendsSince = epochSeconds(since),
              online = live != null,
              name = name,
              lastSeen = if (live != null) now else profile?.lastSeen?.let(::epochSeconds) ?: 0,
              head = profile?.head ?: PlayerHead(),
          )
        }
    return FriendListPacket(mode = 0, entries = entries)
  }

  /**
   * A character entered or left the world: every online player who lists it as a friend gets a
   * fresh list. [left] is the leaving character (already unbound) so its last seen and head are current.
   */
  fun notifyPresence(name: String, left: StoredCharacter? = null) {
    val overrides = left?.let { mapOf(name.lowercase() to it.profile()) }.orEmpty()
    val notified = mutableSetOf<Int>()
    for (id in sessionRegistry.onlineCharacterIds()) {
      val session = sessionRegistry.getByCharacterId(id) ?: continue
      val userId = session.attributes[PLAYER_STATE]?.userId ?: continue
      if (!notified.add(userId)) continue
      if (socialStore.getFriends(userId).none { it.equals(name, ignoreCase = true) }) continue
      session.send(buildFriendList(userId, overrides))
    }
  }

  /** Stored times are the server clock's UTC wall time, as the character dates on the wire are. */
  private fun epochSeconds(time: java.time.LocalDateTime): Int = time.toEpochSecond(java.time.ZoneOffset.UTC).toInt()

  private fun isOnlineByName(name: String): Boolean =
      sessionRegistry.onlineCharacterIds().any { id ->
        characterStore.getCharacter(id)?.info?.name == name
      }

  private fun syntheticId(name: String): Long = (name.hashCode().toLong() shl 16) or 0x9000L
}
