package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.network.PacketEvent
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.dialog.DialogLine
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.net.game.packets.MapLoadedAckPacket
import de.fiereu.openmmo.net.game.packets.battle.BattleFieldStatePacket
import de.fiereu.openmmo.net.game.packets.dialog.DialogActionResponsePacket
import de.fiereu.openmmo.pokemon.LearnsetRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.script.LabelArg
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.script.TextArg
import de.fiereu.openmmo.script.TrainerArg
import de.fiereu.openmmo.server.game.battle.BattlePacketEmitter
import de.fiereu.openmmo.server.game.battle.BattleRegistry
import de.fiereu.openmmo.server.game.battle.BattleResult
import de.fiereu.openmmo.server.game.battle.BattleRewards
import de.fiereu.openmmo.server.game.battle.MoveLearner
import de.fiereu.openmmo.server.game.battle.TurnEngine
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
import de.fiereu.openmmo.server.game.services.BattleService
import de.fiereu.openmmo.server.game.services.ClassicModeService
import de.fiereu.openmmo.server.game.services.DexProgressService
import de.fiereu.openmmo.server.game.services.DialogService
import de.fiereu.openmmo.server.game.services.NpcService
import de.fiereu.openmmo.server.game.services.ScriptMovementService
import de.fiereu.openmmo.server.game.services.StoryService
import de.fiereu.openmmo.server.game.session.PLAYER_STATE
import de.fiereu.openmmo.server.game.session.SCRIPT_SCOPE
import de.fiereu.openmmo.server.game.session.ScriptLockScope
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import de.fiereu.openmmo.server.game.testsupport.scriptRunner
import de.fiereu.openmmo.server.game.world.interest.InterestManager
import de.fiereu.openmmo.trainer.TrainerRegistry
import de.fiereu.openmmo.typechart.TypeChart
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private const val BASE_TRAINER = "TRAINER_YOUNGSTER_BEN"
private const val BASE_TRAINER_ID = 89
private const val FIRST_REMATCH_ID = 101
private const val TACKLE: Short = 33

private data class TestLine(override val textId: Int) : DialogLine

private class TrainerBattleFixture private constructor(scope: CoroutineScope) {
  val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
  val session = FakeSession(regionId = Region.KANTO.wireValue.toInt())
  val dialog = DialogService()
  val story = StoryService(store)
  val battleRegistry = BattleRegistry()
  val trainerRegistry = TrainerRegistry()
  private val maps = MapManager()
  private val interest = InterestManager()
  val battles =
      BattleService(
          characterStore = store,
          battles = battleRegistry,
          engine = TurnEngine(MoveRegistry(), TypeChart()),
          wildMons =
              WildMonFactory(
                  SpeciesRegistry(), MoveRegistry(), LearnsetRegistry(), EntityIdService()),
          emitter = BattlePacketEmitter(interest),
          rewards = BattleRewards(),
          moveLearner = MoveLearner(LearnsetRegistry(), MoveRegistry()),
          interestManager = interest,
          speciesRegistry = SpeciesRegistry(),
          moveRegistry = MoveRegistry(),
          dexProgress = DexProgressService(store),
          trainers = trainerRegistry,
          items = ItemRegistry(),
          classicMode = ClassicModeService(store),
      )
  val runner = scriptRunner(store, maps, interest, battles, dialog)
  lateinit var ctx: ScriptContext
  var charId: Long = 0

  suspend fun initialize() {
    val created = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO)
    charId = created.info.id
    session.state().characterId = charId
    store.addPokemon(charId, bulbasaur(charId))
    ctx =
        ScriptContext(
            session,
            session.attributes[PLAYER_STATE]!!,
            entityId = 42,
            dialog = dialog,
            story = story,
            movement = ScriptMovementService(maps, NpcService(maps, store), store),
            battles = battles,
        )
  }

  fun program(lines: List<String>, source: String = "firered") =
      PretScriptParser.parse(
          ScriptId("gba", source, if (source == "firered") "BPRE" else "BPEE", "TrainerTest"),
          if (source == "firered") "kanto" else "hoenn",
          "trainer-test.inc",
          listOf("TrainerTest::") + lines,
      )

  fun interpreted(lines: List<String>, text: Map<String, DialogLine> = TEXT) =
      InterpretedScript(program(lines), text)

  fun acknowledgeIntro() {
    dialog.onInteractive(PacketEvent(DialogActionResponsePacket(id = 0, unk = 0), session))
  }

  fun activeBattleTrainerId(): Int =
      battleRegistry.byChar(charId).shouldNotBeNull().trainer.shouldNotBeNull().id

  fun finish(result: BattleResult) {
    if (result == BattleResult.DISCONNECTED) {
      battles.onDisconnect(session)
      return
    }
    battleRegistry.byChar(charId).shouldNotBeNull().pendingResult = result
    battles.onClientReady(PacketEvent(MapLoadedAckPacket(), session))
  }

  companion object {
    suspend fun create(scope: CoroutineScope) = TrainerBattleFixture(scope).also { it.initialize() }
  }
}

private val TEXT = mapOf("Intro_Text" to TestLine(100), "Defeat_Text" to TestLine(101))

private fun battleCommand(continuation: String? = null): String =
    "trainerbattle_single $BASE_TRAINER, Intro_Text, Defeat_Text" +
        continuation?.let { ", $it" }.orEmpty()

private fun bulbasaur(ownerId: Long) =
    Pokemon(
        id = EntityIdService().newMonsterId(),
        ownerId = ownerId,
        container = PokemonContainer.PARTY,
        containerSlot = 0,
        dexId = 1,
        seed = 0,
        ot = "Red",
        nickname = "",
        level = 50,
        hp = 999,
        xp = 0,
        eVs = EVs(),
        iVs = IVs(),
        moves =
            listOf(
                PokemonMove(TACKLE, 35),
                PokemonMove(0, 0),
                PokemonMove(0, 0),
                PokemonMove(0, 0),
            ),
        isShiny = false,
        hasHiddenAbility = false,
        isAlpha = false,
        isSecret = false,
        isFatefulEncounter = false,
        isRaidEncounter = false,
        caughtAt = LocalDateTime.now(),
    )

@OptIn(ExperimentalCoroutinesApi::class)
class TrainerBattleInterpreterTest :
    FunSpec({
      test("parser types trainer text and continuation arguments") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val instruction =
              fixture
                  .program(listOf(battleCommand("Won"), "end", "Won::", "end"))
                  .instructions
                  .first()

          instruction.args shouldBe
              listOf(
                  TrainerArg(BASE_TRAINER),
                  TextArg("Intro_Text"),
                  TextArg("Defeat_Text"),
                  LabelArg("Won"),
              )
        }
      }

      test("trainerbattle single accepts the authentic no-music continuation form") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val line = "${battleCommand("Won")}, NO_MUSIC"
          val script =
              fixture.interpreted(
                  listOf(
                      line,
                      "end",
                      "Won::",
                      "setvar VAR_RESULT, 5",
                      "end",
                  ))
          val job = launch { script.run(fixture.ctx) }
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()
          fixture.finish(BattleResult.VICTORY)
          job.join()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 5
        }
      }

      test("first trainer battle starts and suspends the interpreted script") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val job = launch { fixture.interpreted(listOf(battleCommand(), "end")).run(fixture.ctx) }
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()

          fixture.activeBattleTrainerId() shouldBe BASE_TRAINER_ID
          job.isActive.shouldBeTrue()
          fixture.session.sent.filterIsInstance<BattleFieldStatePacket>().size shouldBe 1

          fixture.finish(BattleResult.VICTORY)
          job.join()
        }
      }

      test("victory resumes at the explicit post-battle continuation") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val script =
              fixture.interpreted(
                  listOf(
                      battleCommand("Won"),
                      "setvar VAR_RESULT, 1",
                      "end",
                      "Won::",
                      "setvar VAR_RESULT, 7",
                      "end",
                  ))
          val job = launch { script.run(fixture.ctx) }
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()
          fixture.finish(BattleResult.VICTORY)
          job.join()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 7
        }
      }

      test("defeated state persists and a second interaction follows the post-battle path") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val script =
              fixture.interpreted(listOf(battleCommand(), "setvar VAR_POST_BATTLE, 1", "end"))
          val first = launch { script.run(fixture.ctx) }
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()
          fixture.finish(BattleResult.VICTORY)
          first.join()

          val defeated = TrainerStoryState.defeated("kanto", BASE_TRAINER_ID)
          fixture.ctx.isFlagSet(defeated).shouldBeTrue()
          fixture.ctx.getVar("kanto/VAR_POST_BATTLE") shouldBe 0

          script.run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_POST_BATTLE") shouldBe 1
          fixture.battleRegistry.byChar(fixture.charId) shouldBe null
        }
      }

      test("call stack and continuation survive battle suspension") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val script =
              fixture.interpreted(
                  listOf(
                      "call Fight",
                      "addvar VAR_RESULT, 4",
                      "end",
                      "Fight::",
                      battleCommand("Won"),
                      "return",
                      "Won::",
                      "addvar VAR_RESULT, 2",
                      "return",
                  ))
          val job = launch { script.run(fixture.ctx) }
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()
          fixture.finish(BattleResult.VICTORY)
          job.join()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 6
        }
      }

      test("script ownership and lifecycle lock survive the battle wait") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          fixture.session.attributes[SCRIPT_SCOPE] = backgroundScope
          val script =
              fixture.interpreted(
                  listOf(battleCommand("Won"), "end", "Won::", "setvar VAR_RESULT, 1", "end"))

          fixture.runner.run(fixture.session, fixture.session.state(), script, 42)
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()

          fixture.session.state().scriptRunning.shouldBeTrue()
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.LOCAL
          fixture.runner.run(
              fixture.session,
              fixture.session.state(),
              Script { it.setVar("kanto/VAR_RESULT", 99) },
              43,
          )
          runCurrent()
          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 0

          fixture.finish(BattleResult.VICTORY)
          runCurrent()
          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 1
          fixture.session.state().scriptRunning.shouldBeFalse()
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("disconnect stops the suspended script and cleans owned state") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          fixture.session.attributes[SCRIPT_SCOPE] = backgroundScope
          fixture.runner.run(
              fixture.session,
              fixture.session.state(),
              fixture.interpreted(
                  listOf(battleCommand("Won"), "end", "Won::", "setflag FLAG_BAD", "end")),
              42,
          )
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()

          fixture.finish(BattleResult.DISCONNECTED)
          runCurrent()

          fixture.ctx.isFlagSet("kanto/FLAG_BAD").shouldBeFalse()
          fixture.ctx
              .isFlagSet(TrainerStoryState.defeated("kanto", BASE_TRAINER_ID))
              .shouldBeFalse()
          fixture.session.state().scriptRunning.shouldBeFalse()
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("battle startup failure rolls back and releases script ownership") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          fixture.store.replaceProgress(
              fixture.charId,
              party = emptyList(),
              items = emptyMap(),
              storyFlags = emptySet(),
              storyVars = emptyMap(),
          )
          fixture.session.attributes[SCRIPT_SCOPE] = backgroundScope
          fixture.runner.run(
              fixture.session,
              fixture.session.state(),
              fixture.interpreted(
                  listOf(
                      "setflag FLAG_BEFORE_BATTLE",
                      battleCommand(),
                      "end",
                  )),
              42,
          )
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()

          fixture.ctx.isFlagSet("kanto/FLAG_BEFORE_BATTLE").shouldBeFalse()
          fixture.session.state().scriptRunning.shouldBeFalse()
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("rematch unavailable falls through without starting a battle") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val script =
              fixture.interpreted(
                  listOf(
                      "trainerbattle_rematch $BASE_TRAINER, Intro_Text, Defeat_Text",
                      "setvar VAR_RESULT, 3",
                      "end",
                  ))

          script.run(fixture.ctx)

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 3
          fixture.battleRegistry.byChar(fixture.charId) shouldBe null
        }
      }

      test("available rematch selects the next authentic trainer and consumes readiness") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val ready = TrainerStoryState.rematchReady("kanto", BASE_TRAINER_ID)
          fixture.ctx.setVar(ready, 1)
          fixture.ctx.setFlag("kanto/FLAG_GOT_VS_SEEKER")
          val script =
              fixture.interpreted(
                  listOf(
                      "trainerbattle_rematch $BASE_TRAINER, Intro_Text, Defeat_Text",
                      "end",
                  ))
          val job = launch { script.run(fixture.ctx) }
          runCurrent()
          fixture.acknowledgeIntro()
          runCurrent()

          fixture.activeBattleTrainerId() shouldBe FIRST_REMATCH_ID
          fixture.finish(BattleResult.VICTORY)
          job.join()

          fixture.ctx.getVar(ready) shouldBe 0
          fixture.ctx
              .isFlagSet(TrainerStoryState.defeated("kanto", FIRST_REMATCH_ID))
              .shouldBeTrue()
        }
      }

      listOf(
              emptyMap<String, DialogLine>() to "unknown text label Intro_Text",
              mapOf("Intro_Text" to TestLine(100)) to "unknown text label Defeat_Text",
          )
          .forEach { (availableText, expected) ->
            test("trainer text resolution failure is clear for $expected") {
              runTest {
                val fixture = TrainerBattleFixture.create(backgroundScope)
                val script =
                    InterpretedScript(
                        fixture.program(listOf(battleCommand(), "end")), availableText)

                val error = shouldThrow<IllegalStateException> { script.run(fixture.ctx) }
                error.message!!.contains(expected).shouldBeTrue()
                error.message!!.contains(battleCommand()).shouldBeTrue()
              }
            }
          }

      test("trainer constant resolution failure includes source context") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val line = "trainerbattle_single TRAINER_MISSING, Intro_Text, Defeat_Text"
          val error =
              shouldThrow<IllegalStateException> {
                fixture.interpreted(listOf(line, "end")).run(fixture.ctx)
              }

          error.message!!.contains("cannot resolve trainer TRAINER_MISSING").shouldBeTrue()
          error.message!!.contains(line).shouldBeTrue()
        }
      }

      test("unsupported trainer battle variants still fail loudly") {
        runTest {
          val fixture = TrainerBattleFixture.create(backgroundScope)
          val line = "trainerbattle_double $BASE_TRAINER, Intro_Text, Defeat_Text, NeedTwo_Text"
          val error =
              shouldThrow<UnsupportedScriptCommandException> {
                fixture.interpreted(listOf(line, "end")).run(fixture.ctx)
              }

          error.message shouldBe
              "Unsupported script command in gba:firered:BPRE:TrainerTest: " +
                  "trainerbattle_double from `$line`"
        }
      }
    })
