package de.fiereu.openmmo.server.game.script.interpreter

/**
 * Command subsets the executor and [ScriptSupportAnalyzer] must agree on exactly: anything the
 * analyzer admits here, the executor runs without throwing.
 */
internal object InterpreterSupport {
  /**
   * Commands whose whole effect is client-side presentation (sound, screen fades, tile dressing) or
   * bookkeeping with no gameplay the server models (game stats, fame checker, quest log). Executed
   * as no-ops so the scripts around them keep working.
   */
  val NOOP_COMMANDS =
      setOf(
          "playse",
          "waitse",
          "playbgm",
          "savebgm",
          "fadedefaultbgm",
          "playfanfare",
          "waitfanfare",
          "playmoncry",
          "waitmoncry",
          "fadescreen",
          "fadescreenspeed",
          "fadescreenswapbuffers",
          "dofieldeffect",
          "waitfieldeffect",
          "setfieldeffectargument",
          "incrementgamestat",
          "dotimebasedevents",
          "setrespawn",
          "setmetatile",
          "setobjectmovementtype",
          "turnobject",
          "famechecker",
          "waitstate",
          "showmoneybox",
          "hidemoneybox",
          "updatemoneybox",
          // Taken only during FireRed quest-log playback, which this server never enters.
          "goto_if_questlog",
          "nop",
          "nop1",
          // Match Call rematch registration; rematches are not modeled yet.
          "register_matchcall",
          "setweather",
          "resetweather",
          "doweather",
          "opendoor",
          "closedoor",
          "waitdooranim",
      )

  /** Branches on a trainer's defeated flag, which TrainerStoryState already tracks. */
  val DEFEATED_BRANCHES =
      setOf(
          "goto_if_defeated",
          "goto_if_not_defeated",
          "call_if_defeated",
          "call_if_not_defeated",
      )

  /** Specials that are pure client presentation or quest-log bookkeeping. */
  val NOOP_SPECIALS =
      setOf(
          "SetUsedPkmnCenterQuestLogEvent",
          "QuestLog_CutRecording",
          "DrawWholeMapView",
          "DisableMsgBoxWalkaway",
          "ShakeScreen",
          "DoPokemonLeagueLightingEffect",
          "PlayerFaceTrainerAfterBattle",
      )

  /** Specials the executor implements for real. */
  val IMPLEMENTED_SPECIALS = setOf("HealPlayerParty")

  val SUPPORTED_SPECIALS = NOOP_SPECIALS + IMPLEMENTED_SPECIALS

  /**
   * specialvar functions whose answer on this server is a constant: there are no trainer tower/hill
   * lobbies, no union rooms, no gold trainer cards, and no Pokerus. The values are the truthful
   * ones, not shortcuts - control flow after them lands where the real game would.
   */
  val SPECIALVAR_RESULTS =
      mapOf(
          "CountPlayerTrainerStars" to 0,
          "IsPlayerNotInTrainerTowerLobby" to 1,
          "PlayerNotAtTrainerHillEntrance" to 1,
          "BufferUnionRoomPlayerName" to 0,
          "IsPokerusInParty" to 0,
          // No Vs Seeker / Match Call rematch offers until the server models them; scripts fall
          // through to their ordinary already-defeated dialog.
          "ShouldTryRematchBattle" to 0,
      )

  /** Bag commands: first arg an ITEM_ constant, optional second a count. */
  val ITEM_COMMANDS = setOf("giveitem", "checkitem", "removeitem", "checkitemspace", "finditem")
}
