package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Makes the in-game wardrobe (client f/Te) apply what the player picks.
 *
 * Two things stand between a click and the server:
 * - The in-game opener (f/HQ) hard-codes mode f/r4.XO0, the PREVIEW mode: the floppy applies the
 *   set locally and sends nothing, and the option list skips every free addon (f/Te.sf1 lists
 *   them only when f/r4.gB(), which is "any mode but XO0"). Mode Qc0 sends the full set (c2s
 *   0x29) on the floppy and lists the free addons. This was once a hand-made byte patch
 *   (tools/clientpatch/PatchHQ) that fell out of the overlay when the jar was regenerated.
 * - An option row is an f/ce; its constructor for an addon-backed row sets the by-addon flag
 *   (HO) to false, so the per-click apply (c2s 0x30, f/PQ) sends the row's BAG STACK id - and a
 *   free addon has no stack, so the click went out as stack 0, which is also what the "None" row
 *   sends. With the flag true every addon-backed row sends its addon id, which the server can
 *   resolve and check ownership for; the "None" row (the String constructor) keeps its zero.
 */
object WardrobePatch {
  private const val MODE_ENUM = "f/r4"
  private const val PREVIEW_MODE = "XO0"
  private const val SEND_MODE = "Qc0"
  private const val OPENER_OWNER = "f/ON0"
  private const val OPENER_NAME = "KF0"
  private const val OPENER_DESCRIPTOR = "(Lf/r4;Lf/jK1;B)V"

  private const val ENTRY_CONSTRUCTOR = "<init>"
  private const val ADDON_ENTRY_DESCRIPTOR = "(Lf/Te;Lf/J61;BB)V"
  private const val NONE_ENTRY_DESCRIPTOR = "(Lf/Te;Ljava/lang/String;)V"
  private const val BY_ADDON_FLAG = "HO"

  /** True for the in-game opener: it reads the preview mode and hands it to the dialog opener. */
  fun isOpener(classBytes: ByteArray): Boolean {
    var readsPreviewMode = false
    var opensDialog = false
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
                    override fun visitFieldInsn(o: Int, owner: String, n: String, d: String) {
                      if (o == Opcodes.GETSTATIC && owner == MODE_ENUM && n == PREVIEW_MODE) {
                        readsPreviewMode = true
                      }
                    }

                    override fun visitMethodInsn(
                        o: Int,
                        owner: String,
                        n: String,
                        d: String,
                        itf: Boolean,
                    ) {
                      if (owner == OPENER_OWNER && n == OPENER_NAME && d == OPENER_DESCRIPTOR) {
                        opensDialog = true
                      }
                    }
                  }
            },
            ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    return readsPreviewMode && opensDialog
  }

  /** Retargets the opener from the preview mode to the sending mode. */
  fun patchOpener(classBytes: ByteArray): ByteArray {
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
                override fun visitFieldInsn(o: Int, owner: String, n: String, d: String) {
                  if (o == Opcodes.GETSTATIC && owner == MODE_ENUM && n == PREVIEW_MODE) {
                    rewritten++
                    super.visitFieldInsn(o, owner, SEND_MODE, d)
                  } else {
                    super.visitFieldInsn(o, owner, n, d)
                  }
                }
              }
        },
        0)
    check(rewritten == 1) { "Expected one preview-mode read in the wardrobe opener, found $rewritten" }
    return writer.toByteArray()
  }

  /** True for the option-row class: the one with both row constructors. */
  fun isOptionRow(classBytes: ByteArray): Boolean {
    var addonRow = false
    var noneRow = false
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
                if (name == ENTRY_CONSTRUCTOR && descriptor == ADDON_ENTRY_DESCRIPTOR) addonRow = true
                if (name == ENTRY_CONSTRUCTOR && descriptor == NONE_ENTRY_DESCRIPTOR) noneRow = true
                return null
              }
            },
            ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    return addonRow && noneRow
  }

  /**
   * Flips the addon row's by-addon flag to true: the `iconst_0` feeding `putfield HO` in the
   * addon constructor becomes `iconst_1`. Same stack shape, so the frames are copied unchanged.
   */
  fun patchOptionRow(classBytes: ByteArray): ByteArray {
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
          ): MethodVisitor {
            val target = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != ENTRY_CONSTRUCTOR || descriptor != ADDON_ENTRY_DESCRIPTOR) return target
            return object : MethodVisitor(Opcodes.ASM9, target) {
              // An iconst_0 is held back until the next instruction shows what it feeds.
              private var pendingZero = false

              private fun flush() {
                if (pendingZero) {
                  super.visitInsn(Opcodes.ICONST_0)
                  pendingZero = false
                }
              }

              override fun visitInsn(opcode: Int) {
                flush()
                if (opcode == Opcodes.ICONST_0) pendingZero = true else super.visitInsn(opcode)
              }

              override fun visitFieldInsn(o: Int, owner: String, n: String, d: String) {
                if (pendingZero && o == Opcodes.PUTFIELD && n == BY_ADDON_FLAG && d == "Z") {
                  pendingZero = false
                  rewritten++
                  super.visitInsn(Opcodes.ICONST_1)
                } else {
                  flush()
                }
                super.visitFieldInsn(o, owner, n, d)
              }

              override fun visitIntInsn(opcode: Int, operand: Int) {
                flush()
                super.visitIntInsn(opcode, operand)
              }

              override fun visitVarInsn(opcode: Int, varIndex: Int) {
                flush()
                super.visitVarInsn(opcode, varIndex)
              }

              override fun visitTypeInsn(opcode: Int, type: String) {
                flush()
                super.visitTypeInsn(opcode, type)
              }

              override fun visitMethodInsn(
                  o: Int,
                  owner: String,
                  n: String,
                  d: String,
                  itf: Boolean,
              ) {
                flush()
                super.visitMethodInsn(o, owner, n, d, itf)
              }

              override fun visitInvokeDynamicInsn(
                  n: String,
                  d: String,
                  handle: org.objectweb.asm.Handle,
                  vararg args: Any,
              ) {
                flush()
                super.visitInvokeDynamicInsn(n, d, handle, *args)
              }

              override fun visitJumpInsn(opcode: Int, label: org.objectweb.asm.Label) {
                flush()
                super.visitJumpInsn(opcode, label)
              }

              override fun visitLabel(label: org.objectweb.asm.Label) {
                flush()
                super.visitLabel(label)
              }

              override fun visitLdcInsn(value: Any) {
                flush()
                super.visitLdcInsn(value)
              }

              override fun visitIincInsn(varIndex: Int, increment: Int) {
                flush()
                super.visitIincInsn(varIndex, increment)
              }

              override fun visitTableSwitchInsn(
                  min: Int,
                  max: Int,
                  dflt: org.objectweb.asm.Label,
                  vararg labels: org.objectweb.asm.Label,
              ) {
                flush()
                super.visitTableSwitchInsn(min, max, dflt, *labels)
              }

              override fun visitLookupSwitchInsn(
                  dflt: org.objectweb.asm.Label,
                  keys: IntArray,
                  labels: Array<out org.objectweb.asm.Label>,
              ) {
                flush()
                super.visitLookupSwitchInsn(dflt, keys, labels)
              }

              override fun visitMultiANewArrayInsn(descriptor: String, numDimensions: Int) {
                flush()
                super.visitMultiANewArrayInsn(descriptor, numDimensions)
              }

              override fun visitFrame(
                  type: Int,
                  numLocal: Int,
                  local: Array<out Any>?,
                  numStack: Int,
                  stack: Array<out Any>?,
              ) {
                flush()
                super.visitFrame(type, numLocal, local, numStack, stack)
              }

              override fun visitLineNumber(line: Int, start: org.objectweb.asm.Label) {
                flush()
                super.visitLineNumber(line, start)
              }

              override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                flush()
                super.visitMaxs(maxStack, maxLocals)
              }
            }
          }
        },
        0)
    check(rewritten == 1) { "Expected one by-addon flag store in the addon row constructor, found $rewritten" }
    return writer.toByteArray()
  }
}
