package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesAssets
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.imageio.ImageIO

class ExpansionAssetStagingTest :
    FunSpec({
      val red = 0xffcc2222.toInt()
      val blue = 0xff2222cc.toInt()

      /** Index 1 everywhere, so the palette alone decides the rendered colour. */
      fun indexedPng(root: Path, name: String, size: Int) {
        val model =
            IndexColorModel(8, 2, byteArrayOf(0, 0), byteArrayOf(0, 0), byteArrayOf(0, 0), 0)
        val image = BufferedImage(size, size, BufferedImage.TYPE_BYTE_INDEXED, model)
        for (y in 0 until size) for (x in 0 until size) image.raster.setSample(x, y, 0, 1)
        ImageIO.write(image, "png", root.resolve(name).toFile())
      }

      fun palette(root: Path, name: String, colour: String) {
        Files.writeString(root.resolve(name), "JASC-PAL\n0100\n2\n0 0 0\n$colour\n")
      }

      fun stage(): Pair<Path, Path> {
        val root = Files.createTempDirectory("expansion-assets")
        Files.createDirectories(root.resolve("graphics/pokemon/icon_palettes"))
        indexedPng(root, "graphics/pokemon/front.png", 8)
        indexedPng(root, "graphics/pokemon/back.png", 8)
        indexedPng(root, "graphics/pokemon/icon.png", 8)
        palette(root, "graphics/pokemon/normal.pal", "204 34 34")
        palette(root, "graphics/pokemon/shiny.pal", "34 34 204")
        palette(root, "graphics/pokemon/icon_palettes/pal2.pal", "204 34 34")
        Files.writeString(root.resolve("cry.wav"), "RIFF")

        val species =
            ExpansionSpeciesRegistry()
                .get("SPECIES_TYRUNT")!!
                .copy(
                    assets =
                        ExpansionSpeciesAssets(
                            partyIcon = true,
                            frontSprite = true,
                            backSprite = true,
                            cry = true,
                            follower = false,
                            frontPicPath = "graphics/pokemon/front.png",
                            backPicPath = "graphics/pokemon/back.png",
                            iconPath = "graphics/pokemon/icon.png",
                            normalPalettePath = "graphics/pokemon/normal.pal",
                            shinyPalettePath = "graphics/pokemon/shiny.pal",
                            cryPath = "cry.wav",
                            iconPalIndex = 2,
                        ))
        val archive = root.resolve("out/expansion.zip")
        ExpansionAssetStaging(root).stage(listOf(species), archive).let { summary ->
          summary.staged shouldBe 1
          summary.cries shouldBe 1
          summary.failures shouldBe emptyMap()
        }
        return root to archive
      }

      test("writes every resource the client looks up by wire id") {
        val (_, archive) = stage()
        ZipFile(archive.toFile()).use { zip ->
          zip.entries().toList().map { it.name } shouldContainAll
              listOf(
                  "info.xml",
                  // The client rejects an archive that declares no directories.
                  "sprites/",
                  "sprites/battlesprites/",
                  "sprites/monstericons/",
                  "cries/",
                  "sprites/battlesprites/696-front-n.gif",
                  "sprites/battlesprites/696-front-s.gif",
                  "sprites/battlesprites/696-back-n.gif",
                  "sprites/battlesprites/696-back-s.gif",
                  "sprites/monstericons/696-0.png",
                  "sprites/monstericons/696-1.png",
                  "cries/696.wav",
              )
        }
      }

      test("shiny reuses the sprite indices against the shiny palette") {
        val (_, archive) = stage()
        ZipFile(archive.toFile()).use { zip ->
          fun read(name: String): BufferedImage =
              ImageIO.read(ByteArrayInputStream(zip.getInputStream(zip.getEntry(name)).readBytes()))

          val normal = read("sprites/battlesprites/696-front-n.gif")
          val shiny = read("sprites/battlesprites/696-front-s.gif")
          normal.width shouldBe 64
          normal.getRGB(0, 0) shouldBe red
          shiny.getRGB(0, 0) shouldBe blue
          // The source is 8x8, so the rest of the client's frame must stay transparent.
          (normal.getRGB(63, 63) ushr 24) shouldBe 0
        }
      }
    })
