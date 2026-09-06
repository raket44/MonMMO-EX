package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.EntityAppearanceInfo
import de.fiereu.openmmo.net.game.packets.EntityGroupMember
import de.fiereu.openmmo.net.game.packets.EntityGroupMemberRemovePacket
import de.fiereu.openmmo.net.game.packets.EntityGroupSnapshotPacket
import de.fiereu.openmmo.net.game.packets.GroupListFrameSet
import de.fiereu.openmmo.net.game.packets.LinkKickMemberPacket
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SessionRegistry
import de.fiereu.openmmo.server.game.storage.CharacterStore
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Links: the small party overlay of up to four players. Bytecode-verified wire: s2c 0xD0 (f/r9)
 * is the whole link - present flag, leader entity, then members each as entity + appearance
 * record (f/tK0.QL1: name, byte, int, byte, byte count, count shorts) + party preview frames
 * (f/tK0.Nr1: byte count, [list type, count x (short, byte, byte, short)]); the client builds
 * f/Vc1 and the overlay f/fW from it. 0xD1 adds one member, 0xD2 (removed, leader) drops one.
 * c2s 0xD1 kicks a member by entity id. Links live only in memory, like retail.
 */
@Singleton
class LinkService
@Inject
constructor(
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
) {

  class Link(val id: Long, var leaderId: Long, val members: MutableList<Long>)

  private val links = ConcurrentHashMap<Long, Link>()
  private val byChar = ConcurrentHashMap<Long, Long>()
  private val nextId = AtomicLong(1)

  fun linkOf(charId: Long): Link? = byChar[charId]?.let { links[it] }

  /** [target] joins [requester]'s link (created if needed). Returns a client string id on refusal. */
  fun join(requester: Long, target: Long): Int? {
    if (byChar[target] != null) return 6049 // "{00} is already a member of a link."
    val link =
        linkOf(requester)
            ?: Link(nextId.getAndIncrement(), requester, mutableListOf(requester)).also {
              links[it.id] = it
              byChar[requester] = it.id
            }
    if (link.members.size >= MAX_MEMBERS) return 6050 // "Your link is currently full."
    link.members += target
    byChar[target] = link.id
    log.info { "Link ${link.id}: char=$target joined (leader=${link.leaderId}, size=${link.members.size})" }
    val snapshot = snapshot(link)
    for (member in link.members) sessionRegistry.getByCharacterId(member)?.send(snapshot)
    return null
  }

  fun leave(charId: Long) {
    val link = linkOf(charId) ?: return
    link.members.remove(charId)
    byChar.remove(charId)
    sessionRegistry.getByCharacterId(charId)?.send(EntityGroupSnapshotPacket(false, null, null))
    if (link.members.size <= 1) {
      // A link of one is no link: the last player gets the overlay cleared too.
      links.remove(link.id)
      for (member in link.members) {
        byChar.remove(member)
        sessionRegistry.getByCharacterId(member)?.send(EntityGroupSnapshotPacket(false, null, null))
      }
      log.info { "Link ${link.id} dissolved" }
      return
    }
    if (link.leaderId == charId) link.leaderId = link.members.first()
    val removed = EntityGroupMemberRemovePacket(charId, link.leaderId)
    for (member in link.members) sessionRegistry.getByCharacterId(member)?.send(removed)
    log.info { "Link ${link.id}: char=$charId left (leader=${link.leaderId}, size=${link.members.size})" }
  }

  /** The leader kicks a member; anyone kicking themselves simply leaves. */
  fun onKick(event: PacketEvent<LinkKickMemberPacket>) {
    val charId = event.session.attributes[PLAYER_STATE]?.characterId ?: return
    val target = event.packet.targetEntityId
    val link = linkOf(charId) ?: return
    if (target != charId && link.leaderId != charId) return
    if (target !in link.members) return
    log.info { "Link ${link.id}: char=$charId kicks char=$target" }
    leave(target)
  }

  fun onDisconnect(charId: Long) = leave(charId)

  private fun snapshot(link: Link): EntityGroupSnapshotPacket =
      EntityGroupSnapshotPacket(true, link.leaderId, link.members.map { member(it) })

  private fun member(charId: Long): EntityGroupMember {
    val name = characterStore.getCharacter(charId)?.info?.name ?: "?"
    // Appearance fields beyond the name are not modelled yet; the client renders the name and
    // a default sprite. The count byte must match the shorts that follow (four).
    return EntityGroupMember(
        entityId = charId,
        appearance = EntityAppearanceInfo(name, 0, 0, 0, 4, List(4) { 0.toShort() }),
        frames = GroupListFrameSet(null, emptyList()),
    )
  }

  fun sendTo(ctx: SessionContext, charId: Long) {
    val link = linkOf(charId) ?: return
    ctx.send(snapshot(link))
  }

  private companion object {
    const val MAX_MEMBERS = 4
  }
}
