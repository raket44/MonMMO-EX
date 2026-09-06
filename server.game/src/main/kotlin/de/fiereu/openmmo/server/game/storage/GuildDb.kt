package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.enums.GuildPermission
import de.fiereu.openmmo.common.enums.GuildRank
import javax.inject.Inject
import javax.inject.Singleton
import org.jooq.DSLContext

/** Teams in the game database (tables guilds, guild_members, guild_rank_permissions, guild_log). */
@Singleton
class GuildDb @Inject constructor(private val dsl: DSLContext) {

  fun loadAll(): List<Guild> {
    val guilds =
        dsl.fetch("select id, name, tag from guilds order by id").map { r ->
          Guild(r.get(0, Long::class.java), r.get(1, String::class.java), r.get(2, String::class.java), mutableListOf())
        }
    val byId = guilds.associateBy { it.id }
    dsl.fetch("select guild_id, char_id, name, rank, leader from guild_members order by guild_id, position, char_id")
        .forEach { r ->
          byId[r.get(0, Long::class.java)]
              ?.members
              ?.add(
                  GuildMember(
                      r.get(1, Long::class.java),
                      r.get(2, String::class.java),
                      rank(r.get(3, Int::class.java)),
                      r.get(4, Boolean::class.java)))
        }
    dsl.fetch("select guild_id, rank, permissions from guild_rank_permissions").forEach { r ->
      val bits = r.get(2, Int::class.java)
      byId[r.get(0, Long::class.java)]
          ?.permissions
          ?.put(rank(r.get(1, Int::class.java)), GuildPermission.entries.filter { bits and (1 shl it.ordinal) != 0 }.toSet())
    }
    dsl.fetch("select guild_id, type, actor, target, ts from guild_log order by id").forEach { r ->
      val type = GuildActivityType.entries.firstOrNull { it.code == r.get(1, Int::class.java) } ?: return@forEach
      byId[r.get(0, Long::class.java)]
          ?.activityLog
          ?.add(GuildLogEntry(type, r.get(2, String::class.java), r.get(3, String::class.java), r.get(4, Int::class.java)))
    }
    return guilds
  }

  fun insertGuild(name: String, tag: String): Long =
      dsl.fetchOne("insert into guilds (name, tag) values (?, ?) returning id", name, tag)!!.get(0, Long::class.java)

  fun saveMember(guildId: Long, member: GuildMember, position: Int) {
    dsl.execute(
        "insert into guild_members (char_id, guild_id, name, rank, leader, position) values (?, ?, ?, ?, ?, ?) " +
            "on conflict (char_id) do update set guild_id = excluded.guild_id, name = excluded.name, " +
            "rank = excluded.rank, leader = excluded.leader, position = excluded.position",
        member.id,
        guildId,
        member.name,
        member.rank.ordinal,
        member.leader,
        position)
  }

  fun deleteMember(charId: Long) {
    dsl.execute("delete from guild_members where char_id = ?", charId)
  }

  fun deleteGuild(guildId: Long) {
    dsl.execute("delete from guilds where id = ?", guildId)
  }

  fun savePermissions(guildId: Long, permissions: Map<GuildRank, Set<GuildPermission>>) {
    dsl.execute("delete from guild_rank_permissions where guild_id = ?", guildId)
    for ((rank, perms) in permissions) {
      val bits = perms.fold(0) { acc, p -> acc or (1 shl p.ordinal) }
      dsl.execute(
          "insert into guild_rank_permissions (guild_id, rank, permissions) values (?, ?, ?)", guildId, rank.ordinal, bits)
    }
  }

  fun insertLog(guildId: Long, entry: GuildLogEntry) {
    dsl.execute(
        "insert into guild_log (guild_id, type, actor, target, ts) values (?, ?, ?, ?, ?)",
        guildId,
        entry.type.code,
        entry.actor,
        entry.target,
        entry.timestamp)
  }

  private fun rank(ordinal: Int): GuildRank = GuildRank.entries[ordinal.coerceIn(GuildRank.entries.indices)]
}
