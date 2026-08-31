package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Stops the Pokedex from dropping species the client considers "unavailable".
 *
 * Late in the entry-building loop, after the search filter, sits a hard gate:
 * ```
 * invokevirtual Cl(0, id)   // an availability table baked into the client
 * ifne  build-the-button
 * getstatic Ob1.w51; pop    // dead read
 * goto  next-species        // dropped entirely - not even a silhouette
 * ```
 *
 * Measured at runtime: a Kalos list of 72 species built 0 buttons, and Kanto's 244 built 223 - the
 * exact size of that table's Kanto slice. It is not player progress (a fresh account shows the same
 * 223), so no packet can add to it, and imported species can never pass it.
 *
 * The Expansion is the source of truth for what exists, so the gate is removed: every species the
 * region list produced is rendered, and seen/caught presentation stays with the ownership sets. The
 * site is identified by its exact shape - a `(BS)Z` call, `ifne`, then a static boolean read that
 * is popped - which occurs once.
 */
object PokedexAvailabilityPatch {
  private const val AVAILABILITY_CHECK_DESCRIPTOR = "(BS)Z"

  fun isPokedexScreen(classBytes: ByteArray): Boolean = dropSites(classBytes) == 1

  /** Occurrences of the drop-site shape, to prove the match is unique before rewriting it. */
  fun dropSites(classBytes: ByteArray): Int {
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
                    private var stage = 0

                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String,
                        name2: String,
                        descriptor2: String,
                        isInterface: Boolean,
                    ) {
                      stage = if (descriptor2 == AVAILABILITY_CHECK_DESCRIPTOR) 1 else 0
                    }

                    override fun visitJumpInsn(opcode: Int, label: Label) {
                      stage = if (stage == 1 && opcode == Opcodes.IFNE) 2 else 0
                    }

                    override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                      if (stage == 2 && o == Opcodes.GETSTATIC && d == "Z") count++
                      stage = 0
                    }

                    override fun visitInsn(opcode: Int) {
                      stage = 0
                    }

                    override fun visitVarInsn(opcode: Int, varIndex: Int) {
                      stage = 0
                    }
                  }
            },
            0,
        )
    return count
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val patched = rewrite(classBytes)
    // A matcher that misses produces identical bytes and no error, which already shipped once.
    check(dropSites(patched) == 0) {
      "The availability drop site was not rewritten; the shape matcher missed it"
    }
    check(!patched.contentEquals(classBytes)) { "The patch returned the class unchanged" }
    return patched
  }

  private fun rewrite(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer =
        object : ClassWriter(reader, COMPUTE_FRAMES) {
          // The client classes are not on our classpath, so the reflective lookup cannot work.
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
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
              private var afterCheck = false
              private var heldJump: Label? = null

              private fun releaseJump() {
                heldJump?.let { super.visitJumpInsn(Opcodes.IFNE, it) }
                heldJump = null
              }

              override fun visitMethodInsn(
                  opcode: Int,
                  owner: String,
                  name2: String,
                  descriptor2: String,
                  isInterface: Boolean,
              ) {
                releaseJump()
                afterCheck = descriptor2 == AVAILABILITY_CHECK_DESCRIPTOR
                super.visitMethodInsn(opcode, owner, name2, descriptor2, isInterface)
              }

              override fun visitJumpInsn(opcode: Int, label: Label) {
                if (afterCheck && opcode == Opcodes.IFNE && heldJump == null) {
                  // Held back until the next instruction confirms this is the drop site.
                  heldJump = label
                  afterCheck = false
                  return
                }
                releaseJump()
                afterCheck = false
                super.visitJumpInsn(opcode, label)
              }

              override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                val held = heldJump
                if (held != null && o == Opcodes.GETSTATIC && d == "Z") {
                  // The drop site: take the branch unconditionally instead of testing it. The
                  // stale getstatic/pop/goto behind it become dead code, which frame
                  // recomputation clears.
                  heldJump = null
                  super.visitInsn(Opcodes.POP)
                  super.visitJumpInsn(Opcodes.GOTO, held)
                  super.visitFieldInsn(o, ow, n, d)
                  return
                }
                releaseJump()
                afterCheck = false
                super.visitFieldInsn(o, ow, n, d)
              }

              override fun visitInsn(opcode: Int) {
                releaseJump()
                afterCheck = false
                super.visitInsn(opcode)
              }

              override fun visitVarInsn(opcode: Int, varIndex: Int) {
                releaseJump()
                afterCheck = false
                super.visitVarInsn(opcode, varIndex)
              }

              override fun visitLabel(label: Label) {
                // A held jump survives labels: the client keeps line-number tables, so a label sits
                // between the ifne and the getstatic at the very site being matched. Releasing on
                // it made the whole patch a silent no-op - the bytes came back identical and the
                // build still said "patched".
                super.visitLabel(label)
              }

              override fun visitIntInsn(opcode: Int, operand: Int) {
                releaseJump()
                afterCheck = false
                super.visitIntInsn(opcode, operand)
              }

              override fun visitLdcInsn(value: Any?) {
                releaseJump()
                afterCheck = false
                super.visitLdcInsn(value)
              }

              override fun visitTypeInsn(opcode: Int, type: String) {
                releaseJump()
                afterCheck = false
                super.visitTypeInsn(opcode, type)
              }

              override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                releaseJump()
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

/**
 * Unhides imported species from the Pokedex.
 *
 * Every species built from data.pak section 10 is constructed with the hide flag already true, and
 * section 6 turned out to be **set-only**: `if (flags & bit) field = true` - it can raise `JI` and
 * `C30` but never clear them. So clearing the bits in our records changed nothing, and every
 * imported species has carried both hide flags since the moment it was created.
 *
 * The dex screen drops an unseen species when either flag is set - which is all of ours, always.
 * Rather than re-teach the loader to clear flags, the four flag-checks in the screen are
 * neutralised: the field is still read, then popped, and the skip never taken. The client's own
 * hidden event species become listable too, which suits a dex meant to show everything.
 */
object PokedexHideFlagPatch {
  private val HIDE_FLAGS = setOf("JI", "C30")

  fun isPokedexScreen(classBytes: ByteArray): Boolean = skipSites(classBytes) == 4

  fun skipSites(classBytes: ByteArray): Int {
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
                    private var armed = false

                    override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                      armed = o == Opcodes.GETFIELD && d == "Z" && n in HIDE_FLAGS
                    }

                    override fun visitJumpInsn(opcode: Int, label: Label) {
                      if (armed && (opcode == Opcodes.IFNE || opcode == Opcodes.IFEQ)) count++
                      armed = false
                    }

                    override fun visitInsn(opcode: Int) {
                      armed = false
                    }

                    override fun visitVarInsn(opcode: Int, varIndex: Int) {
                      armed = false
                    }
                    // Labels and line numbers pass through without disarming.
                  }
            },
            0,
        )
    return count
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val before = skipSites(classBytes)
    check(before > 0) { "No hide-flag skips found to remove" }
    val reader = ClassReader(classBytes)
    val writer =
        object : ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
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
              private var armed = false

              override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                armed = o == Opcodes.GETFIELD && d == "Z" && n in HIDE_FLAGS
                super.visitFieldInsn(o, ow, n, d)
              }

              override fun visitJumpInsn(opcode: Int, label: Label) {
                if (armed && opcode == Opcodes.IFNE) {
                  // Skip-when-set: drop the test, never skip.
                  armed = false
                  super.visitInsn(Opcodes.POP)
                  return
                }
                if (armed && opcode == Opcodes.IFEQ) {
                  // Render-when-clear, with the skip on the fall-through: always take the render
                  // branch instead.
                  armed = false
                  super.visitInsn(Opcodes.POP)
                  super.visitJumpInsn(Opcodes.GOTO, label)
                  return
                }
                armed = false
                super.visitJumpInsn(opcode, label)
              }

              override fun visitInsn(opcode: Int) {
                armed = false
                super.visitInsn(opcode)
              }

              override fun visitVarInsn(opcode: Int, varIndex: Int) {
                armed = false
                super.visitVarInsn(opcode, varIndex)
              }
            }
          }
        },
        0,
    )
    val patched = writer.toByteArray()
    check(skipSites(patched) == 0) { "Hide-flag skips survived the rewrite" }
    check(!patched.contentEquals(classBytes)) { "The patch returned the class unchanged" }
    return patched
  }
}

/**
 * Hooks the end of the data.pak load to run [monmmo.DexPatch].
 *
 * The load method is found by its unique string literal rather than its name, and the call is
 * inserted before every return, so the fixups run exactly once per load - after the ROM loader and
 * every data.pak section, which is the only moment the live registry is complete.
 */
object DexFixupHookPatch {
  private const val LOAD_MARKER = "data/data.pak"
  private const val HELPER = "monmmo/DexPatch"

  fun isLoader(classBytes: ByteArray): Boolean = loaderMethod(classBytes) != null

  fun loaderMethod(classBytes: ByteArray): String? {
    var found: String? = null
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
                      if (value == LOAD_MARKER && found == null) found = name
                    }
                  }
            },
            0,
        )
    return found
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val target = loaderMethod(classBytes) ?: error("No data.pak loader in this class")
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
    var calls = 0
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
            if (name != target) return next
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.IRETURN || opcode == Opcodes.RETURN) {
                  super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "apply", "()V", false)
                  calls++
                }
                super.visitInsn(opcode)
              }
            }
          }
        },
        0,
    )
    check(calls > 0) { "The loader had no return to hook" }
    return writer.toByteArray()
  }
}
