package de.fiereu.openmmo.server.game.script.interpreter

import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Direction
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.maps.MapManager
import de.fiereu.openmmo.net.game.packets.DialogDataPacket
import de.fiereu.openmmo.script.MovementProgram
import de.fiereu.openmmo.script.PretMovementParser
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import de.fiereu.openmmo.server.game.script.Script
import de.fiereu.openmmo.server.game.script.ScriptContext
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private const val TEST_NPC = "LOCALID_TEST_NPC"
private const val WALK = "Test_Movement_Walk"

private data class MovementFixture(
    val ctx: ScriptContext,
    val session: FakeSession,
    val store: CharacterStore,
    val mapManager: MapManager,
    val npcEntityId: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MovementInterpreterTest :
    FunSpec({
      fun movement(label: String = WALK, vararg actions: String): MovementProgram =
          PretMovementParser.parse(
              ScriptId("gba", "firered", "BPRE", label),
              "movement-test.inc",
              listOf("$label::") + actions + "step_end",
          )

      fun interpreted(
          lines: List<String>,
          movements: Map<String, MovementProgram> = emptyMap(),
          objectIds: Map<String, Int> = mapOf(TEST_NPC to 0),
      ): InterpretedScript =
          InterpretedScript(
              PretScriptParser.parse(
                  id = ScriptId("gba", "firered", "BPRE", "MovementTest"),
                  storyNamespace = "kanto",
                  sourceFile = "movement-test.inc",
                  lines = listOf("MovementTest::") + lines,
                  objectIds = objectIds,
              ),
              emptyMap(),
              movements,
          )

      suspend fun fixture(scope: CoroutineScope): MovementFixture {
        val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
        val character = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO)
        store.updatePosition(character.info.id, 5, 5, bankId = 3, mapId = 0)
        val session =
            FakeSession(
                characterId = character.info.id,
                regionId = 0,
                bankId = 3,
                mapId = 0,
                facing = Direction.UP,
            )
        session.state().x = 5
        session.state().y = 5
        val maps = MapManager()
        val npcs = NpcService(maps, store)
        val npcEntityId = npcs.entityIdFor(0, 3, 0, 0)
        val ctx =
            ScriptContext(
                session,
                session.attributes[PLAYER_STATE]!!,
                entityId = npcEntityId,
                dialog = DialogService(),
                story = StoryService(store),
                movement = ScriptMovementService(maps, npcs, store),
            )
        return MovementFixture(ctx, session, store, maps, npcEntityId)
      }

      test("applymovement starts an npc movement through ScriptMovementService") {
        runTest {
          val fixture = fixture(backgroundScope)
          val walk = movement(actions = arrayOf("walk_up"))

          interpreted(
                  listOf("applymovement $TEST_NPC, $WALK", "end"),
                  mapOf(WALK to walk),
              )
              .run(fixture.ctx)

          fixture.session.sent shouldBe
              listOf(DialogDataPacket(fixture.npcEntityId, 0, 1, byteArrayOf(0x11)))
          advanceUntilIdle()
        }
      }

      test("waitmovement waits for the remembered movement and then resumes") {
        runTest {
          val fixture = fixture(backgroundScope)
          val script =
              interpreted(
                  listOf(
                      "applymovement $TEST_NPC, $WALK",
                      "waitmovement",
                      "setvar VAR_RESULT, 1",
                      "end",
                  ),
                  mapOf(WALK to movement(actions = arrayOf("walk_up"))),
              )

          val job = launch { script.run(fixture.ctx) }
          runCurrent()
          job.isActive shouldBe true
          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 0
          advanceTimeBy(249)
          runCurrent()
          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 0
          advanceTimeBy(1)
          runCurrent()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 1
          job.isActive shouldBe false
        }
      }

      test("faceplayer turns the selected npc opposite the player's facing") {
        runTest {
          val fixture = fixture(backgroundScope)

          interpreted(listOf("faceplayer", "end")).run(fixture.ctx)

          fixture.session.sent shouldBe
              listOf(DialogDataPacket(fixture.npcEntityId, 0, 1, byteArrayOf(0x00)))
        }
      }

      test("movement actions remain ordered in one service packet") {
        runTest {
          val fixture = fixture(backgroundScope)
          val sequence = movement(actions = arrayOf("walk_up", "walk_left", "face_down"))

          interpreted(
                  listOf("applymovement $TEST_NPC, $WALK", "waitmovement $TEST_NPC", "end"),
                  mapOf(WALK to sequence),
              )
              .run(fixture.ctx)

          fixture.session.sent shouldBe
              listOf(
                  DialogDataPacket(
                      fixture.npcEntityId,
                      0,
                      3,
                      byteArrayOf(0x11, 0x12, 0x00),
                  ))
        }
      }

      test("nested call and lifecycle lock survive movement suspension") {
        runTest {
          val fixture = fixture(backgroundScope)
          val script =
              interpreted(
                  listOf(
                      "lock",
                      "call MoveNpc",
                      "addvar VAR_RESULT, 2",
                      "release",
                      "end",
                      "MoveNpc::",
                      "applymovement $TEST_NPC, $WALK",
                      "waitmovement",
                      "addvar VAR_RESULT, 1",
                      "return",
                  ),
                  mapOf(WALK to movement(actions = arrayOf("walk_up"))),
              )

          val job = launch { script.run(fixture.ctx) }
          runCurrent()
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.LOCAL
          advanceUntilIdle()
          job.join()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 3
          fixture.ctx.state.scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("LOCALID_PLAYER moves and commits the player through the existing service") {
        runTest {
          val fixture = fixture(backgroundScope)
          val characterId = fixture.ctx.state.characterId!!

          interpreted(
                  listOf(
                      "applymovement LOCALID_PLAYER, $WALK",
                      "waitmovement",
                      "end",
                  ),
                  mapOf(WALK to movement(actions = arrayOf("walk_up", "walk_left"))),
              )
              .run(fixture.ctx)

          fixture.store.getCharacter(characterId)!!.info.positionX shouldBe 4
          fixture.store.getCharacter(characterId)!!.info.positionY shouldBe 4
        }
      }

      test("wrong local object id reports script and source context") {
        runTest {
          val fixture = fixture(backgroundScope)
          val script =
              interpreted(
                  listOf("applymovement LOCALID_MISSING, $WALK", "end"),
                  mapOf(WALK to movement(actions = arrayOf("walk_up"))),
                  mapOf("LOCALID_MISSING" to 99),
              )

          val error = shouldThrow<IllegalStateException> { script.run(fixture.ctx) }

          error.message shouldBe
              "Script gba:firered:BPRE:MovementTest cannot apply movement to npc 99 from " +
                  "`applymovement LOCALID_MISSING, $WALK`: No npc with local id 99 on map 0:3:0"
        }
      }

      test("unresolved movement label reports script and source context") {
        runTest {
          val fixture = fixture(backgroundScope)
          val script = interpreted(listOf("applymovement $TEST_NPC, Missing_Movement", "end"))

          val error = shouldThrow<IllegalStateException> { script.run(fixture.ctx) }

          error.message shouldBe
              "Script gba:firered:BPRE:MovementTest has no movement Missing_Movement from " +
                  "`applymovement $TEST_NPC, Missing_Movement`"
        }
      }

      test("unsupported movement action reports movement id action and source line") {
        runTest {
          val fixture = fixture(backgroundScope)
          val unsupported = movement(actions = arrayOf("jump_special_up"))
          val script =
              interpreted(
                  listOf("applymovement $TEST_NPC, $WALK", "end"),
                  mapOf(WALK to unsupported),
              )

          val error = shouldThrow<UnsupportedMovementActionException> { script.run(fixture.ctx) }

          error.message shouldBe
              "Unsupported movement action in gba:firered:BPRE:$WALK: jump_special_up from " +
                  "`jump_special_up`"
        }
      }

      test("ScriptRunner blocks another script while waitmovement owns execution") {
        runTest {
          val fixture = fixture(backgroundScope)
          fixture.session.attributes[SCRIPT_SCOPE] = backgroundScope
          val runner = scriptRunner(fixture.store, fixture.mapManager)
          val waiting =
              interpreted(
                  listOf(
                      "applymovement $TEST_NPC, $WALK",
                      "waitmovement",
                      "setvar VAR_RESULT, 1",
                      "end",
                  ),
                  mapOf(WALK to movement(actions = arrayOf("walk_up"))),
              )
          val competing = Script { it.setVar("kanto/VAR_RESULT", 9) }

          runner.run(fixture.session, fixture.session.state(), waiting, fixture.npcEntityId)
          runCurrent()
          runner.run(fixture.session, fixture.session.state(), competing, fixture.npcEntityId)
          advanceTimeBy(250)
          runCurrent()

          fixture.ctx.getVar("kanto/VAR_RESULT") shouldBe 1
        }
      }

      test("movement cancellation releases runner ownership and lifecycle lock") {
        runTest {
          val fixture = fixture(backgroundScope)
          val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
          fixture.session.attributes[SCRIPT_SCOPE] = scope
          val runner = scriptRunner(fixture.store, fixture.mapManager)
          val waiting =
              interpreted(
                  listOf(
                      "lockall",
                      "applymovement $TEST_NPC, $WALK",
                      "waitmovement",
                      "end",
                  ),
                  mapOf(WALK to movement(actions = arrayOf("walk_up", "walk_up"))),
              )

          runner.run(fixture.session, fixture.session.state(), waiting, fixture.npcEntityId)
          runCurrent()
          fixture.session.state().scriptRunning shouldBe true
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.ALL

          scope.cancel()
          runCurrent()

          fixture.session.state().scriptRunning shouldBe false
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }

      test("movement failure rolls back story state and releases owned state") {
        runTest {
          val fixture = fixture(backgroundScope)
          fixture.session.attributes[SCRIPT_SCOPE] = backgroundScope
          val runner = scriptRunner(fixture.store, fixture.mapManager)
          val failing =
              interpreted(
                  listOf(
                      "setflag FLAG_BEFORE_MOVEMENT",
                      "lockall",
                      "applymovement $TEST_NPC, $WALK",
                      "end",
                  ),
                  mapOf(WALK to movement(actions = arrayOf("jump_special_up"))),
              )

          runner.run(fixture.session, fixture.session.state(), failing, fixture.npcEntityId)
          runCurrent()

          fixture.store
              .getCharacter(fixture.ctx.state.characterId!!)!!
              .storyFlags
              .contains("kanto/FLAG_BEFORE_MOVEMENT") shouldBe false
          fixture.session.state().scriptRunning shouldBe false
          fixture.session.state().scriptLockScope shouldBe ScriptLockScope.NONE
        }
      }
    })
