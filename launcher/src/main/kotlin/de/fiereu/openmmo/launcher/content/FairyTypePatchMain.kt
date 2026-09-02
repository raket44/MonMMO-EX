@file:JvmName("FairyTypePatchMain")

package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Writes the patched client classes into an overlay jar. Nothing in the client is modified: the jar
 * goes ahead of the client on the classpath, so the JVM loads these classes instead of the packaged
 * ones. The 50MB executable stays pristine and the change is undone by deleting one file.
 *
 * A **jar**, not a directory. The client holds four classes whose names differ only in case - RP0,
 * Rp0, rP0 and rp0 - and a Windows directory is case-insensitive, so one file there answers lookups
 * for all four. The JVM then asks for f/rP0, is handed f/Rp0 and refuses it outright with
 * NoClassDefFoundError, taking the client down before it draws a frame. Zip entries are
 * case-sensitive, so the same file in a jar shadows only itself.
 */
fun main(args: Array<String>) {
  require(args.size == 3) { "Usage: <client-executable> <expansion-root> <overlay-jar>" }
  val client = Path.of(args[0])
  val expansionRoot = Path.of(args[1])
  val overlay = Path.of(args[2])
  require(Files.isRegularFile(client)) { "Client executable not found: $client" }
  require(Files.isDirectory(expansionRoot)) { "Expansion root not found: $expansionRoot" }

  val chart = TypeChart.parse(expansionRoot)
  val patched = linkedMapOf<String, ByteArray>()
  var typeCount = 0

  ZipFile(client.toFile()).use { archive ->
    val classes = archive.entries().asSequence().filter { it.name.endsWith(".class") }.toList()

    val typeEnum =
        classes.firstOrNull { candidate ->
          runCatching {
                val bytes = archive.getInputStream(candidate).use { it.readBytes() }
                FairyTypePatch.inspect(bytes).constants.map { it.first }.containsAll(TYPES)
              }
              .getOrDefault(false)
        } ?: error("No type enum in $client; the client may be packaged differently")
    val typeBytes = archive.getInputStream(typeEnum).use { it.readBytes() }
    val patchedEnum = FairyTypePatch.patch(typeBytes, chart)
    typeCount = FairyTypePatch.inspect(patchedEnum).constants.size
    patched[typeEnum.name] = patchedEnum

    // The badge tables are sized independently of the enum, so widening one without the other
    // leaves Fairy with no icon even though the type itself works.
    val badgeLoader =
        classes.firstOrNull { candidate ->
          runCatching {
                BadgeArrayPatch.isBadgeLoader(
                    archive.getInputStream(candidate).use { it.readBytes() })
              }
              .getOrDefault(false)
        }
    if (badgeLoader == null) {
      println("[fairy-type] no badge loader found; type icons will fall back to a default")
    } else {
      val badgeBytes = archive.getInputStream(badgeLoader).use { it.readBytes() }
      val before = BadgeArrayPatch.arraySizes(badgeBytes)
      val patchedBadges = BadgeArrayPatch.patch(badgeBytes, typeCount)
      patched[badgeLoader.name] = patchedBadges
      println(
          "[fairy-type] badge tables ${before} -> ${BadgeArrayPatch.arraySizes(patchedBadges)} " +
              "in ${badgeLoader.name}")
    }

    // The Pokedex reads a copy of the registry that the client fills with ids 1-649 only, which is
    // why it ends at Unova. Raise that copy rather than redirecting the reader: the same method
    // rebuilds the registry it copies from, so reading it directly races that rebuild and leaves
    // the Pokedex empty.
    val dexMap =
        classes.firstOrNull { candidate ->
          runCatching {
                PokedexRangePatch.isSpeciesRegistry(
                    archive.getInputStream(candidate).use { it.readBytes() })
              }
              .getOrDefault(false)
        }
    if (dexMap == null) {
      println("[fairy-type] no species registry found; the Pokedex will stay stale")
    } else {
      val bytes = archive.getInputStream(dexMap).use { it.readBytes() }
      patched[dexMap.name] = PokedexRangePatch.patch(bytes)
      println("[fairy-type] Pokedex map refreshed on open in ${dexMap.name}")
    }

    // The four places the client stops at five regions. They move together or a new tab either
    // never renders or renders empty.
    val newRegions = NEW_POKEDEX_REGIONS
    val totalRegions = PokedexRegionPatch.STOCK_REGIONS + newRegions.size

    fun applyOne(label: String, matches: (ByteArray) -> Boolean, apply: (ByteArray) -> ByteArray) {
      val entry =
          classes.firstOrNull { candidate ->
            runCatching { matches(archive.getInputStream(candidate).use { it.readBytes() }) }
                .getOrDefault(false)
          }
      if (entry == null) {
        println("[fairy-type] $label not found; the Pokedex will keep its stock regions")
        return
      }
      val bytes = patched[entry.name] ?: archive.getInputStream(entry).use { it.readBytes() }
      patched[entry.name] = apply(bytes)
      println("[fairy-type] $label patched in ${entry.name}")
    }

    // The national dex number lives at slot 5, and the National tab reaches it through the array's
    // last element. Pin that read to the slot before widening, or growing the array moves it.
    applyOne(
        "national dex slot",
        NationalSlotPatch::isSpeciesRecord,
        { NationalSlotPatch.patch(it, NationalSlotPatch.NATIONAL_SLOT) },
    )
    applyOne(
        "per-species region slots",
        PokedexRegionPatch::isSpeciesRecord,
        { PokedexRegionPatch.patchRegionSlots(it, totalRegions + 1) },
    )
    applyOne(
        "region display order",
        PokedexRegionPatch::isRegionTable,
        { PokedexRegionPatch.patchRegionTable(it, newRegions) },
    )
    applyOne(
        "Pokedex tab loop",
        PokedexRegionPatch::isPokedexScreen,
        { PokedexRegionPatch.patchTabLoop(it, totalRegions) },
    )
    applyOne(
        "region availability gate",
        PokedexRegionPatch::isRegionGate,
        { PokedexRegionPatch.patchRegionGate(it, newRegions) },
    )

    // Diagnostic: four builds have been shipped on static reading and the client disagreed each
    // time, so make it report what region each tab actually asks for and how many species answer.
    applyOne(
        "Pokedex diagnostic logging",
        PokedexDiagnosticPatch::isListBuilder,
        PokedexDiagnosticPatch::patch,
    )

    // Second probe: the list is measured and correct, the tabs are still blank, so count what the
    // screen actually stores to draw.
    applyOne(
        "Pokedex screen probe",
        PokedexScreenProbe::isScreen,
        PokedexScreenProbe::patch,
    )

    // The availability gate: species outside a baked-in table are dropped from the dex outright.
    // Imported species can never be in that table, so the gate goes.
    applyOne(
        "Pokedex availability gate removed",
        PokedexAvailabilityPatch::isPokedexScreen,
        PokedexAvailabilityPatch::patch,
    )

    // A scissor pop with nothing pushed - a battle frame drawn while the window is minimized -
    // was a fatal render error; the guard makes it a no-op instead.
    applyOne(
        "Scissor stack pop guarded",
        ScissorGuardPatch::isScissorStack,
        ScissorGuardPatch::patch,
    )

    // Section 6 is set-only for the hide flags, so every imported species is constructed hidden
    // and stays hidden. The screen's four flag-skips go instead.
    applyOne(
        "Pokedex hide-flag skips removed",
        PokedexHideFlagPatch::isPokedexScreen,
        PokedexHideFlagPatch::patch,
    )

    // Probe: when a detail page opens, log what the client holds for that species.
    applyOne(
        "Pokedex detail probe",
        PokedexDetailProbe::isDetailHost,
        PokedexDetailProbe::patch,
    )

    // The data.pak loader gets a call to our fixup helper at its end - the only moment both the
    // ROM registry and every data.pak section are final.
    applyOne(
        "dex fixup hook",
        DexFixupHookPatch::isLoader,
        DexFixupHookPatch::patch,
    )

    // Section 10 defaults every species it creates to hidden, and no data can clear it, so the
    // Pokedex drew nothing for ours. Flip that default in the loader.
    applyOne(
        "species visibility default",
        PokedexVisibilityPatch::isLoader,
        PokedexVisibilityPatch::patch,
    )

    // The NDS interiors that never appear (Cold Storage, Pinwheel inside, Chargestone) fail
    // somewhere inside the LoadMap apply with no logging at all; this prints the packet and any
    // throwable to the console log.
    applyOne(
        "LoadMap apply diagnostic",
        MapLoadDiagnosticPatch::isLoadMapPacket,
        MapLoadDiagnosticPatch::patch,
    )

    // The LoadMap apply is clean for the broken interiors, so probe the next step: the entity
    // placement that actually switches the client onto the new map.
    applyOne(
        "LoadEntity apply diagnostic",
        LoadEntityDiagnosticPatch::isLoadEntityPacket,
        LoadEntityDiagnosticPatch::patch,
    )

    applyOne(
        "Gen 5 converter diagnostic",
        ConvertDiagnosticPatch::isConverter,
        ConvertDiagnosticPatch::patch,
    )

    applyOne(
        "Unova map build stage markers",
        K90DiagnosticPatch::isUnovaMap,
        K90DiagnosticPatch::patch,
    )

    applyOne(
        "silent packet-drop diagnostic",
        PacketDropDiagnosticPatch::isPacketRegistry,
        PacketDropDiagnosticPatch::patch,
    )

    applyOne(
        "movement rail-line bits",
        MovementRailLinePatch::isMovementSender,
        MovementRailLinePatch::patch,
    )
  }

  Files.createDirectories(overlay.parent)
  ZipOutputStream(Files.newOutputStream(overlay)).use { jar ->
    patched.forEach { (name, bytes) ->
      jar.putNextEntry(ZipEntry(name))
      jar.write(bytes)
      jar.closeEntry()
    }
    // The fixup helper rides in the overlay too: a real class, reached from a one-instruction
    // hook, editing the live registry instead of replacing records - replacement is what cost the
    // retyped species their evolution chains. It ships as source and is compiled here for the
    // client's own runtime: the bundled JRE is Java 17, and a class file from a newer JDK is
    // rejected at load with a fatal launch error, which happened.
    val helperName = "monmmo/DexPatch.class"
    jar.putNextEntry(ZipEntry(helperName))
    jar.write(compileHelperForClientRuntime("DexPatch"))
    jar.closeEntry()
    // The LoadMap diagnostic's file-backed logger; javaw discards System.err.
    jar.putNextEntry(ZipEntry("monmmo/MapLog.class"))
    jar.write(compileHelperForClientRuntime("MapLog"))
    jar.closeEntry()
    val expansion = ExpansionSpeciesRegistry().all()
    val wireBySymbol =
        expansion
            .filter { it.clientWireId != null }
            .associate { it.symbol.removePrefix("SPECIES_") to it.clientWireId!! }
    val retypeLines =
        expansion
            .filter { !it.isNewToClient && !it.isForm && "TYPE_FAIRY" in it.typeSymbols }
            .mapNotNull { entry ->
              val wire = entry.clientWireId ?: return@mapNotNull null
              val types = entry.typeSymbols.map(::overlayClientType)
              "retype:$wire:${types[0]}:${types.getOrElse(1) { types[0] }}"
            }
    // MOVE types for every canonical move, from the Expansion tables: the client's own 1-559 get
    // their types from the ROMs, which predate Fairy - Sweet Kiss and Charm rendered Normal while
    // the species around them were retyped. The helper stamps the Expansion type onto the live
    // move registry after load, the same moment the species retypes land. Emitted for all 559
    // rather than a diff: the client's per-move baseline lives in the ROMs where we do not read.
    val moveRoot = Path.of(args[1])
    val parsedMoves = MoveText.parse(moveRoot, MoveText.ids(moveRoot))
    val moveTypeLines =
        parsedMoves
            .filter { it.id in 1..559 }
            .map { "movetype:${it.id}:${overlayClientType(it.type)}" }
    // Battle animations for the imported moves (operator-directed): each new move aliases a
    // retail move's animation factory in the client's registry. The donor is the retail move of
    // the same type, preferring the same damage category and the closest power, so a Fairy
    // special attack sparkles rather than falling back to the generic thump. Handpicked
    // improvements can override any line later; this guarantees no imported move animates blank.
    val retailMoves = parsedMoves.filter { it.id in 1..559 }
    val moveAnimLines =
        parsedMoves
            .filter { it.id in 560..999 }
            .mapNotNull { move ->
              val sameType = retailMoves.filter { it.type == move.type }
              val pool = sameType.filter { it.category == move.category }.ifEmpty { sameType }
              val donor = pool.minByOrNull { kotlin.math.abs(it.power - move.power) * 1000 + it.id }
              donor?.let { "moveanim:${move.id}:${it.id}" }
            }
    // Evolution entries where either end is a species we add; chains fully inside the canonical
    // range already come from the ROM. Eevee to Sylveon starts from a canonical species, which an
    // ours-only pass silently skipped.
    val evolutions = ExpansionEvolutions.parse(Path.of(args[1]))
    // "New" now includes the staged forms in the 1079+ wire block, so Alolan chains - Vulpix
    // (Alola) to Ninetales (Alola) by Ice Stone - link like any other imported line.
    val newSymbols =
        expansion
            .filter { entry ->
              val wire = entry.clientWireId ?: return@filter false
              entry.isNewToClient || wire >= 1079
            }
            .map { it.symbol.removePrefix("SPECIES_") }
            .toSet()
    val evoLines =
        wireBySymbol.keys.flatMap { symbol ->
          val from = wireBySymbol.getValue(symbol)
          (evolutions[symbol] ?: emptyList()).mapNotNull { evo ->
            val to = wireBySymbol[evo.targetSymbol] ?: return@mapNotNull null
            if (symbol !in newSymbols && evo.targetSymbol !in newSymbols) return@mapNotNull null
            "evo:$from:${evo.method}:${evo.param}:$to"
          }
        }
    // A tool for every taught move, so the taught tab never labels a row "TM??". The helper only
    // creates the ones no existing tool covers, and each new tool is named by its move's own
    // string, so Growl's row reads Growl rather than a tool number that does not exist.
    val expansionRootPath = Path.of(args[1])
    // TM items for every taught move an existing tool does not cover. The helper clones a real
    // gen-5 TM as the donor - right kind and flags - names it from the staged string, and files it
    // in the free id space above PokeMMO..s own tools. Same deterministic plan as the string pass.
    val toolLines =
        TmPlan.taughtMoves(expansionRootPath).mapIndexed { index, moveId ->
          "tmitem:$moveId:${TmPlan.NAME_STRING_BASE + index}:${TmPlan.FIRST_ITEM_ID + index}"
        } +
            EvoItemPlan.ITEMS.mapIndexed { index, (_, _) ->
              "evoitem:${EvoItemPlan.FIRST_ITEM_ID + index}:" +
                  "${EvoItemPlan.NAME_STRING_BASE + index}:${EvoItemPlan.DONOR_ITEM_ID}:" +
                  "${EvoItemPlan.DESC_STRING_BASE + index}"
            } +
            ItemImportPlan.compute(expansionRootPath).mapIndexed { index, item ->
              "evoitem:${item.itemId}:${ItemImportPlan.NAME_STRING_BASE + index}:" +
                  "${item.donorId}:${ItemImportPlan.DESC_STRING_BASE + index}"
            }
    // The same calibrated evolution entries the client gets, exported for the server: the
    // use-item handler resolves stone evolutions from this file, so both sides always agree.
    val evolutionsCsv = Path.of("build/expansion-client/evolutions.csv")
    Files.createDirectories(evolutionsCsv.parent)
    Files.write(evolutionsCsv, evoLines.map { it.removePrefix("evo:") })

    val dumpLines = listOf("locations", "dump:7", "dump:133", "dumptools", "dumpitems")
    val fixups = retypeLines + moveTypeLines + moveAnimLines + evoLines + toolLines + dumpLines
    // One wild-location table per season, installed at load by the season the world is in. The
    // dex row format has no season field, so a season is a whole table rather than a flag.
    var locationBytes = 0
    WildLocationsSection.SeasonFilter.entries
        .filter { it != WildLocationsSection.SeasonFilter.ANY }
        .forEach { season ->
          val built = WildLocationsSection.build(season)
          jar.putNextEntry(ZipEntry("monmmo/locations-${season.name.lowercase()}.bin"))
          jar.write(built.payload)
          jar.closeEntry()
          locationBytes += built.payload.size
        }
    println("[fairy-type] wild-location tables packed: 4 seasons, $locationBytes bytes")

    jar.putNextEntry(ZipEntry("monmmo/fixups.txt"))
    jar.write(fixups.joinToString("\n").toByteArray())
    jar.closeEntry()
    println(
        "[fairy-type] fixup helper packed: ${retypeLines.size} retypes, ${evoLines.size} evolutions, " +
            "${toolLines.size} tool candidates")
    if (ExpansionEvolutions.unmappedItems.isNotEmpty()) {
      println(
          "[fairy-type] evolution items awaiting import: " +
              ExpansionEvolutions.unmappedItems.joinToString())
    }
  }
  println("[fairy-type] $typeCount types, wrote ${patched.size} classes to $overlay")
  patched.keys.forEach { println("[fairy-type]   $it") }
}

/** Enough of the type list to identify the enum without relying on obfuscated names. */
private val TYPES = listOf("NORMAL", "FIGHTING", "DRAGON", "DARK", "STEEL", "PSYCHIC")

/**
 * The regions the imported species are filed under, continuing the client's own numbering.
 *
 * Kalos, Alola, Galar and Paldea - the four national dex ranges the new species fall into, with
 * 183, 148, 197 and 147 members. Each becomes a Pokedex tab whose name is string id `250000 +
 * region`.
 */
private val NEW_POKEDEX_REGIONS = listOf(6, 7, 8, 9)

/** The client type ordinal for an Expansion type symbol; Fairy is 19 via the enum patch. */
private fun overlayClientType(symbol: String): Int =
    when (symbol) {
      "TYPE_NORMAL" -> 0
      "TYPE_FIGHTING" -> 1
      "TYPE_FLYING" -> 2
      "TYPE_POISON" -> 3
      "TYPE_GROUND" -> 4
      "TYPE_ROCK" -> 5
      "TYPE_BUG" -> 6
      "TYPE_GHOST" -> 7
      "TYPE_STEEL" -> 8
      "TYPE_MYSTERY" -> 9
      "TYPE_FIRE" -> 10
      "TYPE_WATER" -> 11
      "TYPE_GRASS" -> 12
      "TYPE_ELECTRIC" -> 13
      "TYPE_PSYCHIC" -> 14
      "TYPE_ICE" -> 15
      "TYPE_DRAGON" -> 16
      "TYPE_DARK" -> 17
      "TYPE_FAIRY" -> 19
      else -> error("No client ordinal for $symbol")
    }

/**
 * Compiles the fixup helper against the client's Java version.
 *
 * `--release 17` matches the client's bundled JRE. Compiling at patch time from a resource keeps
 * the build free of a second Java toolchain, which the UI dependencies could not resolve.
 */
private fun compileHelperForClientRuntime(name: String): ByteArray {
  val source =
      checkNotNull(object {}.javaClass.classLoader.getResourceAsStream("monmmo/$name.java")) {
            "The $name helper source is not on the launcher classpath"
          }
          .use { String(it.readBytes()) }
  val compiler =
      checkNotNull(javax.tools.ToolProvider.getSystemJavaCompiler()) {
        "A JDK is required to compile the fixup helper"
      }
  val work = Files.createTempDirectory("monmmo-helper")
  val sourceDir = work.resolve("monmmo")
  Files.createDirectories(sourceDir)
  val sourceFile = sourceDir.resolve("$name.java")
  Files.writeString(sourceFile, source)
  val result =
      compiler.run(
          null,
          null,
          null,
          "--release",
          "17",
          "-d",
          work.toString(),
          sourceFile.toString(),
      )
  check(result == 0) { "The fixup helper does not compile" }
  return Files.readAllBytes(work.resolve("monmmo").resolve("$name.class"))
}
