package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Hooks the client's NDS map constructor (`f/Fk1(byte region, short mapIndex)`) so every DS map
 * the client builds from its own ROM readers is written out by `monmmo.DexPatch.dumpNdsMap`:
 * the tile grid with the client's own walkability and terrain answers, and the map's event
 * records (npcs, warps, triggers). The server imports those files for Johto, Sinnoh and Unova,
 * which it otherwise has no map data for.
 */
object NdsMapDumpPatch {
  private const val OWNER = "f/Fk1"
  private const val CTOR = "<init>"
  private const val DESC = "(BS)V"

  fun isNdsMap(classBytes: ByteArray): Boolean {
    val reader = ClassReader(classBytes)
    if (reader.className != OWNER) return false
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
            if (name == CTOR && descriptor == DESC) found = true
            return null
          }
        },
        0)
    return found
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
            if (name != CTOR || descriptor != DESC) return base
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.RETURN) {
                  // The map is fully built here: this, region, index -> the dumper.
                  visitVarInsn(Opcodes.ALOAD, 0)
                  visitVarInsn(Opcodes.ILOAD, 1)
                  visitVarInsn(Opcodes.ILOAD, 2)
                  visitMethodInsn(
                      Opcodes.INVOKESTATIC,
                      "monmmo/DexPatch",
                      "dumpNdsMap",
                      "(Ljava/lang/Object;BS)V",
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
