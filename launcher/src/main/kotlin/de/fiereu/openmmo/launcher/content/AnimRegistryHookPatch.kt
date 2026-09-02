package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Hooks the battle-animation registry's builder so runtime move-animation entries survive.
 *
 * The registry (f/Rj1) maps move id to animation factory and is populated by its m7() builder - but
 * the battle scene (f/R61) constructs a FRESH registry when a battle loads (`aP0 = new Rj1()`), so
 * entries registered at data.pak-fixup time were silently discarded before the first move ever
 * played: registration logged 173 applied, and Moonblast still hit with the generic thump.
 *
 * The patch inserts `DexPatch.animRegistryRebuilt(this)` in the constructor right after its m7()
 * call, so each rebuild - including the very first one during class initialization - re-applies the
 * movevfx and moveanim fixups onto the instance being built, before anything can read it.
 */
object AnimRegistryHookPatch {
  private const val HELPER = "monmmo/DexPatch"
  private const val BUILDER = "m7"

  /** The registry class: the only one with the aP0/Kp1 field pair and a ()V builder named m7. */
  fun isRegistry(classBytes: ByteArray): Boolean {
    var hasApo = false
    var hasKp1 = false
    var hasBuilder = false
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitField(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  value: Any?,
              ): Nothing? {
                if (name == "aP0") hasApo = true
                if (name == "Kp1") hasKp1 = true
                return null
              }

              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): MethodVisitor? {
                if (name == BUILDER && descriptor == "()V") hasBuilder = true
                return null
              }
            },
            0,
        )
    return hasApo && hasKp1 && hasBuilder
  }

  /**
   * The hook rides in the CONSTRUCTOR, immediately after its call to the builder - not at the
   * builder's own returns. The builder's exit is a branch join whose stack-map frame declares ZERO
   * locals, so an inserted ALOAD 0 there is a verifier error: the first attempt took the client
   * down with a fatal VerifyError at boot. Right after `invokevirtual m7()` inside `<init>` the
   * receiver is provably live (it was just used) and the tiny constructor has no branch targets, so
   * no stack-map juggling is needed.
   */
  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    var calls = 0
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): MethodVisitor {
            val next = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "<init>") return next
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitMethodInsn(
                  opcode: Int,
                  owner: String,
                  methodName: String,
                  methodDescriptor: String,
                  isInterface: Boolean,
              ) {
                super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface)
                if (opcode == Opcodes.INVOKEVIRTUAL &&
                    methodName == BUILDER &&
                    methodDescriptor == "()V") {
                  super.visitVarInsn(Opcodes.ALOAD, 0)
                  super.visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      HELPER,
                      "animRegistryRebuilt",
                      "(Ljava/lang/Object;)V",
                      false,
                  )
                  calls++
                }
              }
            }
          }
        },
        0,
    )
    check(calls > 0) { "The registry constructor never calls the builder" }
    return writer.toByteArray()
  }
}
