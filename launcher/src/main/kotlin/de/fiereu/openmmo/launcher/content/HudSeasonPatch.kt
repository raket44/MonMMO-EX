package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Corrects the menu header's day-and-time line.
 *
 * The header widget (client f/gz, style "trainer-hud") rebuilds that line once a minute. Two
 * things are wrong with it for this server:
 * - The weekday is the in-game one: `(inGameSeconds % 604800) / 86400` fed to a tableswitch of
 *   DayOfWeek constants. In-game days pass four times faster than real ones, so the name moved
 *   on every six hours. The hook discards that quotient and asks monmmo/HudSeason for the
 *   calendar weekday in the same numbering (0 Sunday .. 6 Saturday).
 * - The season is never named. The line is formatted as string 1155 "{00}, {01}" through
 *   f/nV0.oP(int, String[]); the hook passes the result through HudSeason.withSeason, which
 *   appends ", Winter" and the like from the client's own global season.
 * Both hooks are single inserted instructions at unambiguous sites; the stack shape is unchanged.
 */
object HudSeasonPatch {
  private const val HEADER_STYLE = "trainer-hud"
  private const val TIME_LABEL_STYLE = "label-time"
  private const val FORMATTER_OWNER = "f/nV0"
  private const val FORMATTER_NAME = "oP"
  private const val FORMATTER_DESCRIPTOR = "(I[Ljava/lang/String;)Ljava/lang/String;"
  private const val SECONDS_PER_DAY = 86400
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
    var seasonHooks = 0
    var weekdayHooks = 0
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
                // The weekday quotient is `ldc 86400; idiv` immediately before the tableswitch.
                private var lastLdc: Any? = null
                private var lastWasIdiv = false

                override fun visitLdcInsn(value: Any?) {
                  lastLdc = value
                  lastWasIdiv = false
                  super.visitLdcInsn(value)
                }

                override fun visitInsn(opcode: Int) {
                  lastWasIdiv = opcode == Opcodes.IDIV
                  super.visitInsn(opcode)
                }

                override fun visitTableSwitchInsn(
                    min: Int,
                    max: Int,
                    dflt: Label,
                    vararg labels: Label,
                ) {
                  if (lastWasIdiv && lastLdc == SECONDS_PER_DAY) {
                    weekdayHooks++
                    super.visitInsn(Opcodes.POP)
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "realWeekday", "()I", false)
                  }
                  lastWasIdiv = false
                  super.visitTableSwitchInsn(min, max, dflt, *labels)
                }

                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    n: String,
                    d: String,
                    isInterface: Boolean,
                ) {
                  lastWasIdiv = false
                  super.visitMethodInsn(opcode, owner, n, d, isInterface)
                  if (owner == FORMATTER_OWNER && n == FORMATTER_NAME && d == FORMATTER_DESCRIPTOR) {
                    seasonHooks++
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
    check(seasonHooks == 1) { "Expected one day-and-time format call in the header, found $seasonHooks" }
    check(weekdayHooks == 1) { "Expected one weekday switch in the header, found $weekdayHooks" }
    return writer.toByteArray()
  }
}
