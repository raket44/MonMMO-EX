package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import org.objectweb.asm.ClassReader

/**
 * The four places the client stops at five regions, each found by shape and each moved together.
 */
class PokedexRegionPatchTest :
    FunSpec({
      val client = run {
        val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
        listOf(
                "$local/MonMMO-EX/Client-31914/MonMMO-Local.exe",
                "$local/MonMMO-EX/Client-31914/PokeMMO.exe")
            .map(Path::of)
            .firstOrNull(Files::isRegularFile)
      }

      fun find(predicate: (ByteArray) -> Boolean): Pair<String, ByteArray>? {
        val file = client ?: return null
        return ZipFile(file.toFile()).use { archive ->
          archive
              .entries()
              .asSequence()
              .filter { it.name.endsWith(".class") }
              .firstNotNullOfOrNull {
                val bytes = archive.getInputStream(it).use { s -> s.readBytes() }
                if (predicate(bytes)) it.name to bytes else null
              }
        }
      }

      val regions = listOf(5, 6, 7, 8)

      test("the species record keeps six region slots, and widening gives it more") {
        val (_, bytes) = find(PokedexRegionPatch::isSpeciesRecord) ?: return@test
        PokedexRegionPatch.regionSlots(bytes) shouldBe PokedexRegionPatch.STOCK_REGION_SLOTS
        val patched = PokedexRegionPatch.patchRegionSlots(bytes, 10)
        PokedexRegionPatch.regionSlots(patched) shouldBe 10
        ClassReader(patched).className shouldBe ClassReader(bytes).className
      }

      test("the region display order grows by exactly the regions added") {
        val (_, bytes) = find(PokedexRegionPatch::isRegionTable) ?: return@test
        val patched = PokedexRegionPatch.patchRegionTable(bytes, regions)
        ClassReader(patched).className shouldBe ClassReader(bytes).className
      }

      test("the tab loop is bounded by the stock count, and the patch raises it") {
        val (_, bytes) = find(PokedexRegionPatch::isPokedexScreen) ?: return@test
        PokedexRegionPatch.tabLoopBounds(bytes) shouldBe 1
        val patched = PokedexRegionPatch.patchTabLoop(bytes, 9)
        PokedexRegionPatch.tabLoopBounds(patched) shouldBe 0
      }

      test("the region gate rejects anything past four, and the patch admits the new ones") {
        val (_, bytes) = find(PokedexRegionPatch::isRegionGate) ?: return@test
        PokedexRegionPatch.regionGateMethod(bytes) shouldBe "fG"
        val patched = PokedexRegionPatch.patchRegionGate(bytes, regions)
        ClassReader(patched).className shouldBe ClassReader(bytes).className
      }
    })
