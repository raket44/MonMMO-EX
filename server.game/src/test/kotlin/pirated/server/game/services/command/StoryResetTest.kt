package pirated.server.game.services.command

import pirated.server.game.testsupport.testOcarinas
import pirated.common.CharacterPermissions
import pirated.common.enums.CharacterGender
import pirated.common.enums.PokemonContainer
import pirated.common.enums.Region
import pirated.items.ItemRegistry
import pirated.maps.MapManager
import pirated.moves.MoveRegistry
import pirated.net.game.packets.ChatMessagePacket
import pirated.pokemon.LearnsetRegistry
import pirated.pokemon.SpeciesRegistry
import pirated.server.game.battle.BattlePacketEmitter
import pirated.server.game.battle.BattleRegistry
import pirated.server.game.battle.BattleRewards
import pirated.server.game.battle.BattleRng
import pirated.server.game.battle.MoveLearner
import pirated.server.game.battle.TurnEngine
import pirated.server.game.battle.WildMonFactory
import pirated.server.game.services.BattleService
import pirated.server.game.services.ClassicModeService
import pirated.server.game.services.DexProgressService
import pirated.server.game.services.MapLoadService
import pirated.server.game.services.PresenceService
import pirated.server.game.services.StoryPlayerService
import pirated.server.game.services.WarpRules
import pirated.server.game.services.WarpService
import pirated.server.game.services.WorldStateService
import pirated.server.game.storage.CharacterStore
import pirated.server.game.storage.EntityIdService
import pirated.server.game.storage.NewGameStarts
import pirated.server.game.testsupport.FakeCharacterRepository
import pirated.server.game.testsupport.FakeSession
import pirated.server.game.world.interest.InterestManager
import pirated.server.game.world.interest.PassThroughInterestPolicy
import pirated.story.generated.kanto.KantoFlags
import pirated.story.generated.kanto.KantoVars
import pirated.trainer.TrainerRegistry
import pirated.typechart.TypeChart
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

/** Dev tools on, the way the private dev server runs, so the command dispatches. */
private fun enabledTools() =
    pirated.server.game.developer.DeveloperTools(
        pirated.server.game.config.GameServerConfig(
            host = "127.0.0.1",
            port = 0,
            checksumSize = 2,
            rootKeyResource = "game.private.pem",
            sessionSecret = "test-secret".toByteArray(),
            developer = pirated.server.game.config.DeveloperToolsConfig(enabled = true),
        ))

@OptIn(ExperimentalCoroutinesApi::class)
class StoryResetTest :
    FunSpec({
      fun storyCommand(store: CharacterStore): StoryCommand {
        val mapManager = MapManager()
        val mapLoad = MapLoadService(mapManager)
        val interest = InterestManager()
        val species = SpeciesRegistry()
        val moves = MoveRegistry()
        val items = ItemRegistry()
        return StoryCommand(
            characterStore = store,
            worldStateService = WorldStateService(DexProgressService(store)),
            storyPlayerService =
                StoryPlayerService(
                    store,
                    WildMonFactory(species, moves, LearnsetRegistry(), EntityIdService()),
                    species,
                    moves,
                    items,
                    DexProgressService(store),
                    WorldStateService(DexProgressService(store)),
                    testOcarinas(store),
                ),
            warpService =
                WarpService(
                    mapLoad,
                    mapManager,
                    store,
                    PresenceService(interest, PassThroughInterestPolicy(), mapLoad, store),
                    WarpRules(),
                ),
            battleService =
                BattleService(
                    characterStore = store,
                    battles = BattleRegistry(),
                    engine = TurnEngine(moves, TypeChart()),
                    wildMons =
                        WildMonFactory(species, moves, LearnsetRegistry(), EntityIdService()),
                    emitter = BattlePacketEmitter(interest),
                    rewards = BattleRewards(pirated.items.ItemRegistry()),
                    moveLearner = MoveLearner(LearnsetRegistry(), moves),
                    interestManager = interest,
                    speciesRegistry = species,
                    moveRegistry = moves,
                    dexProgress = DexProgressService(store),
                    trainers = TrainerRegistry(),
                    items = items,
                    classicMode = ClassicModeService(store),
                ),
            items = items,
            mapManager = mapManager,
        )
      }

      suspend fun developer(store: CharacterStore): Long {
        val created = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO)
        store.updateCharacter(
            created.info.copy(
                permissions = created.info.permissions or CharacterPermissions.DEVELOPER))
        return created.info.id
      }

      test("reset puts the character back on its region's new game story state") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = developer(store)
          store.setStoryFlag(charId, KantoFlags.FLAG_SYS_POKEMON_GET)
          store.setStoryVar(charId, KantoVars.VAR_STARTER_MON, 2)
          store.addItem(charId, itemId = 4, amount = 5)
          val before = store.getCharacter(charId)!!.storyFlags.toSet()
          val session = FakeSession(characterId = charId)
          val service = ChatCommandService(store, setOf(storyCommand(store)), enabledTools())

          service.tryHandle(session, "/story reset") shouldBe true

          val after = store.getCharacter(charId)!!
          // Only Kanto restarts: its progress is gone, its opening flags are back, and the other
          // regions' opening state (set at creation) is untouched.
          after.storyFlags.filter { it.startsWith("kanto/") }.toSet() shouldBe NewGameStarts.forRegion(Region.KANTO, female = false).storyFlags
          after.storyFlags.filterNot { it.startsWith("kanto/") }.toSet() shouldBe before.filterNot { it.startsWith("kanto/") }.toSet()
          after.storyVars.filterKeys { it.startsWith("kanto/") } shouldBe emptyMap()
          // The bag belongs to every region: a plain reset keeps it (it used to be emptied).
          after.items shouldBe mapOf(4 to 5)
          session.sent.filterIsInstance<ChatMessagePacket>().last().message shouldContain "Reset"
        }
      }

      test("a mistyped reset option is refused and destroys nothing") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = developer(store)
          val factory =
              WildMonFactory(SpeciesRegistry(), MoveRegistry(), LearnsetRegistry(), EntityIdService())
          store.addPokemon(charId, factory.create(1, 5, BattleRng(seed = 1))!!)
          store.addItem(charId, itemId = 4, amount = 5)
          store.setStoryFlag(charId, KantoFlags.FLAG_SYS_POKEMON_GET)
          val session = FakeSession(characterId = charId)
          val service = ChatCommandService(store, setOf(storyCommand(store)), enabledTools())

          // The owner's typo (2026-09-19): "keep" misspelt used to mean "wipe everything".
          service.tryHandle(session, "/story reset kepe") shouldBe true

          val after = store.getCharacter(charId)!!
          after.pokemon.size shouldBe 1
          after.items shouldBe mapOf(4 to 5)
          after.storyFlags shouldContain KantoFlags.FLAG_SYS_POKEMON_GET
          session.sent.filterIsInstance<ChatMessagePacket>().last().message shouldContain "Unknown option"
        }
      }

      test("reset wipe empties the party and the pc") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = developer(store)
          val species = SpeciesRegistry()
          val factory =
              WildMonFactory(species, MoveRegistry(), LearnsetRegistry(), EntityIdService())
          store.addPokemon(charId, factory.create(1, 5, BattleRng(seed = 1))!!)
          // A loaded character keeps boxed monsters here, so addPokemon would test the wrong shape.
          store
              .getCharacter(charId)!!
              .pcStorage
              .add(
                  factory
                      .create(4, 5, BattleRng(seed = 2))!!
                      .copy(container = PokemonContainer.PC, containerSlot = 0))
          val session = FakeSession(characterId = charId)
          val service = ChatCommandService(store, setOf(storyCommand(store)), enabledTools())

          service.tryHandle(session, "/story reset wipe") shouldBe true

          val after = store.getCharacter(charId)!!
          after.pokemon shouldBe emptyList()
          after.pcStorage shouldBe emptyList()
        }
      }

      test("reset is refused while a dialog is open") {
        runTest {
          val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), backgroundScope)
          val charId = developer(store)
          store.setStoryFlag(charId, KantoFlags.FLAG_SYS_POKEMON_GET)
          val session = FakeSession(characterId = charId)
          session.state().scriptRunning = true
          val service = ChatCommandService(store, setOf(storyCommand(store)), enabledTools())

          service.tryHandle(session, "/story reset") shouldBe true

          store.getCharacter(charId)!!.storyFlags shouldContain KantoFlags.FLAG_SYS_POKEMON_GET
          session.sent.filterIsInstance<ChatMessagePacket>().last().message shouldContain "Finish"
        }
      }
    })
