package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import java.awt.image.BufferedImage
import java.awt.image.DataBuffer
import java.awt.image.IndexColorModel
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

data class ExpansionAssetSummary(
    val staged: Int,
    val skipped: Int,
    val cries: Int,
    val animated: Int,
    val followers: Int,
    val followerFallbacks: Int,
    val failures: Map<String, String>,
)

/**
 * Converts pokeemerald-expansion sprite sources into the resource layout the client loads, and
 * packages them as a mod archive. Battle sprites use .gif and party icons use .png because that is
 * what the client's own resources.zip ships; the archive is keyed by client wire id, so it is only
 * meaningful alongside a data.pak that registers the same ids.
 */
class ExpansionAssetStaging(private val expansionRoot: Path) {

  fun stage(
      species: List<ExpansionSpeciesDef>,
      archive: Path,
      itemIcons: Map<Int, ByteArray> = emptyMap(),
  ): ExpansionAssetSummary {
    var staged = 0
    var cries = 0
    var animated = 0
    var followers = 0
    var followerFallbacks = 0
    val failures = linkedMapOf<String, String>()
    val anims = ExpansionFrontAnims.parse(expansionRoot)
    val asymFollowers = parseAsymFollowers()
    Files.createDirectories(archive.parent)
    ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
      // The client refuses an archive with no directory entries as a "flattened zip structure",
      // so every folder it will read from is declared before the files land in it.
      listOf("sprites/", "$SPRITES/", "$ICONS/", "$FOLLOWERS/", "$CRIES/", "sprites/itemicons/")
          .forEach { zip.directory(it) }
      zip.write("info.xml", INFO_XML.toByteArray())
      itemIcons.forEach { (itemId, png) -> zip.write("sprites/itemicons/$itemId.png", png) }
      species.forEach { entry ->
        val wireId = entry.clientWireId ?: return@forEach
        runCatching {
              val normal = palette(entry.assets.normalPalettePath)
              val shiny = palette(entry.assets.shinyPalettePath)
              val front = indexed(entry.assets.frontPicPath)
              val back = indexed(entry.assets.backPicPath)
              val icon = indexed(entry.assets.iconPath)
              val iconPalette = palette("$ICON_PALETTES/pal${entry.assets.iconPalIndex}.pal")

              // anim_front.png stacks the animation frames; the Expansion's own playback script
              // turns them into an animated GIF, which the client plays as-is. Sprites the
              // Expansion keeps as a single frame - most Gen 6+ fronts, and every back - get the
              // synthesized breathing idle instead, so nothing stands frozen.
              val script = anims[entry.symbol.removePrefix("SPECIES_")].orEmpty()
              if (script.size > 1 && front.height / FRAME > 1) {
                zip.write("$SPRITES/$wireId-front-n.gif", scriptedGif(front, normal, FRAME, script))
                zip.write("$SPRITES/$wireId-front-s.gif", scriptedGif(front, shiny, FRAME, script))
                animated++
              } else {
                zip.write("$SPRITES/$wireId-front-n.gif", idleGif(front, normal, FRAME))
                zip.write("$SPRITES/$wireId-front-s.gif", idleGif(front, shiny, FRAME))
              }
              zip.write("$SPRITES/$wireId-back-n.gif", idleGif(back, normal, FRAME))
              zip.write("$SPRITES/$wireId-back-s.gif", idleGif(back, shiny, FRAME))
              // icon.png stacks the two idle-bounce frames the party UI alternates between.
              zip.write("$ICONS/$wireId-0.png", png(icon, iconPalette, ICON, 0))
              zip.write("$ICONS/$wireId-1.png", png(icon, iconPalette, ICON, ICON))

              // Follower sprites: the overworld sheet that walks behind the player. The client
              // exits with a fatal error when the lead party member has no follower sheet at all,
              // so every staged species ships one - converted from the Expansion's overworld art
              // when it exists, and a static grid of the party icon when it does not.
              val spriteDir = Path.of(entry.assets.frontPicPath).parent
              val overworld = spriteDir?.resolve("overworld.png")?.toString().orEmpty()
              if (overworld.isNotEmpty() && Files.isRegularFile(expansionRoot.resolve(overworld))) {
                val sheet = indexed(overworld)
                val followNormal =
                    palette(
                        followerPalette(
                            spriteDir!!, "overworld_normal.pal", entry.assets.normalPalettePath))
                val followShiny =
                    palette(
                        followerPalette(
                            spriteDir, "overworld_shiny.pal", entry.assets.shinyPalettePath))
                val asym = entry.symbol.removePrefix("SPECIES_") in asymFollowers
                zip.write("$FOLLOWERS/$wireId-b-n.png", followerSheet(sheet, followNormal, asym))
                zip.write("$FOLLOWERS/$wireId-b-s.png", followerSheet(sheet, followShiny, asym))
                followers++
              } else {
                zip.write("$FOLLOWERS/$wireId-b-n.png", followerFromIcon(icon, iconPalette))
                zip.write("$FOLLOWERS/$wireId-b-s.png", followerFromIcon(icon, iconPalette))
                followerFallbacks++
              }

              if (entry.assets.cryPath.isNotEmpty()) {
                zip.write("$CRIES/$wireId.wav", Files.readAllBytes(source(entry.assets.cryPath)))
                cries++
              }
              staged++
            }
            .onFailure { failures[entry.stableId] = it.message ?: it::class.simpleName.orEmpty() }
      }
    }
    return ExpansionAssetSummary(
        staged, species.size - staged, cries, animated, followers, followerFallbacks, failures)
  }

  private fun source(path: String): Path {
    require(path.isNotEmpty()) { "Missing Expansion source path" }
    val file = expansionRoot.resolve(path)
    require(Files.isRegularFile(file)) { "Expansion source not found: $path" }
    return file
  }

  /**
   * JASC-PAL is the palette format the decomp keeps beside every sprite. Index 0 is transparent.
   */
  private fun palette(path: String): IntArray {
    val lines = Files.readAllLines(source(path)).map(String::trim).filter(String::isNotEmpty)
    check(lines.firstOrNull() == "JASC-PAL") { "Not a JASC palette: $path" }
    val declared = lines[2].toInt()
    return IntArray(declared) { index ->
      if (index == 0) return@IntArray 0
      val (red, green, blue) = lines[3 + index].split(WHITESPACE).map(String::toInt)
      (0xff shl 24) or (red shl 16) or (green shl 8) or blue
    }
  }

  /**
   * Reads raw palette indices rather than resolved colours, because the decomp's PNG palette is
   * only a preview: the shiny variant reuses the same indices against a different palette file.
   */
  private fun indexed(path: String): IndexedImage {
    val image = ImageIO.read(source(path).toFile()) ?: error("Unreadable image: $path")
    check(image.colorModel is IndexColorModel) { "Expected an indexed sprite: $path" }
    val indices = IntArray(image.width * image.height)
    for (y in 0 until image.height) {
      for (x in 0 until image.width) {
        indices[y * image.width + x] = image.raster.getSample(x, y, 0)
      }
    }
    return IndexedImage(image.width, image.height, indices)
  }

  /**
   * Plays the Expansion's animation script as a looping GIF. Durations are GBA ticks - sixtieths of
   * a second - and GIF delays are hundredths, so each step is rescaled and rounded. On the GBA the
   * script ran once when the species entered battle and then rested; a GIF loops forever, so the
   * final step is held long enough to read as the idle pose between passes instead of a one-tick
   * flash.
   */
  private fun scriptedGif(
      image: IndexedImage,
      palette: IntArray,
      size: Int,
      script: List<ExpansionFrontAnims.Step>,
  ): ByteArray {
    val model = colorModel(palette)
    val available = image.height / size
    return animatedGif(
        script.mapIndexed { index, step ->
          val target = BufferedImage(size, size, BufferedImage.TYPE_BYTE_INDEXED, model)
          copy(image, target, size, step.frame.coerceIn(0, available - 1) * size)
          val ticks = step.durationTicks
          val delay =
              if (index == script.lastIndex) maxOf(REST_DELAY, ticks * 100 / 60)
              else maxOf(2, (ticks * 100 + 30) / 60)
          target to delay
        })
  }

  /**
   * A breathing idle for sprites the Expansion keeps as a single frame - most species past Gen 5
   * never received sheet animations, and every back sprite is a lone frame. Rather than sitting
   * frozen next to the scripted species, the art itself is squashed a pixel or two toward its
   * baseline and released, the same idle bounce the games and the client's own follower sprites
   * use. No new art is invented; every pixel comes from the real sprite.
   */
  private fun idleGif(image: IndexedImage, palette: IntArray, size: Int): ByteArray {
    val model = colorModel(palette)
    fun squashed(amount: Int): BufferedImage {
      val target = BufferedImage(size, size, BufferedImage.TYPE_BYTE_INDEXED, model)
      for (y in 0 until size) {
        for (x in 0 until size) {
          val index =
              if (y < amount) TRANSPARENT_INDEX
              else image.index(x, (y - amount) * size / (size - amount))
          target.raster.setSample(x, y, 0, index)
        }
      }
      return target
    }
    return animatedGif(
        listOf(
            squashed(0) to 110,
            squashed(1) to 12,
            squashed(2) to 20,
            squashed(1) to 12,
        ))
  }

  private fun colorModel(palette: IntArray): IndexColorModel =
      IndexColorModel(8, palette.size, palette, 0, true, TRANSPARENT_INDEX, DataBuffer.TYPE_BYTE)

  /** Overworld art keeps its own palette pair beside it when it has one; battle otherwise. */
  private fun followerPalette(spriteDir: Path, name: String, battle: String): String {
    val candidate = spriteDir.resolve(name).toString()
    return if (Files.isRegularFile(expansionRoot.resolve(candidate))) candidate else battle
  }

  /**
   * Converts the Expansion's overworld strip into the follower sheet the client walks with.
   *
   * The strip is `frames x 1` cells sized by its height: 0,1 face and step south, 2,3 north, 4,5
   * west, and east is a mirror of west unless the species declares the asymmetric anim table, in
   * which case frames 6,7 are the real east. The client sheet - measured from the Gen 5 follower
   * mod - is a 4x4 grid, rows south, west, east, north, each row alternating face and step. Both
   * step frames repeat the one step the Expansion draws.
   */
  private fun followerSheet(sheet: IndexedImage, palette: IntArray, asym: Boolean): ByteArray {
    val cell = sheet.height
    val frames = sheet.width / cell
    fun cellAt(frame: Int) = frame.coerceIn(0, frames - 1)
    val east =
        if (asym && frames >= 8) listOf(Cell(6, false), Cell(7, false))
        else listOf(Cell(4, true), Cell(5, true))
    val rows =
        listOf(
            listOf(Cell(0, false), Cell(1, false)),
            listOf(Cell(4, false), Cell(5, false)),
            east,
            listOf(Cell(2, false), Cell(3, false)),
        )
    val target = BufferedImage(cell * 4, cell * 4, BufferedImage.TYPE_INT_ARGB)
    rows.forEachIndexed { rowIndex, (face, step) ->
      listOf(face, step, face, step).forEachIndexed { column, source ->
        val frame = cellAt(source.frame)
        for (y in 0 until cell) {
          for (x in 0 until cell) {
            val sourceX = frame * cell + if (source.mirrored) cell - 1 - x else x
            val index = sheet.index(sourceX, y)
            if (index != TRANSPARENT_INDEX && index < palette.size) {
              target.setRGB(column * cell + x, rowIndex * cell + y, palette[index])
            }
          }
        }
      }
    }
    return encode(target, "png")
  }

  private data class Cell(val frame: Int, val mirrored: Boolean)

  private operator fun <T> List<T>.component1(): T = this[0]

  private operator fun <T> List<T>.component2(): T = this[1]

  /** A static 4x4 grid of the party icon: not pretty, but the client never crashes over it. */
  private fun followerFromIcon(icon: IndexedImage, palette: IntArray): ByteArray {
    val target = BufferedImage(ICON * 4, ICON * 4, BufferedImage.TYPE_INT_ARGB)
    for (row in 0 until 4) {
      for (column in 0 until 4) {
        for (y in 0 until ICON) {
          for (x in 0 until ICON) {
            val index = icon.index(x, y)
            if (index != TRANSPARENT_INDEX && index < palette.size) {
              target.setRGB(column * ICON + x, row * ICON + y, palette[index])
            }
          }
        }
      }
    }
    return encode(target, "png")
  }

  /** Species whose follower art has real east-facing frames instead of mirrored west ones. */
  private fun parseAsymFollowers(): Set<String> {
    val speciesInfo = expansionRoot.resolve("src/data/pokemon/species_info")
    val result = mutableSetOf<String>()
    Files.list(speciesInfo).use { files ->
      files
          .filter { it.fileName.toString().endsWith(".h") }
          .sorted()
          .forEach { file ->
            SPECIES_ENTRY.findAll(Files.readString(file)).forEach { entry ->
              if ("sAnimTable_Following_Asym" in entry.groupValues[2]) {
                result.add(entry.groupValues[1])
              }
            }
          }
    }
    return result
  }

  /** Encodes prepared frames and their delays, in hundredths of a second, as a looping GIF. */
  private fun animatedGif(frames: List<Pair<BufferedImage, Int>>): ByteArray {
    val writer = ImageIO.getImageWritersByFormatName("gif").next()
    val bytes = ByteArrayOutputStream()
    ImageIO.createImageOutputStream(bytes).use { output ->
      writer.output = output
      writer.prepareWriteSequence(null)
      frames.forEachIndexed { index, (target, delay) ->
        val metadata =
            writer.getDefaultImageMetadata(
                javax.imageio.ImageTypeSpecifier(target), writer.defaultWriteParam)
        val format = metadata.nativeMetadataFormatName
        val root = metadata.getAsTree(format) as javax.imageio.metadata.IIOMetadataNode
        val control = childNode(root, "GraphicControlExtension")
        control.setAttribute("disposalMethod", "restoreToBackgroundColor")
        control.setAttribute("userInputFlag", "FALSE")
        control.setAttribute("transparentColorFlag", "TRUE")
        control.setAttribute("transparentColorIndex", TRANSPARENT_INDEX.toString())
        control.setAttribute("delayTime", delay.toString())
        if (index == 0) {
          val applications = childNode(root, "ApplicationExtensions")
          val loop = javax.imageio.metadata.IIOMetadataNode("ApplicationExtension")
          loop.setAttribute("applicationID", "NETSCAPE")
          loop.setAttribute("authenticationCode", "2.0")
          // Loop count zero: repeat forever, the convention every animated sprite mod uses.
          loop.userObject = byteArrayOf(1, 0, 0)
          applications.appendChild(loop)
        }
        metadata.setFromTree(format, root)
        writer.writeToSequence(javax.imageio.IIOImage(target, null, metadata), null)
      }
      writer.endWriteSequence()
    }
    writer.dispose()
    return bytes.toByteArray()
  }

  private fun childNode(
      parent: javax.imageio.metadata.IIOMetadataNode,
      name: String,
  ): javax.imageio.metadata.IIOMetadataNode {
    for (index in 0 until parent.length) {
      val child = parent.item(index)
      if (child.nodeName == name) return child as javax.imageio.metadata.IIOMetadataNode
    }
    val created = javax.imageio.metadata.IIOMetadataNode(name)
    parent.appendChild(created)
    return created
  }

  private fun gif(image: IndexedImage, palette: IntArray, size: Int): ByteArray {
    val model =
        IndexColorModel(
            8,
            palette.size,
            palette,
            0,
            true,
            TRANSPARENT_INDEX,
            DataBuffer.TYPE_BYTE,
        )
    val target = BufferedImage(size, size, BufferedImage.TYPE_BYTE_INDEXED, model)
    copy(image, target, size, 0)
    return encode(target, "gif")
  }

  private fun png(image: IndexedImage, palette: IntArray, size: Int, top: Int): ByteArray {
    val target = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until size) {
      for (x in 0 until size) {
        target.setRGB(x, y, palette[image.index(x, y + top)])
      }
    }
    return encode(target, "png")
  }

  private fun copy(image: IndexedImage, target: BufferedImage, size: Int, top: Int) {
    for (y in 0 until size) {
      for (x in 0 until size) {
        target.raster.setSample(x, y, 0, image.index(x, y + top))
      }
    }
  }

  private fun encode(image: BufferedImage, format: String): ByteArray {
    val bytes = ByteArrayOutputStream()
    check(ImageIO.write(image, format, bytes)) { "No $format encoder available" }
    return bytes.toByteArray()
  }

  private fun ZipOutputStream.directory(name: String) {
    putNextEntry(ZipEntry(name))
    closeEntry()
  }

  private fun ZipOutputStream.write(name: String, content: ByteArray) {
    putNextEntry(ZipEntry(name))
    write(content)
    closeEntry()
  }

  private class IndexedImage(val width: Int, val height: Int, private val indices: IntArray) {
    fun index(x: Int, y: Int): Int =
        if (x >= width || y >= height) TRANSPARENT_INDEX else indices[y * width + x]
  }

  private companion object {
    const val FRAME = 64
    /** Hundredths of a second the loop rests on its final pose before replaying. */
    const val REST_DELAY = 150
    const val ICON = 32
    const val TRANSPARENT_INDEX = 0
    const val SPRITES = "sprites/battlesprites"
    const val ICONS = "sprites/monstericons"
    const val FOLLOWERS = "sprites/followsprites"
    const val CRIES = "cries"
    val SPECIES_ENTRY =
        Regex(
            """\[SPECIES_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[SPECIES_[A-Z0-9_]+]\s*=|\Z)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    const val ICON_PALETTES = "graphics/pokemon/icon_palettes"
    val WHITESPACE = Regex("""\s+""")
    val INFO_XML =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <resource name="MonMMO by Lost Knights" version="1.0" description="Pokemon beyond the stock client, sourced from pokeemerald-expansion." author="Lost Knights">
        </resource>
        """
            .trimIndent()
  }
}
