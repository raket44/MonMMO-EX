package de.fiereu.openmmo.launcher.content

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Routes the Pokedex detail panel's ROM-text reads through the overlay.
 *
 * The panel's category box ("Ninja Pokemon"), dex-entry paragraph, height and weight lines do not
 * come from the runtime string table at all: they call `nV0.f7(source, language, table, speciesId,
 * args)`, which reads the ROM's own text archives (`nV0.bw1`) - tables 235/236, 260, 245 and 268,
 * indexed by species id. An imported species has no row in a ROM archive, which is why those boxes
 * render empty no matter what the string table says.
 *
 * Every `f7` call in the panel is retargeted to `monmmo.DexPatch.dexText`, which serves the
 * imported species' text from a packed resource and delegates untouched ids straight back to `f7`.
 * The replacement descriptor widens the language parameter to `Object` so the overlay compiles
 * without the client on its classpath; the verifier accepts the narrowing because the value on the
 * stack is always the client's language object.
 */
object DexTextRedirectPatch {
  private const val ROM_TEXT_OWNER = "f/nV0"
  private const val ROM_TEXT_NAME = "f7"
  private const val ROM_TEXT_DESC = "(BLf/cX;II[Ljava/lang/String;)Ljava/lang/String;"
  private const val TARGET_OWNER = "monmmo/DexPatch"
  private const val TARGET_NAME = "dexText"
  private const val TARGET_DESC = "(BLjava/lang/Object;II[Ljava/lang/String;)Ljava/lang/String;"

  /**
   * True only for the Pokedex detail panel: the one class that both reads the ROM text archives and
   * renders the held-item list off the species record.
   */
  fun isDetailPanel(classBytes: ByteArray): Boolean {
    var romTextCalls = 0
    var readsHeldItems = false
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
                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String,
                        name: String,
                        descriptor: String,
                        isInterface: Boolean,
                    ) {
                      if (opcode == Opcodes.INVOKESTATIC &&
                          owner == ROM_TEXT_OWNER &&
                          name == ROM_TEXT_NAME &&
                          descriptor == ROM_TEXT_DESC) {
                        romTextCalls++
                      }
                    }

                    override fun visitFieldInsn(o: Int, ow: String, n: String, d: String) {
                      if (o == Opcodes.GETFIELD && n == "YC1" && d == "[S") {
                        readsHeldItems = true
                      }
                    }
                  }
            },
            ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
    return romTextCalls >= 4 && readsHeldItems
  }

  fun patch(classBytes: ByteArray): ByteArray {
    val reader = ClassReader(classBytes)
    val writer = ClassWriter(reader, 0)
    var rewritten = 0
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
                override fun visitMethodInsn(
                    opcode: Int,
                    owner: String,
                    methodName: String,
                    methodDescriptor: String,
                    isInterface: Boolean,
                ) {
                  if (opcode == Opcodes.INVOKESTATIC &&
                      owner == ROM_TEXT_OWNER &&
                      methodName == ROM_TEXT_NAME &&
                      methodDescriptor == ROM_TEXT_DESC) {
                    rewritten++
                    super.visitMethodInsn(
                        Opcodes.INVOKESTATIC, TARGET_OWNER, TARGET_NAME, TARGET_DESC, false)
                  } else {
                    super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface)
                  }
                }
              }
        },
        0)
    check(rewritten >= 4) {
      "Expected at least 4 ROM text reads in the detail panel, saw $rewritten"
    }
    return writer.toByteArray()
  }
}
