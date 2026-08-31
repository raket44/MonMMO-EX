package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader

class PokedexVisibilityPatchTest :
    FunSpec({
      val client = run {
        val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        listOf(
                "$local/MonMMO-EX/Client-31914/MonMMO-Local.exe",
                "$local/MonMMO-EX/Client-31914/PokeMMO.exe")
            .map(Path::of)
            .firstOrNull(Files::isRegularFile)
      }

      fun loader(): Pair<String, ByteArray>? {
        val file = client ?: return null
        return ZipFile(file.toFile()).use { archive ->
          archive
              .entries()
              .asSequence()
              .filter { it.name.endsWith(".class") }
              .firstNotNullOfOrNull {
                val bytes = archive.getInputStream(it).use { s -> s.readBytes() }
                if (PokedexVisibilityPatch.isLoader(bytes)) it.name to bytes else null
              }
        }
      }

      test("the loader sets the hidden default in two places, and the patch flips only one") {
        val (_, bytes) = loader() ?: return@test
        // The section-10 default and the section-6 explicit hide.
        PokedexVisibilityPatch.defaultHides(bytes) shouldBe 2
        val patched = PokedexVisibilityPatch.patch(bytes)
        // The explicit hide survives; only the unconditional default is gone.
        PokedexVisibilityPatch.defaultHides(patched) shouldBe 1
        ClassReader(patched).className shouldBe ClassReader(bytes).className
      }
    })
