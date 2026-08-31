package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Gives the Pokedex the regions the imported species belong to.
 *
 * Four things in the client stop at the five regions it shipped with, and all four have to move
 * together or a new tab either never appears or appears empty:
 * 1. Each species stores its per-region number in a `short[6]`. Six slots, so a sixth region is the
 *    last one that fits and a seventh has nowhere to go.
 * 2. `CH0` is a `byte[5]` holding the regions in display order.
 * 3. The Pokedex screen walks that table with a loop bounded by a literal 5.
 * 4. A `boolean(byte)` gate answers false for anything outside a `tableswitch 0..4`.
 *
 * Nothing else is needed: membership is the per-region number that data.pak section 11 writes, and
 * a tab's name is string id `250000 + region`, so both are data. See [PokedexRangePatch] for the
 * other half of the ceiling - the map the list is built from.
 */
object PokedexRegionPatch {
  /** Regions the client ships with, numbered 0-4. */
  const val STOCK_REGIONS = 5

  /** Per-species region slots the client allocates. */
  const val STOCK_REGION_SLOTS = 6

  private const val REGION_TABLE = "CH0"
  private const val REGION_GATE_DESCRIPTOR = "(B)Z"
  private const val REGION_NUMBER_DESCRIPTOR = "(B)S"

  // ---------------------------------------------------------------- per-species region slots

  /** The species record: it keeps a `short[]` of region numbers and reads it by region. */
  fun isSpeciesRecord(classBytes: ByteArray): Boolean =
      hasMethod(classBytes, REGION_NUMBER_DESCRIPTOR) &&
          regionSlots(classBytes) == STOCK_REGION_SLOTS

  /** The width of the per-species region array, as allocated in the constructor. */
  fun regionSlots(classBytes: ByteArray): Int {
    var slots = -1
    forEachMethod(classBytes) { name, _ ->
      if (name != "<init>") null
      else
          object : MethodVisitor(Opcodes.ASM9) {
            private var pending = -1

            override fun visitIntInsn(opcode: Int, operand: Int) {
              if (opcode == Opcodes.BIPUSH) pending = operand
              else if (opcode == Opcodes.NEWARRAY && operand == Opcodes.T_SHORT && pending > 0) {
                if (slots < 0) slots = pending
                pending = -1
              } else pending = -1
            }

            override fun visitInsn(opcode: Int) {
              pending = -1
            }
          }
    }
    return slots
  }

  /** Widens that array so the new regions have somewhere to be stored. */
  fun patchRegionSlots(classBytes: ByteArray, slots: Int): ByteArray {
    require(slots > STOCK_REGION_SLOTS) { "$slots slots would not widen anything" }
    return transform(classBytes) { name, next ->
      if (name != "<init>") next
      else
          object : MethodVisitor(Opcodes.ASM9, next) {
            private var pending = -1
            private var done = false

            override fun visitIntInsn(opcode: Int, operand: Int) {
              if (opcode == Opcodes.BIPUSH && operand == STOCK_REGION_SLOTS && !done) {
                pending = operand
                return
              }
              if (opcode == Opcodes.NEWARRAY && pending > 0) {
                // Only the short array holds region numbers; the byte array beside it is unrelated.
                if (operand == Opcodes.T_SHORT && !done) {
                  super.visitIntInsn(Opcodes.BIPUSH, slots)
                  done = true
                } else {
                  super.visitIntInsn(Opcodes.BIPUSH, pending)
                }
                pending = -1
                super.visitIntInsn(opcode, operand)
                return
              }
              flush()
              super.visitIntInsn(opcode, operand)
            }

            override fun visitInsn(opcode: Int) {
              flush()
              super.visitInsn(opcode)
            }

            override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
              flush()
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

            private fun flush() {
              if (pending > 0) {
                super.visitIntInsn(Opcodes.BIPUSH, pending)
                pending = -1
              }
            }
          }
    }
  }

  // ---------------------------------------------------------------------- region display order

  fun isRegionTable(classBytes: ByteArray): Boolean =
      byteArrayFields(classBytes).contains(REGION_TABLE)

  /** Appends the new regions to the display order table and widens it to match. */
  fun patchRegionTable(classBytes: ByteArray, regions: List<Int>): ByteArray {
    val total = STOCK_REGIONS + regions.size
    return transform(classBytes, ClassWriter.COMPUTE_MAXS) { name, next ->
      if (name != "<clinit>") next
      else
          object : MethodVisitor(Opcodes.ASM9, next) {
            private var sized = false

            override fun visitInsn(opcode: Int) {
              // The table's own length is the first `iconst_5` in the initialiser.
              if (!sized && opcode == Opcodes.ICONST_0 + STOCK_REGIONS) {
                sized = true
                super.visitIntInsn(Opcodes.BIPUSH, total)
                return
              }
              super.visitInsn(opcode)
            }

            override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
              // The array reference is only on the stack up to this point.
              if (o == Opcodes.PUTSTATIC && n == REGION_TABLE) {
                regions.forEachIndexed { offset, region ->
                  super.visitInsn(Opcodes.DUP)
                  super.visitIntInsn(Opcodes.BIPUSH, STOCK_REGIONS + offset)
                  super.visitIntInsn(Opcodes.BIPUSH, region)
                  super.visitInsn(Opcodes.BASTORE)
                }
              }
              super.visitFieldInsn(o, ow, n, d)
            }
          }
    }
  }

  // -------------------------------------------------------------------------- the tab loop

  fun isPokedexScreen(classBytes: ByteArray): Boolean = tabLoopBounds(classBytes) > 0

  /** Loop bounds that immediately follow a read of the region table. */
  fun tabLoopBounds(classBytes: ByteArray): Int {
    var count = 0
    forEachMethod(classBytes) { _, _ ->
      object : MethodVisitor(Opcodes.ASM9) {
        private var afterTable = false

        override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
          afterTable = n == REGION_TABLE && d == "[B"
        }

        override fun visitInsn(opcode: Int) {
          if (afterTable && opcode == Opcodes.ICONST_0 + STOCK_REGIONS) {
            count++
            afterTable = false
          }
        }
      }
    }
    return count
  }

  fun patchTabLoop(classBytes: ByteArray, total: Int): ByteArray =
      transform(classBytes, ClassWriter.COMPUTE_MAXS) { _, next ->
        object : MethodVisitor(Opcodes.ASM9, next) {
          private var afterTable = false

          override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
            afterTable = n == REGION_TABLE && d == "[B"
            super.visitFieldInsn(o, ow, n, d)
          }

          override fun visitInsn(opcode: Int) {
            if (afterTable && opcode == Opcodes.ICONST_0 + STOCK_REGIONS) {
              afterTable = false
              super.visitIntInsn(Opcodes.BIPUSH, total)
              return
            }
            super.visitInsn(opcode)
          }
        }
      }

  // ------------------------------------------------------------------------- the region gate

  fun isRegionGate(classBytes: ByteArray): Boolean = regionGateMethod(classBytes) != null

  fun regionGateMethod(classBytes: ByteArray): String? {
    var found: String? = null
    forEachMethod(classBytes) { name, descriptor ->
      if (descriptor != REGION_GATE_DESCRIPTOR) null
      else
          object : MethodVisitor(Opcodes.ASM9) {
            override fun visitTableSwitchInsn(
                min: Int,
                max: Int,
                dflt: Label,
                vararg labels: Label,
            ) {
              if (min == 0 && max == STOCK_REGIONS - 1 && found == null) found = name
            }
          }
    }
    return found
  }

  /**
   * Accepts the new regions before the stock switch runs.
   *
   * A prologue rather than a reshaped switch: the existing cases keep whatever conditions they
   * carry, and the new regions are simply always available.
   *
   * This is the one patch here that introduces a branch, so it needs frames recomputed rather than
   * just stack sizes - a new jump target with no stackmap entry is rejected by the verifier before
   * the client draws anything.
   */
  fun patchRegionGate(classBytes: ByteArray, regions: List<Int>): ByteArray {
    val target = regionGateMethod(classBytes) ?: return classBytes
    val lowest = regions.min()
    val highest = regions.max()
    val reader = ClassReader(classBytes)
    val writer =
        object : ClassWriter(reader, COMPUTE_FRAMES) {
          // The client's classes are not on our classpath, so the usual reflective lookup fails.
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
            if (name != target) return next
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitCode() {
                super.visitCode()
                val skip = Label()
                super.visitVarInsn(Opcodes.ILOAD, 1)
                super.visitIntInsn(Opcodes.BIPUSH, lowest)
                super.visitJumpInsn(Opcodes.IF_ICMPLT, skip)
                super.visitVarInsn(Opcodes.ILOAD, 1)
                super.visitIntInsn(Opcodes.BIPUSH, highest)
                super.visitJumpInsn(Opcodes.IF_ICMPGT, skip)
                super.visitInsn(Opcodes.ICONST_1)
                super.visitInsn(Opcodes.IRETURN)
                super.visitLabel(skip)
              }
            }
          }
        },
        0,
    )
    return writer.toByteArray()
  }

  // ------------------------------------------------------------------------------- plumbing

  private fun hasMethod(classBytes: ByteArray, descriptor: String): Boolean {
    var found = false
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  methodDescriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): MethodVisitor? {
                if (methodDescriptor == descriptor) found = true
                return null
              }
            },
            ClassReader.SKIP_CODE,
        )
    return found
  }

  private fun byteArrayFields(classBytes: ByteArray): Set<String> {
    val names = mutableSetOf<String>()
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitField(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  value: Any?,
              ): org.objectweb.asm.FieldVisitor? {
                if (descriptor == "[B") names.add(name)
                return null
              }
            },
            ClassReader.SKIP_CODE,
        )
    return names
  }

  private fun forEachMethod(
      classBytes: ByteArray,
      visitor: (name: String, descriptor: String) -> MethodVisitor?,
  ) {
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ) = visitor(name, descriptor)
            },
            0,
        )
  }

  private fun transform(
      classBytes: ByteArray,
      flags: Int = 0,
      visitor: (name: String, next: MethodVisitor) -> MethodVisitor,
  ): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, flags)
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ) = visitor(name, super.visitMethod(access, name, descriptor, signature, exceptions))
        },
        0,
    )
    return writer.toByteArray()
  }
}

/**
 * Keeps the national dex number reachable after the region array is widened.
 *
 * The per-species array is not five regional slots plus a spare - it is five regional slots plus
 * the **national number at index 5**, and `s7` returns `Vy1[length - 1]` for a negative region,
 * which is how the National tab reads it. Widening the array therefore moves what "last" means and
 * the National tab silently reads an empty slot; writing an extra region into index 5 overwrites
 * the national number and makes that region match every species.
 *
 * Measured, before this: `region=5 size=1377` out of 1378, and the National tab blank.
 *
 * This pins the fallback to the national slot instead of the end of the array, so the array can
 * grow without moving it.
 */
object NationalSlotPatch {
  /** Where the national dex number lives, in the client's own six-slot layout. */
  const val NATIONAL_SLOT = 5

  private const val REGION_NUMBER_DESCRIPTOR = "(B)S"

  fun isSpeciesRecord(classBytes: ByteArray): Boolean = fallbackReads(classBytes) == 1

  /** Reads of `Vy1[length - 1]`, the pattern that stands in for the national number. */
  fun fallbackReads(classBytes: ByteArray): Int {
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
                  if (descriptor != REGION_NUMBER_DESCRIPTOR) null
                  else
                      object : MethodVisitor(Opcodes.ASM9) {
                        private var sawLength = false
                        private var sawOne = false

                        override fun visitInsn(opcode: Int) {
                          when {
                            opcode == Opcodes.ARRAYLENGTH -> {
                              sawLength = true
                              sawOne = false
                            }
                            opcode == Opcodes.ICONST_1 && sawLength -> sawOne = true
                            opcode == Opcodes.ISUB && sawOne -> {
                              count++
                              sawLength = false
                              sawOne = false
                            }
                            else -> {
                              sawLength = false
                              sawOne = false
                            }
                          }
                        }
                      }
            },
            0,
        )
    return count
  }

  /** Replaces `length - 1` with the national slot's fixed index. */
  fun patch(classBytes: ByteArray, slot: Int): ByteArray {
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
            val next = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (descriptor != REGION_NUMBER_DESCRIPTOR) return next
            return object : MethodVisitor(Opcodes.ASM9, next) {
              private val buffered = mutableListOf<Int>()

              private fun flush() {
                buffered.forEach { super.visitInsn(it) }
                buffered.clear()
              }

              override fun visitInsn(opcode: Int) {
                when {
                  opcode == Opcodes.ARRAYLENGTH && buffered.isEmpty() -> buffered.add(opcode)
                  opcode == Opcodes.ICONST_1 && buffered.size == 1 -> buffered.add(opcode)
                  opcode == Opcodes.ISUB && buffered.size == 2 -> {
                    // Drop `arraylength; iconst_1; isub` and push the slot directly. The array
                    // reference beneath it is still what gets indexed.
                    buffered.clear()
                    super.visitInsn(Opcodes.POP)
                    super.visitIntInsn(Opcodes.BIPUSH, slot)
                  }
                  else -> {
                    flush()
                    super.visitInsn(opcode)
                  }
                }
              }

              override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                flush()
                super.visitFieldInsn(o, ow, n, d)
              }

              override fun visitVarInsn(opcode: Int, varIndex: Int) {
                flush()
                super.visitVarInsn(opcode, varIndex)
              }

              override fun visitJumpInsn(opcode: Int, label: Label) {
                flush()
                super.visitJumpInsn(opcode, label)
              }

              override fun visitLabel(label: Label) {
                flush()
                super.visitLabel(label)
              }

              override fun visitIntInsn(opcode: Int, operand: Int) {
                flush()
                super.visitIntInsn(opcode, operand)
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
