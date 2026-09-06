package de.fiereu.openmmo.server.game.storage

import javax.inject.Inject
import javax.inject.Singleton
import org.jooq.DSLContext

/** The friends and block lists in the game database (tables friends, blocked_players). */
@Singleton
class SocialDb @Inject constructor(private val dsl: DSLContext) {

  fun friends(userId: Int): List<String> =
      dsl.fetch("select name from friends where user_id = ? order by name", userId).map {
        it.get(0, String::class.java)
      }

  fun addFriend(userId: Int, name: String) {
    dsl.execute("insert into friends (user_id, name) values (?, ?) on conflict do nothing", userId, name)
  }

  fun removeFriend(userId: Int, name: String) {
    dsl.execute("delete from friends where user_id = ? and name = ?", userId, name)
  }

  fun blocked(userId: Int): List<String> =
      dsl.fetch("select name from blocked_players where user_id = ? order by name", userId).map {
        it.get(0, String::class.java)
      }

  fun block(userId: Int, name: String) {
    dsl.execute("insert into blocked_players (user_id, name) values (?, ?) on conflict do nothing", userId, name)
  }

  fun unblock(userId: Int, name: String) {
    dsl.execute("delete from blocked_players where user_id = ? and name = ?", userId, name)
  }
}
