package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Hardens the client's scissor-rectangle stack (`f/RE`) against a pop with nothing pushed.
 *
 * The client's own render code ignores the push's return value, but the push (`fH0`) silently
 * declines a zero-area rectangle - which is exactly what the battle viewport is on a frame drawn
 * while the window is minimized or freshly opened. The unconditional pop (`ca0`) then throws "Array
 * is empty" and the whole client dies with a fatal render error, over a frame nobody could even
 * see.
 *
 * The guard prepends to `ca0`: when the stack is empty, disable GL_SCISSOR_TEST (3089) - the same
 * cleanup the empty-stack branch of the original method performs - and return null. Every observed
 * caller pops the return value, and the null only flows where the unpatched client would have
 * crashed outright.
 */
object ScissorGuardPatch {
  private const val OWNER = "f/RE"
  private const val STACK_FIELD = "bo0"
  private const val STACK_TYPE = "Lf/ri;"
  private const val SIZE_FIELD = "LV0"
  private const val GL_FIELD_OWNER = "f/YW"
  private const val GL_FIELD = "n00"
  private const val GL_TYPE = "Lf/gI1;"
  private const val GL_INTERFACE = "f/gI1"
  private const val GL_SCISSOR_TEST = 3089

  fun isScissorStack(classBytes: ByteArray): Boolean {
    if (ClassReader(classBytes).className != OWNER) return false
    var hasPop = false
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
                if (name == "ca0" && descriptor == "()Lf/a4;") hasPop = true
                return null
              }
            },
            0,
        )
    return hasPop
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer =
        object : ClassWriter(reader, COMPUTE_FRAMES) {
          // The client classes are not on our classpath, so the reflective lookup cannot work.
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
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
            if (name != "ca0" || descriptor != "()Lf/a4;") return base
            patched = true
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitCode() {
                super.visitCode()
                val continueLabel = Label()
                visitFieldInsn(Opcodes.GETSTATIC, OWNER, STACK_FIELD, STACK_TYPE)
                visitFieldInsn(Opcodes.GETFIELD, "f/ri", SIZE_FIELD, "I")
                visitJumpInsn(Opcodes.IFNE, continueLabel)
                visitFieldInsn(Opcodes.GETSTATIC, GL_FIELD_OWNER, GL_FIELD, GL_TYPE)
                visitIntInsn(Opcodes.SIPUSH, GL_SCISSOR_TEST)
                visitMethodInsn(Opcodes.INVOKEINTERFACE, GL_INTERFACE, "glDisable", "(I)V", true)
                visitInsn(Opcodes.ACONST_NULL)
                visitInsn(Opcodes.ARETURN)
                visitLabel(continueLabel)
              }
            }
          }
        },
        0,
    )
    check(patched) { "f/RE.ca0 was not found; the scissor guard did not apply" }
    return writer.toByteArray()
  }
}
