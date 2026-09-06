package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.enums.GuildPermission
import de.fiereu.openmmo.common.enums.GuildRank
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class GuildMember(
    val id: Long,
    val name: String,
    val rank: GuildRank,
    val leader: Boolean,
)

// Wire codes are provisional and unverified against the real client.
enum class GuildActivityType(val code: Int) {
  FOUNDED(0),
  JOINED(1),
}

data class GuildLogEntry(
    val type: GuildActivityType,
    val actor: String,
    val target: String,
    val timestamp: Int,
)

data class Guild(
    val id: Long,
    val name: String,
    val tag: String,
    val members: MutableList<GuildMember>,
    val permissions: MutableMap<GuildRank, Set<GuildPermission>> = mutableMapOf(),
    val activityLog: MutableList<GuildLogEntry> = mutableListOf(),
)

/**
 * Teams. Held in memory for speed, loaded from [GuildDb] once on first use and written through
 * on every change; without a database (tests) it is memory only. Every mutation goes through
 * here so the database never drifts from what players see.
 */
@Singleton
class GuildStore @Inject constructor(private val db: GuildDb? = null) {
  private val guilds = ConcurrentHashMap<Long, Guild>()
  private val guildByChar = ConcurrentHashMap<Long, Long>()
  private val nextId = AtomicLong(1)
  @Volatile private var loaded = db == null

  fun createGuild(name: String, tag: String, leaderId: Long, leaderName: String): Guild {
    ensureLoaded()
    val leader = GuildMember(leaderId, leaderName, GuildRank.BOSS, leader = true)
    val id = db?.insertGuild(name, tag) ?: nextId.getAndIncrement()
    val guild = Guild(id, name, tag, mutableListOf(leader))
    val founded = GuildLogEntry(GuildActivityType.FOUNDED, leaderName, "", now())
    guild.activityLog.add(founded)
    guilds[guild.id] = guild
    guildByChar[leaderId] = guild.id
    db?.saveMember(guild.id, leader, 0)
    db?.insertLog(guild.id, founded)
    return guild
  }

  fun getGuildForChar(charId: Long): Guild? {
    ensureLoaded()
    return guildByChar[charId]?.let { guilds[it] }
  }

  /** The guild holding a member of this name, for a member recorded before their id was known. */
  fun getGuildForName(name: String): Guild? {
    ensureLoaded()
    return guilds.values.firstOrNull { g -> g.members.any { it.name.equals(name, ignoreCase = true) } }
  }

  fun addMember(guild: Guild, member: GuildMember) {
    guild.members.add(member)
    guildByChar[member.id] = guild.id
    val joined = GuildLogEntry(GuildActivityType.JOINED, member.name, "", now())
    guild.activityLog.add(joined)
    db?.saveMember(guild.id, member, guild.members.size - 1)
    db?.insertLog(guild.id, joined)
  }

  /** A member recorded under a placeholder id (invited while offline) takes their real id. */
  fun adoptMemberId(guild: Guild, name: String, realId: Long) {
    val index = guild.members.indexOfFirst { it.name.equals(name, ignoreCase = true) }
    if (index < 0) return
    val old = guild.members[index]
    if (old.id == realId) return
    guildByChar.remove(old.id)
    guild.members[index] = old.copy(id = realId)
    guildByChar[realId] = guild.id
    db?.deleteMember(old.id)
    db?.saveMember(guild.id, guild.members[index], index)
  }

  fun setMemberRank(guild: Guild, entityId: Long, rank: GuildRank) {
    val index = guild.members.indexOfFirst { it.id == entityId }
    if (index >= 0) {
      guild.members[index] = guild.members[index].copy(rank = rank)
      db?.saveMember(guild.id, guild.members[index], index)
    }
  }

  fun removeMember(guild: Guild, entityId: Long) {
    if (guild.members.removeAll { it.id == entityId }) {
      guildByChar.remove(entityId)
      db?.deleteMember(entityId)
    }
  }

  fun transferLeadership(guild: Guild, newLeaderId: Long) {
    for (i in guild.members.indices) {
      val member = guild.members[i]
      val updated =
          when {
            member.id == newLeaderId -> member.copy(rank = GuildRank.BOSS, leader = true)
            member.leader || member.rank == GuildRank.BOSS ->
                member.copy(rank = GuildRank.EXECUTIVE, leader = false)
            else -> member
          }
      if (updated != member) {
        guild.members[i] = updated
        db?.saveMember(guild.id, updated, i)
      }
    }
  }

  fun savePermissions(guild: Guild) {
    db?.savePermissions(guild.id, guild.permissions)
  }

  fun leaveGuild(charId: Long) {
    val guildId = guildByChar.remove(charId) ?: return
    guilds[guildId]?.members?.removeAll { it.id == charId }
    db?.deleteMember(charId)
  }

  fun disbandGuild(charId: Long) {
    val guildId = guildByChar[charId] ?: return
    guilds.remove(guildId)
    guildByChar.entries.removeIf { it.value == guildId }
    db?.deleteGuild(guildId)
  }

  private fun ensureLoaded() {
    if (loaded) return
    synchronized(this) {
      if (loaded) return
      for (guild in db!!.loadAll()) {
        guilds[guild.id] = guild
        for (m in guild.members) guildByChar[m.id] = guild.id
      }
      loaded = true
    }
  }

  private fun now(): Int = Instant.now().epochSecond.toInt()
}
