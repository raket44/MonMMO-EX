package de.fiereu.openmmo.server.game.handler

import de.fiereu.network.PacketEvent
import de.fiereu.network.Side
import de.fiereu.network.coroutines.CoroutineProtocolHandler
import de.fiereu.openmmo.common.enums.ChatType
import de.fiereu.openmmo.common.enums.Language
import de.fiereu.openmmo.net.game.GameProtocol
import de.fiereu.openmmo.net.game.packets.AddFriendPacket
import de.fiereu.openmmo.net.game.packets.BlockPlayerPacket
import de.fiereu.openmmo.net.game.packets.CancelSocialInteractionPacket
import de.fiereu.openmmo.net.game.packets.DuelChallengePacket
import de.fiereu.openmmo.net.game.packets.LinkKickMemberPacket
import de.fiereu.openmmo.net.game.packets.LinkRequestPacket
import de.fiereu.openmmo.net.game.packets.TradeActionPacket
import de.fiereu.openmmo.net.game.packets.TradeRequestPacket
import de.fiereu.openmmo.net.game.packets.TradeSelectMonPacket
import de.fiereu.openmmo.net.game.packets.ChatMessagePacket
import de.fiereu.openmmo.net.game.packets.ChatMessageSendPacket
import de.fiereu.openmmo.net.game.packets.MonsterRecordBookPacket
import de.fiereu.openmmo.net.game.packets.ContainerActionPacket
import de.fiereu.openmmo.net.game.packets.CosmeticSlotApplyPacket
import de.fiereu.openmmo.net.game.packets.CreateCharacterPacket
import de.fiereu.openmmo.net.game.packets.CustomizeCharacterAppearancePacket
import de.fiereu.openmmo.net.game.packets.DeleteCharacterPacket
import de.fiereu.openmmo.net.game.packets.DialogChoicePacket
import de.fiereu.openmmo.net.game.packets.EntityInteractPacket
import de.fiereu.openmmo.net.game.packets.ExchangeItemRequestPacket
import de.fiereu.openmmo.net.game.packets.FaceDirectionPacket
import de.fiereu.openmmo.net.game.packets.JoinPacket
import de.fiereu.openmmo.net.game.packets.KeepAlivePacket
import de.fiereu.openmmo.net.game.packets.MapLoadedAckPacket
import de.fiereu.openmmo.net.game.packets.MovementPacket
import de.fiereu.openmmo.net.game.packets.NullPacket
import de.fiereu.openmmo.net.game.packets.PartyReorderPacket
import de.fiereu.openmmo.net.game.packets.RemoveFriendPacket
import de.fiereu.openmmo.net.game.packets.RequestCharactersPacket
import de.fiereu.openmmo.net.game.packets.RequestPlayerPacket
import de.fiereu.openmmo.net.game.packets.RequestSocialProfilePacket
import de.fiereu.openmmo.net.game.packets.SelectCharacterPacket
import de.fiereu.openmmo.net.game.packets.ShopSellRequestPacket
import de.fiereu.openmmo.net.game.packets.TileInteractPacket
import de.fiereu.openmmo.net.game.packets.UnblockPlayerPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleActionSelectPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleActionSubmitPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleAppearancePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleCancelRequestPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleChatMessagePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleLeavePacket
import de.fiereu.openmmo.net.game.packets.battle.BattlePartySlotSelectPacket
import de.fiereu.openmmo.net.game.packets.ReleasePokemonPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleReadyPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleRewardSelectPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSequencePacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSimulationRequestPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSlotActionPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleSwitchSelectionsPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleTargetPickPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleTeamPreviewConfirmPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleTierSelectPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleTransitionReadyPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleUseItemPacket
import de.fiereu.openmmo.net.game.packets.battle.moves.MoveLearnReplyPacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionResponsePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildActivityLogPageRequestPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildCreatePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildDisbandPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildInvitePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildLeavePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMemberKickPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMemberRankAssignPacket
import de.fiereu.openmmo.net.game.packets.guild.GuildMotdUpdatePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildRankLabelUpdatePacket
import de.fiereu.openmmo.net.game.packets.guild.GuildRankPermissionUpdatePacket
import de.fiereu.openmmo.server.game.script.ScriptRunner
import de.fiereu.openmmo.server.game.services.AppearanceService
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.BreedingService
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.GuildService
import de.fiereu.openmmo.server.game.services.InteractionService
import de.fiereu.openmmo.server.game.services.InventoryActionService
import de.fiereu.openmmo.server.game.services.LoginService
import de.fiereu.openmmo.server.game.services.MovementService
import de.fiereu.openmmo.server.game.services.MultiplayerService
import de.fiereu.openmmo.server.game.services.PresenceService
import de.fiereu.openmmo.server.game.services.ShopService
import de.fiereu.openmmo.server.game.services.LinkService
import de.fiereu.openmmo.server.game.services.SocialRequestService
import de.fiereu.openmmo.server.game.services.TradeService
import de.fiereu.openmmo.server.game.services.SocialService
import de.fiereu.openmmo.server.game.services.command.ChatCommandService
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

private val log = KotlinLogging.logger {}

class GameAppHandler
@Inject
constructor(
    private val loginService: LoginService,
    private val movementService: MovementService,
    private val dialogService: DialogService,
    private val interactionService: InteractionService,
    private val multiplayerService: MultiplayerService,
    private val presenceService: PresenceService,
    private val socialService: SocialService,
    private val guildService: GuildService,
    private val socialRequestService: SocialRequestService,
    private val tradeService: TradeService,
    private val linkService: LinkService,
    private val battleService: BattleService,
    private val chatCommandService: ChatCommandService,
    private val shopService: ShopService,
    private val breedingService: BreedingService,
    private val inventoryActionService: InventoryActionService,
    private val evolutionService: de.fiereu.openmmo.server.game.services.EvolutionService,
    private val appearanceService: AppearanceService,
    private val mapTourService: de.fiereu.openmmo.server.game.services.MapTourService,
    private val scriptRunner: ScriptRunner,
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
    private val storyPlayerService: de.fiereu.openmmo.server.game.services.StoryPlayerService,
    private val flyService: de.fiereu.openmmo.server.game.services.FlyService,
    private val encounterTracker: de.fiereu.openmmo.server.game.services.EncounterTrackerService,
    private val speciesRegistry: de.fiereu.openmmo.pokemon.SpeciesRegistry,
    private val chatLinkService: de.fiereu.openmmo.server.game.services.ChatLinkService,
    private val releaseService: de.fiereu.openmmo.server.game.services.ReleaseService,
    scope: CoroutineScope,
) : CoroutineProtocolHandler<GameProtocol>(GameProtocol, Side.SERVER, scope) {

  init {
    onSuspend<JoinPacket> { event -> loginService.onJoinGame(event) }
    onSuspend<CreateCharacterPacket> { event -> loginService.onCreateCharacter(event) }
    onSuspend<RequestCharactersPacket> { event -> loginService.onCharacterRequest(event) }
    onSuspend<SelectCharacterPacket> { event -> loginService.onCharacterSelected(event) }
    onSuspend<DeleteCharacterPacket> { event -> loginService.onDeleteCharacter(event) }
    onSuspend<RequestPlayerPacket> { event -> loginService.onRequestPlayer(event) }

    onSuspend<MovementPacket> { event -> movementService.onMovement(event) }
    onSuspend<FaceDirectionPacket> { event -> movementService.onFaceDirection(event) }

    onSuspend<EntityInteractPacket> { event -> interactionService.onEntityInteract(event) }
    on<de.fiereu.openmmo.net.game.packets.EntityActionRequestPacket> { event -> interactionService.onFieldMoveRequest(event) }
    onSuspend<TileInteractPacket> { event -> interactionService.onTileInteract(event) }
    onSuspend<DialogActionResponsePacket> { event -> dialogService.onInteractive(event) }
    onSuspend<DialogChoicePacket> { event -> dialogService.onDialogChoice(event) }
    on<de.fiereu.openmmo.net.game.packets.EncounterTrackerPinPacket> { event -> encounterTracker.onPin(event) }
    onSuspend<de.fiereu.openmmo.net.game.packets.ChatLinkInspectRequestPacket> { event -> chatLinkService.onInspect(event) }
    onSuspend<ExchangeItemRequestPacket> { event -> shopService.onBuy(event) }
    onSuspend<ShopSellRequestPacket> { event -> shopService.onSell(event) }

    on<de.fiereu.openmmo.net.game.packets.AssignBreedingSlotPacket> { event ->
      breedingService.onAssignSlot(event)
    }
    on<de.fiereu.openmmo.net.game.packets.SubmitBreedingPartyPacket> { event ->
      breedingService.onSubmit(event)
    }

    onSuspend<ContainerActionPacket> { event -> inventoryActionService.onContainerAction(event) }
    on<PartyReorderPacket> { event -> inventoryActionService.onPartyReorder(event) }
    on<de.fiereu.openmmo.net.game.packets.ChannelChangePacket> { event ->
      // The Change Channel window. This server runs one channel, so the switch is declined in the
      // client's own words instead of dropping the frame.
      val channel = event.packet.channel + 1
      log.info { "Channel change to ch.$channel preferred=${event.packet.preferred} from ${event.session}" }
      event.session.send(
          de.fiereu.openmmo.server.game.services.notice(
              "Channel $channel is not available: this server runs a single channel."))
    }
    onSuspend<de.fiereu.openmmo.net.game.packets.FlyRequestPacket> { event -> flyService.onFly(event) }
    on<de.fiereu.openmmo.net.game.packets.PartyMemberSelectPacket> { event ->
      presenceService.onPartyMemberSelect(event)
    }
    onSuspend<de.fiereu.openmmo.net.game.packets.PokemonListAddPacket> { event ->
      inventoryActionService.onGiveHeldItem(event)
    }
    onSuspend<de.fiereu.openmmo.net.game.packets.EvolutionPromptResponsePacket> { event ->
      evolutionService.onResponse(event)
    }
    on<CustomizeCharacterAppearancePacket> { event ->
      appearanceService.onCustomizeAppearance(event)
    }

    on<AddFriendPacket> { event -> socialService.onAddFriend(event) }
    on<RemoveFriendPacket> { event -> socialService.onRemoveFriend(event) }
    on<BlockPlayerPacket> { event -> socialService.onBlockPlayer(event) }
    on<UnblockPlayerPacket> { event -> socialService.onUnblockPlayer(event) }
    on<RequestSocialProfilePacket> { event -> socialService.onRequestSocialProfile(event) }
    on<CancelSocialInteractionPacket> { event -> socialService.onCancelSocialInteraction(event) }
    on<TradeRequestPacket> { event -> socialRequestService.onTradeRequest(event) }
    on<LinkRequestPacket> { event -> socialRequestService.onLinkRequest(event) }
    on<DuelChallengePacket> { event -> socialRequestService.onDuelChallenge(event) }
    onSuspend<TradeActionPacket> { event -> tradeService.onAction(event) }
    on<TradeSelectMonPacket> { event -> tradeService.onMoney(event) }
    on<LinkKickMemberPacket> { event -> linkService.onKick(event) }

    onSuspend<GuildCreatePacket> { event -> guildService.onCreateGuild(event) }
    onSuspend<GuildInvitePacket> { event -> guildService.onGuildInvite(event) }
    onSuspend<GuildRankPermissionUpdatePacket> { event ->
      guildService.onRankPermissionUpdate(event)
    }
    onSuspend<GuildMemberRankAssignPacket> { event -> guildService.onRankAssign(event) }
    onSuspend<GuildMemberKickPacket> { event -> guildService.onKick(event) }
    onSuspend<GuildLeavePacket> { event -> guildService.onLeave(event) }
    onSuspend<GuildDisbandPacket> { event -> guildService.onDisband(event) }
    onSuspend<GuildMotdUpdatePacket> { event -> guildService.onMotdUpdate(event) }
    onSuspend<GuildRankLabelUpdatePacket> { event -> guildService.onRankLabelUpdate(event) }
    onSuspend<GuildActivityLogPageRequestPacket> { event ->
      guildService.onActivityLogPageRequest(event)
    }

    onSuspend<MoveLearnReplyPacket> { event -> battleService.onMoveLearnReply(event) }
    onSuspend<ReleasePokemonPacket> { event -> releaseService.onRelease(event) }
    on<CosmeticSlotApplyPacket> { event -> appearanceService.onSlotApply(event) }
    onSuspend<BattleActionSelectPacket> { event -> battleService.onBattleAction(event) }
    on<BattleLeavePacket> { event -> battleService.onBattlePacket(event) }
    on<BattleSequencePacket> { event -> battleService.onBattlePacket(event) }
    on<BattleSlotActionPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleSwitchSelectionsPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleUseItemPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleAppearancePacket> { event -> battleService.onBattlePacket(event) }
    on<BattleCancelRequestPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleSimulationRequestPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleReadyPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleActionSubmitPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleTierSelectPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleChatMessagePacket> { event -> battleService.onBattlePacket(event) }
    // 0x53 is a trade offer while a trade is open, a battle party pick otherwise.
    on<BattlePartySlotSelectPacket> { event -> if (!tradeService.onOffer(event)) battleService.onBattlePacket(event) }
    on<BattleTargetPickPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleTransitionReadyPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleTeamPreviewConfirmPacket> { event -> battleService.onBattlePacket(event) }
    on<BattleRewardSelectPacket> { event -> battleService.onBattlePacket(event) }
    onSuspend<MapLoadedAckPacket> { event ->
      // The payload was never decoded by OpenMMO; on NDS maps it may name the map the client just
      // loaded, which would fix seam tracking. Logged raw until the format is pinned down.
      if (event.packet.data.isNotEmpty()) {
        log.info {
          "MapLoadedAck payload (${event.packet.data.size}B): " +
              event.packet.data.joinToString(" ") { "%02x".format(it) }
        }
      }
      val result = battleService.onClientReady(event)
      // GBA whiteout: a lost battle heals the party and returns to the last heal spot.
      if (result == de.fiereu.openmmo.server.game.battle.BattleResult.DEFEAT) {
        val state = event.session.attributes[PLAYER_STATE]
        if (state != null) {
          storyPlayerService.healParty(event.session, state)
          movementService.respawnAfterWhiteout(event.session, state)
        }
      }
    }

    // The client sends an empty heartbeat packet.
    on<NullPacket> {}
    on<KeepAlivePacket> { event -> event.session.send(event.packet) }
    onSuspend<ChatMessagePacket> { event -> onChatMessage(event) }
    // What the client sends when the player types. The text rides in target unless the mode
    // carries a message of its own. During a map-directory tour, plain chat is the naming input.
    onSuspend<ChatMessageSendPacket> { event ->
      val text = event.packet.message ?: event.packet.target
      if (chatCommandService.tryHandle(event.session, text)) return@onSuspend
      val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return@onSuspend
      if (mapTourService.onChat(event.session, charId, text)) return@onSuspend
      relayChat(event.session, charId, event.packet)
    }
  }

  override fun onInactive() {
    // A cancelled scope left behind would make every later launch a silent no-op.
    session.attributes.remove(SCRIPT_SCOPE)?.cancel()
    val state = session.attributes[PLAYER_STATE] ?: return
    log.info { "Player ${state.characterId} disconnected." }
    val charId = state.characterId
    // A stale session - one that went silent and was replaced by a fresh login for the same
    // character before its socket finally died - must not tear down the LIVE session's state:
    // doing so evicted the character (every later write dropped, the hotbar with them) and
    // unbound it, so the live player was "not in the world" until they relogged (2026-09-08).
    // Only the session the registry still points at owns the cleanup; a ghost just leaves.
    if (charId != null && sessionRegistry.getByCharacterId(charId) !== session) {
      log.info { "Stale session for character $charId left; the live session keeps it." }
      presenceService.leave(session)
      return
    }
    if (charId != null) {
      // The battle flush must land before the unload evicts the character from the cache, and
      // before the rollback, which would otherwise be overwritten by the party it persists.
      battleService.onDisconnect(session)
      tradeService.onDisconnect(charId)
      linkService.onDisconnect(charId)
      // Undo the interrupted script here rather than leaving it to the coroutine's own cleanup,
      // which runs on another thread and would race the flush below.
      scriptRunner.rollBack(session, state, entityId = -1)
      state.dialogVisible = false
      state.scriptRunning = false
      state.releaseScriptLock()
      presenceService.leave(session)
      sessionRegistry.unbindCharacter(charId)
      // Before the unload: the last seconds of play time and the last-seen moment go out with it.
      characterStore.endPlaySession(charId)
      // Friends and team members still online see this character go offline, with its fresh
      // last-seen moment (the unload's save has not landed yet).
      characterStore.getCharacter(charId)?.let { left ->
        socialService.notifyPresence(left.info.name, left)
        guildService.notifyPresence(charId, left)
      }
      characterStore.unloadCharacterAsync(charId)
    }
    multiplayerService.broadcastMessage(
        ChatMessagePacket(
            type = ChatType.GAME_NOTIFICATIONS,
            language = Language.EN,
            message = "A player left the game.",
            sender = "",
        ),
    )
  }

  private suspend fun onChatMessage(event: PacketEvent<ChatMessagePacket>) {
    val state = event.session.attributes[PLAYER_STATE]
    if (state == null) {
      log.warn { "Chat message from session without PlayerState" }
      return
    }
    val charId = state.characterId ?: return
    val msg = event.packet
    val sender = characterStore.getCharacter(charId)?.info?.name ?: "Unknown"
    log.info { "Chat [${msg.type}] $sender: ${msg.message}" }
    multiplayerService.broadcastMessage(
        ChatMessagePacket(
            type = msg.type,
            language = msg.language,
            message = msg.message,
            sender = sender,
        ),
    )
  }

  /**
   * Player chat. The client's c2s 0x08 (f/YK) carries the chat type as its mode byte (f/XR0, the
   * same order as ChatType), the text in target - or, for a whisper (mode 4), the recipient in
   * target and the text in message. Nothing echoes locally: the sender sees their own line only
   * when the server sends it back (s2c 0x09 with the sender's character id, the entity the
   * overhead bubble sits on). Typed chat had never been relayed before 2026-09-11.
   */
  private fun relayChat(session: de.fiereu.network.SessionContext, charId: Long, packet: ChatMessageSendPacket) {
    val type = ChatType.entries.getOrNull(packet.mode.toInt()) ?: ChatType.NORMAL
    val text = (packet.message ?: packet.target).trim()
    if (text.isEmpty()) return
    val stored = characterStore.getCharacter(charId) ?: return
    val sender = stored.info.name
    log.info { "Chat [$type] $sender: $text" }
    val (expanded, records) = expandChatLinks(stored, text)
    val out = ChatMessagePacket(type = type, language = Language.EN, message = expanded, sender = sender, senderId = charId)
    // Linked monsters ride ahead of the line as loose records (0x8B -> the client's Com8.Gw0 cache);
    // the expanded token's O: id is what the click looks up there.
    val attachments = if (records.isEmpty()) null else MonsterRecordBookPacket(records)
    when (type) {
      ChatType.WHISPER -> {
        val recipient =
            sessionRegistry.onlineCharacterIds().firstNotNullOfOrNull { id ->
              characterStore.getCharacter(id)?.takeIf { it.info.name.equals(packet.target, ignoreCase = true) }?.let { sessionRegistry.getByCharacterId(id) }
            }
        if (recipient == null) {
          session.send(de.fiereu.openmmo.server.game.services.notice("${packet.target} is not online."))
          return
        }
        attachments?.let { recipient.send(it); if (recipient !== session) session.send(it) }
        recipient.send(out)
        if (recipient !== session) session.send(out)
      }
      // Local chat reaches the players who can see the speaker (the map group), the sender included.
      ChatType.NORMAL -> {
        attachments?.let { presenceService.broadcastToObservers(session, it); session.send(it) }
        presenceService.broadcastToObservers(session, out)
        session.send(out)
      }
      else -> {
        attachments?.let(multiplayerService::broadcastMessage)
        multiplayerService.broadcastMessage(out)
      }
    }
  }

  /**
   * Chat links (bytecode, f/lP1.zB1): the client drops `{M:<monster id>}` / `{I:<item code>}` into the
   * text it sends, and renders a received `{...}` only as `key:value` pairs split on `;` - O the
   * monster id (long), M the species (short), G gender, F form (bytes), S shiny and A alpha ("1"),
   * I the item id (short) with C a byte. The monster is looked up by O in the loose-record cache
   * the 0x8B packet fills, so the records go out with the line (2026-09-11).
   */
  private fun expandChatLinks(stored: de.fiereu.openmmo.server.game.storage.StoredCharacter, text: String): Pair<String, List<de.fiereu.openmmo.common.Pokemon>> {
    val records = ArrayList<de.fiereu.openmmo.common.Pokemon>()
    var out =
        MONSTER_LINK.replace(text) { m ->
          val id = m.groupValues[1].toLongOrNull() ?: return@replace m.value
          val mon = (stored.pokemon + stored.pcStorage).firstOrNull { it.id == id } ?: return@replace m.value
          records += mon
          val def = speciesRegistry.forMonster(mon)
          val gender = if (def == null) de.fiereu.openmmo.server.game.battle.Gender.GENDERLESS else de.fiereu.openmmo.server.game.battle.Gender.of(def.genderRatio, mon.seed)
          "{O:${mon.id};M:${de.fiereu.openmmo.common.clientSpeciesId(mon.dexId)};G:$gender;F:${mon.form};S:${if (mon.isShiny) 1 else 0};A:${if (mon.isAlpha) 1 else 0}}"
        }
    // The bag stack code is the client item id in the high 16 bits.
    out = ITEM_LINK.replace(out) { m -> m.groupValues[1].toLongOrNull()?.let { "{I:${it shr 16};C:0}" } ?: m.value }
    return out to records
  }

  private companion object {
    val MONSTER_LINK = Regex("""\{M:(\d+)\}""")
    val ITEM_LINK = Regex("""\{I:(\d+)\}""")
  }
}
