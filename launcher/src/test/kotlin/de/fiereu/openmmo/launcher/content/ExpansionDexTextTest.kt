package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dex paragraphs keep the Expansion's line breaks: the r32645 dex panel does not wrap, and a paragraph
 * joined into one line ran off the page (project owner, 2026-09-13).
 */
class ExpansionDexTextTest :
    FunSpec({
      test("the Expansion's lines stay lines, written as the string table's \\n") {
        val body =
            """
            "The quills on its head are usually soft.\n"
            "When it flexes them, the points become\n"
            "so hard and sharp that they can pierce\n"
            "rock without any effort."
            """
        ExpansionDexText.joinQuoted(body) shouldBe
            "The quills on its head are usually soft.\\nWhen it flexes them, the points become\\n" +
                "so hard and sharp that they can pierce\\nrock without any effort."
      }

      test("spacing around a break and a trailing break are tidied, quotes unescaped") {
        ExpansionDexText.joinQuoted(""""A \"shy\"  one. \n" " It hides.\n"""") shouldBe
            "A \"shy\" one.\\nIt hides."
      }
    })
