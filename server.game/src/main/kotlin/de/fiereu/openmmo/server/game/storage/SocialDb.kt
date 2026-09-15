package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.packets.PlayerHead
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import org.jooq.DSLContext

/** What a friend list or team roster shows of a character that is not online: last seen and head. */
data class FriendProfile(val lastSeen: LocalDateTime?, val head: PlayerHead)

/**
 * One character's profile from rows of (key, last seen, skin tone, slot, type, colour, variant) - a
 * character left-joined to its outfit rows, as SocialDb and GuildDb select them.
 */
internal fun profileOf(rows: List<org.jooq.Record>): FriendProfile {
  val first = rows.first()
  val skins =
      rows
          .mapNotNull { r ->
            val slot = r.get(3, String::class.java)?.let { name -> SkinSlot.entries.firstOrNull { it.name == name } }
                ?: return@mapNotNull null
            Skin(slot, r.get(4, Int::class.java)?.toUShort(), r.get(5, Int::class.java)?.toUByte(), (r.get(6, Int::class.java) ?: 0).toUByte())
          }
          .associateBy { it.slot }
  return FriendProfile(first.get(1, LocalDateTime::class.java), PlayerHead(first.get(2, Int::class.java) ?: 0, skins))
}

/** The profile a list shows for a character still in memory. */
internal fun StoredCharacter.profile(): FriendProfile =
    FriendProfile(info.lastLogout ?: info.lastLogin, PlayerHead(info.skinRegionSelectionIndex, skins))

/** The friends and block lists in the game database (tables friends, blocked_players). */
@Singleton
class SocialDb @Inject constructor(private val dsl: DSLContext) {

  /** Each friend's name with the moment they were added. */
  fun friends(userId: Int): List<Pair<String, LocalDateTime>> =
      dsl.fetch("select name, added_at from friends where user_id = ? order by name", userId).map {
        it.get(0, String::class.java) to it.get(1, LocalDateTime::class.java)
      }

  fun addFriend(userId: Int, name: String, addedAt: LocalDateTime) {
    dsl.execute(
        "insert into friends (user_id, name, added_at) values (?, ?, ?) on conflict do nothing", userId, name, addedAt)
  }

  fun removeFriend(userId: Int, name: String) {
    dsl.execute("delete from friends where user_id = ? and name = ?", userId, name)
  }

  /**
   * What a list shows of each named character, keyed by lowercase name: when it was last in the world
   * (its last logout, or its last login for one that has not left since the column existed) and its
   * head (skin tone and outfit slots).
   */
  fun profiles(names: Collection<String>): Map<String, FriendProfile> {
    if (names.isEmpty()) return emptyMap()
    val lowered = names.map { it.lowercase() }.distinct()
    val rows =
        dsl.fetch(
            "select lower(c.name), coalesce(c.last_logout, c.last_login), c.skin_region_selection_index, " +
                "s.slot, s.skin_type, s.skin_color, s.skin_variant from characters c " +
                "left join character_skins s on s.character_id = c.id where lower(c.name) in (" +
                lowered.joinToString(",") { "?" } + ")",
            *lowered.toTypedArray())
    return rows
        .groupBy { it.get(0, String::class.java) }
        .mapValues { (_, byName) -> profileOf(byName) }
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
