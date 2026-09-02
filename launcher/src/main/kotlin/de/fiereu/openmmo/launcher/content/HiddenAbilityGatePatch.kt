package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Shows a species' hidden ability in the Pokedex whether or not the client's release table lists
 * it.
 *
 * The detail screen draws the hidden-ability line only after `Oo.Rz0(speciesId, Ob1.Ls)` or the
 * `Ob1.r5` variant answers true - a `(S[S)Z` membership test against a short[] whitelist BAKED into
 * the client. No packet adds to it, so an imported species can never pass, and its hidden ability
 * silently vanishes from the dex even though the record carries it (the ability line gap the
 * operator reported). The species id checked is the BASE form's, read through the kv1 pointer.
 *
 * The gate site is the two-instruction shape `getstatic <short[] field>` followed immediately by
 * `invokestatic (S[S)Z`. Every such site in the detail screen is replaced with pop-pop-true, so the
 * line renders whenever the record's hidden slot is filled. The class is identified by that shape
 * occurring several times alongside the egg-group string base 181000, which only the detail screen
 * reads.
 */
object HiddenAbilityGatePatch {
  private const val MEMBERSHIP_DESCRIPTOR = "(S[S)Z"
  private const val EGG_GROUP_STRING_BASE = 181000

  fun isDexDetailScreen(classBytes: ByteArray): Boolean {
    var gates = 0
    var readsEggGroupBase = false
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
                    private var afterShortArrayRead = false

                    override fun visitFieldInsn(o: Int, owner: String, n: String, d: String) {
                      afterShortArrayRead = o == Opcodes.GETSTATIC && d == "[S"
                    }

                    override fun visitLdcInsn(value: Any?) {
                      if (value == EGG_GROUP_STRING_BASE) readsEggGroupBase = true
                      afterShortArrayRead = false
                    }

                    override fun visitMethodInsn(
                        o: Int,
                        owner: String,
                        n: String,
                        d: String,
                        itf: Boolean,
                    ) {
                      if (afterShortArrayRead &&
                          o == Opcodes.INVOKESTATIC &&
                          d == MEMBERSHIP_DESCRIPTOR) {
                        gates++
                      }
                      afterShortArrayRead = false
                    }

                    override fun visitInsn(opcode: Int) {
                      afterShortArrayRead = false
                    }

                    override fun visitVarInsn(opcode: Int, variable: Int) {
                      afterShortArrayRead = false
                    }
                  }
            },
            0,
        )
    return readsEggGroupBase && gates >= 4
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
            val down = super.visitMethod(access, name, descriptor, signature, exceptions)
            return object : MethodVisitor(Opcodes.ASM9, down) {
              private var afterShortArrayRead = false

              override fun visitFieldInsn(o: Int, owner: String, n: String, d: String) {
                afterShortArrayRead = o == Opcodes.GETSTATIC && d == "[S"
                super.visitFieldInsn(o, owner, n, d)
              }

              override fun visitMethodInsn(
                  o: Int,
                  owner: String,
                  n: String,
                  d: String,
                  itf: Boolean,
              ) {
                if (afterShortArrayRead &&
                    o == Opcodes.INVOKESTATIC &&
                    d == MEMBERSHIP_DESCRIPTOR) {
                  // Stack holds (short, short[]): drop both, answer "released".
                  super.visitInsn(Opcodes.POP)
                  super.visitInsn(Opcodes.POP)
                  super.visitInsn(Opcodes.ICONST_1)
                } else {
                  super.visitMethodInsn(o, owner, n, d, itf)
                }
                afterShortArrayRead = false
              }

              override fun visitInsn(opcode: Int) {
                afterShortArrayRead = false
                super.visitInsn(opcode)
              }

              override fun visitVarInsn(opcode: Int, variable: Int) {
                afterShortArrayRead = false
                super.visitVarInsn(opcode, variable)
              }

              override fun visitLdcInsn(value: Any?) {
                afterShortArrayRead = false
                super.visitLdcInsn(value)
              }
            }
          }
        },
        0,
    )
    return writer.toByteArray()
  }
}
