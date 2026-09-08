package de.fiereu.openmmo.server.game.services

import de.fiereu.network.SessionContext
import de.fiereu.openmmo.common.clientSpeciesId
import de.fiereu.openmmo.net.game.packets.WorldFlagTableResetPacket
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.StoredCharacter
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.BitSet
import java.util.zip.DeflaterOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The player's Pokedex progress: which species they have seen, own, and own as the original
 * trainer.
 *
 * The client keeps these as tiered per-species bitsets, synced in bulk by the packet this server
 * had mislabelled "world flags". What it was sending there was a hex dump captured from one early
 * play session - Snivy and Patrat owned, the three starters and Lillipup seen - replayed
 * identically at every login, for every character, forever. The dex's ownership display was not
 * broken so much as frozen: it showed that captured moment regardless of what the player did.
 *
 * There is no retail server to imitate, so the semantics are defined here and the client's decoded
 * reader is the contract:
 * - group 0 is seen, group 1 owned, group 2 owned-as-OT; the fourth group is unused and empty.
 * - each group is `zlib( s16 byteCount, bitsetBytes, u8 0 )`, big-endian length, bitset in
 *   `BitSet.valueOf` byte order, bit index = client wire id.
 *
 * The captured blob's bitset was 64 bytes - species past id 512 could not even be represented,
 * which is one reason imported species never showed a tier. Ours is sized for the full roster.
 *
 * Owned and owned-as-OT are derived from what the character actually holds, so they cannot drift.
 * Seen is accumulated: everything ever owned plus every species marked by [markSeen] (wild
 * encounters, gifts), persisted as story flags so it survives restarts.
 */
@Singleton
class DexProgressService @Inject constructor(private val characters: CharacterStore) {

  fun resetPacket(stored: StoredCharacter): WorldFlagTableResetPacket {
    val owned = heldWireIds(stored)
    val asOriginalTrainer =
        (stored.pokemon + stored.pcStorage)
            .filter { it.ot == stored.info.name }
            .map { clientSpeciesId(it.dexId) }
            .filter { it in 1..LAST_WIRE_ID }
            .toSet()
    val seen =
        owned +
            stored.storyFlags
                .filter { it.startsWith(SEEN_FLAG_PREFIX) }
                .mapNotNull { it.removePrefix(SEEN_FLAG_PREFIX).toIntOrNull() }
                .filter { it in 1..LAST_WIRE_ID }
    return WorldFlagTableResetPacket(
        listOf(group(seen), group(owned), group(asOriginalTrainer), ByteArray(0)))
  }

  /**
   * Marks species as seen and, when anything actually changed, pushes the refreshed tiers to the
   * client. Call it for wild encounters and gifts; ownership needs no marking because it is
   * derived.
   */
  fun markSeen(ctx: SessionContext, characterId: Long, wireIds: Collection<Int>) {
    val stored = characters.getCharacter(characterId) ?: return
    val unseen =
        wireIds
            .filter { it in 1..LAST_WIRE_ID }
            .filter { SEEN_FLAG_PREFIX + it !in stored.storyFlags }
            .filter { it !in heldWireIds(stored) }
    if (unseen.isEmpty()) return
    unseen.forEach { characters.setStoryFlag(characterId, SEEN_FLAG_PREFIX + it) }
    refresh(ctx, characterId)
  }

  /** Recomputes and resends the tiers, for callers that changed what the character holds. */
  fun refresh(ctx: SessionContext, characterId: Long) {
    val stored = characters.getCharacter(characterId) ?: return
    ctx.send(resetPacket(stored))
  }

  private fun heldWireIds(stored: StoredCharacter): Set<Int> =
      (stored.pokemon + stored.pcStorage)
          .map { clientSpeciesId(it.dexId) }
          .filter { it in 1..LAST_WIRE_ID }
          .toSet()

  private fun group(wireIds: Set<Int>): ByteArray = DexProgressGroups.encode(wireIds)

  companion object {
    /** Covers the Dex-numbered range and the staged form block above 1079, with headroom. */
    const val LAST_WIRE_ID = DexProgressGroups.LAST_WIRE_ID

    const val SEEN_FLAG_PREFIX = "dex_seen:"
  }
}

/** One tier blob, shaped exactly as the client's `f/Sd1.xx0` decodes it. */
object DexProgressGroups {
  /**
   * The staged catalogue reaches past the Dex-numbered 1-1078 range into the form block at 1079+,
   * so the tier bitsets cover that too; the client reads the stated length, and a bit for an id it
   * never looks up is simply never read.
   */
  const val LAST_WIRE_ID = 1600

  /** Bits for every listable species, byte-aligned. */
  private const val BITSET_BYTES = (LAST_WIRE_ID + 8) / 8

  fun encode(wireIds: Set<Int>): ByteArray {
    val bits = java.util.BitSet(LAST_WIRE_ID + 1)
    wireIds.filter { it in 1..LAST_WIRE_ID }.forEach(bits::set)
    // The client reads a stated length, so the block keeps its full size even when the high bits
    // are clear; BitSet.toByteArray alone shrinks to the highest set bit.
    val block = bits.toByteArray().copyOf(BITSET_BYTES)
    val out = java.io.ByteArrayOutputStream()
    java.io.DataOutputStream(java.util.zip.DeflaterOutputStream(out)).use { data ->
      data.writeShort(block.size)
      data.write(block)
      data.writeByte(0)
    }
    return out.toByteArray()
  }
}
