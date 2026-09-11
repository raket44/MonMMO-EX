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
          // The monster picture window has no client counterpart on this dialog channel.
          "showmonpic",
          "hidemonpic",
          "fadeoutbgm",
          "fadeinbgm",
          "fadenewbgm",
          "playse",
          "waitse",
          "playbgm",
          "savebgm",
          "fadedefaultbgm",
          "playfanfare",
          "waitfanfare",
          "playmoncry",
          "waitmoncry",
          "dofieldeffect",
          "waitfieldeffect",
          "setfieldeffectargument",
          "incrementgamestat",
          "dotimebasedevents",
          "setrespawn",
          // The Dig / Escape Rope exit: neither is modelled, so the cave's escape target is not kept.
          "setescapewarp",
          // A whole-layout swap (dug-out tunnel, Seafoam with the current stopped): the client's
          // footer switch (LayoutVariants) is the real implementation; until a variant is mapped
          // the map keeps its default layout and the rest of the entry script still runs.
          "setmaplayoutindex",
          "turnobject",
          "famechecker",
          // Seeds the gym-statue "trainers defeated" bookkeeping (setvar VAR_0x8008 + call).
          // Statue text is cosmetic; blocking every gym leader on it was the real cost.
          "set_gym_trainers",
          "waitstate",
          // Flash: the flicker is client-side; setflashlevel below carries the lit state.
          "animateflash",
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
          // The walk-away-from-a-sign cancel timer; there is no walk-away cancel here.
          "SetWalkingIntoSignVars",
          "ShakeScreen",
          "DoPokemonLeagueLightingEffect",
          "PlayerFaceTrainerAfterBattle",
          // Buffers "big guy"/"cute girl" into STR_VAR_1 for a handful of Route 104 intros. No
          // dialog channel carries plain string vars yet, so the buffered word goes unfilled -
          // a cosmetic gap in one line, against whole trainers doing nothing (the pre-battle
          // approach with no battle). Revisit with the text-override pipeline.
          "GetPlayerBigGuyGirlString",
          // Dex "seen" bookkeeping is the dex service's, not the script's.
          "SetSeenMon",
          // The elevator's "Now on: nF" window and its shake: presentation the client has no box for.
          "DrawElevatorCurrentFloorWindow",
          "CloseElevatorCurrentFloorWindow",
          "AnimateElevator",
          "RemoveCameraObject",
          "SpawnCameraObject",
          "AnimateTeleporterHousing",
          "AnimateTeleporterCable",
          // The Sevii ferry: the client has no destination menu yet, so the menu draws nothing and
          // GetSelectedSeagallopDestination answers "cancel"; the sailor's own lines still play.
          "DrawSeagallopDestinationMenu",
          // The GBA help-system toggles: nothing to do off a cartridge.
          "HelpSystem_Disable",
          // Shows the easy-chat profile the club woman was given; no profile is ever entered here.
          "ShowEasyChatMessage",
          "HelpSystem_Enable",
          // save_location.c: GameCube-link save flags, nothing off a cartridge.
          "SetPostgameFlags",
          // The roaming legendary (Raikou/Entei/Suicune) is not modelled; Celio's Sapphire scene continues.
          "InitRoamer",
          // Gift monsters: the "give it a nickname?" screen. The client has no server-driven nickname
          // entry yet, so a YES answer keeps the species name; the gift itself already landed.
          "ChangePokemonNickname",
          "ChangeBoxPokemonNickname",
      )

  /**
   * String-buffer commands with no dialog channel to carry them yet: the ROM line renders its
   * variable unfilled. Cosmetic, against Cut and Surf not working at all.
   */
  val BUFFER_COMMANDS =
      setOf("bufferpartymonnick", "buffermovename", "bufferstdstring", "buffernumberstring", "bufferspeciesname", "bufferitemname", "bufferleadmonspeciesname", "bufferfirstpokemon", "bufferboxname")

  /**
   * ROM multichoice menus the client draws from its own registry (category 10 sets, f/Lx.R40):
   * menu constant -> set id. Sets are matched by the ROM text ids they list; unmatched menus
   * answer as B pressed. Only the yes/no set is verified so far.
   */
  val BUILTIN_MENUS: Map<String, Int> = emptyMap()

  /** Specials the executor implements for real. */
  val IMPLEMENTED_SPECIALS =
      setOf(
          "HealPlayerParty",
          "StartMarowakBattle",
          "GetElevatorFloor",
          "ListMenu",
          "EnterSafariMode",
          "SetHiddenItemFlag",
          "ExitSafariMode",
          "ChoosePartyMon",
          "DoSeagallopFerryScene",
          "BufferBigGuyOrBigGirlString",
          "BufferSonOrDaughterString",
          "ShowEasyChatScreen",
          "CreateInGameTradePokemon",
          "DoInGameTradeScene",
          "GetMagikarpSizeRecordInfo",
          "CompareMagikarpSize",
          "SetVermilionTrashCans",
          "RockSmashWildEncounter",
          "ChooseMonForMoveTutor",
          "DoSSAnneDepartureCutscene",
      )

  /**
   * The tutor indexes `setvar VAR_0x8005, <symbol>` hands ChooseMonForMoveTutor: pokefirered's
   * MOVETUTOR_* (include/constants/moves.h) and pokeemerald's TUTOR_MOVE_* (constants/party_menu.h),
   * each in its ROM's own order. MoveTutorService maps the index back to a move per region.
   */
  val MOVE_TUTOR_INDEXES: Map<String, Int> =
      listOf(
              "MEGA_PUNCH", "SWORDS_DANCE", "MEGA_KICK", "BODY_SLAM", "DOUBLE_EDGE", "COUNTER", "SEISMIC_TOSS",
              "MIMIC", "METRONOME", "SOFT_BOILED", "DREAM_EATER", "THUNDER_WAVE", "EXPLOSION", "ROCK_SLIDE",
              "SUBSTITUTE", "FRENZY_PLANT", "BLAST_BURN", "HYDRO_CANNON")
          .withIndex()
          .associate { (index, name) -> "MOVETUTOR_$name" to index } +
          listOf(
                  "MEGA_PUNCH", "SWORDS_DANCE", "MEGA_KICK", "BODY_SLAM", "DOUBLE_EDGE", "COUNTER", "SEISMIC_TOSS",
                  "MIMIC", "METRONOME", "SOFT_BOILED", "DREAM_EATER", "THUNDER_WAVE", "EXPLOSION", "ROCK_SLIDE",
                  "SUBSTITUTE", "DYNAMIC_PUNCH", "ROLLOUT", "PSYCH_UP", "SNORE", "ICY_WIND", "ENDURE", "MUD_SLAP",
                  "ICE_PUNCH", "SWAGGER", "SLEEP_TALK", "SWIFT", "DEFENSE_CURL", "THUNDER_PUNCH", "FIRE_PUNCH",
                  "FURY_CUTTER")
              .withIndex()
              .associate { (index, name) -> "TUTOR_MOVE_$name" to index }

  val SUPPORTED_SPECIALS = NOOP_SPECIALS + IMPLEMENTED_SPECIALS

  /** src/script_menu.c sStdStrings, by STDSTRING_* id (include/constants/menu.h) - bufferstdstring's texts. */
  val STD_STRINGS: List<String> =
      listOf(
          "COOL", "BEAUTY", "CUTE", "SMART", "TOUGH", "COOL", "BEAUTY", "CUTE", "SMART", "TOUGH",
          "ITEMS", "KEY ITEMS", "POKé BALLS", "TMs & HMs", "BERRIES",
          "BOULDERBADGE", "CASCADEBADGE", "THUNDERBADGE", "RAINBOWBADGE", "SOULBADGE", "MARSHBADGE", "VOLCANOBADGE", "EARTHBADGE",
          "COINS", "ITEMS POCKET", "KEY ITEMS POCKET", "POKé BALLS POCKET", "TM CASE", "BERRY POUCH",
      )

  /**
   * src/seagallop.c sSeagallopSpawnTable, by SEAGALLOP_* id: where the ferry ride ends (map source
   * name, x, y). All of these are Kanto-region maps.
   */
  val SEAGALLOP_DESTINATIONS: List<Triple<String, Int, Int>> =
      listOf(
          Triple("VermilionCity", 0x17, 0x20),
          Triple("OneIsland_Harbor", 8, 5),
          Triple("TwoIsland_Harbor", 8, 5),
          Triple("ThreeIsland_Harbor", 8, 5),
          Triple("FourIsland_Harbor", 8, 5),
          Triple("FiveIsland_Harbor", 8, 5),
          Triple("SixIsland_Harbor", 8, 5),
          Triple("SevenIsland_Harbor", 8, 5),
          Triple("CinnabarIsland", 0x15, 0x07),
          Triple("NavelRock_Harbor", 8, 5),
          Triple("BirthIsland_Harbor", 8, 5),
      )

  /**
   * A multichoice drawn as the client's text-button list: entries of one DS text bank, plus the
   * dialog arguments those entries read (the raw label route).
   */
  data class DsTextList(
      val region: Int,
      val bank: Int,
      val entries: List<Int>,
      val args: List<de.fiereu.openmmo.net.game.packets.dialog.DialogMessageArg> = emptyList(),
  )

  /**
   * The client draws a ROM menu with its text-button list (dialog kind wire 49, f/gl0) whose
   * buttons are entries of one DS message bank (f/EO.oG1(region, bank, entry)), formatted with
   * the dialog's arguments (f/nV0.CoM5 -> Tu1). Two routes, both from the game's own option
   * strings (corpus MenuIndex):
   * - every label is an entry of the DS menu-entry bank (Platinum bank 361: 1F..5F, B1F, B2F,
   *   LOOKOUT, EXIT, ...) -> those entries;
   * - otherwise bank 354's entries 1..12 are bare placeholders: the client's DS decoder (f/EO)
   *   turns a STRVAR tag into "{index:02X}" ({00}..{0B}) and Tu1 replaces each with the dialog
   *   argument of that slot, so every button shows the ROM's own text sent as a raw string
   *   argument - B4F, 11F, anything, up to twelve buttons.
   * Null only past twelve options: the menu then answers B so the script takes its cancel path.
   */
  fun dsTextList(options: List<String>): DsTextList? {
    val bank = de.fiereu.openmmo.script.GeneratedScriptCorpus.dsMenuEntries
    val entries = options.map { bank[it] }
    if (entries.all { it != null }) return DsTextList(DS_MENU_REGION, DS_MENU_BANK, entries.map { it!! })
    if (options.size > RAW_LABEL_SLOTS) return null
    return DsTextList(
        DS_MENU_REGION,
        DS_RAW_LABEL_BANK,
        (1..options.size).toList(),
        options.mapIndexed { slot, text ->
          de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg(slot = slot.toByte(), kind = RAW_TEXT_ARG, text = text)
        })
  }

  const val DS_MENU_REGION = 3
  const val DS_MENU_BANK = 361
  /** Platinum unk_0354.json: entries 1..12 are `{STRVAR_1 t, 0..11, 0}`, one placeholder each. */
  const val DS_RAW_LABEL_BANK = 354
  const val RAW_LABEL_SLOTS = 12
  /** Dialog argument kind 5: a raw UTF-16 string (DialogActionPacket codec). */
  const val RAW_TEXT_ARG: Byte = 5

  /** specialvar functions the executor answers from live state. */
  val IMPLEMENTED_SPECIALVARS =
      setOf(
          "GetBattleOutcome",
          "IsPlayerLeftOfVermilionSailor",
          "GetPokedexCount",
          "InitElevatorFloorSelectMenuPos",
          "IsThereRoomInAnyBoxForMorePokemon",
          "DoesPlayerPartyContainSpecies",
          "GetInGameTradeSpeciesInfo",
          "GetTradeSpecies",
      )


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
          // The ferry desk: Vermilion is seagallop number 7 (src/seagallop.c).
          "GetSeagallopNumber" to 7,
          "GetSelectedSeagallopDestination" to 127,
          "IsNationalPokedexEnabled" to 0,
          // No wireless adapter on a server build: the Joyful Game Corner attendant explains the
          // minigames need one, the way the cartridge does without it.
          "IsWirelessAdapterConnected" to 0,
          // No Vs Seeker / Match Call rematch offers until the server models them; scripts fall
          // through to their ordinary already-defeated dialog.
          "ShouldTryRematchBattle" to 0,
          // Match Call registration is not modeled either, so no trainer is ever registered;
          // post-battle scripts take their plain-dialog branch, which is the truthful answer.
          "IsTrainerRegistered" to 0,
          // Rock Smash in Rusturf Tunnel: the special answers TRUE only for the two story rocks that
          // open the tunnel, whose scene is not modeled; every other rock smashes normally on FALSE.
          "TryUpdateRusturfTunnelState" to 0,
          // Gift monsters never route to a PC box here (givemon answers party or no room), so the
          // box-full follow-ups are unreachable; the constants keep the scripts interpretable.
          "ShouldShowBoxWasFullMessage" to 0,
          "GetPCBoxToSendMon" to 0,
      )

  /**
   * ROM lines replaced by a client string, by text label. The GBA "no more room" line blames full
   * boxes, which is not why a gift is refused here: the party is full. Client string 2305 says so.
   */
  /**
   * "{mon} used CUT!" and kin: the client shows its own HM banner for these (ScriptContext
   * .fieldMoveBanner at the script's dofieldeffect), so the ROM's box is not shown.
   */
  val USED_MOVE_TEXTS =
      setOf(
          "Text_MonUsedMove", "Text_MonUsedStrengthCanMoveBoulders", "Text_MonUsedWaterfall",
          "Text_MonUsedDive", "Text_UsedSurf", "Text_MonUsedFieldMove", "Text_MonUsedStrength",
          "gText_PlayerUsedSurf")

  val CLIENT_STRING_OVERRIDES: Map<String, Int> =
      mapOf("Text_NoMoreRoomForPokemon" to 2305, "gText_NoMoreRoomForPokemon" to 2305)

  /** Bag commands: first arg an ITEM_ constant, optional second a count. */
  val ITEM_COMMANDS = setOf("giveitem", "additem", "checkitem", "removeitem", "checkitemspace", "finditem")
}
