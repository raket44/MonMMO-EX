package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.net.game.packets.LocalCharacterDeltaPacket
import de.fiereu.openmmo.net.game.packets.dialog.RawMessageArg
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.battle.CrystalOnixRaid
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.server.game.storage.BATTLE_POINTS_KEY
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private val log = KotlinLogging.logger {}

/**
 * Talking to the Crystal Onix in Cerulean Cave B1F: once a day per character, fight the raid
 * ([BattleService.startRaidBattle]); a win pays the Battle Points, rolls the rare cosmetic and
 * sends the boss back into the rock until tomorrow. The prize money is paid by the battle itself.
 */
@Singleton
class CrystalOnixRaidService
@Inject
constructor(
    private val characterStore: CharacterStore,
    private val battles: BattleService,
    private val npcService: NpcService,
    private val worldState: WorldStateService,
) {
  /** Replaced in tests. */
  internal var random: Random = Random.Default

  fun script(stored: StoredCharacter): Script = Script { ctx -> talk(ctx, stored) }

  private suspend fun talk(ctx: ScriptContext, stored: StoredCharacter) {
    val charId = stored.info.id
    val current = characterStore.getCharacter(charId) ?: return
    // A beaten boss is off this character's map until tomorrow; a stale talk just ends.
    if (CrystalOnixRaid.beatenToday(current.storyVars, WorldClock.today())) return
    ctx.say(line(CrystalOnixRaid.INTRO_TEXT))
    if (!ctx.askYesNo(line(CrystalOnixRaid.CHALLENGE_TEXT))) {
      ctx.closeMessage()
      return
    }
    ctx.say(line(CrystalOnixRaid.AWAKEN_TEXT))
    ctx.closeMessage()
    log.info { "Crystal Onix raid: char=$charId challenges" }
    val result = battles.startRaidBattle(ctx.session)
    log.info { "Crystal Onix raid: char=$charId -> $result" }
    if (result != BattleResult.VICTORY) return

    characterStore.setStoryVar(charId, CrystalOnixRaid.WIN_DAY_KEY, WorldClock.today().toEpochDay().toInt())
    val info = current.info
    npcService.despawnRaidBoss(
        ctx.session, info.positionRegionId.toInt(), (info.positionBankId.toInt() and 0xFF), info.positionMapId.toInt())

    if (characterStore.addBattlePoints(charId, CrystalOnixRaid.BATTLE_POINTS)) {
      val balance = characterStore.getCharacter(charId)?.storyVars?.get(BATTLE_POINTS_KEY) ?: 0
      ctx.session.send(LocalCharacterDeltaPacket(battlePoints = balance))
    } else {
      log.error { "Could not pay char=$charId the raid's ${CrystalOnixRaid.BATTLE_POINTS} Battle Points" }
    }
    ctx.setMessageArg(0, RawMessageArg(0, RAW_STRING, text = "%,d".format(CrystalOnixRaid.BATTLE_POINTS)))
    ctx.say(line(CrystalOnixRaid.WIN_TEXT))

    val cosmetic = rollCosmetic(characterStore.getCharacter(charId)?.items?.keys.orEmpty()) ?: return
    if (!characterStore.addItem(charId, cosmetic.itemId, 1)) {
      log.error { "Could not give char=$charId the raid cosmetic ${cosmetic.name}" }
      return
    }
    log.info { "Crystal Onix raid: char=$charId found cosmetic ${cosmetic.name} (item ${cosmetic.itemId})" }
    // Cosmetics unlock through the state block, not the bag delta.
    characterStore.getCharacter(charId)?.let { worldState.refreshUnlocks(ctx.session, it) }
    ctx.setMessageArg(0, RawMessageArg(0, RAW_STRING, text = cosmetic.name))
    ctx.say(line(CrystalOnixRaid.COSMETIC_TEXT))
  }

  /** A bag cosmetic the character does not own yet, on the raid's rare-drop roll. */
  private fun rollCosmetic(owned: Set<Int>): CosmeticsRegistry.Addon? =
      CrystalOnixRaid.rareDrop(
          CosmeticsRegistry.itemBacked().filter { !it.free && it.itemId !in owned }, random)

  private fun line(stringId: Int): DialogLine = object : DialogLine { override val textId = stringId }

  private companion object {
    /** Formatter kind 5: the argument is shown as the raw string it carries. */
    const val RAW_STRING: Byte = 5
  }
}
