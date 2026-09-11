package de.fiereu.openmmo.server.game.storage

import de.fiereu.openmmo.common.CharacterInfo
import de.fiereu.openmmo.common.DynamicWarp
import de.fiereu.openmmo.common.MAX_PARTY_SIZE
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.Skin
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.common.enums.SkinSlot
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val log = KotlinLogging.logger {}

/** The permission bits of a freshly created character on an ordinary account. */
private const val DEFAULT_PERMISSIONS = 8

private val FLUSH_TICK = 5.seconds
private val FLUSH_DEBOUNCE = 10.seconds

data class StoredCharacter(
    val info: CharacterInfo,
    val pokemon: MutableList<Pokemon>,
    val pcStorage: MutableList<Pokemon>,
    val items: MutableMap<Int, Int>,
    // Story progression. Flags are set/unset booleans, vars are named integers that default to 0.
    // Keys are opaque strings supplied by the content layer, so the store stays game agnostic.
    val storyFlags: MutableSet<String> = mutableSetOf(),
    val storyVars: MutableMap<String, Int> = mutableMapOf(),
    val skins: Map<SkinSlot, Skin> = emptyMap(),
)

/**
 * Write-through cache over [CharacterRepository]. Memory is the live version and the database
 * mirrors it. Aggregates enter the database on creation and are written back when marked dirty:
 * after a debounce by the periodic flusher, or immediately through [flushCharacterAsync] on events
 * like warps. A disconnect goes through [unloadCharacterAsync], which also evicts the aggregate
 * from the cache once its last write succeeded, so only connected players stay in memory.
 */
@Singleton
class CharacterStore
@Inject
constructor(
    private val repository: CharacterRepository,
    private val entityIds: EntityIdService,
    scope: CoroutineScope,
) {
  private val flushJob = SupervisorJob()
  private val flushScope = CoroutineScope(scope.coroutineContext + flushJob)
  private var periodicJob: Job? = null

  private val characters = ConcurrentHashMap<Long, StoredCharacter>()
  private val charactersByUser = ConcurrentHashMap<Int, CopyOnWriteArrayList<Long>>()
  // The last aggregate a write succeeded for. A flush sends the difference to it.
  private val persisted = ConcurrentHashMap<Long, StoredCharacter>()
  private val dirtySince = ConcurrentHashMap<Long, Long>()
  private val pendingUnload = ConcurrentHashMap.newKeySet<Long>()
  private val flushLocks = ConcurrentHashMap<Long, Mutex>()

  /** Create a character with its own entity id and an empty party. */
  suspend fun createCharacter(
      userId: Int,
      name: String,
      gender: CharacterGender,
      startingRegion: Region,
      skins: Map<SkinSlot, Skin> = emptyMap(),
      skinRegionSelectionIndex: Int = 0,
  ): StoredCharacter {
    val female = gender == CharacterGender.FEMALE
    val start = NewGameStarts.forRegion(startingRegion, female)
    val id = entityIds.newCharacterId()
    val now = LocalDateTime.now()
    // Staff accounts (user_permissions) start their characters with their granted bits.
    val permissions = repository.defaultPermissions(userId) ?: DEFAULT_PERMISSIONS
    val info =
        CharacterInfo(
            id = id,
            name = name,
            namePrefix = "",
            userId = userId,
            // This historical field stores the player's gender.
            rivalSex = gender.wireValue,
            skinRegionSelectionIndex = skinRegionSelectionIndex,
            lastLogin = now,
            createdAt = now,
            money = 30000,
            permissions = permissions,
            remainingSafariSteps = 0,
            remainingSafariBalls = 0,
            pcExtraSlots = 0,
            battleBoxExtraSlots = 0,
            templateAmount = 0,
            positionRegionId = startingRegion.wireValue,
            positionBankId = start.bankId,
            positionMapId = start.mapId,
            positionX = start.x,
            positionY = start.y,
            repelLeft = 0,
            repelItemId = 0,
            lureLeft = 0,
            lureItemId = 0,
            dynamicWarp = start.dynamicWarp,
        )
    val stored =
        StoredCharacter(
            info,
            mutableListOf(),
            mutableListOf(),
            mutableMapOf(),
            storyFlags = start.storyFlags.toMutableSet(),
            storyVars = start.storyVars.toMutableMap(),
            skins = skins.toMap(),
        )
    repository.insertAggregate(stored)
    characters[id] = stored
    persisted[id] = stored
    charactersByUser.computeIfAbsent(userId) { CopyOnWriteArrayList() }.add(id)
    return stored
  }

  fun getCharacter(id: Long): StoredCharacter? = characters[id]

  /** Whether [name] is already in use by any character, cached or not, ignoring case. */
  suspend fun isNameTaken(name: String): Boolean =
      characters.values.any { it.info.name.equals(name, ignoreCase = true) } || repository.nameExists(name)

  suspend fun donatorUntil(userId: Int): Long? = repository.donatorUntil(userId)

  suspend fun setDonatorUntil(userId: Int, untilEpoch: Long?) = repository.setDonatorUntil(userId, untilEpoch)

  /** Like [getCharacter] but falls back to the database when the cache has no entry. */
  suspend fun getOrLoadCharacter(id: Long): StoredCharacter? {
    pendingUnload.remove(id)
    characters[id]?.let {
      return it
    }
    val loaded = repository.loadById(id) ?: return null
    return cache(loaded)
  }

  suspend fun getCharactersByUser(userId: Int): List<StoredCharacter> {
    val cachedIds: List<Long>? = charactersByUser[userId]
    if (cachedIds != null) {
      cachedIds.forEach { pendingUnload.remove(it) }
      return cachedIds.mapNotNull { characters[it] }
    }
    val loaded = repository.loadByUser(userId).map { cache(it) }
    loaded.forEach { pendingUnload.remove(it.info.id) }
    charactersByUser.putIfAbsent(userId, CopyOnWriteArrayList(loaded.map { it.info.id }))
    return loaded
  }

  /** Permanently delete an owned character and evict every cached reference to it. */
  suspend fun deleteCharacter(userId: Int, characterId: Long): Boolean {
    if (!repository.deleteById(userId, characterId)) return false
    characters.remove(characterId)
    persisted.remove(characterId)
    charactersByUser[userId]?.remove(characterId)
    dirtySince.remove(characterId)
    pendingUnload.remove(characterId)
    return true
  }

  /**
   * Applies [change] to the cached character under the map's own lock. Scripts run on their own
   * coroutine while packets are answered on the mailbox coroutine, so a plain read, copy and write
   * would let one thread drop the other's field.
   */
  private fun mutate(characterId: Long, change: (StoredCharacter) -> StoredCharacter?): Boolean {
    var applied = false
    val present =
        characters.computeIfPresent(characterId) { _, stored ->
          val updated = change(stored)
          if (updated == null) {
            stored
          } else {
            applied = true
            updated
          }
        }
    if (present == null) {
      // A disconnect evicts the character, so a script finishing its last statements reaches this.
      log.warn { "Dropped a write for character $characterId, it is no longer cached" }
      return false
    }
    if (applied) markDirty(characterId)
    return applied
  }

  fun updateCharacter(info: CharacterInfo) {
    mutate(info.id) { it.copy(info = info) }
  }

  fun updatePosition(
      characterId: Long,
      x: Short,
      y: Short,
      bankId: Byte? = null,
      mapId: Byte? = null,
      facing: Direction? = null,
  ) {
    mutate(characterId) { stored ->
      stored.copy(
          info =
              stored.info.copy(
                  positionX = x,
                  positionY = y,
                  positionBankId = bankId ?: stored.info.positionBankId,
                  positionMapId = mapId ?: stored.info.positionMapId,
                  positionFacing = facing ?: stored.info.positionFacing,
              ),
      )
    }
  }

  /** False when the monster could not be written, in which case the party is left as it was. */
  suspend fun addPokemon(characterId: Long, pokemon: Pokemon): Boolean {
    // A seventh party member corrupts the character: the client rejects the whole character
    // list and the player can no longer log in. Monsters in any other container land in
    // pcStorage.
    val toParty = pokemon.container == de.fiereu.openmmo.common.enums.PokemonContainer.PARTY
    if (toParty &&
        (getCharacter(characterId)?.pokemon?.size ?: 0) >= de.fiereu.openmmo.common.MAX_PARTY_SIZE)
        return false
    return mutateDurably(
        characterId,
        // Copy instead of mutating in place, so flusher snapshots never see a half-updated list.
        apply = {
          if (toParty) it.copy(pokemon = (it.pokemon + pokemon).toMutableList())
          else it.copy(pcStorage = (it.pcStorage + pokemon).toMutableList())
        },
        rollback = {
          if (toParty)
              it.copy(pokemon = it.pokemon.filter { m -> m.id != pokemon.id }.toMutableList())
          else it.copy(pcStorage = it.pcStorage.filter { m -> m.id != pokemon.id }.toMutableList())
        },
    )
        .also { added ->
          // The Pokedex "caught" tier is what the character EVER owned, not what it holds now:
          // a released or traded-away species stays caught, as on the cartridge (GetSetPokedexFlag
          // on receipt). Held species still count without the flag; this covers the departures.
          if (added) setStoryFlag(characterId, de.fiereu.openmmo.server.game.services.DexProgressService.OWNED_FLAG_PREFIX + de.fiereu.openmmo.common.clientSpeciesId(pokemon.dexId))
        }
  }

  /**
   * Removes one party monster and closes the gap behind it.
   *
   * Slots are renumbered because the client draws the party by slot: leaving a hole makes the
   * remaining monsters render in the wrong places. False when nothing was written.
   */
  suspend fun removePokemon(characterId: Long, pokemonId: Long): Boolean {
    val previous = getCharacter(characterId)?.pokemon?.toList() ?: return false
    if (previous.none { it.id == pokemonId }) return false
    return mutateDurably(
        characterId,
        apply = { stored ->
          stored.copy(
              pokemon =
                  stored.pokemon
                      .filter { it.id != pokemonId }
                      .mapIndexed { slot, mon -> mon.copy(containerSlot = slot.toShort()) }
                      .toMutableList())
        },
        rollback = { it.copy(pokemon = previous.toMutableList()) },
    )
  }

  /** Swap two zero-based party slots, keeping each monster's slot field in step with the list. */
  fun swapPartySlots(characterId: Long, first: Int, second: Int): Boolean =
      mutate(characterId) { stored ->
        val party = stored.pokemon.toMutableList()
        if (first !in party.indices || second !in party.indices || first == second) {
          return@mutate null
        }
        val moved = party[first]
        party[first] = party[second].copy(containerSlot = first.toShort())
        party[second] = moved.copy(containerSlot = second.toShort())
        stored.copy(pokemon = party)
      }

  /**
   * Move one monster between the party and the PC, or within either, the way the client's
   * container drag (c2s 0x09) describes it: zero-based slots, PC slots absolute (box * 60 + cell).
   * An occupied destination swaps. The party stays contiguous - leaving it closes the gap, joining
   * it inserts at the dropped slot - and it is never emptied. False when nothing changed.
   */
  fun moveBetweenContainers(
      characterId: Long,
      from: PokemonContainer,
      fromSlot: Int,
      to: PokemonContainer,
      toSlot: Int,
  ): Boolean =
      mutate(characterId) { stored ->
        if (from == to && fromSlot == toSlot) return@mutate null
        if (!validSlot(from, fromSlot) || !validSlot(to, toSlot)) return@mutate null
        val party = stored.pokemon.toMutableList()
        val pc = stored.pcStorage.toMutableList()
        fun occupant(container: PokemonContainer, slot: Int): Pokemon? =
            if (container == PokemonContainer.PARTY) party.getOrNull(slot)
            else pc.firstOrNull { it.containerSlot.toInt() == slot }
        val moving = occupant(from, fromSlot) ?: return@mutate null
        val displaced = occupant(to, toSlot)
        val leavesParty = from == PokemonContainer.PARTY && to != PokemonContainer.PARTY
        if (leavesParty && displaced == null && party.size == 1) return@mutate null
        if (to == PokemonContainer.PARTY && displaced == null && party.size >= MAX_PARTY_SIZE)
            return@mutate null
        val movedIds = setOfNotNull(moving.id, displaced?.id)
        party.removeAll { it.id in movedIds }
        pc.removeAll { it.id in movedIds }
        fun place(mon: Pokemon, container: PokemonContainer, slot: Int) {
          if (container == PokemonContainer.PARTY) {
            party.add(minOf(slot, party.size), mon.copy(container = container))
          } else {
            pc.add(mon.copy(container = container, containerSlot = slot.toShort()))
          }
        }
        place(moving, to, toSlot)
        if (displaced != null) place(displaced, from, fromSlot)
        stored.copy(
            pokemon =
                party
                    .mapIndexed { slot, mon -> mon.copy(containerSlot = slot.toShort()) }
                    .toMutableList(),
            pcStorage = pc,
        )
      }

  private fun validSlot(container: PokemonContainer, slot: Int): Boolean =
      when (container) {
        PokemonContainer.PARTY -> slot in 0 until MAX_PARTY_SIZE
        PokemonContainer.PC -> slot in 0 until PC_CAPACITY
        else -> false
      }

  /** Replace one party monster by id, for example after a battle changed hp, xp, or level. */
  fun updatePokemon(characterId: Long, updated: Pokemon) {
    mutate(characterId) { stored ->
      stored.copy(
          pokemon = stored.pokemon.map { if (it.id == updated.id) updated else it }.toMutableList())
    }
  }

  /** False when the change could not be written, in which case the balance is left as it was. */
  suspend fun addMoney(characterId: Long, amount: Int): Boolean =
      mutateDurably(
          characterId,
          apply = { it.copy(info = it.info.copy(money = it.info.money + amount)) },
          // Undo the delta rather than restoring a snapshot, so a concurrent edit to another field
          // of the same character survives.
          rollback = { it.copy(info = it.info.copy(money = it.info.money - amount)) },
      )

  /** Add (or remove with a negative amount) one persisted bag stack. */
  /** False when the bag would go negative, or when the change could not be written. */
  suspend fun addItem(characterId: Long, itemId: Int, amount: Int): Boolean =
      mutateDurably(
          characterId,
          apply = { stored ->
            val newQuantity = (stored.items[itemId] ?: 0) + amount
            if (newQuantity < 0) return@mutateDurably null
            val items = stored.items.toMutableMap()
            if (newQuantity == 0) items.remove(itemId) else items[itemId] = newQuantity
            stored.copy(items = items)
          },
          rollback = { stored ->
            val reverted = (stored.items[itemId] ?: 0) - amount
            val items = stored.items.toMutableMap()
            if (reverted <= 0) items.remove(itemId) else items[itemId] = reverted
            stored.copy(items = items)
          },
      )

  /** Set (or clear with null) the runtime destination for MAP_DYNAMIC warps (setdynamicwarp). */
  fun setDynamicWarp(characterId: Long, warp: DynamicWarp?) {
    mutate(characterId) { it.copy(info = it.info.copy(dynamicWarp = warp)) }
  }

  /** Set (or clear with null) one skin slot - the BIKE slot is how riding persists. */
  fun setSkin(characterId: Long, slot: SkinSlot, skin: Skin?) {
    mutate(characterId) { stored ->
      val skins = stored.skins.toMutableMap()
      if (skin == null) skins.remove(slot) else skins[slot] = skin
      stored.copy(skins = skins)
    }
  }

  /**
   * Commit a whole appearance from the customization dialog (0x29): the full skin map replaces the
   * old one (an absent slot is clothing taken off), plus gender (the historical rivalSex field) and
   * the skin set's leading region-outfit byte.
   */
  fun setAppearance(
      characterId: Long,
      skins: Map<SkinSlot, Skin>,
      gender: Byte,
      skinRegionSelectionIndex: Int,
  ) {
    mutate(characterId) { stored ->
      stored.copy(
          skins = skins.toMutableMap(),
          info =
              stored.info.copy(
                  rivalSex = gender, skinRegionSelectionIndex = skinRegionSelectionIndex))
    }
  }

  /** Set a story flag. Copies the set so flusher snapshots never see a half-updated collection. */
  fun setStoryFlag(characterId: Long, flag: String) {
    mutate(characterId) { stored ->
      if (flag in stored.storyFlags) null
      else stored.copy(storyFlags = (stored.storyFlags + flag).toMutableSet())
    }
  }

  fun clearStoryFlag(characterId: Long, flag: String) {
    mutate(characterId) { stored ->
      if (flag !in stored.storyFlags) null
      else stored.copy(storyFlags = (stored.storyFlags - flag).toMutableSet())
    }
  }

  /** Set a story var. A value of 0 is the default, so it drops the row instead of storing it. */
  fun setStoryVar(characterId: Long, key: String, value: Int) {
    mutate(characterId) { stored ->
      val newVars = stored.storyVars.toMutableMap()
      if (value == 0) newVars.remove(key) else newVars[key] = value
      if (newVars == stored.storyVars) null else stored.copy(storyVars = newVars)
    }
  }

  /** Replaces every monster, the party and the pc alike, along with the bag and story state. */
  fun replaceProgress(
      characterId: Long,
      party: List<Pokemon>,
      items: Map<Int, Int>,
      storyFlags: Set<String>,
      storyVars: Map<String, Int>,
      pc: List<Pokemon> = emptyList(),
  ) {
    mutate(characterId) { stored ->
      stored.copy(
          pokemon = party.toMutableList(),
          pcStorage = pc.toMutableList(),
          items = items.toMutableMap(),
          storyFlags = storyFlags.toMutableSet(),
          // A var of 0 is the default, so it is stored as absent everywhere else too.
          storyVars = storyVars.filterValues { it != 0 }.toMutableMap(),
      )
    }
  }

  /** Puts a character back to a [snapshot] taken earlier. */
  fun restoreProgress(characterId: Long, snapshot: StoredCharacter) {
    mutate(characterId) { snapshot }
  }

  fun startPeriodicFlush() {
    periodicJob =
        flushScope.launch {
          while (isActive) {
            delay(FLUSH_TICK)
            flushOlderThan(FLUSH_DEBOUNCE.inWholeMilliseconds)
          }
        }
  }

  /** Flush one character soon, skipping the debounce. Safe to call from Netty threads. */
  fun flushCharacterAsync(characterId: Long) {
    flushScope.launch { flush(characterId) }
  }

  /**
   * Marks the character dirty and writes it before returning. Anything a player can trade or spend
   * goes through here, so a crash cannot lose an item that the client was already told it has.
   * Position, hp and story progress do not, since replaying a few seconds of those costs nothing.
   */
  /**
   * Applies a change to something a player can trade or spend and writes it before returning. A
   * failed write is undone by [rollback] and reported, so a caller never tells a player about an
   * item, a coin or a monster the database did not accept.
   */
  private suspend fun mutateDurably(
      characterId: Long,
      apply: (StoredCharacter) -> StoredCharacter?,
      rollback: (StoredCharacter) -> StoredCharacter,
  ): Boolean =
      lockFor(characterId).withLock {
        // Through [mutate] rather than a read and a write, so a script writing another field
        // between the two is not clobbered by the one this puts back.
        if (!mutate(characterId, apply)) return@withLock false
        if (flushLocked(characterId, allowEvict = false)) return@withLock true
        mutate(characterId, rollback)
        dirtySince.remove(characterId)
        false
      }

  /**
   * Persist the character and drop it from the cache once the write succeeded. While the save keeps
   * failing the character stays cached and dirty, and the periodic flusher finishes the eviction on
   * its next successful write. Loading the character again cancels the unload.
   */
  fun unloadCharacterAsync(characterId: Long) {
    pendingUnload.add(characterId)
    flushScope.launch { flush(characterId) }
  }

  suspend fun flushAll() {
    for (id in dirtySince.keys) flush(id)
  }

  /** Stop the periodic loop, wait for in-flight flushes, then persist whatever is still dirty. */
  suspend fun shutdown() {
    periodicJob?.cancel()
    flushJob.children.toList().joinAll()
    flushAll()
  }

  private fun cache(stored: StoredCharacter): StoredCharacter {
    val existing = characters.putIfAbsent(stored.info.id, stored)
    if (existing == null) persisted[stored.info.id] = stored
    return existing ?: stored
  }

  private fun markDirty(id: Long) {
    dirtySince.putIfAbsent(id, System.currentTimeMillis())
  }

  private suspend fun flushOlderThan(ageMs: Long) {
    val now = System.currentTimeMillis()
    for ((id, since) in dirtySince) {
      if (now - since >= ageMs) flush(id)
    }
  }

  // Serialised per character. Without this a second flush takes the dirty marker, skips its own
  // write and returns while the first is still inside saveChanges, so persistNow would promise a
  // write it did not make.
  private suspend fun flush(id: Long) {
    lockFor(id).withLock { flushLocked(id, allowEvict = true) }
  }

  private fun lockFor(id: Long): Mutex = flushLocks.computeIfAbsent(id) { Mutex() }

  /** True when the character is in the database, either because it was written or was not dirty. */
  private suspend fun flushLocked(id: Long, allowEvict: Boolean): Boolean {
    val since = dirtySince.remove(id)
    val stored = characters[id]
    if (since != null && stored != null) {
      try {
        repository.saveChanges(persisted[id], stored)
        // The written instance, not the current one, so a racing mutation stays dirty.
        persisted[id] = stored
      } catch (e: CancellationException) {
        // A disconnect cancelling the caller must not read as a failed write.
        dirtySince.putIfAbsent(id, since)
        throw e
      } catch (e: Exception) {
        log.warn(e) { "Failed to persist character $id, will retry" }
        dirtySince.putIfAbsent(id, since)
        return false
      }
    }
    // A durable mutation must not evict the character its own caller is still working with.
    if (allowEvict) maybeEvict(id)
    return true
  }

  private fun maybeEvict(id: Long) {
    if (!pendingUnload.remove(id)) return
    if (dirtySince.containsKey(id)) {
      pendingUnload.add(id)
      return
    }
    val stored = characters.remove(id) ?: return
    persisted.remove(id)
    flushLocks.remove(id)
    charactersByUser.remove(stored.info.userId)
  }
}

/** Slots in the client's PC container (f/Cy ordinal 0): 11 boxes of 60, addressed absolutely. */
const val PC_CAPACITY = 660
