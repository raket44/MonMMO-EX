package pirated.server.game.services

import pirated.common.enums.GameMode
import pirated.server.game.storage.CharacterStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassicModeService @Inject constructor(private val store: CharacterStore) {

  fun getMode(charId: Long): GameMode {
    val char = store.getCharacter(charId) ?: return GameMode.REMAKE
    return GameMode.fromVar(char.storyVars[GameMode.VAR_KEY])
  }

  fun setMode(charId: Long, mode: GameMode) {
    val char = store.getCharacter(charId) ?: return
    char.storyVars[GameMode.VAR_KEY] = mode.ordinal
    store.flushCharacterAsync(charId)
  }

  /** Sets Yellow-mode follower flag so the Pikachu companion mechanic activates. */
  fun applyYellowStarter(charId: Long) {
    val char = store.getCharacter(charId) ?: return
    char.storyVars["yellow_pikachu_follower"] = 1
    char.storyFlags.add("FLAG_PIKACHU_STARTER")
    store.flushCharacterAsync(charId)
  }

  fun hasYellowFollower(charId: Long): Boolean =
      store.getCharacter(charId)?.storyVars?.get("yellow_pikachu_follower") == 1
}
