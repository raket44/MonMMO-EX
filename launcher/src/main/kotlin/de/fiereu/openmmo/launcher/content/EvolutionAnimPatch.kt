package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Animates mod-sprite species in the evolution scene.
 *
 * `f/rP0` (the evolution cinematic) sets up each side in `As()`: a species the registry marks as a
 * mod sprite (`T81.C4`) gets its GIF frames fetched into `KK1[i]` and a sprite `DQ[i]` built from
 * frame 0 only; a native species gets a `um0` whose region object updates in place, which is what
 * keeps Gen 1-5 moving. The per-tick update `TV0()` never revisits `KK1`, so imported species stand
 * still. This hooks the start of `TV0()` to `monmmo.DexPatch.evolutionTick(this)`, which swaps
 * `DQ[i]`'s region to the current frame of `KK1[i]` at the GIF's cadence.
 */
object EvolutionAnimPatch {
  private const val OWNER = "f/rP0"
  private const val TICK = "TV0"
  private const val TICK_DESC = "()V"

  fun isEvolutionScene(classBytes: ByteArray): Boolean {
    if (ClassReader(classBytes).className != OWNER) return false
    var hasTick = false
    var hasFrames = false
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitField(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  value: Any?,
              ): org.objectweb.asm.FieldVisitor? {
                if (name == "KK1" && descriptor == "[[Lf/Pq1;") hasFrames = true
                return null
              }

              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): MethodVisitor? {
                if (name == TICK && descriptor == TICK_DESC) hasTick = true
                return null
              }
            },
            0,
        )
    return hasTick && hasFrames
  }

  fun patch(classBytes: ByteArray): ByteArray {
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
            if (name != TICK || descriptor != TICK_DESC) return base
            patched = true
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitCode() {
                super.visitCode()
                visitVarInsn(Opcodes.ALOAD, 0)
                visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "monmmo/DexPatch",
                    "evolutionTick",
                    "(Ljava/lang/Object;)V",
                    false)
              }
            }
          }
        },
        0)
    check(patched) { "Evolution scene tick not found in $OWNER" }
    return writer.toByteArray()
  }
}
