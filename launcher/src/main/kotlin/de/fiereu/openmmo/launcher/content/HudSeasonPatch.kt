package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Shows the season on the menu header's day-and-time line.
 *
 * The header widget (client f/gz, style "trainer-hud") formats that line once per minute as
 * string 1155 "{00}, {01}" through f/nV0.oP(int, String[]) and hands the result to its label.
 * This client build has no season anywhere in that header, so the hook appends one: right after
 * the format call the string goes through monmmo/HudSeason.withSeason, which adds ", Winter" and
 * the like from the client's own global season. One call site, one inserted instruction.
 */
object HudSeasonPatch {
  private const val HEADER_STYLE = "trainer-hud"
  private const val TIME_LABEL_STYLE = "label-time"
  private const val FORMATTER_OWNER = "f/nV0"
  private const val FORMATTER_NAME = "oP"
  private const val FORMATTER_DESCRIPTOR = "(I[Ljava/lang/String;)Ljava/lang/String;"
  private const val HELPER = "monmmo/HudSeason"

  /** True for the header widget: the one class styling itself "trainer-hud" with a time label. */
  fun isHeader(classBytes: ByteArray): Boolean {
    var header = false
    var timeLabel = false
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
                      if (value == HEADER_STYLE) header = true
                      if (value == TIME_LABEL_STYLE) timeLabel = true
                    }
                  }
            },
            ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    return header && timeLabel
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    var hooked = 0
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
                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    n: String,
                    d: String,
                    isInterface: Boolean,
                ) {
                  super.visitMethodInsn(opcode, owner, n, d, isInterface)
                  if (owner == FORMATTER_OWNER && n == FORMATTER_NAME && d == FORMATTER_DESCRIPTOR) {
                    hooked++
                    // The formatted line is on the stack; the helper takes and returns a String,
                    // so the stack shape is unchanged and no frame needs recomputing.
                    super.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        HELPER,
                        "withSeason",
                        "(Ljava/lang/String;)Ljava/lang/String;",
                        false)
                  }
                }
              }
        },
        0)
    check(hooked == 1) { "Expected exactly one day-and-time format call in the header, found $hooked" }
    return writer.toByteArray()
  }
}
