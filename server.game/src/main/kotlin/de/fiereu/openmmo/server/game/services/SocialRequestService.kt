package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.DuelChallengePacket
import de.fiereu.openmmo.net.game.packets.DuelInvitePacket
import de.fiereu.openmmo.net.game.packets.LinkRequestPacket
import de.fiereu.openmmo.net.game.packets.ServerMessageArg
import de.fiereu.openmmo.net.game.packets.ServerMessagePacket
import de.fiereu.openmmo.net.game.packets.TradeActionPacket
import de.fiereu.openmmo.net.game.packets.TradeRequestPacket
import de.fiereu.openmmo.net.game.packets.TradeSelectMonPacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionPacket
import de.fiereu.openmmo.server.game.session.PENDING_SOCIAL_REQUESTS
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * The ask-first handshake behind trade, link, friend, team and duel requests.
 *
 * Bytecode-verified against client 31914: a request reaches the target as an ordinary dialog
 * action packet (s2c 0x21) whose kind byte selects the prompt window - f/qM1 wire 16 challenge
 * (f/O21, buttons 200/201), 17 friend (f/oT0, string 2201), 18 trade (f/Jp, 203/204, string
 * 2202), 19 link (f/Fn1, 2204), 20 team invite (f/W8, 2203, two strings). The payload after the
 * common header is the requester's name (UTF-16, nul); the team kind adds the team name and the
 * challenge kind adds the battle rules. The flags byte is echoed back: the window answers with
 * c2s 0x21 DialogActionResponse(id = that byte, code) where 1 = accept (f/c10.qe), 0 = reject,
 * 3 = auto-declined or superseded, 4 = block. Nothing here is a guess from packet names.
 */
@Singleton
class SocialRequestService
@Inject
constructor(
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
    private val guildService: Provider<GuildService>,
    private val socialService: Provider<SocialService>,
) {

  enum class Kind(val wire: Int, val accepted: Int, val rejected: Int, val autoDeclined: Int) {
    CHALLENGE(16, 6026, 6027, 6017),
    FRIEND(17, 6020, 6021, 6019),
    TRADE(18, 6029, 6030, 6016),
    LINK(19, 6046, 6047, 6045),
    TEAM(20, 6023, 6024, 6018),
  }

  class Pending(
      val kind: Kind,
      val requesterCharId: Long,
      val requesterName: String,
      val requesterUserId: Int,
      val guildId: Long = 0,
      val challenge: DuelChallengePacket? = null,
  )

  fun onTradeRequest(event: PacketEvent<TradeRequestPacket>) =
      request(event.session, Kind.TRADE, event.packet.targetName)

  fun onLinkRequest(event: PacketEvent<LinkRequestPacket>) =
      request(event.session, Kind.LINK, event.packet.targetName)

  fun onDuelChallenge(event: PacketEvent<DuelChallengePacket>) =
      request(event.session, Kind.CHALLENGE, event.packet.targetPlayerName, challenge = event.packet)

  /** The trade window's buttons (0/1/2). The trade itself is not implemented yet: logged. */
  fun onTradeAction(event: PacketEvent<TradeActionPacket>) {
    log.info { "TradeAction char=${event.session.attributes[PLAYER_STATE]?.characterId} action=${event.packet.action}" }
  }

  fun onTradeSelectMon(event: PacketEvent<TradeSelectMonPacket>) {
    log.info { "TradeSelectMon char=${event.session.attributes[PLAYER_STATE]?.characterId} slot=${event.packet.slotIndex}" }
  }

  /**
   * Sends the prompt for [kind] to the player called [targetName]. Returns false when the target
   * is not online (the requester is told with string 1540 "Player {00} could not be found.").
   */
  fun request(
      requester: SessionContext,
      kind: Kind,
      targetName: String,
      extra: String = "",
      guildId: Long = 0,
      challenge: DuelChallengePacket? = null,
  ): Boolean {
    val state = requester.attributes[PLAYER_STATE] ?: return false
    val charId = state.characterId ?: return false
    val me = characterStore.getCharacter(charId) ?: return false
    if (me.info.name.equals(targetName, ignoreCase = true)) {
      if (kind == Kind.LINK) requester.send(message(6044))
      return false
    }
    val target = onlineByName(targetName)
    if (target == null) {
      requester.send(message(1540, targetName))
      log.info { "${kind.name} request char=$charId -> '$targetName': not online" }
      return false
    }
    val (targetCharId, targetCtx) = target
    val targetState = targetCtx.attributes[PLAYER_STATE] ?: return false
    val pending = targetCtx.attributes[PENDING_SOCIAL_REQUESTS] ?: ConcurrentHashMap<Int, Pending>().also { targetCtx.attributes[PENDING_SOCIAL_REQUESTS] = it }
    // Ids share the dialog sequence so a prompt never collides with a script's box.
    val id = targetState.dialogSeqId and 0xFF
    targetState.dialogSeqId = targetState.dialogSeqId + 1
    pending[id] = Pending(kind, charId, me.info.name, state.userId, guildId, challenge)
    val detail =
        utf16(me.info.name) +
            when (kind) {
              Kind.TEAM -> utf16(extra)
              Kind.CHALLENGE -> challengeRules(challenge)
              else -> ByteArray(0)
            }
    targetCtx.send(
        DialogActionPacket(
            flags = id.toByte(),
            actionType = kind.wire.toByte(),
            textId = 0,
            entityId = charId,
            contextValue = 0,
            messageArgs = emptyList(),
            detail = detail,
        ))
    log.info { "${kind.name} request id=$id char=$charId '${me.info.name}' -> char=$targetCharId '$targetName'" }
    return true
  }

  /** The target's answer, routed here from the dialog response. True when it was ours. */
  fun onAnswer(target: SessionContext, id: Int, code: Int): Boolean {
    val pending = target.attributes[PENDING_SOCIAL_REQUESTS]?.remove(id) ?: return false
    val targetState = target.attributes[PLAYER_STATE] ?: return true
    val targetCharId = targetState.characterId ?: return true
    val targetName = characterStore.getCharacter(targetCharId)?.info?.name ?: return true
    val requester = sessionRegistry.getByCharacterId(pending.requesterCharId)
    log.info { "${pending.kind.name} request id=$id answered code=$code by char=$targetCharId '$targetName'" }
    if (code != ACCEPT) {
      requester?.send(message(if (code == AUTO_DECLINED) pending.kind.autoDeclined else pending.kind.rejected, targetName))
      return true
    }
    requester?.send(message(pending.kind.accepted, targetName))
    when (pending.kind) {
      Kind.FRIEND -> socialService.get().completeFriendship(pending.requesterUserId, pending.requesterName, requester, targetState.userId, targetName, target)
      Kind.TEAM -> guildService.get().acceptInvite(pending.guildId, target, targetCharId, targetName, requester)
      Kind.TRADE -> {
        // Opens the trade window on both sides (s2c 0x50 f/jm0: flags, my side, peer name). The
        // trade's own packets (0x52 offers, 0x50 actions, 0x51 outcome) are still to be built.
        requester?.send(DuelInvitePacket(0.toByte(), 0.toByte(), targetName))
        target.send(DuelInvitePacket(0.toByte(), 1.toByte(), pending.requesterName))
        log.info { "Trade window opened between '${pending.requesterName}' and '$targetName' (trade mechanics not implemented yet)" }
      }
      Kind.LINK -> log.info { "Link accepted between '${pending.requesterName}' and '$targetName' (links not implemented yet)" }
      Kind.CHALLENGE -> log.info { "Duel accepted between '${pending.requesterName}' and '$targetName' (player battles not implemented yet)" }
    }
    return true
  }

  fun onlineByName(name: String): Pair<Long, SessionContext>? =
      sessionRegistry.onlineCharacterIds().firstNotNullOfOrNull { id ->
        val stored = characterStore.getCharacter(id) ?: return@firstNotNullOfOrNull null
        if (!stored.info.name.equals(name, ignoreCase = true)) return@firstNotNullOfOrNull null
        sessionRegistry.getByCharacterId(id)?.let { id to it }
      }

  private fun challengeRules(c: DuelChallengePacket?): ByteArray {
    // Mirrors the client's own reader for the challenge prompt: the rules block the challenger
    // sent, then the battle type. A missing challenge means default singles.
    if (c == null) return byteArrayOf(0, 0, 0, 0, 0, 0)
    val out = ArrayList<Byte>()
    var flags = 0
    if (c.timed) flags = flags or 1
    if (c.itemLevelCap != null) flags = flags or 8
    if (c.natureCap != null) flags = flags or 16
    if (c.tier != null) flags = flags or 64
    out += flags.toByte()
    out += c.battleFormat
    out += c.typeRestriction
    out += c.natureRestriction
    out += c.allowedFormat
    c.itemLevelCap?.let { out += it }
    c.natureCap?.let { out += it }
    c.tier?.let { out += it }
    out += c.battleTypeId
    return out.toByteArray()
  }

  private fun utf16(s: String): ByteArray = s.toByteArray(Charsets.UTF_16LE) + byteArrayOf(0, 0)

  private fun message(stringId: Int, vararg names: String): ServerMessagePacket =
      ServerMessagePacket(
          stringId,
          names.mapIndexed { i, n -> ServerMessageArg(i.toByte(), RAW_STRING, false, 0, null, null, n, null) },
          showOnMap = true,
          mode = null)

  private companion object {
    const val ACCEPT = 1
    const val AUTO_DECLINED = 3
    const val RAW_STRING = 5
  }
}
