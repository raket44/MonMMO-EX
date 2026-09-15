package de.fiereu.openmmo.server.game.storage

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Friends and block lists. Cached per user, loaded from [SocialDb] on first use and written
 * through on every change; without a database (tests) it is memory only.
 */
@Singleton
class SocialStore @Inject constructor(private val db: SocialDb? = null) {
  private val friendsByUser = ConcurrentHashMap<Int, MutableMap<String, LocalDateTime>>()
  private val blockedByUser = ConcurrentHashMap<Int, MutableSet<String>>()

  fun getFriends(userId: Int): Set<String> = friends(userId).keys

  /** Each friend's name with the moment they were added, in list order. */
  fun friendsSince(userId: Int): Map<String, LocalDateTime> = LinkedHashMap(friends(userId))

  fun addFriend(userId: Int, name: String, addedAt: LocalDateTime = LocalDateTime.now()) {
    if (friends(userId).putIfAbsent(name, addedAt) == null) db?.addFriend(userId, name, addedAt)
  }

  fun removeFriend(userId: Int, name: String): Boolean {
    val removed = friends(userId).remove(name) != null
    if (removed) db?.removeFriend(userId, name)
    return removed
  }

  /** Last seen and head of each named character, keyed by lowercase name (empty without a database). */
  fun profiles(names: Collection<String>): Map<String, FriendProfile> = db?.profiles(names).orEmpty()

  fun getBlocked(userId: Int): Set<String> = blocked(userId)

  fun block(userId: Int, name: String) {
    if (blocked(userId).add(name)) db?.block(userId, name)
  }

  fun unblock(userId: Int, name: String): Boolean {
    val removed = blocked(userId).remove(name)
    if (removed) db?.unblock(userId, name)
    return removed
  }

  private fun friends(userId: Int): MutableMap<String, LocalDateTime> =
      friendsByUser.getOrPut(userId) {
        LinkedHashMap<String, LocalDateTime>().also { map -> db?.friends(userId)?.forEach { (name, since) -> map[name] = since } }
      }

  private fun blocked(userId: Int): MutableSet<String> =
      blockedByUser.getOrPut(userId) {
        linkedSetOf<String>().also { set -> db?.blocked(userId)?.let(set::addAll) }
      }
}
