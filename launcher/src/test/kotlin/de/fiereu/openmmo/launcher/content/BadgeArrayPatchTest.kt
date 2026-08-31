package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader

/**
 * Runs against the installed client, because the point of this patch is a number the client
 * hardcodes and nothing else records.
 */
class BadgeArrayPatchTest :
    FunSpec({
      val client = clientExecutable()

      fun badgeLoader(): Pair<String, ByteArray>? {
        val file = client ?: return null
        return ZipFile(file.toFile()).use { archive ->
          archive
              .entries()
              .asSequence()
              .filter { it.name.endsWith(".class") }
              .firstNotNullOfOrNull { entry ->
                val bytes = archive.getInputStream(entry).use { it.readBytes() }
                if (BadgeArrayPatch.isBadgeLoader(bytes)) entry.name to bytes else null
              }
        }
      }

      test("the client allocates exactly two badge tables, both at the stock type count") {
        val (_, bytes) = badgeLoader() ?: return@test
        // One for the species badge, one for the move banner. Both 18: the type count the game
        // shipped with, which is why a nineteenth type is never looked up.
        BadgeArrayPatch.arraySizes(bytes) shouldContainExactly
            listOf(BadgeArrayPatch.STOCK_TYPE_COUNT, BadgeArrayPatch.STOCK_TYPE_COUNT)
      }

      test("patching widens both tables and leaves the class loadable") {
        val (name, bytes) = badgeLoader() ?: return@test
        val patched = BadgeArrayPatch.patch(bytes, 20)

        BadgeArrayPatch.arraySizes(patched) shouldContainExactly listOf(20, 20)
        // The class must keep its own name, or the JVM rejects it on load.
        ClassReader(patched).className shouldBe ClassReader(bytes).className
        BadgeArrayPatch.isBadgeLoader(patched) shouldBe true
      }

      test("the overlay must be a jar, because four classes share this name") {
        val file = client ?: return@test
        val (name, _) = badgeLoader() ?: return@test
        val siblings =
            ZipFile(file.toFile()).use { archive ->
              archive
                  .entries()
                  .asSequence()
                  .map { it.name }
                  .filter { it.equals(name, ignoreCase = true) }
                  .toList()
            }
        // RP0, Rp0, rP0 and rp0 all exist. A Windows overlay directory holds one of them and
        // answers lookups for all four, so the JVM asks for f/rP0, receives f/Rp0 and refuses it
        // with NoClassDefFoundError. Zip entries are case-sensitive, so a jar overlay does not
        // collide. This is not true of the type enum, which has no case twins.
        siblings.size shouldBe 4
      }
    })

private fun clientExecutable(): Path? {
  val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
  return listOf(
          "$local/MonMMO-EX/Client-31914/MonMMO-Local.exe",
          "$local/MonMMO-EX/Client-31914/PokeMMO.exe")
      .map(Path::of)
      .firstOrNull(Files::isRegularFile)
}
