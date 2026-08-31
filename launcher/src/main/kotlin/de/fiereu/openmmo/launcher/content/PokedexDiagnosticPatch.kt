package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Makes the Pokedex say what it is actually doing.
 *
 * Four builds have now been shipped on static reading of the bytecode, and the client's behaviour
 * has disagreed with that reading every time - a tab labelled one region showing another's contents
 * fits none of the models. Reading harder has stopped paying; the missing thing is what the values
 * are at runtime.
 *
 * This prints one line per region list built:
 * ```
 * [dex] region=<the region the screen asked for> size=<species that matched> slots=<array width>
 * ```
 *
 * `region` tells us whether the tab is passing what its label claims. `size` tells us whether the
 * data reached the species. `slots` tells us whether the widened per-species array actually took
 * effect at runtime, which is the assumption underneath all of it.
 *
 * Diagnostic only - it changes no behaviour, and comes out once the question is answered.
 */
object PokedexDiagnosticPatch {
  private const val LIST_BUILDER_DESCRIPTOR = "(B)Ljava/util/ArrayList;"
  private const val REGION_NUMBER_DESCRIPTOR = "(B)S"
  private const val PREFIX = "[dex] region="

  /** Written beside the client, whose working directory the launcher sets. */
  private const val LOG_FILE = "dex-diagnostic.log"

  /** A local index past anything the method uses; frames are recomputed to describe it. */
  private const val MAP_SIZE_LOCAL = 12

  private const val DEX_MAP_FIELD = "Sp0"

  /** The full registry, for comparison: it is what the map is copied from. */
  private const val FULL_MAP_FIELD = "aX1"

  private const val FULL_SIZE_LOCAL = 13

  fun isListBuilder(classBytes: ByteArray): Boolean {
    var found = false
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
                if (descriptor == LIST_BUILDER_DESCRIPTOR) found = true
                return null
              }
            },
            ClassReader.SKIP_CODE,
        )
    return found
  }

  /** Logs the region asked for and how many species answered, on every list build. */
  fun patch(classBytes: ByteArray): ByteArray {
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
            if (descriptor != LIST_BUILDER_DESCRIPTOR) return next
            val className = ClassReader(classBytes).className
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitCode() {
                super.visitCode()
                // Capture the map's size before the method overwrites local 0 with its result.
                super.visitVarInsn(Opcodes.ALOAD, 0)
                super.visitFieldInsn(
                    Opcodes.GETFIELD, className, DEX_MAP_FIELD, "Ljava/util/HashMap;")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "size", "()I", false)
                super.visitVarInsn(Opcodes.ISTORE, MAP_SIZE_LOCAL)
                super.visitVarInsn(Opcodes.ALOAD, 0)
                super.visitFieldInsn(
                    Opcodes.GETFIELD, className, FULL_MAP_FIELD, "Ljava/util/HashMap;")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/util/HashMap", "size", "()I", false)
                super.visitVarInsn(Opcodes.ISTORE, FULL_SIZE_LOCAL)
              }

              override fun visitInsn(opcode: Int) {
                if (opcode != Opcodes.ARETURN) {
                  super.visitInsn(opcode)
                  return
                }
                // Build the message, then append it to a file beside the client. stdout is not an
                // option: the launcher runs javaw, which has no console to write to.
                super.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
                super.visitInsn(Opcodes.DUP)
                super.visitMethodInsn(
                    Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                super.visitLdcInsn(PREFIX)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
                super.visitVarInsn(Opcodes.ILOAD, 1)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(I)Ljava/lang/StringBuilder;",
                    false)
                super.visitLdcInsn(" size=")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
                // stack: list, builder -> need the list's size without losing the list
                super.visitInsn(Opcodes.SWAP)
                super.visitInsn(Opcodes.DUP_X1)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(I)Ljava/lang/StringBuilder;",
                    false)
                super.visitLdcInsn(" map=")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
                super.visitVarInsn(Opcodes.ILOAD, MAP_SIZE_LOCAL)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(I)Ljava/lang/StringBuilder;",
                    false)
                super.visitLdcInsn(" all=")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
                super.visitVarInsn(Opcodes.ILOAD, FULL_SIZE_LOCAL)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(I)Ljava/lang/StringBuilder;",
                    false)
                super.visitLdcInsn(System.lineSeparator())
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "toString",
                    "()Ljava/lang/String;",
                    false)
                // stack: list, message
                super.visitTypeInsn(Opcodes.NEW, "java/io/FileWriter")
                super.visitInsn(Opcodes.DUP)
                super.visitLdcInsn(LOG_FILE)
                super.visitInsn(Opcodes.ICONST_1)
                super.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    "java/io/FileWriter",
                    "<init>",
                    "(Ljava/lang/String;Z)V",
                    false)
                super.visitInsn(Opcodes.DUP_X1)
                super.visitInsn(Opcodes.SWAP)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/io/Writer",
                    "write",
                    "(Ljava/lang/String;)V",
                    false)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/io/Writer", "close", "()V", false)
                super.visitInsn(Opcodes.ARETURN)
              }
            }
          }
        },
        0,
    )
    return writer.toByteArray()
  }

  /** True for the species record, which is where the region-number array lives. */
  fun isSpeciesRecord(classBytes: ByteArray): Boolean {
    var found = false
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
                if (descriptor == REGION_NUMBER_DESCRIPTOR) found = true
                return null
              }
            },
            ClassReader.SKIP_CODE,
        )
    return found
  }
}

/**
 * Second probe, one layer down.
 *
 * The list builder is measured and correct - 72, 88, 96 species handed over per new tab - yet the
 * tabs render empty, and no filter or exception accounts for it. So measure the other end: the
 * screen's build method stores its finished buttons into an `[Lf/oc0;` field, and this logs how
 * many were stored for which region. If the count matches the list, the loss is below rendering; if
 * it is zero, the loss is inside the loop and the next probe bisects it.
 */
object PokedexScreenProbe {
  private const val BUTTON_ARRAY_DESCRIPTOR = "[Lf/oc0;"
  private const val REGION_FIELD = "vL0"
  private const val LOG_FILE = "dex-diagnostic.log"
  private const val COUNT_LOCAL = 30

  fun isScreen(classBytes: ByteArray): Boolean {
    var found = false
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
                    override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                      if (o == Opcodes.PUTFIELD && d == BUTTON_ARRAY_DESCRIPTOR) found = true
                    }
                  }
            },
            0,
        )
    return found
  }

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
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                if (o != Opcodes.PUTFIELD || d != BUTTON_ARRAY_DESCRIPTOR) {
                  super.visitFieldInsn(o, ow, n, d)
                  return
                }
                // Stack: screen, array. Take the length without disturbing either.
                super.visitInsn(Opcodes.DUP)
                super.visitInsn(Opcodes.ARRAYLENGTH)
                super.visitVarInsn(Opcodes.ISTORE, COUNT_LOCAL)
                super.visitFieldInsn(o, ow, n, d)
                // Straight-line logging; no branches, so no frame work.
                super.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
                super.visitInsn(Opcodes.DUP)
                super.visitMethodInsn(
                    Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                super.visitLdcInsn("[dexbuild] region=")
                append("(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                super.visitVarInsn(Opcodes.ALOAD, 0)
                super.visitFieldInsn(Opcodes.GETFIELD, owner, REGION_FIELD, "B")
                append("(I)Ljava/lang/StringBuilder;")
                super.visitLdcInsn(" buttons=")
                append("(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                super.visitVarInsn(Opcodes.ILOAD, COUNT_LOCAL)
                append("(I)Ljava/lang/StringBuilder;")
                super.visitLdcInsn(System.lineSeparator())
                append("(Ljava/lang/String;)Ljava/lang/StringBuilder;")
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "toString",
                    "()Ljava/lang/String;",
                    false)
                super.visitTypeInsn(Opcodes.NEW, "java/io/FileWriter")
                super.visitInsn(Opcodes.DUP)
                super.visitLdcInsn(LOG_FILE)
                super.visitInsn(Opcodes.ICONST_1)
                super.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    "java/io/FileWriter",
                    "<init>",
                    "(Ljava/lang/String;Z)V",
                    false)
                super.visitInsn(Opcodes.DUP_X1)
                super.visitInsn(Opcodes.SWAP)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/io/Writer",
                    "write",
                    "(Ljava/lang/String;)V",
                    false)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/io/Writer", "close", "()V", false)
              }

              private fun append(descriptor2: String) {
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", descriptor2, false)
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
 * Probe for the species detail page.
 *
 * One line whenever a detail page opens, stating what the client actually holds for that species:
 * level-up list, the taught-move arrays, the three ability ids, and the evolution-chain fields.
 * Distinguishes "the data never reached the object" from "the panel refuses to draw it" - the two
 * failure modes that needed five builds to tell apart on the list screen.
 */
object PokedexDetailProbe {
  private const val DETAIL_DESCRIPTOR = "(BLf/zK0;)V"
  private const val SPECIES = "f/zK0"
  private const val LOG_FILE = "dex-diagnostic.log"

  fun isDetailHost(classBytes: ByteArray): Boolean {
    var found = false
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
                if (descriptor == DETAIL_DESCRIPTOR) found = true
                return null
              }
            },
            ClassReader.SKIP_CODE,
        )
    return found
  }

  fun patch(classBytes: ByteArray): ByteArray {
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
            if (descriptor != DETAIL_DESCRIPTOR) return next
            return object : MethodVisitor(Opcodes.ASM9, next) {
              override fun visitCode() {
                super.visitCode()
                val sb = "java/lang/StringBuilder"
                fun appendStr() =
                    super.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        sb,
                        "append",
                        "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                        false)
                fun appendInt() =
                    super.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL, sb, "append", "(I)Ljava/lang/StringBuilder;", false)
                fun label(text: String) {
                  super.visitLdcInsn(text)
                  appendStr()
                }
                fun species() = super.visitVarInsn(Opcodes.ALOAD, 2)
                fun field(fieldName: String, fieldDescriptor: String) {
                  species()
                  super.visitFieldInsn(Opcodes.GETFIELD, SPECIES, fieldName, fieldDescriptor)
                }
                fun nullSafeSize(load: () -> Unit, size: () -> Unit) {
                  val isNull = Label()
                  val done = Label()
                  load()
                  super.visitInsn(Opcodes.DUP)
                  super.visitJumpInsn(Opcodes.IFNULL, isNull)
                  size()
                  super.visitJumpInsn(Opcodes.GOTO, done)
                  super.visitLabel(isNull)
                  super.visitInsn(Opcodes.POP)
                  super.visitIntInsn(Opcodes.BIPUSH, -1)
                  super.visitLabel(done)
                  appendInt()
                }

                super.visitTypeInsn(Opcodes.NEW, sb)
                super.visitInsn(Opcodes.DUP)
                super.visitMethodInsn(Opcodes.INVOKESPECIAL, sb, "<init>", "()V", false)
                label("[detail] id=")
                field("PA0", "S")
                appendInt()
                label(" tab=")
                super.visitVarInsn(Opcodes.ILOAD, 1)
                appendInt()
                label(" levelup=")
                nullSafeSize(
                    { field("Bg", "Ljava/util/List;") },
                    {
                      super.visitMethodInsn(
                          Opcodes.INVOKEINTERFACE, "java/util/List", "size", "()I", true)
                    })
                label(" taught=")
                nullSafeSize({ field("YC1", "[S") }, { super.visitInsn(Opcodes.ARRAYLENGTH) })
                label(" abilities=")
                nullSafeSize({ field("Oa1", "[S") }, { super.visitInsn(Opcodes.ARRAYLENGTH) })
                label(" ability0=")
                field("Oa1", "[S")
                super.visitInsn(Opcodes.ICONST_0)
                super.visitInsn(Opcodes.SALOAD)
                appendInt()
                label(" evoChain=")
                field("gr1", "S")
                appendInt()
                label(" evoCount=")
                field("pq", "B")
                appendInt()
                label(" evoEntries=")
                nullSafeSize(
                    { field("sv0", "Ljava/util/ArrayList;") },
                    {
                      super.visitMethodInsn(
                          Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false)
                    })
                super.visitLdcInsn(System.lineSeparator())
                appendStr()
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, sb, "toString", "()Ljava/lang/String;", false)
                super.visitTypeInsn(Opcodes.NEW, "java/io/FileWriter")
                super.visitInsn(Opcodes.DUP)
                super.visitLdcInsn(LOG_FILE)
                super.visitInsn(Opcodes.ICONST_1)
                super.visitMethodInsn(
                    Opcodes.INVOKESPECIAL,
                    "java/io/FileWriter",
                    "<init>",
                    "(Ljava/lang/String;Z)V",
                    false)
                super.visitInsn(Opcodes.DUP_X1)
                super.visitInsn(Opcodes.SWAP)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/io/Writer",
                    "write",
                    "(Ljava/lang/String;)V",
                    false)
                super.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL, "java/io/Writer", "close", "()V", false)
              }
            }
          }
        },
        0,
    )
    return writer.toByteArray()
  }
}
