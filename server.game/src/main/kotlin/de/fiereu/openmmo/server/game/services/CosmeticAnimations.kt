package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.enums.SkinSlot
import de.fiereu.openmmo.net.game.packets.CosmeticAnimationPacket
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * A hotbar click on a worn animated cosmetic (the Werewolf Masks carry an idle loop and a one-shot
 * howl in addons.pak) plays that one-shot on the player's entity: s2c 0x6C names the entity, the
 * slot and the worn addon, and every client on the map that receives it swaps the slot from its
 * idle loop to the second animation until it has run out (client f/Sv1 -> f/Di0.Jy1 -> f/F90.fR).
 * The packet only reaches players that render the entity, so it goes to the player and to
 * everyone observing them.
 */
@Singleton
class CosmeticAnimations @Inject constructor(private val presence: PresenceService) {
  fun play(ctx: SessionContext, charId: Long, slot: SkinSlot, addonId: Int) {
    val packet = CosmeticAnimationPacket(charId, slot, addonId.toShort(), playSound = true)
    ctx.send(packet)
    presence.broadcastToObservers(ctx, packet)
    log.info { "[Cosmetic] entity $charId plays $slot addon $addonId" }
  }
}
