package de.fiereu.openmmo.codegen.script

import de.fiereu.openmmo.codegen.dialog.DialogTable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ScriptCorpusGeneratorTest :
    FunSpec({
      test("malformed source is diagnosed and omitted instead of producing a corrupt program") {
        val root = kotlin.io.path.createTempDirectory("script-corpus").toFile()
        val decomp = root.resolve("pokefirered")
        val scripts = decomp.resolve("data/scripts").also { it.mkdirs() }
        scripts
            .resolve("malformed.inc")
            .writeText(
                """
                Bad_EventScript::
                    case 1
                    end
                """
                    .trimIndent())
        val dialog = root.resolve("dialog")
        DialogTable.write(dialog.resolve("kanto.json"), "kanto", "BPRE", emptyList())

        val corpus =
            ScriptCorpusGenerator(dialog)
                .build(ScriptCorpusSpec("kanto", "firered", "BPRE", decomp))

        corpus.indexedLabels shouldBe 1
        corpus.programs.size shouldBe 0
        corpus.parseFailureCategories shouldBe mapOf("case argument shape" to 1)
        corpus.parseFailureSamples.getValue("case argument shape").single() shouldContain
            "Bad_EventScript"
      }
    })
