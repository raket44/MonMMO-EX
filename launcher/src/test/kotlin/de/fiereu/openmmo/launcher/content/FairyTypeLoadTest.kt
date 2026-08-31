package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Patches the installed client's type enum and then actually loads it, so the result is known to
 * initialise rather than merely to look right as bytes.
 */
class FairyTypeLoadTest :
    FunSpec({
      val scratch =
          Path.of(
              System.getProperty("user.home"),
              "AppData/Local/Temp/claude/C--Users-raket-AppData-Local-OpenMMO",
              "09f0899a-8f87-44cd-bcd3-eb800d3ab989/scratchpad/cls/UP_rK1.class")
      val client =
          Path.of(
              System.getProperty("user.home"), "AppData/Local/MonMMO-EX/Client-31914/PokeMMO.exe")
      val expansion = Path.of("../../pokeemerald-expansion")
      val runnable = Files.isRegularFile(scratch) && Files.isRegularFile(client)

      test("Fairy joins the enum and the class still initialises").config(enabled = runnable) {
        val original = Files.readAllBytes(scratch)
        val patched = FairyTypePatch.patch(original, TypeChart.parse(expansion))

        val shape = FairyTypePatch.inspect(patched)
        shape.constants.map { it.first }.last() shouldBe "FAIRY"
        shape.ordinalOf("FAIRY") shouldBe 19
        shape.tableSize shouldBe 20

        val classDir = Files.createTempDirectory("fairy-enum")
        val target = classDir.resolve("${shape.internalName}.class")
        Files.createDirectories(target.parent)
        Files.write(target, patched)

        val loader =
            URLClassLoader(arrayOf<URL>(classDir.toUri().toURL(), client.toUri().toURL()), null)
        val type = loader.loadClass(shape.internalName.replace('/', '.'))
        val values = type.getMethod("values").invoke(null) as Array<*>
        values.size shouldBe 20

        val fairy = type.getField("FAIRY").get(null)
        (fairy as Enum<*>).name shouldBe "FAIRY"
        fairy.ordinal shouldBe 19

        // The matchups themselves, read straight out of the per-type table the client battles with.
        val table =
            type.declaredFields.single { it.type.name == "[D" }.apply { isAccessible = true }
        val slot =
            type.declaredFields.first { it.type.name == "byte" }.apply { isAccessible = true }
        fun effectiveness(attacker: Any?, defender: Any?): Double =
            (table.get(attacker) as DoubleArray)[(slot.get(defender) as Byte).toInt()]
        fun constant(name: String): Any? = values.single { (it as Enum<*>).name == name }

        effectiveness(fairy, constant("FIGHTING")) shouldBe 2.0
        effectiveness(fairy, constant("DRAGON")) shouldBe 2.0
        effectiveness(fairy, constant("DARK")) shouldBe 2.0
        effectiveness(fairy, constant("STEEL")) shouldBe 0.5
        effectiveness(fairy, constant("FIRE")) shouldBe 0.5
        effectiveness(fairy, constant("NORMAL")) shouldBe 1.0

        effectiveness(constant("DRAGON"), fairy) shouldBe 0.0
        effectiveness(constant("POISON"), fairy) shouldBe 2.0
        effectiveness(constant("STEEL"), fairy) shouldBe 2.0
        effectiveness(constant("BUG"), fairy) shouldBe 0.5

        // The type badge is text: the enum hands the UI a string id. Fairy needs its own, and the
        // other types must keep theirs, since the switch had to be rebuilt to carry the new case.
        val nameId =
            type.methods
                .single { it.returnType.name == "short" && it.parameterCount == 0 }
                .apply { isAccessible = true }
        (nameId.invoke(fairy) as Short).toInt() shouldBe FairyTypePatch.FAIRY_NAME_STRING_ID
        (nameId.invoke(constant("DARK")) as Short).toInt() shouldBe 5328
        (nameId.invoke(constant("NORMAL")) as Short).toInt() shouldBe 5332
        (nameId.invoke(constant("QUESTIONQUESTIONQUESTION")) as Short).toInt() shouldBe 0

        // Untouched matchups must survive the widening.
        effectiveness(constant("WATER"), constant("FIRE")) shouldBe 2.0
        effectiveness(constant("GHOST"), constant("NORMAL")) shouldBe 0.0
      }
    })
