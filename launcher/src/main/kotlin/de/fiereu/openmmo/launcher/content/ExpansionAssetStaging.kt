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
    /** Species whose battle sprites came from the Gen 5-style pack rather than the Expansion. */
    val packSprites: Int = 0,
    /** One `symbol,wireId,front,back` line per pack-sourced species; back may read "expansion". */
    val packReport: List<String> = emptyList(),
    /**
     * Art the pack still lacks, one `symbol,wireId,name,nationalDex,missing` line each - the list
     * to hand a sprite artist. `missing` is "front+back" or "back".
     */
    val packMissing: List<String> = emptyList(),
    /** Species whose normal front is Showdown's animated GIF. */
    val showdownAnimated: Int = 0,
)

/**
 * Converts pokeemerald-expansion sprite sources into the resource layout the client loads, and
 * packages them as a mod archive. Battle sprites use .gif and party icons use .png because that is
 * what the client's own resources.zip ships; the archive is keyed by client wire id, so it is only
 * meaningful alongside a data.pak that registers the same ids.
 */
class ExpansionAssetStaging(
    private val expansionRoot: Path,
    private val spritePack: Gen5StyleSpritePack? = null,
    private val showdown: ShowdownSprites? = null,
    private val ebs: EbsStripPack? = null,
) {

  /**
   * [allSpecies] is the whole catalogue, staged or not: a form of a retail species (a Gigantamax
   * Venusaur) finds its family's Dex number through the base entry, which is never staged itself.
   */
  fun stage(
      species: List<ExpansionSpeciesDef>,
      archive: Path,
      itemIcons: Map<Int, ByteArray> = emptyMap(),
      allSpecies: List<ExpansionSpeciesDef> = species,
  ): ExpansionAssetSummary {
    var staged = 0
    var cries = 0
    var animated = 0
    var followers = 0
    var followerFallbacks = 0
    var packSprites = 0
    var showdownAnimated = 0
    val packReport = mutableListOf<String>()
    val packMissing = mutableListOf<String>()
    val failures = linkedMapOf<String, String>()
    val anims = ExpansionFrontAnims.parse(expansionRoot)
    val asymFollowers = parseAsymFollowers()
    val bySymbol = allSpecies.associateBy { it.symbol }
    Files.createDirectories(archive.parent)
    ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
      // The client refuses an archive with no directory entries as a "flattened zip structure",
      // so every folder it will read from is declared before the files land in it.
      listOf("sprites/", "$SPRITES/", "$ICONS/", "$FOLLOWERS/", "$CRIES/", "sprites/itemicons/", "$NPC_SPRITES/", "$NPC_SPRITES/10/")
          .forEach { zip.directory(it) }
      zip.write("info.xml", INFO_XML.toByteArray())
      // Alternate block grids the server swaps in with s2c 0x2D (ClientMapFooters).
      zip.directory("world_map_footers/")
      ClientMapFooters.entries().forEach { (name, bytes) -> zip.write("world_map_footers/$name", bytes) }
      // The follower renderer slices a mod's sheets by this grid (uu1.yz1/ha1 from the loader's
      // atlasdata parse) and reads which row faces where. Without the file the grid stays 0x0,
      // every sheet sliced to nothing and the client crashed the moment an imported species
      // followed. The row order (front, left, right, back) is the Gen 5 follower mod's exactly, so its
      // descriptor is copied verbatim - the client's direction labels are its own convention.
      zip.write("$FOLLOWERS/atlasdata.txt", FOLLOWER_ATLAS.toByteArray())
      itemIcons.forEach { (itemId, png) -> zip.write("sprites/itemicons/$itemId.png", png) }
      species.forEach { entry ->
        val wireId = entry.clientWireId ?: return@forEach
        runCatching {
              val normal = palette(entry.assets.normalPalettePath)
              val shiny = palette(entry.assets.shinyPalettePath)
              val front = indexed(entry.assets.frontPicPath)
              val back = indexed(entry.assets.backPicPath)
              val icon = indexed(entry.assets.iconPath)
              // MonMMO-EX's own species (codegen/custom-species) can carry animated battle GIFs
              // and an icon palette of their own beside the decomp-style art.
              val artDir = Path.of(entry.assets.frontPicPath).parent?.let(expansionRoot::resolve)
              fun customArt(name: String): Path? = artDir?.resolve(name)?.takeIf(Files::isRegularFile)
              val customFront = customArt("monmmo_front.gif")
              val customBack = customArt("monmmo_back.gif")
              val iconPalette =
                  customArt("monmmo_icon.pal")?.let { palette(it.toString()) }
                      ?: palette("$ICON_PALETTES/pal${entry.assets.iconPalIndex}.pal")

              // anim_front.png stacks the animation frames; the Expansion's own playback script
              // turns them into an animated GIF, which the client plays as-is. Sprites the
              // Expansion keeps as a single frame - most Gen 6+ fronts, and every back - get the
              // synthesized breathing idle instead, so nothing stands frozen.
              val script = anims[entry.symbol.removePrefix("SPECIES_")].orEmpty()
              val scripted = script.size > 1 && front.height / FRAME > 1
              val packed = spritePack?.resolve(entry, bySymbol)
              val online = showdown?.resolve(entry.symbol)
              // The EBS strip pack is keyed by Dex number and carries no forms, so only the base
              // species resolves there; a form never borrows its base's strip.
              val strips = if (entry.isForm) null else ebs?.resolve(entry.nationalDexId)
              // Battle sprites, best source first per side, all Gen 5-style (owner: "animated,
              // period, all gen 5"): Showdown's Gen 5-style animated GIF, then the DeviantArt BW-style
              // animators' EBS strips (real shiny animations), then the operator's Gen 5-style still,
              // Showdown's still, and finally the Expansion's own art. Stills get the idle bounce.
              // The XY-era 3D set is not approved and is never used.
              // A custom species' own animation wins; it has no shiny art, so both sides use it.
              val frontN =
                  customFront?.let { reencodeGif(it) to CUSTOM }
                      ?: online?.aniFront?.let { reencodeGif(it) to ANI }
                      ?: strips?.front?.let { stripGif(it) to EBS }
                      ?: packed?.front?.let { packGif(it) to PACK }
                      ?: online?.front?.let { packGif(it) to SHOWDOWN }
                      ?: (if (scripted) scriptedGif(front, normal, FRAME, script)
                      else idleGif(front, normal, FRAME)) to EXPANSION
              val frontS =
                  customFront?.let { reencodeGif(it) to CUSTOM }
                      ?: online?.let { o ->
                    if (o.aniFront != null && o.front != null && o.frontShiny != null)
                        shinyGif(o.aniFront, o.front, o.frontShiny) to ANI_SHINY
                    else null
                  }
                      ?: strips?.frontShiny?.let { stripGif(it) to EBS }
                      ?: packed?.frontShiny?.let { packGif(it) to PACK }
                      ?: online?.frontShiny?.let { packGif(it) to SHOWDOWN }
                      ?: (if (scripted) scriptedGif(front, shiny, FRAME, script)
                      else idleGif(front, shiny, FRAME)) to EXPANSION
              val backN =
                  customBack?.let { reencodeGif(it) to CUSTOM }
                      ?: online?.aniBack?.let { reencodeGif(it) to ANI }
                      ?: strips?.back?.let { stripGif(it) to EBS }
                      ?: packed?.back?.let { packGif(it) to PACK }
                      ?: online?.back?.let { packGif(it) to SHOWDOWN }
                      ?: idleGif(back, normal, FRAME) to EXPANSION
              val backS =
                  customBack?.let { reencodeGif(it) to CUSTOM }
                      ?: online?.let { o ->
                    if (o.aniBack != null && o.back != null && o.backShiny != null)
                        shinyGif(o.aniBack, o.back, o.backShiny) to ANI_SHINY
                    else null
                  }
                      ?: strips?.backShiny?.let { stripGif(it) to EBS }
                      ?: packed?.backShiny?.let { packGif(it) to PACK }
                      ?: online?.backShiny?.let { packGif(it) to SHOWDOWN }
                      ?: idleGif(back, shiny, FRAME) to EXPANSION
              zip.write("$SPRITES/$wireId-front-n.gif", frontN.first)
              zip.write("$SPRITES/$wireId-front-s.gif", frontS.first)
              zip.write("$SPRITES/$wireId-back-n.gif", backN.first)
              zip.write("$SPRITES/$wireId-back-s.gif", backS.first)
              val sources = listOf(frontN.second, frontS.second, backN.second, backS.second)
              if (frontN.second == EXPANSION && scripted) animated++
              if (frontN.second == ANI || frontN.second == EBS || frontN.second == CUSTOM) showdownAnimated++
              if (sources.any { it != EXPANSION }) packSprites++
              packReport += "${entry.symbol},$wireId,${sources.joinToString(",")}"
              if (spritePack != null || showdown != null) {
                val lacking =
                    listOf("front", "frontShiny", "back", "backShiny").filterIndexed { index, _ ->
                      sources[index] == EXPANSION
                    }
                if (lacking.isNotEmpty()) {
                  packMissing +=
                      "${entry.symbol},$wireId,${entry.displayName},${entry.nationalDexId ?: 0}," +
                          lacking.joinToString("+")
                }
              }
              // icon.png stacks the two idle-bounce frames the party UI alternates between.
              zip.write("$ICONS/$wireId-0.png", png(icon, iconPalette, ICON, 0))
              zip.write("$ICONS/$wireId-1.png", png(icon, iconPalette, ICON, ICON))
              // The shiny icon (client name form ID-FRAME-s.png, f/c85; without it a shiny falls
              // back to the normal icon). The Expansion has no shiny icon art - icons draw from six
              // shared palettes - so the icon is recoloured through the species' own normal -> shiny
              // sprite palettes: each icon colour takes the shiny counterpart of the nearest normal
              // sprite colour. A custom species may ship monmmo_icon_shiny.pal instead.
              val shinyIconPalette =
                  customArt("monmmo_icon_shiny.pal")?.let { palette(it.toString()) }
                      ?: shinyIconPalette(iconPalette, normal, shiny)
              zip.write("$ICONS/$wireId-0-s.png", png(icon, shinyIconPalette, ICON, 0))
              zip.write("$ICONS/$wireId-1-s.png", png(icon, shinyIconPalette, ICON, ICON))

              // A custom species that stands in the overworld as a plain file-sprite npc names its
              // sprite slot in monmmo_npc_sprite.txt as "region id" (the Crystal Onix raid boss no
              // longer does: it is drawn with its follower sheet, CrystalOnixRaidPlacement). The
              // client reads overworldsprites/<region>/<id>-<F>.png, F the GBA frame (0 face
              // south, 1 north, 2 west, 3-4 / 5-6 / 7-8 walking); the strip is the Expansion's
              // overworld layout, cells 0,1 south, 2,3 north, 4,5 west.
              customArt("monmmo_npc_sprite.txt")?.let { slotFile ->
                val (region, spriteId) =
                    Files.readString(slotFile).trim().split(WHITESPACE).map(String::toInt)
                val strip = indexed(artDir!!.resolve("overworld.png").toString())
                val colours = palette(artDir.resolve("overworld_normal.pal").toString())
                NPC_FRAME_CELLS.forEachIndexed { frame, cell ->
                  zip.write("$NPC_SPRITES/$region/$spriteId-$frame.png", overworldFrame(strip, colours, cell))
                }
              }

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
        staged,
        species.size - staged,
        cries,
        animated,
        followers,
        followerFallbacks,
        failures,
        packSprites,
        packReport,
        packMissing,
        showdownAnimated,
    )
  }

  /**
   * A pack still as a looping idle GIF. The PNG is already indexed with its own transparent entry;
   * its opaque colours are re-indexed from 1 so index 0 can be the GIF's transparent colour, the
   * convention every other sprite here follows.
   */
  /**
   * Rewrites an animated GIF into the structure the client animates: every frame a full logical
   * screen at 0,0 with restore-to-background disposal, one global palette, no interlacing - the
   * shape of every GIF in the HD battle sprite mod. Showdown's GIFs store each frame as an offset
   * sub-rectangle with restore-to-previous disposal, and the client showed those as a still.
   */
  private fun reencodeGif(path: Path): ByteArray = quantisedGif(decodeGif(path))

  /**
   * An EBS battler strip (see [EbsStripPack]) as the client's animated GIF: square frames, frame
   * side = strip height, read left to right. Elite Battle System steps these strips at the game's
   * 20 fps, so every frame gets 5 cs - the same pace as Showdown's Gen 5-style GIFs.
   */
  private fun stripGif(path: Path): ByteArray {
    val strip = ImageIO.read(path.toFile()) ?: error("Unreadable EBS strip: $path")
    val side = strip.height
    val frames = maxOf(1, strip.width / side)
    // The strips are cropped tight (Chespin is a 64px frame); the rest of the mod is on Showdown's
    // 96x96 canvas, so each frame is centred on at least that, and a bigger frame keeps its size.
    // Pixel scale is left as drawn: the animators worked at 1.1-1.4x the BW scale and a fractional
    // downscale would destroy the pixel art (measured 2026-09-19 against the Showdown stills).
    val canvas = maxOf(SHOWDOWN_CANVAS, side)
    val offset = (canvas - side) / 2
    return quantisedGif(
        List(frames) { index ->
          val frame = BufferedImage(canvas, canvas, BufferedImage.TYPE_INT_ARGB)
          val source = strip.getSubimage(index * side, 0, side, side)
          for (y in 0 until side) for (x in 0 until side) frame.setRGB(x + offset, y + offset, source.getRGB(x, y))
          frame to EBS_FRAME_DELAY
        })
  }

  /**
   * An animated SHINY from the normal animation: Gen 5 shinies are palette swaps of the same
   * drawing, so the normal and shiny stills pair up pixel for pixel into a colour map, and that map
   * recolours every frame of the normal animation. Showdown ships no shiny animations at all, and
   * a shiny monster's summary and battle sprite would otherwise fall back to a still.
   */
  private fun shinyGif(animation: Path, still: Path, shinyStill: Path): ByteArray {
    val normal = ImageIO.read(still.toFile()) ?: error("Unreadable still: $still")
    val shiny = ImageIO.read(shinyStill.toFile()) ?: error("Unreadable shiny still: $shinyStill")
    val votes = mutableMapOf<Int, MutableMap<Int, Int>>()
    for (y in 0 until minOf(normal.height, shiny.height)) {
      for (x in 0 until minOf(normal.width, shiny.width)) {
        val from = normal.getRGB(x, y)
        val to = shiny.getRGB(x, y)
        if (from ushr 24 < 128 || to ushr 24 < 128) continue
        votes.getOrPut(from or (0xff shl 24)) { mutableMapOf() }.merge(to or (0xff shl 24), 1, Int::plus)
      }
    }
    val map = votes.mapValues { (_, targets) -> targets.maxByOrNull { it.value }!!.key }
    val known = map.keys.toList()
    fun recolour(argb: Int): Int {
      map[argb]?.let {
        return it
      }
      if (known.isEmpty()) return argb
      var best = known[0]
      var bestDistance = Int.MAX_VALUE
      for (candidate in known) {
        val dr = ((argb shr 16) and 0xff) - ((candidate shr 16) and 0xff)
        val dg = ((argb shr 8) and 0xff) - ((candidate shr 8) and 0xff)
        val db = (argb and 0xff) - (candidate and 0xff)
        val distance = dr * dr + dg * dg + db * db
        if (distance < bestDistance) {
          bestDistance = distance
          best = candidate
        }
      }
      return map.getValue(best)
    }
    val frames =
        decodeGif(animation).map { (image, delay) ->
          val out = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
          for (y in 0 until image.height) {
            for (x in 0 until image.width) {
              val argb = image.getRGB(x, y)
              out.setRGB(x, y, if (argb ushr 24 < 128) 0 else recolour(argb or (0xff shl 24)))
            }
          }
          out to delay
        }
    return quantisedGif(frames)
  }

  /** Composites every frame of a GIF onto its logical screen, honouring offsets and disposal. */
  private fun decodeGif(path: Path): List<Pair<BufferedImage, Int>> {
    val reader = ImageIO.getImageReadersByFormatName("gif").next()
    val composed = mutableListOf<Pair<BufferedImage, Int>>()
    ImageIO.createImageInputStream(path.toFile()).use { input ->
      reader.input = input
      val stream = reader.getStreamMetadata()
      val screen = childNode(stream.getAsTree(stream.nativeMetadataFormatName) as javax.imageio.metadata.IIOMetadataNode, "LogicalScreenDescriptor")
      val width = screen.getAttribute("logicalScreenWidth").toInt()
      val height = screen.getAttribute("logicalScreenHeight").toInt()
      var canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      val frames = reader.getNumImages(true)
      for (index in 0 until frames) {
        val meta = reader.getImageMetadata(index)
        val root = meta.getAsTree(meta.nativeMetadataFormatName) as javax.imageio.metadata.IIOMetadataNode
        val descriptor = childNode(root, "ImageDescriptor")
        val control = childNode(root, "GraphicControlExtension")
        val left = descriptor.getAttribute("imageLeftPosition").toIntOrNull() ?: 0
        val top = descriptor.getAttribute("imageTopPosition").toIntOrNull() ?: 0
        val delay = maxOf(2, control.getAttribute("delayTime").toIntOrNull() ?: 10)
        val disposal = control.getAttribute("disposalMethod")
        val before = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        before.graphics.drawImage(canvas, 0, 0, null)
        val frame = reader.read(index)
        canvas.createGraphics().apply {
          drawImage(frame, left, top, null)
          dispose()
        }
        val snapshot = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        snapshot.graphics.drawImage(canvas, 0, 0, null)
        composed += snapshot to delay
        when (disposal) {
          "restoreToPrevious" -> canvas = before
          "restoreToBackgroundColor" -> {
            val g = canvas.createGraphics()
            g.composite = java.awt.AlphaComposite.Clear
            g.fillRect(left, top, frame.width, frame.height)
            g.dispose()
          }
        }
      }
    }
    reader.dispose()
    return composed
  }

  /** Full-frame ARGB images to a looping GIF on one shared palette (index 0 transparent). */
  private fun quantisedGif(frames: List<Pair<BufferedImage, Int>>): ByteArray {
    val counts = mutableMapOf<Int, Int>()
    frames.forEach { (image, _) ->
      for (y in 0 until image.height) {
        for (x in 0 until image.width) {
          val argb = image.getRGB(x, y)
          if (argb ushr 24 >= 128) counts.merge(argb or (0xff shl 24), 1, Int::plus)
        }
      }
    }
    val kept = counts.entries.sortedByDescending { it.value }.take(255).map { it.key }
    val palette = IntArray(kept.size + 1)
    kept.forEachIndexed { index, argb -> palette[index + 1] = argb }
    val exact = kept.withIndex().associate { (index, argb) -> argb to index + 1 }
    fun nearest(argb: Int): Int {
      exact[argb]?.let {
        return it
      }
      var best = 1
      var bestDistance = Int.MAX_VALUE
      for (index in 1..kept.size) {
        val candidate = palette[index]
        val dr = ((argb shr 16) and 0xff) - ((candidate shr 16) and 0xff)
        val dg = ((argb shr 8) and 0xff) - ((candidate shr 8) and 0xff)
        val db = (argb and 0xff) - (candidate and 0xff)
        val distance = dr * dr + dg * dg + db * db
        if (distance < bestDistance) {
          bestDistance = distance
          best = index
        }
      }
      return best
    }
    val model = colorModel(palette)
    return animatedGif(
        frames.map { (image, delay) ->
          val target = BufferedImage(image.width, image.height, BufferedImage.TYPE_BYTE_INDEXED, model)
          for (y in 0 until image.height) {
            for (x in 0 until image.width) {
              val argb = image.getRGB(x, y)
              target.raster.setSample(
                  x, y, 0, if (argb ushr 24 < 128) TRANSPARENT_INDEX else nearest(argb or (0xff shl 24)))
            }
          }
          target to delay
        })
  }

  private fun packGif(path: Path): ByteArray {
    val image = ImageIO.read(path.toFile()) ?: error("Unreadable pack sprite: $path")
    // A GIF frame holds 255 colours plus transparency. Some Showdown stills are true-colour PNGs
    // with anti-aliased edges; those are quantised by dropping low bits per channel until they fit.
    for (keepBits in intArrayOf(8, 6, 5, 4)) {
      val mask = (0xff shl (8 - keepBits)) and 0xff
      val colors = linkedMapOf<Int, Int>()
      val indices = IntArray(image.width * image.height)
      var overflow = false
      loop@ for (y in 0 until image.height) {
        for (x in 0 until image.width) {
          val argb = image.getRGB(x, y)
          if (argb ushr 24 < 128) {
            indices[y * image.width + x] = TRANSPARENT_INDEX
            continue
          }
          val quantised =
              (0xff shl 24) or
                  (((argb shr 16) and mask) shl 16) or
                  (((argb shr 8) and mask) shl 8) or
                  (argb and mask)
          val index = colors.getOrPut(quantised) { colors.size + 1 }
          if (index > 255) {
            overflow = true
            break@loop
          }
          indices[y * image.width + x] = index
        }
      }
      if (overflow) continue
      val palette = IntArray(colors.size + 1)
      colors.forEach { (argb, index) -> palette[index] = argb }
      return idleGif(IndexedImage(image.width, image.height, indices), palette, maxOf(image.width, image.height))
    }
    // Still too many (gradient-heavy Tera masks): keep the 255 most used colours and snap the
    // rest to the nearest of them.
    val counts = mutableMapOf<Int, Int>()
    for (y in 0 until image.height) {
      for (x in 0 until image.width) {
        val argb = image.getRGB(x, y)
        if (argb ushr 24 >= 128) counts.merge(argb or (0xff shl 24), 1, Int::plus)
      }
    }
    val kept = counts.entries.sortedByDescending { it.value }.take(255).map { it.key }
    val palette = IntArray(kept.size + 1)
    kept.forEachIndexed { index, argb -> palette[index + 1] = argb }
    fun nearest(argb: Int): Int {
      var best = 1
      var bestDistance = Int.MAX_VALUE
      for (index in 1..kept.size) {
        val candidate = palette[index]
        val dr = ((argb shr 16) and 0xff) - ((candidate shr 16) and 0xff)
        val dg = ((argb shr 8) and 0xff) - ((candidate shr 8) and 0xff)
        val db = (argb and 0xff) - (candidate and 0xff)
        val distance = dr * dr + dg * dg + db * db
        if (distance < bestDistance) {
          bestDistance = distance
          best = index
        }
      }
      return best
    }
    val indices = IntArray(image.width * image.height)
    for (y in 0 until image.height) {
      for (x in 0 until image.width) {
        val argb = image.getRGB(x, y)
        indices[y * image.width + x] =
            if (argb ushr 24 < 128) TRANSPARENT_INDEX else nearest(argb or (0xff shl 24))
      }
    }
    return idleGif(IndexedImage(image.width, image.height, indices), palette, maxOf(image.width, image.height))
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
   * The icon palette recoloured the way the species' sprite goes shiny: every icon colour (index 0
   * stays transparent) is replaced by the shiny palette entry that pairs with the closest normal
   * palette entry, closeness being the plain RGB distance. Where the sprite keeps a colour (black
   * outlines, whites, eyes) the icon keeps it too, because that colour maps onto itself.
   */
  private fun shinyIconPalette(iconPalette: IntArray, normal: IntArray, shiny: IntArray): IntArray =
      IntArray(iconPalette.size) { index ->
        if (index == 0) return@IntArray 0
        val colour = iconPalette[index]
        val nearest =
            (1 until minOf(normal.size, shiny.size)).minByOrNull { rgbDistance(colour, normal[it]) }
        if (nearest == null) colour else shiny[nearest]
      }

  private fun rgbDistance(a: Int, b: Int): Int {
    val dr = ((a shr 16) and 0xff) - ((b shr 16) and 0xff)
    val dg = ((a shr 8) and 0xff) - ((b shr 8) and 0xff)
    val db = (a and 0xff) - (b and 0xff)
    return dr * dr + dg * dg + db * db
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

  /** One square cell of an overworld strip as a transparent PNG. */
  private fun overworldFrame(strip: IndexedImage, palette: IntArray, cell: Int): ByteArray {
    val size = strip.height
    val target = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until size) {
      for (x in 0 until size) {
        val index = strip.index(cell * size + x, y)
        if (index != TRANSPARENT_INDEX && index < palette.size) target.setRGB(x, y, palette[index])
      }
    }
    return encode(target, "png")
  }

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
        // Non-interlaced, like every GIF the client is known to animate.
        childNode(root, "ImageDescriptor").setAttribute("interlaceFlag", "FALSE")
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
    /** Sprite source labels in the staging report. */
    const val ANI = "showdown-ani"
    const val ANI_SHINY = "showdown-ani-recoloured"
    const val EBS = "ebs-bw-animated"
    /** The species' own animation (codegen/custom-species, e.g. Crystal Onix). */
    const val CUSTOM = "monmmo-custom"
    /** Centiseconds per EBS strip frame (20 fps). */
    const val EBS_FRAME_DELAY = 5
    /** Showdown's Gen 5-style sprites are all 96x96; EBS frames are centred on the same. */
    const val SHOWDOWN_CANVAS = 96
    const val PACK = "pack"
    const val SHOWDOWN = "showdown"
    const val EXPANSION = "expansion"
    const val ICONS = "sprites/monstericons"
    const val FOLLOWERS = "sprites/followsprites"
    const val NPC_SPRITES = "sprites/overworldsprites"
    /** Strip cell for each GBA frame 0-8: face S/N/W, then the step cells of S, N, W twice. */
    val NPC_FRAME_CELLS = listOf(0, 2, 4, 1, 1, 3, 3, 5, 5)
    const val CRIES = "cries"
    val SPECIES_ENTRY =
        Regex(
            """\[SPECIES_([A-Z0-9_]+)]\s*=\s*\{(.*?)(?=\n\s*\[SPECIES_[A-Z0-9_]+]\s*=|\Z)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    const val ICON_PALETTES = "graphics/pokemon/icon_palettes"
    /** Same format as the Gen 5 follower mod's descriptor; cell indices run row-major. */
    val FOLLOWER_ATLAS =
        """
        rows=4
        columns=4

        north=0,1,2,3
        south=12,13,14,15
        west=4,5,6,7
        east=8,9,10,11
        """
            .trimIndent()
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
