package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * What the client's type enum looks like from the outside, discovered rather than hard coded: the
 * class is obfuscated and its names change between client builds, so every name here is read back
 * out of the bytecode.
 */
data class TypeEnumShape(
    val internalName: String,
    /** Enum constant name to the static field holding it, in ordinal order. */
    val constants: List<Pair<String, String>>,
    /** The synthetic $VALUES array the enum's values() clones. */
    val valuesField: String,
    /** setEffectiveness(defender, multiplier), which writes into the per-type table. */
    val effectivenessMethod: String,
    /** Width of that table as the constructor allocates it. */
    val tableSize: Int,
    val constructorDescriptor: String,
) {
  fun ordinalOf(name: String): Int = constants.indexOfFirst { it.first == name }
}

object FairyTypePatch {
  const val FAIRY = "FAIRY"

  /**
   * String id for the Fairy type name. The client renders a type badge by asking the enum for a
   * string id and looking it up, so Fairy needs one of its own; the stock ids for the other types
   * come from the ROM, which has no Fairy, so this is a free id supplied through strings_en.xml. It
   * has to fit in a short because that is what the accessor returns.
   */
  const val FAIRY_NAME_STRING_ID = 9226
  private const val ENUM_INIT = "(IILjava/lang/String;I)V"

  /** Reads the enum's layout so the patch can be expressed in its own terms. */
  fun inspect(classBytes: ByteArray): TypeEnumShape {
    var internalName = ""
    var valuesField = ""
    var effectivenessMethod = ""
    var tableSize = 0
    val constants = mutableListOf<Pair<String, String>>()

    ClassReader(classBytes)
        .accept(
            object : ClassVisitor(Opcodes.ASM9) {
              override fun visit(
                  version: Int,
                  access: Int,
                  name: String,
                  signature: String?,
                  superName: String?,
                  interfaces: Array<out String>?,
              ) {
                internalName = name
              }

              override fun visitMethod(
                  access: Int,
                  name: String,
                  descriptor: String,
                  signature: String?,
                  exceptions: Array<out String>?,
              ): MethodVisitor {
                // The effectiveness setter is the only (self, double) -> void method.
                if (descriptor == "(L$internalName;D)V") effectivenessMethod = name
                return when (name) {
                  // values() opens by reading the synthetic array, which names it for us.
                  "values" ->
                      object : MethodVisitor(Opcodes.ASM9) {
                        override fun visitFieldInsn(op: Int, owner: String, f: String, d: String) {
                          if (op == Opcodes.GETSTATIC && valuesField.isEmpty()) valuesField = f
                        }
                      }
                  "<clinit>" ->
                      object : MethodVisitor(Opcodes.ASM9) {
                        private var pending: String? = null

                        override fun visitLdcInsn(value: Any) {
                          if (value is String) pending = value
                        }

                        override fun visitFieldInsn(op: Int, owner: String, f: String, d: String) {
                          val name = pending ?: return
                          if (op == Opcodes.PUTSTATIC && d == "L$internalName;") {
                            constants += name to f
                            pending = null
                          }
                        }
                      }
                  "<init>" ->
                      object : MethodVisitor(Opcodes.ASM9) {
                        private var lastInt = 0

                        override fun visitIntInsn(op: Int, operand: Int) {
                          if (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH) lastInt = operand
                          if (op == Opcodes.NEWARRAY && operand == Opcodes.T_DOUBLE) {
                            tableSize = lastInt
                          }
                        }
                      }
                  else -> object : MethodVisitor(Opcodes.ASM9) {}
                }
              }
            },
            0)

    check(internalName.isNotEmpty()) { "Could not read the type enum's name" }
    check(valuesField.isNotEmpty()) { "Could not find the enum's values array" }
    check(effectivenessMethod.isNotEmpty()) { "Could not find the effectiveness setter" }
    check(tableSize > 0) { "Could not read the effectiveness table width" }
    check(constants.isNotEmpty()) { "Could not read the enum constants" }
    return TypeEnumShape(
        internalName, constants, valuesField, effectivenessMethod, tableSize, ENUM_INIT)
  }

  /**
   * Adds Fairy to the enum. The client fills each type's table with 1.0 in the constructor and only
   * writes the exceptions, so this widens that table, appends the constant to the array values()
   * clones, and then writes just the non-neutral matchups. Registration into the byte lookup maps
   * needs no work: the class builds them by looping over values() after the array is assigned.
   */
  fun patch(classBytes: ByteArray, chart: Map<String, Map<String, Double>>): ByteArray {
    val shape = inspect(classBytes)
    require(shape.ordinalOf(FAIRY) < 0) { "The type enum already has a Fairy constant" }
    val ordinal = shape.constants.size
    val self = shape.internalName
    val descriptor = "L$self;"
    val widened = ordinal + 1

    val reader = ClassReader(classBytes)
    // Adding a switch case introduces a branch target, which needs a stack map frame. Computing
    // frames means ASM has to merge types, and it does that by loading classes; the client's
    // classes
    // are not on this classpath, so unresolvable pairs fall back to Object, which is always safe
    // here because nothing this patch adds merges two client types at a join point.
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
              desc: String,
              signature: String?,
              exceptions: Array<out String>?,
          ): MethodVisitor {
            val delegate = super.visitMethod(access, name, desc, signature, exceptions)
            return when (name) {
              "<init>" -> WidenTable(delegate, shape.tableSize, widened)
              "<clinit>" -> AddConstant(delegate, shape, chart, ordinal, self, descriptor, widened)
              // The only no-arg short accessor is the one yielding a type name string id.
              else -> if (desc == "()S") NameStringId(delegate, ordinal) else delegate
            }
          }

          override fun visitEnd() {
            visitField(
                    Opcodes.ACC_PUBLIC or
                        Opcodes.ACC_STATIC or
                        Opcodes.ACC_FINAL or
                        Opcodes.ACC_ENUM,
                    FAIRY,
                    descriptor,
                    null,
                    null)
                ?.visitEnd()
            super.visitEnd()
          }
        },
        0)
    return writer.toByteArray()
  }

  /**
   * Rewrites the ordinal switch that yields a type's name string id. A tableswitch cannot carry a
   * case outside its range, so it becomes a lookupswitch with Fairy's ordinal added.
   */
  private class NameStringId(next: MethodVisitor, private val ordinal: Int) :
      MethodVisitor(Opcodes.ASM9, next) {
    private val fairy = org.objectweb.asm.Label()
    private var rewritten = false

    override fun visitTableSwitchInsn(
        min: Int,
        max: Int,
        dflt: org.objectweb.asm.Label,
        vararg labels: org.objectweb.asm.Label,
    ) {
      rewritten = true
      val keys = (min..max).toMutableList().apply { add(ordinal) }
      val targets = labels.toMutableList().apply { add(fairy) }
      mv.visitLookupSwitchInsn(dflt, keys.toIntArray(), targets.toTypedArray())
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
      if (rewritten) {
        mv.visitLabel(fairy)
        mv.visitIntInsn(Opcodes.SIPUSH, FAIRY_NAME_STRING_ID)
        mv.visitInsn(Opcodes.IRETURN)
      }
      super.visitMaxs(maxStack, maxLocals)
    }
  }

  /** The per-type table is sized for the types that existed; Fairy needs one more slot. */
  private class WidenTable(next: MethodVisitor, private val from: Int, private val to: Int) :
      MethodVisitor(Opcodes.ASM9, next) {
    private var pendingWiden = false

    override fun visitIntInsn(opcode: Int, operand: Int) {
      if ((opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) && operand == from) {
        pendingWiden = true
        super.visitIntInsn(opcode, to)
        return
      }
      pendingWiden = false
      super.visitIntInsn(opcode, operand)
    }
  }

  private class AddConstant(
      next: MethodVisitor,
      private val shape: TypeEnumShape,
      private val chart: Map<String, Map<String, Double>>,
      private val ordinal: Int,
      private val self: String,
      private val descriptor: String,
      private val widened: Int,
  ) : MethodVisitor(Opcodes.ASM9, next) {
    private var appended = false

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, desc: String) {
      super.visitFieldInsn(opcode, owner, name, desc)
      // Straight after the values array is assigned, so the lookup loop below it sees Fairy too.
      if (!appended && opcode == Opcodes.PUTSTATIC && name == shape.valuesField) {
        appended = true
        val arrayType = "[$descriptor"
        mv.visitFieldInsn(Opcodes.GETSTATIC, self, shape.valuesField, arrayType)
        mv.visitIntInsn(Opcodes.BIPUSH, widened)
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/util/Arrays",
            "copyOf",
            "([Ljava/lang/Object;I)[Ljava/lang/Object;",
            false)
        mv.visitTypeInsn(Opcodes.CHECKCAST, arrayType)
        mv.visitFieldInsn(Opcodes.PUTSTATIC, self, shape.valuesField, arrayType)

        mv.visitTypeInsn(Opcodes.NEW, self)
        mv.visitInsn(Opcodes.DUP)
        mv.visitIntInsn(Opcodes.BIPUSH, ordinal)
        mv.visitIntInsn(Opcodes.BIPUSH, ordinal)
        mv.visitLdcInsn(FAIRY)
        mv.visitIntInsn(Opcodes.BIPUSH, ordinal)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL, self, "<init>", shape.constructorDescriptor, false)
        mv.visitFieldInsn(Opcodes.PUTSTATIC, self, FAIRY, descriptor)

        mv.visitFieldInsn(Opcodes.GETSTATIC, self, shape.valuesField, arrayType)
        mv.visitIntInsn(Opcodes.BIPUSH, ordinal)
        mv.visitFieldInsn(Opcodes.GETSTATIC, self, FAIRY, descriptor)
        mv.visitInsn(Opcodes.AASTORE)
      }
    }

    override fun visitInsn(opcode: Int) {
      if (opcode == Opcodes.RETURN) writeMatchups()
      super.visitInsn(opcode)
    }

    /** Only the non-neutral cells, matching how the class already stores the chart. */
    private fun writeMatchups() {
      val fields = shape.constants.toMap()
      fun field(type: String): String? = fields[if (type == "MYSTERY") CLIENT_MYSTERY else type]

      fun set(attackerField: String, defenderField: String, multiplier: Double) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, self, attackerField, descriptor)
        mv.visitFieldInsn(Opcodes.GETSTATIC, self, defenderField, descriptor)
        mv.visitLdcInsn(multiplier)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL, self, shape.effectivenessMethod, "($descriptor" + "D)V", false)
      }

      chart.getValue(FAIRY).forEach { (defender, multiplier) ->
        val target = field(defender) ?: return@forEach
        if (multiplier != 1.0) set(FAIRY, target, multiplier)
      }
      chart.forEach { (attacker, row) ->
        val source = field(attacker) ?: return@forEach
        val multiplier = row[FAIRY] ?: return@forEach
        if (multiplier != 1.0) set(source, FAIRY, multiplier)
      }
    }
  }

  private const val CLIENT_MYSTERY = "QUESTIONQUESTIONQUESTION"
}
