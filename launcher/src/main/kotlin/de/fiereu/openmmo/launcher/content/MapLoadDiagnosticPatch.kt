package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Diagnostic for the silent NDS interior failures (Cold Storage 192, Pinwheel inside 155,
 * Chargestone 195): the LoadMap packet arrives, the client keeps rendering the old map, and nothing
 * is logged anywhere.
 *
 * `f/Tp1` is the s2c 0x10 LoadMap packet (verified through the client's own packet registry `f/E1`,
 * bootstrap #15). Its `X91()` applies the packet: it builds the destination `f/tM` through `tM1()`
 * and hands it to the map registry. Whatever kills that build is being swallowed upstream, so `X91`
 * gets an entry line naming the region/bank/map it is about to apply and a catch-all that prints
 * the throwable before rethrowing. Output goes to System.err, which the client's launcher captures
 * into log/console.log.
 */
object MapLoadDiagnosticPatch {
  private const val OWNER = "f/Tp1"

  fun isLoadMapPacket(classBytes: ByteArray): Boolean {
    if (ClassReader(classBytes).className != OWNER) return false
    var hasApply = false
    var hasBuild = false
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
                if (name == "X91" && descriptor == "()V") hasApply = true
                if (name == "tM1" && descriptor == "()Lf/tM;") hasBuild = true
                return null
              }
            },
            0,
        )
    return hasApply && hasBuild
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer =
        object : ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
    var patched = false
    reader.accept(
        object : ClassVisitor(Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "X91" || descriptor != "()V") return base
            patched = true
            return object : MethodVisitor(Opcodes.ASM9, base) {
              private val start = Label()
              private val handler = Label()

              override fun visitCode() {
                super.visitCode()
                visitTryCatchBlock(start, handler, handler, "java/lang/Throwable")
                visitLabel(start)
                // monmmo.MapLog.log("LoadMap apply r=.. b=.. m=.. romLoaded=..")
                visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
                visitInsn(Opcodes.DUP)
                visitMethodInsn(
                    Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
                visitLdcInsn("LoadMap apply r=")
                appendString()
                loadByteField("Ce0")
                appendInt()
                visitLdcInsn(" b=")
                appendString()
                loadByteField("BJ1")
                appendInt()
                visitLdcInsn(" m=")
                appendString()
                loadByteField("eK")
                appendInt()
                visitLdcInsn(" romLoaded=")
                appendString()
                visitVarInsn(Opcodes.ALOAD, 0)
                visitFieldInsn(Opcodes.GETFIELD, OWNER, "XR1", "Z")
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Z)Ljava/lang/StringBuilder;",
                    false)
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "toString",
                    "()Ljava/lang/String;",
                    false)
                visitMethodInsn(
                    Opcodes.INVOKESTATIC, "monmmo/MapLog", "log", "(Ljava/lang/String;)V", false)
              }

              private fun appendString() {
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
              }

              private fun appendInt() {
                visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(I)Ljava/lang/StringBuilder;",
                    false)
              }

              private fun loadByteField(field: String) {
                visitVarInsn(Opcodes.ALOAD, 0)
                visitFieldInsn(Opcodes.GETFIELD, OWNER, field, "B")
                // Bytes print signed otherwise; the wire values are unsigned.
                visitIntInsn(Opcodes.SIPUSH, 255)
                visitInsn(Opcodes.IAND)
              }

              override fun visitInsn(opcode: Int) {
                // The broken interiors never reach the next packet, so X91 either hangs or
                // returns; log every exit to tell the two apart.
                if (opcode == Opcodes.RETURN) {
                  visitLdcInsn("LoadMap apply done")
                  visitMethodInsn(
                      Opcodes.INVOKESTATIC, "monmmo/MapLog", "log", "(Ljava/lang/String;)V", false)
                }
                super.visitInsn(opcode)
              }

              override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                visitLabel(handler)
                // [Throwable] on the stack.
                visitInsn(Opcodes.DUP)
                visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "monmmo/MapLog",
                    "fail",
                    "(Ljava/lang/Throwable;)V",
                    false)
                visitInsn(Opcodes.ATHROW)
                super.visitMaxs(maxStack + 4, maxLocals + 1)
              }
            }
          }
        },
        0,
    )
    check(patched) { "f/Tp1.X91 was not found; the LoadMap diagnostic did not apply" }
    return writer.toByteArray()
  }
}

/**
 * Companion probe on `f/pJ0`, the s2c 0x05 LoadEntity packet: the LoadMap apply completes without
 * throwing for the broken interiors, so the failure to actually SWITCH maps must be in the entity
 * placement. Logs the position triple every apply and any throwable.
 */
object LoadEntityDiagnosticPatch {
  private const val OWNER = "f/pJ0"

  fun isLoadEntityPacket(classBytes: ByteArray): Boolean {
    if (org.objectweb.asm.ClassReader(classBytes).className != OWNER) return false
    var hasApply = false
    org.objectweb.asm
        .ClassReader(classBytes)
        .accept(
            object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): org.objectweb.asm.MethodVisitor? {
                if (name == "X91" && descriptor == "()V") hasApply = true
                return null
              }
            },
            0,
        )
    return hasApply
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = org.objectweb.asm.ClassReader(classBytes)
    val writer =
        object : org.objectweb.asm.ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
    var patched = false
    reader.accept(
        object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): org.objectweb.asm.MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name == "qP1" && descriptor == "()V") {
              // Parse runs on the read thread before the apply is scheduled; an entry mark here
              // splits "never constructed" from "constructed but never run".
              return object :
                  org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, base) {
                override fun visitCode() {
                  super.visitCode()
                  visitLdcInsn("LoadEntity parse")
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKESTATIC,
                      "monmmo/MapLog",
                      "log",
                      "(Ljava/lang/String;)V",
                      false)
                }
              }
            }
            if (name != "X91" || descriptor != "()V") return base
            patched = true
            return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, base) {
              private val start = org.objectweb.asm.Label()
              private val handler = org.objectweb.asm.Label()

              override fun visitCode() {
                super.visitCode()
                visitTryCatchBlock(start, handler, handler, "java/lang/Throwable")
                visitLabel(start)
                val O = org.objectweb.asm.Opcodes.ALOAD
                visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/lang/StringBuilder")
                visitInsn(org.objectweb.asm.Opcodes.DUP)
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKESPECIAL,
                    "java/lang/StringBuilder",
                    "<init>",
                    "()V",
                    false)
                visitLdcInsn("LoadEntity apply triple=")
                appendString()
                triple("Ai")
                visitLdcInsn(":")
                appendString()
                triple("Nb1")
                visitLdcInsn(":")
                appendString()
                triple("ez")
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "toString",
                    "()Ljava/lang/String;",
                    false)
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKESTATIC,
                    "monmmo/MapLog",
                    "log",
                    "(Ljava/lang/String;)V",
                    false)
              }

              private fun appendString() {
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                    false)
              }

              private fun triple(field: String) {
                visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
                visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, OWNER, "zp1", "Lf/Wi1;")
                visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, "f/Wi1", field, "B")
                visitIntInsn(org.objectweb.asm.Opcodes.SIPUSH, 255)
                visitInsn(org.objectweb.asm.Opcodes.IAND)
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                    "java/lang/StringBuilder",
                    "append",
                    "(I)Ljava/lang/StringBuilder;",
                    false)
              }

              override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                visitLabel(handler)
                visitInsn(org.objectweb.asm.Opcodes.DUP)
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKESTATIC,
                    "monmmo/MapLog",
                    "fail",
                    "(Ljava/lang/Throwable;)V",
                    false)
                visitInsn(org.objectweb.asm.Opcodes.ATHROW)
                super.visitMaxs(maxStack + 4, maxLocals + 1)
              }
            }
          }
        },
        0,
    )
    check(patched) { "f/pJ0.X91 was not found; the LoadEntity diagnostic did not apply" }
    return writer.toByteArray()
  }
}

/**
 * Third probe: the Gen 5 map-model converter `f/NM0(f/Hg, f/ml1)`. If the transition into the
 * broken interiors stalls waiting for a model that never converts, this shows whether the converter
 * is entered at all, finishes, or dies.
 */
object ConvertDiagnosticPatch {
  private const val OWNER = "f/NM0"
  private const val CTOR_DESC = "(Lf/Hg;Lf/ml1;)V"

  fun isConverter(classBytes: ByteArray): Boolean {
    if (org.objectweb.asm.ClassReader(classBytes).className != OWNER) return false
    var hasCtor = false
    org.objectweb.asm
        .ClassReader(classBytes)
        .accept(
            object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9) {
              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): org.objectweb.asm.MethodVisitor? {
                if (name == "<init>" && descriptor == CTOR_DESC) hasCtor = true
                return null
              }
            },
            0,
        )
    return hasCtor
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = org.objectweb.asm.ClassReader(classBytes)
    val writer =
        object : org.objectweb.asm.ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
    var patched = false
    reader.accept(
        object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): org.objectweb.asm.MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "<init>" || descriptor != CTOR_DESC) return base
            patched = true
            return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, base) {
              private val start = org.objectweb.asm.Label()
              private val handler = org.objectweb.asm.Label()
              private var armed = false

              // The try region must start AFTER the super()/this() call - the verifier refuses a
              // handler around an uninitialized `this`. Arm on the first method call instead of
              // visitCode.
              override fun visitMethodInsn(
                  opcode: Int,
                  owner: String,
                  mname: String,
                  mdesc: String,
                  isInterface: Boolean,
              ) {
                super.visitMethodInsn(opcode, owner, mname, mdesc, isInterface)
                if (!armed &&
                    opcode == org.objectweb.asm.Opcodes.INVOKESPECIAL &&
                    mname == "<init>") {
                  armed = true
                  visitTryCatchBlock(start, handler, handler, "java/lang/Throwable")
                  visitLabel(start)
                  visitTypeInsn(org.objectweb.asm.Opcodes.NEW, "java/lang/StringBuilder")
                  visitInsn(org.objectweb.asm.Opcodes.DUP)
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKESPECIAL,
                      "java/lang/StringBuilder",
                      "<init>",
                      "()V",
                      false)
                  visitLdcInsn("NM0 gen5 convert land=")
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                      "java/lang/StringBuilder",
                      "append",
                      "(Ljava/lang/String;)Ljava/lang/StringBuilder;",
                      false)
                  visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 2)
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "f/OP", "CoM5", "()S", false)
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                      "java/lang/StringBuilder",
                      "append",
                      "(I)Ljava/lang/StringBuilder;",
                      false)
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                      "java/lang/StringBuilder",
                      "toString",
                      "()Ljava/lang/String;",
                      false)
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKESTATIC,
                      "monmmo/MapLog",
                      "log",
                      "(Ljava/lang/String;)V",
                      false)
                }
              }

              override fun visitInsn(opcode: Int) {
                if (opcode == org.objectweb.asm.Opcodes.RETURN && armed) {
                  visitLdcInsn("NM0 gen5 convert done")
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKESTATIC,
                      "monmmo/MapLog",
                      "log",
                      "(Ljava/lang/String;)V",
                      false)
                }
                super.visitInsn(opcode)
              }

              override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                visitLabel(handler)
                visitInsn(org.objectweb.asm.Opcodes.DUP)
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKESTATIC,
                    "monmmo/MapLog",
                    "fail",
                    "(Ljava/lang/Throwable;)V",
                    false)
                visitInsn(org.objectweb.asm.Opcodes.ATHROW)
                super.visitMaxs(maxStack + 4, maxLocals + 1)
              }
            }
          }
        },
        0,
    )
    check(patched) { "f/NM0(f/Hg, f/ml1) was not found; the converter diagnostic did not apply" }
    return writer.toByteArray()
  }
}

/**
 * Stage markers inside the Unova map constructor `f/k90(f/ZH, short, byte, short, f/wj1)`: it runs
 * the area/building loader (`LPt9`) then the matrix/land loader (`wu1`). The broken interiors hang
 * somewhere inside; the markers say which stage.
 */
object K90DiagnosticPatch {
  private const val OWNER = "f/k90"
  private const val CTOR_DESC = "(Lf/ZH;SBSLf/wj1;)V"

  fun isUnovaMap(classBytes: ByteArray): Boolean =
      org.objectweb.asm.ClassReader(classBytes).className == OWNER

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = org.objectweb.asm.ClassReader(classBytes)
    val writer =
        object : org.objectweb.asm.ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
    var patched = false
    reader.accept(
        object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): org.objectweb.asm.MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "<init>" || descriptor != CTOR_DESC) return base
            patched = true
            return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, base) {
              private fun mark(text: String) {
                visitLdcInsn(text)
                visitMethodInsn(
                    org.objectweb.asm.Opcodes.INVOKESTATIC,
                    "monmmo/MapLog",
                    "log",
                    "(Ljava/lang/String;)V",
                    false)
              }

              override fun visitMethodInsn(
                  opcode: Int,
                  owner: String,
                  mname: String,
                  mdesc: String,
                  isInterface: Boolean,
              ) {
                if (owner == OWNER && mname == "LPt9") mark("k90 stage LPt9")
                if (owner == OWNER && mname == "wu1") mark("k90 stage wu1")
                super.visitMethodInsn(opcode, owner, mname, mdesc, isInterface)
              }

              override fun visitInsn(opcode: Int) {
                if (opcode == org.objectweb.asm.Opcodes.RETURN) mark("k90 built")
                super.visitInsn(opcode)
              }
            }
          }
        },
        0,
    )
    check(patched) { "f/k90 constructor was not found; the stage markers did not apply" }
    return writer.toByteArray()
  }
}

/**
 * The packet reader `f/E1.Db0` has exactly one silent failure: a DataFormatException from the
 * compressed-packet inflater goes to printStackTrace on stderr, which javaw discards, and the
 * packet is dropped as null. Mirror every printStackTrace in that class into the diagnostic log.
 */
object PacketDropDiagnosticPatch {
  private const val OWNER = "f/E1"

  fun isPacketRegistry(classBytes: ByteArray): Boolean =
      org.objectweb.asm.ClassReader(classBytes).className == OWNER

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = org.objectweb.asm.ClassReader(classBytes)
    val writer =
        object : org.objectweb.asm.ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
    var patched = false
    reader.accept(
        object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): org.objectweb.asm.MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, base) {
              override fun visitMethodInsn(
                  opcode: Int,
                  owner: String,
                  mname: String,
                  mdesc: String,
                  isInterface: Boolean,
              ) {
                if (mname == "printStackTrace" && mdesc == "()V") {
                  patched = true
                  visitInsn(org.objectweb.asm.Opcodes.DUP)
                  visitMethodInsn(
                      org.objectweb.asm.Opcodes.INVOKESTATIC,
                      "monmmo/MapLog",
                      "fail",
                      "(Ljava/lang/Throwable;)V",
                      false)
                }
                super.visitMethodInsn(opcode, owner, mname, mdesc, isInterface)
              }
            }
          }
        },
        0,
    )
    check(patched) { "no printStackTrace found in f/E1; the drop diagnostic did not apply" }
    return writer.toByteArray()
  }
}

/**
 * Movement packets carry no rail line, so the server cannot tell which of Castelia's overlapping
 * rail exits the player is actually riding toward. The position struct (f/Wi1) already knows - KS1
 * = on a rail, RW1 = the line - so the sender `f/nm0.d02` gets bits 2-5 of the state byte filled
 * with the line (1-15) whenever KS1 is set. The server reads (state >> 2) & 0xF; zero means "not on
 * a rail / unknown", which every unpatched packet naturally encodes.
 */
object MovementRailLinePatch {
  private const val OWNER = "f/nm0"

  fun isMovementSender(classBytes: ByteArray): Boolean =
      org.objectweb.asm.ClassReader(classBytes).className == OWNER

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = org.objectweb.asm.ClassReader(classBytes)
    val writer =
        object : org.objectweb.asm.ClassWriter(reader, COMPUTE_FRAMES) {
          override fun getCommonSuperClass(type1: String, type2: String): String =
              runCatching { super.getCommonSuperClass(type1, type2) }
                  .getOrDefault("java/lang/Object")
        }
    var patched = false
    reader.accept(
        object : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM9, writer) {
          override fun visitMethod(
              access: Int,
              name: String,
              descriptor: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): org.objectweb.asm.MethodVisitor {
            val base = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "d02") return base
            return object : org.objectweb.asm.MethodVisitor(org.objectweb.asm.Opcodes.ASM9, base) {
              override fun visitMethodInsn(
                  opcode: Int,
                  owner: String,
                  mname: String,
                  mdesc: String,
                  isInterface: Boolean,
              ) {
                if (mname == "put" && mdesc == "(B)Ljava/nio/ByteBuffer;" && !patched) {
                  patched = true
                  // Stack: [buf, state]. Merge the rail line into bits 2-5 when on a rail.
                  val skip = org.objectweb.asm.Label()
                  visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
                  visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, OWNER, "WM1", "Lf/Wi1;")
                  visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, "f/Wi1", "KS1", "Z")
                  visitJumpInsn(org.objectweb.asm.Opcodes.IFEQ, skip)
                  visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
                  visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, OWNER, "WM1", "Lf/Wi1;")
                  visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, "f/Wi1", "RW1", "B")
                  visitIntInsn(org.objectweb.asm.Opcodes.BIPUSH, 15)
                  visitInsn(org.objectweb.asm.Opcodes.IAND)
                  visitInsn(org.objectweb.asm.Opcodes.ICONST_2)
                  visitInsn(org.objectweb.asm.Opcodes.ISHL)
                  visitInsn(org.objectweb.asm.Opcodes.IOR)
                  visitInsn(org.objectweb.asm.Opcodes.I2B)
                  visitLabel(skip)
                }
                super.visitMethodInsn(opcode, owner, mname, mdesc, isInterface)
              }
            }
          }
        },
        0,
    )
    check(patched) { "f/nm0.d02 put(B) was not found; the rail-line patch did not apply" }
    return writer.toByteArray()
  }
}
