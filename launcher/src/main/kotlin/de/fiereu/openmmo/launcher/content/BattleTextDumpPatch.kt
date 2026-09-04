package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Hooks the battle sub-event factory (`f/SF1.XW0()`, which builds every event a move-event
 * packet carries) so the first battle event triggers `monmmo.DexPatch.dumpBattleStrings()`: the
 * client's battle text bank, read through its own accessor, written to battle-strings.log. The
 * dex fixup hook tries the same dump earlier; this one catches the case where the bank only
 * loads with the first battle.
 */
object BattleTextDumpPatch {
  private const val OWNER = "f/SF1"
  private const val METHOD = "XW0"
  private const val DESC = "()Lf/DZ;"

  fun isEventFactory(classBytes: ByteArray): Boolean {
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
            if (name == METHOD && descriptor == DESC) found = true
            return null
          }
        },
        0)
    return found
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
            if (name != METHOD || descriptor != DESC) return base
            patched = true
            return object : MethodVisitor(Opcodes.ASM9, base) {
              override fun visitCode() {
                super.visitCode()
                visitMethodInsn(
                    Opcodes.INVOKESTATIC, "monmmo/DexPatch", "dumpBattleStrings", "()V", false)
              }
            }
          }
        },
        0)
    check(patched) { "$METHOD$DESC not found in $OWNER" }
    return writer.toByteArray()
  }
}
