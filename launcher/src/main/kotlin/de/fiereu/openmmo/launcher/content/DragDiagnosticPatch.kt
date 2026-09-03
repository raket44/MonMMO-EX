package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Diagnostic for container drags that never reach the server.
 *
 * The party/storage window (`f/eI0`) finishes a drag in `Kh1(x, y)`: every selected cell carries
 * the cell it was last hovered over in `j90.vh1`, and the pairs go to `f/ln1.bk(from[], to[])`,
 * which silently returns when the arrays are empty, a slot is negative, or a pair is a no-op -
 * only then does c2s 0x09 (`f/xk`) go out. Both entries are hooked to `monmmo.DexPatch`, which
 * logs the cells (container byte, slot, occupancy) to dex-diagnostic.log.
 */
object DragDiagnosticPatch {
  private const val HELPER = "monmmo/DexPatch"
  private const val SENDER = "f/ln1"
  private const val MOVE = "bk"
  private const val MOVE_DESC = "([Lf/GJ1;[Lf/GJ1;)V"
  private const val WINDOW = "f/eI0"
  private const val DROP = "Kh1"
  private const val DROP_DESC = "(II)V"

  fun isMoveSender(classBytes: ByteArray): Boolean = hasMethod(classBytes, SENDER, MOVE, MOVE_DESC)

  fun isPartyWindow(classBytes: ByteArray): Boolean = hasMethod(classBytes, WINDOW, DROP, DROP_DESC)

  fun patchMoveSender(classBytes: ByteArray): ByteArray =
      hookEntry(classBytes, SENDER, MOVE, MOVE_DESC) { mv ->
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitVarInsn(Opcodes.ALOAD, 2)
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            HELPER,
            "dragProbe",
            "([Ljava/lang/Object;[Ljava/lang/Object;)V",
            false)
      }

  fun patchPartyWindow(classBytes: ByteArray): ByteArray =
      hookEntry(classBytes, WINDOW, DROP, DROP_DESC) { mv ->
        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "dropProbe", "(Ljava/lang/Object;)V", false)
      }

  private fun hasMethod(classBytes: ByteArray, owner: String, method: String, desc: String): Boolean {
    val reader = ClassReader(classBytes)
    if (reader.className != owner) return false
    var found = false
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): MethodVisitor? {
            if (name == method && descriptor == desc) found = true
            return null
          }
        },
        0)
    return found
  }

  private fun hookEntry(
      classBytes: ByteArray,
      owner: String,
      method: String,
      desc: String,
      prologue: (MethodVisitor) -> Unit,
  ): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    var patched = false
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
            if (name != method || descriptor != desc) return base
            patched = true
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitCode() {
                super.visitCode()
                prologue(this)
              }
            }
          }
        },
        0)
    check(patched) { "$method$desc not found in $owner" }
    return writer.toByteArray()
  }
}
