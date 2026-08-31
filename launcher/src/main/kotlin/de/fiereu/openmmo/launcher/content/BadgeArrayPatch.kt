package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Widens the client's type-badge lookup tables.
 *
 * The client builds two arrays of badge regions - `monster_type_<n>_<lang>` for a species and
 * `skill_type_<n>_<lang>` for a move - and both are allocated at a hardcoded 18, the number of
 * types the game shipped with. A type past that is never looked up at all: the accessor
 * bounds-checks the index and quietly substitutes a default, so adding Fairy art to the atlas
 * changes nothing on its own. This raises both allocations so index 19 is real.
 *
 * The class is found by its two string literals rather than its name, which is obfuscated and moves
 * between client builds.
 */
object BadgeArrayPatch {
  /** The number of types the stock client allocates room for. */
  const val STOCK_TYPE_COUNT = 18

  private const val SPECIES_BADGE = "monster_type_"
  private const val MOVE_BADGE = "skill_type_"

  /** True when this class builds the badge tables. */
  fun isBadgeLoader(classBytes: ByteArray): Boolean {
    val literals = mutableSetOf<String>()
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
                      if (value is String) literals.add(value)
                    }
                  }
            },
            ClassReader.SKIP_FRAMES,
        )
    return SPECIES_BADGE in literals && MOVE_BADGE in literals
  }

  /** The sizes of every badge table in the class, in the order they are allocated. */
  fun arraySizes(classBytes: ByteArray): List<Int> {
    val sizes = mutableListOf<Int>()
    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ) = Detector(sizes)
            },
            ClassReader.SKIP_FRAMES,
        )
    return sizes
  }

  fun patch(classBytes: ByteArray, typeCount: Int): ByteArray {
    require(typeCount > STOCK_TYPE_COUNT) { "A type count of $typeCount would not widen anything" }
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ) = Widen(super.visitMethod(access, name, descriptor, signature, exceptions), typeCount)
        },
        0,
    )
    return writer.toByteArray()
  }

  /**
   * Recognises `bipush <n> ... dup ... istore <v> ... anewarray`, the allocation of a badge table.
   *
   * The whole sequence has to match: a bare `bipush 18` means nothing on its own, and rewriting
   * every one of them would corrupt unrelated code in the same class.
   *
   * Everything after the push is buffered rather than swallowed, because one of the two allocations
   * carries a **label** between the store and the array creation. Dropping it would delete a jump
   * target; skipping the match because of it loses that allocation entirely, which is what a first
   * attempt at this did - it widened one table and left the other at 18.
   */
  private open class Sequence(next: MethodVisitor?) : MethodVisitor(Opcodes.ASM9, next) {
    protected var size: Int? = null
    protected var sawDup = false
    protected var storeVar: Int? = null
    private val buffered = mutableListOf<() -> Unit>()

    /** True once the push, the duplicate and the store have all been seen. */
    protected fun matched() = size != null && sawDup && storeVar != null

    protected fun replay() {
      size?.let { super.visitIntInsn(Opcodes.BIPUSH, it) }
      buffered.forEach { it() }
      clear()
    }

    /** Replays the buffer with the array size replaced. */
    protected fun replayResized(newSize: Int) {
      super.visitIntInsn(Opcodes.BIPUSH, newSize)
      buffered.forEach { it() }
      clear()
    }

    protected fun clear() {
      size = null
      sawDup = false
      storeVar = null
      buffered.clear()
    }

    private fun defer(action: () -> Unit) {
      if (size == null) action() else buffered.add(action)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
      // A push always starts a fresh candidate; passing it through when one is already buffered
      // loses the second of two identical allocations.
      if (opcode == Opcodes.BIPUSH) {
        replay()
        size = operand
        return
      }
      replay()
      super.visitIntInsn(opcode, operand)
    }

    override fun visitInsn(opcode: Int) {
      if (opcode == Opcodes.DUP && size != null && !sawDup) {
        sawDup = true
        buffered.add { super.visitInsn(Opcodes.DUP) }
        return
      }
      replay()
      super.visitInsn(opcode)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
      if (opcode == Opcodes.ISTORE && sawDup && storeVar == null) {
        storeVar = varIndex
        buffered.add { super.visitVarInsn(Opcodes.ISTORE, varIndex) }
        return
      }
      replay()
      super.visitVarInsn(opcode, varIndex)
    }

    // Markers carry no stack effect, so they ride along inside a candidate rather than breaking it.
    override fun visitLabel(label: org.objectweb.asm.Label) = defer { super.visitLabel(label) }

    override fun visitLineNumber(line: Int, start: org.objectweb.asm.Label) = defer {
      super.visitLineNumber(line, start)
    }

    override fun visitLdcInsn(value: Any?) {
      replay()
      super.visitLdcInsn(value)
    }

    override fun visitJumpInsn(opcode: Int, label: org.objectweb.asm.Label) {
      replay()
      super.visitJumpInsn(opcode, label)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
      replay()
      super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
      replay()
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitIincInsn(varIndex: Int, increment: Int) {
      replay()
      super.visitIincInsn(varIndex, increment)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
      replay()
      super.visitMaxs(maxStack, maxLocals)
    }
  }

  private class Detector(private val sizes: MutableList<Int>) : Sequence(null) {
    override fun visitTypeInsn(opcode: Int, type: String) {
      if (opcode == Opcodes.ANEWARRAY && matched()) {
        size?.let(sizes::add)
      }
      clear()
    }
  }

  private class Widen(next: MethodVisitor, private val typeCount: Int) : Sequence(next) {
    override fun visitTypeInsn(opcode: Int, type: String) {
      if (opcode == Opcodes.ANEWARRAY && matched() && size == STOCK_TYPE_COUNT) {
        replayResized(typeCount)
      } else {
        replay()
      }
      mv.visitTypeInsn(opcode, type)
    }
  }
}
