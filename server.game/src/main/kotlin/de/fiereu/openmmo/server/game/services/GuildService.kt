package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.GuildPermission
import de.fiereu.openmmo.common.enums.GuildRank
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.ServerMessagePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildActivityLogEntry
import de.fiereu.openmmo.net.game.packets.guild.GuildActivityLogPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildActivityLogPageRequestPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildCreatePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildDisbandPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildInvitePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildLeavePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMemberEntry
import de.fiereu.openmmo.net.game.packets.guild.GuildMemberKickPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMemberRankAssignPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMembershipPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMotdUpdatePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildProfileData
import de.fiereu.openmmo.net.game.packets.guild.GuildRankLabelUpdatePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildRankPermissionUpdatePacket
import de.fiereu.openmmo.net.game.packets.guild.SyncGuildMembersPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.Guild
import de.fiereu.openmmo.server.game.storage.GuildMember
import de.fiereu.openmmo.server.game.storage.GuildStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

private const val GUILD_FOUND_COST = 15000

private const val STRING_NOT_ENOUGH_MONEY = 1927
private const val STRING_ALREADY_IN_TEAM = 2606
private const val STRING_INVALID_NAME = 2607
private const val STRING_NAME_TAKEN = 2608
private const val STRING_INVALID_TAG = 2609
private const val STRING_TAG_TAKEN = 2610
private const val STRING_DATABASE_ERROR = 2611

/**
 * The client-side rules for a new team (strings 2607 and 2609: "2-16 characters with no numbers or
 * symbols", "2-4 characters"), mirrored so a modified client cannot bypass them. Single spaces
 * between words are allowed in names.
 */
object GuildNames {
  private val NAME = Regex("[A-Za-z]+( [A-Za-z]+)*")
  private val TAG = Regex("[A-Za-z]{2,4}")

  fun validName(name: String): Boolean = name.length in 2..16 && NAME.matches(name)

  fun validTag(tag: String): Boolean = TAG.matches(tag)
}

// The entries list is length-prefixed with a single byte, so a page holds at most 255 entries.
private const val MAX_ACTIVITY_LOG_ENTRIES = 255

@Singleton
class GuildService
@Inject
constructor(
    private val guildStore: GuildStore,
    private val characterStore: CharacterStore,
    private val sessionRegistry: SessionRegistry,
    private val socialRequests: Provider<SocialRequestService>,
) {

  /**
   * Founding a team. Refusals are the client's own strings delivered as server messages, the way
   * the trade and link refusals are: 2606 already in a team, 2607/2609 name or tag rules, 2608/
   * 2610 name or tag taken (ignoring case), 1927 not enough money, 2611 database error. The fee
   * is charged only once everything passed, and the new balance is pushed so the money display
   * follows at once instead of on the next login.
   */
  suspend fun onCreateGuild(event: PacketEvent<GuildCreatePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val stored = characterStore.getCharacter(charId) ?: return
    val packet = event.packet
    val name = packet.guildName.trim()
    val tag = packet.guildTag.trim()
    log.info { "CreateGuild name='$name' tag='$tag' char=$charId money=${stored.info.money}" }
    val refusal =
        when {
          guildStore.getGuildForChar(charId) != null -> STRING_ALREADY_IN_TEAM
          !GuildNames.validName(name) -> STRING_INVALID_NAME
          !GuildNames.validTag(tag) -> STRING_INVALID_TAG
          guildStore.findByName(name) != null -> STRING_NAME_TAKEN
          guildStore.findByTag(tag) != null -> STRING_TAG_TAKEN
          stored.info.money < GUILD_FOUND_COST -> STRING_NOT_ENOUGH_MONEY
          else -> null
        }
    if (refusal != null) {
      log.info { "CreateGuild refused for char=$charId: string $refusal" }
      ctx.send(message(refusal))
      return
    }
    if (!characterStore.addMoney(charId, -GUILD_FOUND_COST)) {
      ctx.send(message(STRING_DATABASE_ERROR))
      return
    }
    val guild =
        try {
          guildStore.createGuild(name, tag, charId, stored.info.name)
        } catch (e: Exception) {
          // The unique index caught a founding that raced ours; the fee goes back.
          log.warn(e) { "CreateGuild '$name' [$tag] for char=$charId could not be stored" }
          characterStore.addMoney(charId, GUILD_FOUND_COST)
          ctx.send(message(STRING_DATABASE_ERROR))
          return
        }
    val balance = characterStore.getCharacter(charId)?.info?.money ?: (stored.info.money - GUILD_FOUND_COST)
    ctx.send(LocalCharacterDeltaPacket(money = balance))
    ctx.send(buildMembership(guild))
    ctx.send(buildMemberSync(guild))
  }

  private fun message(stringId: Int): ServerMessagePacket =
      ServerMessagePacket(stringId, emptyList(), showOnMap = true, mode = null)

  fun onActivityLogPageRequest(event: PacketEvent<GuildActivityLogPageRequestPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    val page = event.packet.pageIndex.toInt()
    log.info { "Guild activity log page=$page requested." }
    val packet = buildActivityLog(guild)
    log.info {
      "Sending guild activity log guild=${guild.id} sent=${packet.entries.size} total=${packet.totalCount}"
    }
    ctx.send(packet)
  }

  fun onGuildInvite(event: PacketEvent<GuildInvitePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    val target = event.packet.targetName
    if (guild.members.any { it.name.equals(target, ignoreCase = true) }) return
    log.info { "GuildInvite char=$charId target='$target' guild=${guild.id}" }
    // Retail asks first: the target sees "{00} has invited you to the team {01}" and joins only
    // on accept (SocialRequestService). An offline target cannot be invited.
    socialRequests.get().request(ctx, SocialRequestService.Kind.TEAM, target, extra = guild.name, guildId = guild.id)
  }

  /** The invited player accepted: they join under their real character id and both sides see the roster. */
  fun acceptInvite(guildId: Long, target: SessionContext, targetCharId: Long, targetName: String, inviter: SessionContext?) {
    val guild = guildStore.getGuild(guildId) ?: return
    if (guildStore.getGuildForChar(targetCharId) != null) return
    if (guild.members.none { it.id == targetCharId }) {
      guildStore.addMember(guild, GuildMember(targetCharId, targetName, GuildRank.GRUNT, leader = false))
    }
    log.info { "Guild ${guild.id} '${guild.name}': char=$targetCharId '$targetName' joined" }
    target.send(buildMembership(guild))
    target.send(buildMemberSync(guild))
    inviter?.send(buildMemberSync(guild))
  }

  private fun onlineSessionByName(name: String): Pair<Long, SessionContext>? =
      sessionRegistry.onlineCharacterIds().firstNotNullOfOrNull { id ->
        val stored = characterStore.getCharacter(id) ?: return@firstNotNullOfOrNull null
        if (!stored.info.name.equals(name, ignoreCase = true)) return@firstNotNullOfOrNull null
        sessionRegistry.getByCharacterId(id)?.let { id to it }
      }

  fun onRankAssign(event: PacketEvent<GuildMemberRankAssignPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    val rank = GuildRank.entries.getOrNull(event.packet.rankOrdinal) ?: return
    if (rank == GuildRank.BOSS) {
      guildStore.transferLeadership(guild, event.packet.memberEntityId)
      log.info { "Leadership transferred char=$charId newLeader=${event.packet.memberEntityId}" }
    } else {
      guildStore.setMemberRank(guild, event.packet.memberEntityId, rank)
      log.info { "RankAssign char=$charId member=${event.packet.memberEntityId} rank=$rank" }
    }
    ctx.send(buildMemberSync(guild))
  }

  fun onKick(event: PacketEvent<GuildMemberKickPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    guildStore.removeMember(guild, event.packet.targetEntityId)
    log.info { "Kick char=$charId member=${event.packet.targetEntityId}" }
    ctx.send(buildMemberSync(guild))
  }

  fun onLeave(event: PacketEvent<GuildLeavePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    guildStore.leaveGuild(charId)
    log.info { "GuildLeave char=$charId" }
    ctx.send(GuildMembershipPacket(inGuild = false, profile = null))
  }

  fun onDisband(event: PacketEvent<GuildDisbandPacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val initiate = event.packet.initiate
    val guild = guildStore.getGuildForChar(charId)
    log.info { "GuildDisband char=$charId guild=${guild?.id} initiate=$initiate" }
    if (guild == null) return
    // We disband immediately on initiate, so a follow-up cancel has no pending state to undo.
    if (!initiate) return
    guildStore.disbandGuild(charId)
    log.info { "Guild ${guild.id} disbanded by char=$charId" }
    // TODO: The guild window does not close after disbanding. Sending
    // GuildMembershipPacket(inGuild = false) updates the state (reopening the
    // window shows the create-guild screen) but does not dismiss the currently
    // open window. Check against the real game to see what packet closes it.
    ctx.send(GuildMembershipPacket(inGuild = false, profile = null))
  }

  fun onMotdUpdate(event: PacketEvent<GuildMotdUpdatePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    log.info { "GuildMotdUpdate char=$charId guild=${guild.id} motd='${event.packet.motdText}'" }
  }

  fun onRankLabelUpdate(event: PacketEvent<GuildRankLabelUpdatePacket>) {
    val ctx = event.session
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    val rank = GuildRank.entries.getOrNull(event.packet.rankOrdinal)
    log.info {
      "GuildRankLabelUpdate char=$charId guild=${guild.id} rank=$rank label='${event.packet.rankLabel}'"
    }
  }

  fun onRankPermissionUpdate(event: PacketEvent<GuildRankPermissionUpdatePacket>) {
    val state = event.session.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val guild = guildStore.getGuildForChar(charId) ?: return
    val sanitized =
        event.packet.permissions.mapValues { (rank, perms) ->
          if (rank == GuildRank.GRUNT && GuildPermission.KICK in perms) {
            log.warn { "Rejecting KICK permission for GRUNT" }
            perms - GuildPermission.KICK
          } else {
            perms
          }
        }
    guild.permissions.clear()
    guild.permissions.putAll(sanitized)
    guildStore.savePermissions(guild)
    log.info { "RankPermUpdate char=$charId perms=$sanitized" }
  }

  /**
   * On entering the world: the team this character belongs to, so it survives a relog and a
   * server restart. A member invited before their character id was known (recorded under a
   * placeholder id by name) is matched by name and takes their real id here.
   */
  fun sendMembership(ctx: SessionContext) {
    val state = ctx.attributes[PLAYER_STATE] ?: return
    val charId = state.characterId ?: return
    val name = characterStore.getCharacter(charId)?.info?.name ?: return
    val guild =
        guildStore.getGuildForChar(charId)
            ?: guildStore.getGuildForName(name)?.also { guildStore.adoptMemberId(it, name, charId) }
            ?: return
    log.info { "Guild membership char=$charId guild=${guild.id} '${guild.name}'" }
    ctx.send(buildMembership(guild))
    ctx.send(buildMemberSync(guild))
  }

  private fun buildMembership(guild: Guild): GuildMembershipPacket =
      GuildMembershipPacket(
          inGuild = true,
          profile =
              GuildProfileData(
                  guildId = guild.id,
                  name = guild.name,
                  tag = guild.tag,
                  foundedAt = 0,
                  message = "Your Team has been successfully created!",
                  updatedAt = 0,
                  value1 = 5,
                  value2 = 5,
                  value3 = 5,
                  value4 = 0,
                  value5 = 0,
                  unk1 = 0,
                  rankCount = GuildRank.entries.size,
                  unk2 = 0,
                  unk3 = 0,
                  flag = 0,
              ),
      )

  private fun buildMemberSync(guild: Guild): SyncGuildMembersPacket =
      SyncGuildMembersPacket(
          replace = true,
          members =
              guild.members.map { member ->
                GuildMemberEntry(
                    entityId = member.id,
                    rank = member.rank.ordinal.toByte(),
                    joinedAt = 0,
                    name = member.name,
                    online = true,
                    lastSeen = 0,
                    appearance = List(5) { 0 },
                    leader = member.leader,
                )
              },
      )

  private fun buildActivityLog(guild: Guild): GuildActivityLogPacket =
      GuildActivityLogPacket(
          totalCount = guild.activityLog.size.toShort(),
          entries =
              guild.activityLog.takeLast(MAX_ACTIVITY_LOG_ENTRIES).map { entry ->
                GuildActivityLogEntry(
                    type = entry.type.code,
                    actor = entry.actor,
                    target = entry.target,
                    timestamp = entry.timestamp,
                )
              },
      )

  private fun syntheticId(name: String): Long = (name.hashCode().toLong() shl 16) or 0x9000L
}
