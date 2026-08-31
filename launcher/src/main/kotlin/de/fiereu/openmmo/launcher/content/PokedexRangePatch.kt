package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Refreshes the Pokedex's species map before each list is built.
 *
 * The registry keeps two maps: `aX1`, everything the client knows, and `Sp0`, what the Pokedex
 * reads. `Sp0` is filled once by a loop over ids 1 to 649 that stops at the first id it cannot
 * find, and it is never rebuilt.
 *
 * Measured in the running client, the two disagree badly:
 * ```
 * map=667  all=1378
 * ```
 *
 * The registry holds every imported species. The Pokedex copy holds only what the client knew when
 * it was taken, having stopped at 668 - the first id that is ours. Raising that loop's limit does
 * nothing by itself, because when it runs our content is not registered yet.
 *
 * So the copy is redone on demand. Two calls, no branches, no assumption about load order: the
 * Pokedex sees the registry as it stands when the player opens it.
 *
 * Pointing the reader at `aX1` directly was tried first and left the Pokedex empty - the method
 * that fills `Sp0` also clears and rebuilds `aX1`, so reading it races that rebuild. Copying keeps
 * the client's own structure and fixes only the staleness.
 */
object PokedexRangePatch {
  /** What the Pokedex reads. */
  private const val DEX_MAP = "Sp0"

  /** What it should have been copied from, in full. */
  private const val FULL_MAP = "aX1"

  /** Builds one region's list; the natural place to notice the map is stale. */
  private const val LIST_BUILDER_DESCRIPTOR = "(B)Ljava/util/ArrayList;"

  /** True for the species registry: both maps, and the list builder. */
  fun isSpeciesRegistry(classBytes: ByteArray): Boolean {
    var maps = 0
    var builder = false
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
                if (descriptor == "Ljava/util/HashMap;" && (name == DEX_MAP || name == FULL_MAP))
                    maps++
                return null
              }

              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): MethodVisitor? {
                if (descriptor == LIST_BUILDER_DESCRIPTOR) builder = true
                return null
              }
            },
            ClassReader.SKIP_CODE,
        )
    return maps == 2 && builder
  }

  /** Adds the refresh to the list builder. */
  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val owner = reader.className
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
            if (descriptor != LIST_BUILDER_DESCRIPTOR) return next
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitCode() {
                super.visitCode()
                // Sp0.clear()
                super.visitVarInsn(Opcodes.ALOAD, 0)
                super.visitFieldInsn(Opcodes.GETFIELD, owner, DEX_MAP, "Ljava/util/HashMap;")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "clear", "()V", false)
                // Sp0.putAll(aX1)
                super.visitVarInsn(Opcodes.ALOAD, 0)
                super.visitFieldInsn(Opcodes.GETFIELD, owner, DEX_MAP, "Ljava/util/HashMap;")
                super.visitVarInsn(Opcodes.ALOAD, 0)
                super.visitFieldInsn(Opcodes.GETFIELD, owner, FULL_MAP, "Ljava/util/HashMap;")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/util/HashMap",
                    "putAll",
                    "(Ljava/util/Map;)V",
                    false,
                )
              }
            }
          }
        },
        0,
    )
    return writer.toByteArray()
  }
}
