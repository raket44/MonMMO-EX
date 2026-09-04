package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Hooks the constructors of the client's DS map classes - the concrete subclasses of `f/X3`
 * (one per ROM reader: HeartGold, Platinum, Black/White), each built as
 * `(reader, short mapIndex, byte, short, f/wj1)` - so every DS map the client builds from its
 * own ROM readers is handed to `monmmo.DexPatch.dumpNdsMap2`: the tile grid with the client's
 * own walkability and terrain answers, and the map's event records (npcs, warps, triggers). The
 * dumper also rebuilds the whole region behind the first real map, using the same reader.
 */
object NdsMapDumpPatch {
  private const val SUPER = "f/X3"
  private val ctorDesc = Regex("[(]Lf/[A-Za-z0-9]+;SBSLf/wj1;[)]V")

  fun isNdsMap(classBytes: ByteArray): Boolean = ClassReader(classBytes).superName == SUPER

  /** One matcher per concrete map class: the patch driver applies a matcher to one class only. */
  fun named(owner: String): (ByteArray) -> Boolean = { bytes ->
    val reader = ClassReader(bytes)
    reader.className == owner && reader.superName == SUPER
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
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
            if (name != "<init>" || !ctorDesc.matches(descriptor)) return base
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.RETURN) {
                  // The map is fully built here: this, reader, index, byte, short, wj1.
                  visitVarInsn(Opcodes.ALOAD, 0)
                  visitVarInsn(Opcodes.ALOAD, 1)
                  visitVarInsn(Opcodes.ILOAD, 2)
                  visitVarInsn(Opcodes.ILOAD, 3)
                  visitVarInsn(Opcodes.ILOAD, 4)
                  visitVarInsn(Opcodes.ALOAD, 5)
                  visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      "monmmo/DexPatch",
                      "dumpNdsMap2",
                      "(Ljava/lang/Object;Ljava/lang/Object;SBSLjava/lang/Object;)V",
                      false)
                }
                super.visitInsn(opcode)
              }
            }
          }
        },
        0)
    return writer.toByteArray()
  }
}
