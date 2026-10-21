package pirated.server.game.testsupport

import pirated.items.ItemRegistry
import pirated.maps.MapManager
import pirated.moves.MoveRegistry
import pirated.pokemon.LearnsetRegistry
import pirated.pokemon.SpeciesRegistry
import pirated.server.game.battle.BattlePacketEmitter
import pirated.server.game.battle.BattleRegistry
import pirated.server.game.battle.BattleRewards
import pirated.server.game.battle.MoveLearner
import pirated.server.game.battle.TurnEngine
import pirated.server.game.battle.WildMonFactory
import pirated.server.game.script.ScriptRegistry
import pirated.server.game.script.ScriptRunner
import pirated.server.game.services.BattleService
import pirated.server.game.services.ClassicModeService
import pirated.server.game.services.DexProgressService
import pirated.server.game.services.DialogService
import pirated.server.game.services.EncounterService
import pirated.server.game.services.MapEntryScripts
import pirated.server.game.services.MapLoadService
import pirated.server.game.services.MapScriptService
import pirated.server.game.services.MovementService
import pirated.server.game.services.NpcService
import pirated.server.game.services.PresenceService
import pirated.server.game.services.ScriptMovementService
import pirated.server.game.services.ScriptWarpService
import pirated.server.game.services.ShopService
import pirated.server.game.services.StoryPlayerService
import pirated.server.game.services.StoryService
import pirated.server.game.services.WarpRules
import pirated.server.game.services.WarpService
import pirated.server.game.services.OcarinaService
import pirated.server.game.services.WorldStateService
import pirated.server.game.storage.CharacterStore
import pirated.server.game.storage.EntityIdService
import pirated.server.game.world.interest.InterestManager
import pirated.server.game.world.interest.PassThroughInterestPolicy
import pirated.trainer.TrainerRegistry
import pirated.typechart.TypeChart

/**
 * A real [MovementService] with every collaborator built for real, since none of them are worth
 * faking. Pass a [mapManager] that already holds the maps the test walks on.
 */
fun movementService(
    store: CharacterStore,
    mapManager: MapManager = MapManager(),
): MovementService {
  val mapLoad = MapLoadService(mapManager)
  val interest = InterestManager()
  val presence = PresenceService(interest, PassThroughInterestPolicy(), mapLoad, store)
  val npcs = NpcService(mapManager, store)
  val story = StoryService(store)
  val battles = battleService(store, interest)
  val entryScripts = MapEntryScripts(ScriptRegistry(emptyMap()), story)
  return MovementService(
      WarpService(mapLoad, mapManager, store, presence, WarpRules()),
      mapLoad,
      npcs,
      presence,
      mapManager,
      store,
      EncounterService(store, battles, SpeciesRegistry(), pirated.items.ItemRegistry()),
      MapScriptService(entryScripts, scriptRunner(store, mapManager, interest, battles), npcs, store),
      pirated.server.game.services.CustomWarps(),
      pirated.server.game.services.NdsWarps(),
      pirated.server.game.services.WarpRules(),
      pirated.server.game.services.TrainerSightService(
          npcs,
          store,
          story,
          ScriptRegistry(emptyMap()),
          scriptRunner(store, mapManager, interest, battles),
          battles,
      ),
      ScriptMovementService(mapManager, npcs, store),
      kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
  )
}

/**
 * A real [ScriptRunner]. It launches on whatever scope the session carries under SCRIPT_SCOPE, so
 * put the test's own scope there to be able to step a script.
 */
fun scriptRunner(
    store: CharacterStore,
    mapManager: MapManager = MapManager(),
    interest: InterestManager = InterestManager(),
    battles: BattleService = battleService(store, interest),
    dialog: DialogService = DialogService(),
): ScriptRunner {
  val mapLoad = MapLoadService(mapManager)
  val presence = PresenceService(interest, PassThroughInterestPolicy(), mapLoad, store)
  val npcs = NpcService(mapManager, store)
  val story = StoryService(store)
  val species = SpeciesRegistry()
  val moves = MoveRegistry()
  val wildMons = WildMonFactory(species, moves, LearnsetRegistry(), EntityIdService())
  val items = ItemRegistry()
  return ScriptRunner(
      dialog,
      story,
      ScriptMovementService(mapManager, npcs, store),
      ScriptWarpService(mapManager, mapLoad, store, presence, javax.inject.Provider { WarpService(mapLoad, mapManager, store, presence, WarpRules()) }),
      StoryPlayerService(
          store,
          wildMons,
          species,
          moves,
          items,
          DexProgressService(store),
          WorldStateService(DexProgressService(store)),
          testOcarinas(store)),
      battles,
      store,
      mapManager,
      MapEntryScripts(ScriptRegistry(emptyMap()), story),
      ShopService(store, items),
  )
}

fun battleService(store: CharacterStore, interest: InterestManager): BattleService {
  val species = SpeciesRegistry()
  val moves = MoveRegistry()
  return BattleService(
      characterStore = store,
      battles = BattleRegistry(),
      engine = TurnEngine(moves, TypeChart()),
      wildMons = WildMonFactory(species, moves, LearnsetRegistry(), EntityIdService()),
      emitter = BattlePacketEmitter(interest),
      rewards = BattleRewards(pirated.items.ItemRegistry()),
      moveLearner = MoveLearner(LearnsetRegistry(), moves),
      interestManager = interest,
      speciesRegistry = species,
      moveRegistry = moves,
      dexProgress = DexProgressService(store),
      trainers = TrainerRegistry(),
      items = ItemRegistry(),
      classicMode = ClassicModeService(store),
  )
}

/** An ocarina service with no encounter path: tests never spawn a horde through it. */
fun testOcarinas(store: CharacterStore): OcarinaService =
    OcarinaService(store, javax.inject.Provider { throw IllegalStateException("no encounters in tests") }, MapManager(), SpeciesRegistry())
