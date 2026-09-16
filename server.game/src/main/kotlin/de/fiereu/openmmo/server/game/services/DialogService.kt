package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.DialogChoicePacket
import de.fiereu.openmmo.net.game.packets.DialogStatePacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionPacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionResponsePacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogMessageArg
import de.fiereu.openmmo.server.game.session.PENDING_DIALOG
import de.fiereu.openmmo.server.game.session.PENDING_DIALOG_RESPONSE
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.PlayerState
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred

private val log = KotlinLogging.logger {}

internal val CLOSE_DIALOG_ACTION =
    DialogActionPacket(
        flags = 0,
        actionType = 0x64,
        textId = 0,
        entityId = -1,
        contextValue = 0,
        messageArgs = emptyList(),
        detail = ByteArray(0),
    )

data class DialogPresentation(
    val messageArgs: List<DialogMessageArg> = emptyList(),
    val contextValue: Int = 0,
    val detail: ByteArray = byteArrayOf(0),
)

@Singleton
class DialogService @Inject constructor(private val socialRequests: SocialRequestService? = null) {

  /** Emerald starter picker ROM ids. */
  suspend fun chooseHoennStarter(session: SessionContext, state: PlayerState): Int {
    while (true) {
      val choice =
          showChoiceAndWait(
                  session = session,
                  state = state,
                  textId = HOENN_STARTER_PICK_TEXT,
                  actionType = STARTER_PICK,
                  entityId = NO_ENTITY,
                  contextValue = STARTER_CONTEXT,
                  detail =
                      byteArrayOf(
                          3,
                          (TREECKO and 0xFF).toByte(),
                          (TREECKO shr 8).toByte(),
                          (TORCHIC and 0xFF).toByte(),
                          (TORCHIC shr 8).toByte(),
                          (MUDKIP and 0xFF).toByte(),
                          (MUDKIP shr 8).toByte(),
                      ),
              )
              .unk
      if (choice !in 1..3) continue

      val accepted =
          showChoiceAndWait(
                  session = session,
                  state = state,
                  textId = HOENN_STARTER_CONFIRM_TEXT,
                  actionType = YES_NO,
                  entityId = NO_ENTITY,
                  contextValue = STARTER_CONTEXT,
              )
              .unk != 0
      if (accepted) return listOf(TREECKO, TORCHIC, MUDKIP)[choice - 1]
    }
  }

  /**
   * A DS starter scene in the same starter-pick window: [pickText] over [speciesIds], then the
   * ROM's own question for the one picked ([confirmTexts], same order), until one is accepted.
   * Platinum's choose_starter_app: bank 360 entry 7 "Now choose!", entries 1-3 per ball.
   */
  suspend fun chooseStarter(
      session: SessionContext,
      state: PlayerState,
      pickText: Int,
      speciesIds: List<Int>,
      confirmTexts: List<Int>,
  ): Int {
    while (true) {
      val choice = chooseFromSpecies(session, state, pickText, speciesIds.map { de.fiereu.openmmo.common.clientSpeciesId(it) })
      if (choice !in 1..speciesIds.size) continue
      val accepted =
          showChoiceAndWait(
                  session = session,
                  state = state,
                  textId = confirmTexts[choice - 1],
                  actionType = YES_NO,
                  entityId = NO_ENTITY,
                  contextValue = STARTER_CONTEXT,
              )
              .unk != 0
      if (accepted) return speciesIds[choice - 1]
    }
  }

  /**
   * Shows one of the client's BUILT-IN choice menus over the ROM question [textId] and returns the
   * 1-BASED picked option, 0 when the box is closed without choosing. The menus live in the
   * client's own registry (f/Lx.R40 fills category 10; bytecode-verified): the dialog action 0x16
   * carries [category, setId, 0] and the buttons send index+1. Set 3 is the PC menu - "{01}'s PC" /
   * "Global Trade Link" / "Mail" / Cancel.
   */
  suspend fun builtinMenu(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      menuSet: Int,
  ): Int =
      showChoiceAndWait(
              session,
              state,
              textId,
              BUILTIN_MENU,
              NO_ENTITY,
              contextValue = 0,
              detail = byteArrayOf(BUILTIN_MENU_CATEGORY, menuSet.toByte(), 0),
          )
          .unk

  /**
   * Turns the dialog currently open on the client into a list of text buttons and returns the
   * 0-BASED index of the pressed button. Dialog kind wire 49 = f/qM1.b (internal 40): parse case
   * 9 reads a byte the renderer ignores, the DS region byte, the message bank as s16, then a u8
   * count and one s16 entry per button; renderer f/gl0 draws each through f/EO.oG1(region, bank,
   * entry), the DS message-bank table - so the buttons can only be entries of one DS bank, and
   * each button answers with its own index (f/gl0 -> f/dh1.KO(i)), no +1 and no close code.
   * Wire 49 is an update kind (f/h4.Mq1 = 3): the client applies it to the open dialog
   * (f/A11.IT0 -> f/cg.h80) and, with no dialog open, answers 0 immediately - so the caller must
   * have a message showing. After the pick the box stays (kL0: KO re-renders instead of closing)
   * until the dialog state goes off. The text id is unused on this path and rides as 0.
   */
  suspend fun dsTextListMenu(
      session: SessionContext,
      state: PlayerState,
      region: Int,
      bank: Int,
      entries: List<Int>,
      preselected: Int,
      args: List<DialogMessageArg> = emptyList(),
  ): Int {
    val detail = ByteArray(5 + entries.size * 2)
    detail[0] = 0
    detail[1] = region.toByte()
    detail[2] = (bank and 0xFF).toByte()
    detail[3] = ((bank shr 8) and 0xFF).toByte()
    detail[4] = entries.size.toByte()
    entries.forEachIndexed { i, entry ->
      detail[5 + i * 2] = (entry and 0xFF).toByte()
      detail[6 + i * 2] = ((entry shr 8) and 0xFF).toByte()
    }
    return showChoiceAndWait(session, state, 0, DS_TEXT_LIST, NO_ENTITY, contextValue = preselected, detail = detail, messageArgs = args).unk
  }

  /**
   * Opens the client's daycare BREED-SELECTION window (pick two party monsters) over the ROM
   * question [textId] and returns the client's acknowledgement value. Dialog action wire 26 -
   * discovered live when the byte was mistaken for the registry menu and the breed window appeared
   * instead; the 3-byte payload below is the exact play-verified one.
   */
  suspend fun breedSelection(session: SessionContext, state: PlayerState, textId: Int): Int =
      showChoiceAndWait(
              session,
              state,
              textId,
              BREED_SELECT,
              NO_ENTITY,
              contextValue = 0,
              detail = byteArrayOf(10, 3, 0),
          )
          .unk

  /**
   * The starter-pick window (client f/X50 in its species mode, kind 0x23) over any list of species
   * - the party, for a tutor's "which one?" - returning the 1-based button pressed, 0 when the
   * window was closed. Species 0 renders as "???" (an egg).
   */
  suspend fun chooseFromSpecies(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      speciesIds: List<Int>,
  ): Int {
    val detail = ByteArray(1 + speciesIds.size * 2)
    detail[0] = speciesIds.size.toByte()
    speciesIds.forEachIndexed { i, id ->
      detail[1 + i * 2] = (id and 0xFF).toByte()
      detail[2 + i * 2] = (id shr 8).toByte()
    }
    return showChoiceAndWait(
            session, state, textId, STARTER_PICK, NO_ENTITY, contextValue = STARTER_CONTEXT, detail = detail)
        .unk
  }

  /** Show a ROM-backed yes/no box and return true for YES. */
  suspend fun askYesNo(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      entityId: Long,
      messageArgs: List<DialogMessageArg> = emptyList(),
  ): Boolean =
      showChoiceAndWait(session, state, textId, YES_NO, entityId, contextValue = 0, messageArgs = messageArgs).unk != 0

  /**
   * Shows a dialog box and waits for the player to advance or close it. [actionType] is 3 for a
   * sign and 4 for an npc box, [entityId] is the speaking npc or -1. The flags byte counts up per
   * box.
   */
  suspend fun showAndWait(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      actionType: Int,
      entityId: Long,
      presentation: DialogPresentation = DialogPresentation(),
  ) {
    show(session, state, textId, actionType, entityId, presentation).await()
  }

  /** Starts a dialog operation without consuming the later client acknowledgement. */
  fun show(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      actionType: Int,
      entityId: Long,
      presentation: DialogPresentation = DialogPresentation(),
  ): CompletableDeferred<Unit> {
    val advance = CompletableDeferred<Unit>()
    session.attributes[PENDING_DIALOG] = advance
    val seq = state.dialogSeqId
    state.dialogSeqId = seq + 1
    state.dialogVisible = true
    state.dialogNpcEntityId = entityId
    log.debug {
      "Send dialog box seq=$seq actionType=$actionType textId=0x${textId.toString(16)} entity=$entityId"
    }
    session.send(
        DialogActionPacket(
            flags = seq.toByte(),
            actionType = actionType.toByte(),
            textId = textId,
            entityId = entityId,
            contextValue = presentation.contextValue,
            messageArgs = presentation.messageArgs,
            detail = presentation.detail,
        ))
    return advance
  }

  /** Show a scene page and wait for the client's 0x21 acknowledgement. */
  suspend fun showScenePageAndWait(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      actionType: Int,
      contextValue: Int,
      messageArgs: List<DialogMessageArg>,
  ) =
      showAndWait(
          session,
          state,
          textId,
          actionType,
          NO_ENTITY,
          DialogPresentation(messageArgs, contextValue, ByteArray(0)),
      )

  /** Show a scene menu and return its 1-based choice. */
  suspend fun showSceneMenuAndWait(
      session: SessionContext,
      state: PlayerState,
      detail: ByteArray,
  ): Int =
      showChoiceAndWait(
              session,
              state,
              textId = 0,
              actionType = STARTER_PICK,
              entityId = NO_ENTITY,
              contextValue = 0,
              detail = detail,
          )
          .unk

  /** Closes the dialog once a script has shown its last box. */
  fun close(session: SessionContext, state: PlayerState) {
    session.attributes.remove(PENDING_DIALOG)
    session.attributes.remove(PENDING_DIALOG_RESPONSE)
    if (state.dialogVisible) {
      // A box the client keeps after its answer (the text-button list: KO re-renders a kL0 kind
      // instead of closing it) only goes away on an explicit close: dialog action wire 100
      // (f/qM1.qD0) makes the client close the dialog currently open (f/iq1.X91 ->
      // f/A11.fn0().gq1()), sends nothing back, and does nothing with none open - the boxes
      // the player already dismissed with A are gone by then.
      val seq = state.dialogSeqId
      state.dialogSeqId = seq + 1
      session.send(
          DialogActionPacket(
              flags = seq.toByte(),
              actionType = CLOSE_OPEN_DIALOG,
              textId = 0,
              entityId = NO_ENTITY,
              contextValue = 0,
              messageArgs = emptyList(),
              detail = byteArrayOf(0),
          ))
      // The dialog-state OFF also clears the client's scripted-input-removal flag (ln1.A70) -
      // while a script still owns the player it must NOT be sent; the runner sends the one
      // definitive OFF after the script's final walks have played out.
      if (!state.scriptRunning) session.send(DialogStatePacket(false))
      state.dialogVisible = false
      state.dialogNpcEntityId = 0
    }
  }

  /** Waits for a message already being presented, if one is still pending. */
  suspend fun waitForMessage(session: SessionContext) {
    session.attributes[PENDING_DIALOG]?.await()
  }

  /**
   * Arms the next dialog acknowledgement for a screen this service did not open itself: the
   * client's Hall of Fame closes with the same 0x21 response a message box does (f/ln1.PX1).
   */
  fun expectAcknowledgement(session: SessionContext): CompletableDeferred<Unit> {
    val advance = CompletableDeferred<Unit>()
    session.attributes[PENDING_DIALOG] = advance
    return advance
  }

  /**
   * The current client exposes one dialog acknowledgement, so this is also the closest available
   * representation of the GBA's separate A/B wait.
   */
  suspend fun waitForButtonPress(session: SessionContext) {
    session.attributes[PENDING_DIALOG]?.await()
  }

  /**
   * Releases a script waiting on a choice window that closed through its OWN packet instead of a
   * dialog answer. The breed window does exactly that: cancelling it sends the dialog response the
   * script awaits, but pressing Breed sends SubmitBreedingParty and nothing else, so without this
   * the daycare script never finished and the player stayed locked in scripted state, unable to
   * move until they relogged (owner-reported 2026-09-16, right after a successful breed).
   */
  fun completePendingChoice(session: SessionContext, code: Int = 0) {
    session.attributes.remove(PENDING_DIALOG_RESPONSE)?.complete(DialogActionResponsePacket(0, code))
  }

  fun onInteractive(event: PacketEvent<DialogActionResponsePacket>) {
    val session = event.session
    log.info { "Dialog response id=${event.packet.id} code=${event.packet.unk}" }
    // A social prompt (trade, link, friend, team, duel) answers through the same packet.
    if (socialRequests?.onAnswer(session, event.packet.id, event.packet.unk) == true) return
    val response = session.attributes.remove(PENDING_DIALOG_RESPONSE)
    if (response != null) {
      response.complete(event.packet)
      return
    }
    val advance = session.attributes.remove(PENDING_DIALOG)
    log.debug { "Dialog response id=${event.packet.id} advancing=${advance != null}" }
    if (advance != null) {
      // A script is waiting on this box, let it move on to its next line.
      advance.complete(Unit)
      return
    }
    // No script is driving this dialog, just close whatever is open.
    val state = session.attributes[PLAYER_STATE] ?: return
    if (state.dialogVisible) {
      if (!state.scriptRunning) session.send(DialogStatePacket(false))
      state.dialogVisible = false
      state.dialogNpcEntityId = 0
    }
  }

  fun onDialogChoice(event: PacketEvent<DialogChoicePacket>) {
    val session = event.session
    log.info { "Dialog choice received: unk1=${event.packet.unk1}, unk2=${event.packet.unk2}" }
    val advance = session.attributes.remove(PENDING_DIALOG)
    if (advance != null) {
      advance.complete(Unit)
      return
    }
    val state = session.attributes[PLAYER_STATE] ?: return
    if (state.dialogVisible) {
      if (!state.scriptRunning) session.send(DialogStatePacket(false))
      state.dialogVisible = false
      state.dialogNpcEntityId = 0
    }
  }

  private suspend fun showChoiceAndWait(
      session: SessionContext,
      state: PlayerState,
      textId: Int,
      actionType: Int,
      entityId: Long,
      contextValue: Int,
      detail: ByteArray = byteArrayOf(0),
      messageArgs: List<DialogMessageArg> = emptyList(),
  ): DialogActionResponsePacket {
    session.attributes.remove(PENDING_DIALOG)?.complete(Unit)
    val response = CompletableDeferred<DialogActionResponsePacket>()
    session.attributes[PENDING_DIALOG_RESPONSE] = response
    val seq = state.dialogSeqId
    state.dialogSeqId = seq + 1
    state.dialogVisible = true
    state.dialogNpcEntityId = entityId
    session.send(
        DialogActionPacket(
            flags = seq.toByte(),
            actionType = actionType.toByte(),
            textId = textId,
            entityId = entityId,
            contextValue = contextValue,
            messageArgs = messageArgs,
            detail = detail,
        ))
    return response.await()
  }

  private companion object {
    const val NO_ENTITY = -1L
    const val YES_NO = 0x05
    /** Closes whatever dialog is open: wire 100 = f/qM1.qD0, handled before any parse in f/iq1.X91. */
    const val CLOSE_OPEN_DIALOG: Byte = 100
    /** Text-button list of DS-bank entries: wire 49 = f/qM1.b, parse case 9, renderer f/gl0. */
    const val DS_TEXT_LIST = 49
    /**
     * A built-in client menu addressed by (category, set). Three maps stand between the wire byte
     * and the behavior (all bytecode-decoded): wire -> qM1 constant (ctor args (internal, wire)),
     * XN1.kW[internal] -> parse case, UX.bV[internal] -> renderer. The registry-menu constant is
     * qM1.oU: internal 31, parse case 22 ([category, set, extra]), renderer 14 = f/Nq1, WIRE 36.
     * Wire 22 underflowed (an entity-ref parse) and wire 26 opened the daycare breed window - both
     * misreads of the same table.
     */
    const val BUILTIN_MENU = 36
    /** The daycare breed-selection window (wire 26 = internal 22 in the qM1 table). */
    const val BREED_SELECT = 26
    const val BUILTIN_MENU_CATEGORY: Byte = 10
    /** f/Lx.R40 set 3: "{01}'s PC" / "Global Trade Link" / "Mail" / Cancel. */
    const val PC_MENU_SET = 3
    const val STARTER_PICK = 0x23
    const val STARTER_CONTEXT = 700

    const val TREECKO = 252
    const val TORCHIC = 255
    const val MUDKIP = 258

    // Verified against the captured Emerald dialog database.
    const val HOENN_STARTER_PICK_TEXT = 0x105E8C53
    const val HOENN_STARTER_CONFIRM_TEXT = 0x105E8C90
  }
}
