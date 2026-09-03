package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Diagnostic for battle sprites that render as the wrong file: `f/T81` is the sprite registry and
 * `eJ(side, species, useMods, shiny)` is what the battle scene and the summary call for a monster's
 * frames. Its entry is hooked to `monmmo.DexPatch.spriteProbe`, which logs the request and what the
 * mod stores hold for that key, so the id and gender the client really asks for are on record
 * instead of inferred.
 */
object SpriteFetchDiagnosticPatch {
  private const val OWNER = "f/T81"
  private val METHODS = setOf("eJ" to "(BSZZ)[Lf/Pq1;", "XE0" to "(BSZZ)[Lf/Pq1;")

  fun isSpriteRegistry(classBytes: ByteArray): Boolean {
    if (ClassReader(classBytes).className != OWNER) return false
    var found = 0
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): MethodVisitor? {
                if ((name to descriptor) in METHODS) found++
                return null
              }
            },
            0,
        )
    return found == METHODS.size
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    var patched = 0
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            if ((name to descriptor) !in METHODS) return base
            patched++
            val label = name
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitCode() {
                super.visitCode()
                visitLdcInsn(label)
                visitVarInsn(Opcodes.ILOAD, 1)
                visitVarInsn(Opcodes.ILOAD, 2)
                visitVarInsn(Opcodes.ILOAD, 3)
                visitVarInsn(Opcodes.ILOAD, 4)
                visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "monmmo/DexPatch",
                    "spriteProbe",
                    "(Ljava/lang/String;BSZZ)V",
                    false)
              }
            }
          }
        },
        0)
    check(patched == METHODS.size) { "Sprite fetch methods not found in $OWNER" }
    return writer.toByteArray()
  }
}
