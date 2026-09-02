package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Makes the dex move list's source column read "TM" again.
 *
 * The row painter labels a machine-taught move with the TOOL's name, stripping everything from " -
 * " on - stock names were "TM32 - Thunderbolt", so retail rows displayed "TM32". The operator's
 * numberless rename ("TM Thunderbolt") has no dash, so the whole item name leaked into the column.
 * Widening the strip pattern to the first space keeps the numberless names everywhere else and
 * renders the column as the bare "TM" / "HM" tag.
 */
object DexMoveRowLabelPatch {
  private const val STOCK_STRIP = " - .*"
  private const val NUMBERLESS_STRIP = " .*"
  private const val ROW_CLASS = "f/xf0"

  /** True for the move-row painter: the one class stripping tool names with the dash pattern. */
  fun isRowPainter(classBytes: ByteArray): Boolean {
    var stripsToolName = false
    var readsRow = false
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ) =
                  object : MethodVisitor(Opcodes.ASM9) {
                    override fun visitLdcInsn(value: Any?) {
                      if (value == STOCK_STRIP) stripsToolName = true
                    }

                    override fun visitFieldInsn(o: Int, owner: String, n: String, d: String) {
                      if (owner == ROW_CLASS) readsRow = true
                    }
                  }
            },
            ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    return stripsToolName && readsRow
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    var rewritten = 0
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): MethodVisitor =
              object :
                  MethodVisitor(
                      Opcodes.ASM9,
                      super.visitMethod(access, name, descriptor, signature, exceptions)) {
                override fun visitLdcInsn(value: Any?) {
                  if (value == STOCK_STRIP) {
                    rewritten++
                    super.visitLdcInsn(NUMBERLESS_STRIP)
                  } else {
                    super.visitLdcInsn(value)
                  }
                }
              }
        },
        0)
    check(rewritten >= 1) { "The tool-name strip pattern was not found in the row painter" }
    return writer.toByteArray()
  }
}
