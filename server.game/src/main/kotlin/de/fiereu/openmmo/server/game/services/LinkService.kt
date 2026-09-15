package de.fiereu.openmmo.server.game.services

import de.fiereu.network.PacketEvent
import de.fiereu.network.SessionContext
import de.fiereu.openmmo.net.game.packets.EntityAppearanceInfo
import de.fiereu.openmmo.net.game.packets.EntityFramesUpdatePacket
import de.fiereu.openmmo.net.game.packets.EntityGroupMember
import de.fiereu.openmmo.net.game.packets.EntityGroupMemberRemovePacket
import de.fiereu.openmmo.net.game.packets.EntityGroupSnapshotPacket
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.common.enums.PokemonRarityFlag
import de.fiereu.openmmo.net.game.packets.GroupListFrame
import de.fiereu.openmmo.net.game.packets.GroupListFrameSet
import de.fiereu.openmmo.net.game.packets.LinkKickMemberPacket
import de.fiereu.openmmo.net.game.packets.PARTY_LIST
import de.fiereu.openmmo.net.game.packets.PlayerHead
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.server.game.battle.Gender
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
 * Links: the small party overlay of up to four players. r32645 wire (f/vl4): s2c 0xD0 is the whole
 * link - present flag, leader entity, then members each as entity + player summary (f/ih6.bK1:
 * name, skipped byte, last-seen int, skin tone, variants, four head slots) + party icons (f/ih6.OW:
 * count, container byte, count x (monster id, species, form, gender, rarity bits)). 0xD1 (f/n09)
 * adds one member, 0xDB (f/f5) replaces a member's icons, 0xD2 (removed, leader) drops one. c2s
 * 0xD1 kicks a member by entity id. Links live only in memory, like retail.
 */
@Singleton
class LinkService
@Inject
constructor(
    private val sessionRegistry: SessionRegistry,
    private val characterStore: CharacterStore,
    private val speciesRegistry: SpeciesRegistry,
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

  /** A link member as the overlay draws it: name, head and the party's icons. */
  private fun member(charId: Long): EntityGroupMember {
    val stored = characterStore.getCharacter(charId)
    val name = stored?.info?.name ?: "?"
    val head = stored?.let { PlayerHead(it.info.skinRegionSelectionIndex, it.skins) } ?: PlayerHead()
    return EntityGroupMember(
        entityId = charId,
        appearance = EntityAppearanceInfo(name, head),
        frames = partyFrames(stored?.pokemon.orEmpty()),
    )
  }

  /** The party's icons in slot order. */
  private fun partyFrames(party: List<de.fiereu.openmmo.common.Pokemon>): GroupListFrameSet {
    val ordered = party.sortedBy { it.containerSlot }
    return GroupListFrameSet(if (ordered.isEmpty()) null else PARTY_LIST, ordered.map(::icon))
  }

  /**
   * The character's party changed (a rearrange, a heal, a withdrawal...): everyone in its link gets
   * its new icons (s2c 0xDB, r32645 f/f5).
   */
  fun onPartyChanged(charId: Long) {
    val link = linkOf(charId) ?: return
    val party = characterStore.getCharacter(charId)?.pokemon ?: return
    val update = EntityFramesUpdatePacket(charId, partyFrames(party))
    for (member in link.members) sessionRegistry.getByCharacterId(member)?.send(update)
  }

  private fun icon(mon: de.fiereu.openmmo.common.Pokemon): GroupListFrame {
    val female = speciesRegistry.forMonster(mon)?.let { Gender.of(it.genderRatio, mon.seed) } == Gender.FEMALE
    val rarity =
        (if (mon.isShiny) PokemonRarityFlag.SHINY.mask else 0) or
            (if (mon.isAlpha) PokemonRarityFlag.ALPHA.mask else 0) or
            (if (mon.isSecret) PokemonRarityFlag.SECRET_SHINY.mask else 0)
    return GroupListFrame(
        monsterId = mon.id,
        species = clientSpeciesId(mon.dexId).toShort(),
        form = mon.form.toByte(),
        gender = if (female) 1 else 0,
        rarity = rarity.toShort(),
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
