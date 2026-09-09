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

/** Runs the wardrobe patches over the installed client and reads the result back. */
class WardrobePatchTest :
    FunSpec({
      val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
      val client = Path.of("$local/MonMMO-EX/Client-31914/MonMMO-Local.exe")

      fun classBytes(name: String): ByteArray =
          ZipFile(client.toFile()).use { jar ->
            jar.getInputStream(jar.getEntry(name)).use { it.readBytes() }
          }

      /** The mode constants a class reads, in order. */
      fun modeReads(bytes: ByteArray): List<String> {
        val reads = mutableListOf<String>()
        ClassReader(bytes)
            .accept(
                object : ClassVisitor(Opcodes.ASM9) {
                  override fun visitMethod(a: Int, n: String, d: String, s: String?, e: Array<out String>?) =
                      object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitFieldInsn(o: Int, owner: String, name: String, desc: String) {
                          if (o == Opcodes.GETSTATIC && owner == "f/r4") reads += name
                        }
                      }
                },
                0)
        return reads
      }

      /** The constant stored into the by-addon flag in each constructor, by descriptor. */
      fun flagStores(bytes: ByteArray): Map<String, Int> {
        val stores = mutableMapOf<String, Int>()
        ClassReader(bytes)
            .accept(
                object : ClassVisitor(Opcodes.ASM9) {
                  override fun visitMethod(a: Int, n: String, d: String, s: String?, e: Array<out String>?) =
                      object : MethodVisitor(Opcodes.ASM9) {
                        private var last = -1

                        override fun visitInsn(opcode: Int) {
                          last = opcode
                        }

                        override fun visitFieldInsn(o: Int, owner: String, name: String, desc: String) {
                          if (o == Opcodes.PUTFIELD && name == "HO" && n == "<init>") stores[d] = last
                          last = -1
                        }
                      }
                },
                0)
        return stores
      }

      test("the opener reads the sending mode after the patch") {
        if (!Files.isRegularFile(client)) return@test
        val opener = classBytes("f/HQ.class")
        WardrobePatch.isOpener(opener) shouldBe true
        modeReads(opener) shouldBe listOf("XO0")
        modeReads(WardrobePatch.patchOpener(opener)) shouldBe listOf("Qc0")
      }

      test("addon rows store a true by-addon flag, the None row keeps false") {
        if (!Files.isRegularFile(client)) return@test
        val row = classBytes("f/ce.class")
        WardrobePatch.isOptionRow(row) shouldBe true
        flagStores(row) shouldBe
            mapOf("(Lf/Te;Ljava/lang/String;)V" to Opcodes.ICONST_0, "(Lf/Te;Lf/J61;BB)V" to Opcodes.ICONST_0)
        flagStores(WardrobePatch.patchOptionRow(row)) shouldBe
            mapOf("(Lf/Te;Ljava/lang/String;)V" to Opcodes.ICONST_0, "(Lf/Te;Lf/J61;BB)V" to Opcodes.ICONST_1)
      }
    })
