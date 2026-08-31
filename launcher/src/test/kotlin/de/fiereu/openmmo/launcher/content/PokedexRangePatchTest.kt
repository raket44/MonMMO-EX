package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class PokedexRangePatchTest :
    FunSpec({
      val client = run {
        val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        listOf(
                "$local/MonMMO-EX/Client-31914/MonMMO-Local.exe",
                "$local/MonMMO-EX/Client-31914/PokeMMO.exe")
            .map(Path::of)
            .firstOrNull(Files::isRegularFile)
      }

      fun registry(): Pair<String, ByteArray>? {
        val file = client ?: return null
        return ZipFile(file.toFile()).use { archive ->
          archive
              .entries()
              .asSequence()
              .filter { it.name.endsWith(".class") }
              .firstNotNullOfOrNull {
                val bytes = archive.getInputStream(it).use { s -> s.readBytes() }
                if (PokedexRangePatch.isSpeciesRegistry(bytes)) it.name to bytes else null
              }
        }
      }

      /** Counts calls in the list builder, which is how the refresh shows up. */
      fun calls(bytes: ByteArray, method: String): Int {
        var count = 0
        ClassReader(bytes)
            .accept(
                object : ClassVisitor(Opcodes.ASM9) {
                  override fun visitMethod(
                      access: Int,
                      name: String,
                      descriptor: String,
                      signature: String?,
                      exceptions: Array<out String>?,
                  ) =
                      if (descriptor != "(B)Ljava/util/ArrayList;") null
                      else
                          object : MethodVisitor(Opcodes.ASM9) {
                            override fun visitMethodInsn(
                                opcode: Int,
                                owner: String,
                                name2: String,
                                descriptor2: String,
                                isInterface: Boolean,
                            ) {
                              if (name2 == method) count++
                            }
                          }
                },
                0,
            )
        return count
      }

      test("the registry is found by shape") { registry()?.first shouldBe "f/Fq1.class" }

      test("the list builder does not refresh the map, and the patch makes it") {
        val (_, bytes) = registry() ?: return@test
        // Measured in the running client: the map held 667 species while the registry held 1378.
        calls(bytes, "clear") shouldBe 0
        calls(bytes, "putAll") shouldBe 0

        val patched = PokedexRangePatch.patch(bytes)
        calls(patched, "clear") shouldBe 1
        calls(patched, "putAll") shouldBe 1
        ClassReader(patched).className shouldBe ClassReader(bytes).className
      }
    })
