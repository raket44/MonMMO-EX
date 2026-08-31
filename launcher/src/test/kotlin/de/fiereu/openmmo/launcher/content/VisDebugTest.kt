package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class VisDebugTest :
    FunSpec({
      test("dump ops around each putfield JI") {
        val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        val exe = Path.of("$local/MonMMO-EX/Client-31914/MonMMO-Local.exe")
        if (!Files.isRegularFile(exe)) return@test
        val bytes =
            ZipFile(exe.toFile()).use { a ->
              a.entries()
                  .asSequence()
                  .filter { it.name.endsWith(".class") }
                  .firstNotNullOfOrNull {
                    val b = a.getInputStream(it).use { s -> s.readBytes() }
                    if (PokedexVisibilityPatch.isLoader(b)) b else null
                  }
            } ?: return@test
        ClassReader(bytes)
            .accept(
                object : ClassVisitor(Opcodes.ASM9) {
                  override fun visitMethod(
                      access: Int,
                      name: String,
                      descriptor: String,
                      signature: String?,
                      exceptions: Array<out String>?,
                  ): MethodVisitor {
                    val trail = ArrayDeque<String>()
                    return object : MethodVisitor(Opcodes.ASM9) {
                      fun rec(s: String) {
                        trail.addLast(s)
                        if (trail.size > 3) trail.removeFirst()
                      }

                      override fun visitInsn(op: Int) = rec("INSN($op)")

                      override fun visitVarInsn(op: Int, v: Int) = rec("VAR($op,$v)")

                      override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                        if (n == "JI" && d == "Z")
                            println("[vis] $name PUTFIELD $ow.$n after ${trail.joinToString(" ")}")
                        rec("FIELD")
                      }
                    }
                  }
                },
                0)
      }
    })
