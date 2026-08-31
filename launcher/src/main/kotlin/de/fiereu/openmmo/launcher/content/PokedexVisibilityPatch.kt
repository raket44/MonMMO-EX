package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Stops the data.pak loader from hiding every species it creates from the Pokedex.
 *
 * A species carries a boolean `JI`; the Pokedex render loop skips any entry whose `JI` is set. The
 * loader for the species section builds each record and then sets it:
 * ```
 * new zK0(id);
 * ... .JI = true;        // iconst_1; putfield zK0.JI
 * ```
 *
 * so everything it creates starts hidden. Canonical species come from the ROM loader, which leaves
 * them visible, which is why they appear and ours do not. And no section of data.pak can clear it -
 * the optional-field section only ever sets `JI = true`, never false, so removing the flag leaves
 * the loader's default in place. The empty tabs were this, measured: 72 species handed to the
 * screen, 0 buttons drawn.
 *
 * This flips that one default to `false`. The section that legitimately hides a species - the same
 * `putfield` guarded by a flag test elsewhere in the loader - is untouched, so the client's own
 * reserved block can still be hidden explicitly. A species with no regional number is excluded from
 * every list on its own, so defaulting it visible costs nothing.
 */
object PokedexVisibilityPatch {
  private const val HIDDEN_FIELD = "JI"
  private const val HIDDEN_OWNER_HINT = "zK0"

  /** True for the data.pak loader: it sets the hidden flag, more than once. */
  fun isLoader(classBytes: ByteArray): Boolean = defaultHides(classBytes) >= 1

  /** How many `iconst_1; putfield <species>.JI` pairs the class contains. */
  fun defaultHides(classBytes: ByteArray): Int {
    var count = 0
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
                    private var sawOne = false

                    override fun visitInsn(opcode: Int) {
                      sawOne = opcode == Opcodes.ICONST_1
                    }

                    override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                      if (o == Opcodes.PUTFIELD &&
                          n == HIDDEN_FIELD &&
                          d == "Z" &&
                          ow.endsWith(HIDDEN_OWNER_HINT) &&
                          sawOne) {
                        count++
                      }
                      sawOne = false
                    }

                    override fun visitVarInsn(opcode: Int, varIndex: Int) {
                      sawOne = false
                    }

                    override fun visitMethodInsn(
                        o: Int,
                        ow: String,
                        n: String,
                        d: String,
                        itf: Boolean,
                    ) {
                      sawOne = false
                    }
                  }
            },
            0,
        )
    return count
  }

  /**
   * Flips the first default to `false`, leaving every later one alone.
   *
   * The first `iconst_1; putfield JI` in the loader is the species section's unconditional default,
   * emitted right after the record is constructed. The later ones are the flag-guarded hides, which
   * have to keep working.
   */
  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    var flipped = false
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
            return object : MethodVisitor(Opcodes.ASM9, next) {
              private var pendingOne = false

              private fun flush() {
                if (pendingOne) {
                  super.visitInsn(Opcodes.ICONST_1)
                  pendingOne = false
                }
              }

              override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.ICONST_1 && !flipped) {
                  flush()
                  pendingOne = true
                  return
                }
                flush()
                super.visitInsn(opcode)
              }

              override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                if (pendingOne &&
                    !flipped &&
                    o == Opcodes.PUTFIELD &&
                    n == HIDDEN_FIELD &&
                    d == "Z" &&
                    ow.endsWith(HIDDEN_OWNER_HINT)) {
                  // The default hide: emit false instead of the buffered true.
                  super.visitInsn(Opcodes.ICONST_0)
                  pendingOne = false
                  flipped = true
                } else {
                  flush()
                }
                super.visitFieldInsn(o, ow, n, d)
              }

              override fun visitVarInsn(opcode: Int, varIndex: Int) {
                flush()
                super.visitVarInsn(opcode, varIndex)
              }

              override fun visitMethodInsn(
                  o: Int,
                  ow: String,
                  n: String,
                  d: String,
                  itf: Boolean,
              ) {
                flush()
                super.visitMethodInsn(o, ow, n, d, itf)
              }

              override fun visitTypeInsn(opcode: Int, type: String) {
                flush()
                super.visitTypeInsn(opcode, type)
              }

              override fun visitJumpInsn(opcode: Int, label: org.objectweb.asm.Label) {
                flush()
                super.visitJumpInsn(opcode, label)
              }

              override fun visitLabel(label: org.objectweb.asm.Label) {
                flush()
                super.visitLabel(label)
              }

              override fun visitIntInsn(opcode: Int, operand: Int) {
                flush()
                super.visitIntInsn(opcode, operand)
              }

              override fun visitLdcInsn(value: Any?) {
                flush()
                super.visitLdcInsn(value)
              }

              override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                flush()
                super.visitMaxs(maxStack, maxLocals)
              }
            }
          }
        },
        0,
    )
    return writer.toByteArray()
  }
}
