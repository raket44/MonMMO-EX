package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.DynamicWarp
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.GameMode
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.story.generated.hoenn.HoennFlags
import de.fiereu.openmmo.story.generated.hoenn.HoennVars
import de.fiereu.openmmo.story.generated.johto.JohtoFlags
import de.fiereu.openmmo.story.generated.kanto.KantoFlags

/**
 * Where a fresh character starts and the story state its source game would already have set. Every
 * region needs its own entry, so a new region is one function here rather than another branch in
 * [CharacterStore].
 */
internal data class NewGameStart(
    val bankId: Byte,
    val mapId: Byte,
    val x: Short,
    val y: Short,
    val dynamicWarp: DynamicWarp? = null,
    val storyFlags: Set<String> = emptySet(),
    val storyVars: Map<String, Int> = emptyMap(),
)

internal object NewGameStarts {
  /**
   * The story state of EVERY region's new game at once - the flags each source game sets on its
   * own new-game map reset and the intro vars. A character is one save across five games: the
   * regions it has not started must still sit in their opening state (their later-story npcs
   * hidden, their scene vars at 0), whichever way the player reaches them (ferry, Fly, a warp
   * command). The starting region's Kanto game mode decides Kanto's variant.
   */
  fun storyStateForAllRegions(
      female: Boolean,
      gameMode: GameMode = GameMode.REMAKE,
  ): Pair<Set<String>, Map<String, Int>> {
    val flags = LinkedHashSet<String>()
    val vars = LinkedHashMap<String, Int>()
    for (region in Region.entries) {
      val start = forRegion(region, female, gameMode)
      flags += start.storyFlags
      vars += start.storyVars
    }
    return flags to vars
  }

  /** The namespace prefix a region's story keys carry ("kanto/", "hoenn/", ...). */
  fun namespace(region: Region): String = region.name.lowercase() + "/"

  fun forRegion(
      region: Region,
      female: Boolean,
      gameMode: GameMode = GameMode.REMAKE
  ): NewGameStart =
      when (region) {
        Region.HOENN -> hoenn(female)
        Region.KANTO -> if (GameMode.isClassic(gameMode)) kantoClassic(gameMode) else kanto()
        Region.UNOVA -> unova()
        Region.SINNOH -> sinnoh()
        Region.JOHTO -> johto()
      }

  /** Emerald opens in the moving truck, whose exit goes through the player's dynamic warp. */
  private fun hoenn(female: Boolean): NewGameStart =
      NewGameStart(
          bankId = 75,
          mapId = 40,
          x = 2,
          y = 2,
          dynamicWarp =
              DynamicWarp(
                  Region.HOENN.wireValue,
                  50,
                  9,
                  if (female) 12 else 3,
                  10,
                  Direction.RIGHT,
              ),
          storyFlags =
              HoennFlags.initiallySet +
                  (if (female) HoennFlags.femaleIntro else HoennFlags.maleIntro) +
                  HoennFlags.FLAG_HIDE_MAP_NAME_POPUP,
          storyVars =
              mapOf(
                  HoennVars.VAR_LITTLEROOT_INTRO_STATE to if (female) 2 else 1,
                  (if (female) HoennVars.VAR_LITTLEROOT_HOUSES_STATE_MAY
                  else HoennVars.VAR_LITTLEROOT_HOUSES_STATE_BRENDAN) to 1,
              ),
      )

  /**
   * FireRed opens in the player's bedroom above their house in Pallet Town, at the coordinates its
   * new game code warps to. Nothing about the opening depends on the player's gender.
   */
  private fun kanto(): NewGameStart =
      NewGameStart(
          bankId = 4,
          mapId = 1,
          x = 6,
          y = 6,
          storyFlags = KantoFlags.initiallySet,
      )

  /**
   * Classic Gen 1/Yellow start — same Pallet bedroom, gameMode var written so the engine switches
   * to DV/StatExp formulas and type-based physical/special split.
   */
  private fun kantoClassic(gameMode: GameMode): NewGameStart =
      NewGameStart(
          bankId = 4,
          mapId = 1,
          x = 6,
          y = 6,
          storyFlags = KantoFlags.initiallySet,
          storyVars = mapOf(GameMode.VAR_KEY to gameMode.ordinal),
      )

  /**
   * White opens in the player's bedroom in Nuvema Town: ROM map header 391 (bank 135, map 1), the
   * header whose script file (782) runs the opening and the starter gift. White has no decomp to
   * name the exact start tile, so the player stands on the room's stairs arrival (9,2), where
   * climbing the stairs lands (nds-map-spawns.txt).
   */
  private fun unova(): NewGameStart =
      NewGameStart(
          bankId = 135.toByte(),
          mapId = 1,
          x = 9,
          y = 2,
      )

  /**
   * Platinum opens upstairs in the player's house in Twinleaf Town, where the rival bursts in:
   * pokeplatinum src/location.c sPlayerStartLocation = MAP_HEADER_TWINLEAF_TOWN_PLAYER_HOUSE_2F
   * (header 415 = bank 159, map 1), x 4, z 6.
   */
  private fun sinnoh(): NewGameStart =
      NewGameStart(
          bankId = 159.toByte(),
          mapId = 1,
          x = 4,
          y = 6,
      )

  /**
   * HeartGold opens in the player's room in New Bark Town: pokeheartgold src/location_backup.c
   * sLocation_PlayerRoom = MAP_NEW_BARK_PLAYER_HOUSE_2F (header 64 = bank 64, map 0), x 6, y 6.
   */
  private fun johto(): NewGameStart =
      NewGameStart(
          bankId = 64,
          mapId = 0,
          x = 6,
          y = 6,
          storyFlags = JohtoFlags.initiallySet,
      )
}
