package de.fiereu.openmmo.codegen.script

import de.fiereu.openmmo.script.MovementArg
import de.fiereu.openmmo.script.ObjectArg
import de.fiereu.openmmo.script.PretMovementParser
import de.fiereu.openmmo.script.PretScriptParser
import de.fiereu.openmmo.script.ScriptId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PretMovementParserTest :
    FunSpec({
      test("reuses ScriptIndex to parse a pret movement list") {
        val decomp = kotlin.io.path.createTempDirectory("movement-index").toFile()
        val scripts = decomp.resolve("data/scripts").also { it.mkdirs() }
        scripts
            .resolve("movement.inc")
            .writeText(
                """
                Test_Movement_WalkAndFace::
                    walk_up
                    walk_left @ retained without this comment
                    face_down
                    step_end
                """
                    .trimIndent())

        val body = ScriptIndex.build(decomp).movementBodyFor("Test_Movement_WalkAndFace")!!
        val movement =
            PretMovementParser.parse(
                ScriptId("gba", "firered", "BPRE", "Test_Movement_WalkAndFace"),
                body.sourceFile,
                body.actions,
            )

        movement.actions.map { it.command } shouldBe listOf("walk_up", "walk_left", "face_down")
        movement.actions[1].sourceLine shouldBe "walk_left"
      }

      test("indexes Emerald single-colon movement data without treating it as a script") {
        val decomp = kotlin.io.path.createTempDirectory("emerald-movement-index").toFile()
        val scripts = decomp.resolve("data/scripts").also { it.mkdirs() }
        scripts
            .resolve("movement.inc")
            .writeText(
                """
                Emerald_Movement_Walk:
                    walk_down
                    step_end
                """
                    .trimIndent())

        val index = ScriptIndex.build(decomp)

        index.bodyFor("Emerald_Movement_Walk") shouldBe null
        index.movementBodyFor("Emerald_Movement_Walk")!!.actions shouldBe
            listOf("walk_down", "step_end")
      }

      test("types applymovement and waitmovement object references") {
        val script =
            PretScriptParser.parse(
                id = ScriptId("gba", "firered", "BPRE", "Test"),
                storyNamespace = "kanto",
                sourceFile = "test.inc",
                lines =
                    listOf(
                        "Test::",
                        "applymovement LOCALID_TEST_NPC, Test_Movement",
                        "waitmovement LOCALID_TEST_NPC",
                        "end",
                    ),
            )

        script.instructions[0].args shouldBe
            listOf(ObjectArg("LOCALID_TEST_NPC"), MovementArg("Test_Movement"))
        script.instructions[1].args shouldBe listOf(ObjectArg("LOCALID_TEST_NPC"))
      }

      test("MapEventIndex normalizes pret local object constants to runtime indexes") {
        val decomp = kotlin.io.path.createTempDirectory("movement-map-events").toFile()
        val map = decomp.resolve("data/maps/TestMap").also { it.mkdirs() }
        map.resolve("map.json")
            .writeText(
                """
                {
                  "object_events": [
                    {"local_id":"LOCALID_FIRST", "script":"FirstScript"},
                    {"local_id":"LOCALID_SECOND", "script":"SecondScript"}
                  ],
                  "bg_events": [{"script":"SignScript"}]
                }
                """
                    .trimIndent())

        val index = MapEventIndex.build(decomp)

        index.byMap["TestMap"] shouldBe listOf("FirstScript", "SecondScript", "SignScript")
        index.objectIdsByMap["TestMap"] shouldBe mapOf("LOCALID_FIRST" to 0, "LOCALID_SECOND" to 1)
      }
    })
