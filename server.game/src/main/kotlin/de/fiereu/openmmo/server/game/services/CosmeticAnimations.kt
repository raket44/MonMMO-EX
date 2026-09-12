package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.codecs.SkinSet
import de.fiereu.openmmo.net.game.packets.EntitySpriteChangePacket
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val log = KotlinLogging.logger {}

/**
 * A hotbar click on a worn animated cosmetic (the Werewolf Masks carry an idle loop and a one-shot
 * howl in addons.pak) re-announces the worn skin set with that slot's variant byte swapped to the
 * howl form - the same per-slot "extra" byte the customization menu uses for alternate forms - and
 * puts the base form back once the howl has played (69 frames at 50 ms). Everyone on the map gets
 * both announcements. The variant value is the one open number: `/anim set N` retunes it live,
 * `/anim N` plays it once on the player.
 */
@Singleton
class CosmeticAnimations
@Inject
constructor(
    private val presence: PresenceService,
    private val characters: CharacterStore,
) {
  /** The variant byte a cosmetic click swaps in; adjustable live through `/anim set N`. */
  @Volatile var useVariant: Int = DEFAULT_USE_VARIANT

  private val scope = CoroutineScope(Dispatchers.Default)

  /** Plays [variant] on the [slot] the character wears for [HOWL_MS], then restores the base form. */
  fun play(ctx: SessionContext, charId: Long, slot: SkinSlot, variant: Int) {
    val current = characters.getCharacter(charId) ?: return
    val worn = current.skins[slot] ?: return
    val gender = current.info.rivalSex
    val region = current.info.skinRegionSelectionIndex
    val swapped = current.skins + (slot to Skin(worn.slot, worn.type, worn.color, (variant and 0xFF).toUByte()))
    announce(ctx, charId, EntitySpriteChangePacket(charId, staged = false, appearance = SkinSet(region, swapped), gender = gender))
    log.info { "[Cosmetic] entity $charId plays $slot variant $variant" }
    scope.launch {
      delay(HOWL_MS)
      if (!ctx.channel.isActive) return@launch
      val now = characters.getCharacter(charId) ?: return@launch
      announce(ctx, charId, EntitySpriteChangePacket(charId, staged = false, appearance = SkinSet(now.info.skinRegionSelectionIndex, now.skins), gender = now.info.rivalSex))
    }
  }

  private fun announce(ctx: SessionContext, charId: Long, packet: EntitySpriteChangePacket) {
    ctx.send(packet)
    presence.broadcastToObservers(ctx, packet)
  }

  companion object {
    /** The first candidate: the addon's second form. */
    const val DEFAULT_USE_VARIANT = 1
    /** The Werewolf Mask's howl: 69 frames at 50 ms. */
    const val HOWL_MS = 3500L
  }
}
