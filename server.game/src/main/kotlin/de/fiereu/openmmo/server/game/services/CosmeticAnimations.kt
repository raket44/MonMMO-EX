package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.NpcAnimationPacket
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * A hotbar click on a worn animated cosmetic (the Werewolf Masks carry an idle loop and a one-shot
 * howl in addons.pak) plays the addon's second animation on the player, for everyone on the map:
 * s2c 0xB2 (entity id, animation byte -> f/tT.pJ0), which the client answers by swapping to that
 * animation and falling back to the idle loop when it ends. The animation byte is the one open
 * value; `/anim N` plays any byte on the player so it can be pinned in play.
 */
@Singleton
class CosmeticAnimations @Inject constructor(private val presence: PresenceService) {
  /** The animation byte a cosmetic click plays; adjustable live through `/anim set N`. */
  @Volatile var useAnimation: Int = DEFAULT_USE_ANIMATION

  fun play(ctx: SessionContext, entityId: Long, animation: Int) {
    val packet = NpcAnimationPacket(entityId, animation and 0xFF)
    ctx.send(packet)
    presence.broadcastToObservers(ctx, packet)
    log.info { "[Cosmetic] entity $entityId plays animation $animation" }
  }

  companion object {
    /** The addon's second animation slot, the first candidate. */
    const val DEFAULT_USE_ANIMATION = 1
  }
}
