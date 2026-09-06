package de.fiereu.openmmo.server.game.storage

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Friends and block lists. Cached per user, loaded from [SocialDb] on first use and written
 * through on every change; without a database (tests) it is memory only.
 */
@Singleton
class SocialStore @Inject constructor(private val db: SocialDb? = null) {
  private val friendsByUser = ConcurrentHashMap<Int, MutableSet<String>>()
  private val blockedByUser = ConcurrentHashMap<Int, MutableSet<String>>()

  fun getFriends(userId: Int): Set<String> = friends(userId)

  fun addFriend(userId: Int, name: String) {
    if (friends(userId).add(name)) db?.addFriend(userId, name)
  }

  fun removeFriend(userId: Int, name: String): Boolean {
    val removed = friends(userId).remove(name)
    if (removed) db?.removeFriend(userId, name)
    return removed
  }

  fun getBlocked(userId: Int): Set<String> = blocked(userId)

  fun block(userId: Int, name: String) {
    if (blocked(userId).add(name)) db?.block(userId, name)
  }

  fun unblock(userId: Int, name: String): Boolean {
    val removed = blocked(userId).remove(name)
    if (removed) db?.unblock(userId, name)
    return removed
  }

  private fun friends(userId: Int): MutableSet<String> =
      friendsByUser.getOrPut(userId) {
        linkedSetOf<String>().also { set -> db?.friends(userId)?.let(set::addAll) }
      }

  private fun blocked(userId: Int): MutableSet<String> =
      blockedByUser.getOrPut(userId) {
        linkedSetOf<String>().also { set -> db?.blocked(userId)?.let(set::addAll) }
      }
}
