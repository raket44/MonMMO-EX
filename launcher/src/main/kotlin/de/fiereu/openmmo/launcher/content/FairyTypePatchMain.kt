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
    // The detail panel reads its category box, dex paragraph, height and weight lines straight
    // from the ROM text archives (nV0.f7, tables 235/236/260/245/268 by species id), which no
    // string-table work can reach - imported ids simply have no ROM row. Redirect every read
    // through the overlay, which packs those texts and delegates retail ids back to the ROM.
    applyOne(
        "Pokedex detail text redirect",
        DexTextRedirectPatch::isDetailPanel,
        DexTextRedirectPatch::patch,
    )

    // The move list labels a machine move with the tool name stripped at " - "; numberless
    // names have no dash, so the whole name leaked into the source column. Strip at the first
    // space instead, rendering the bare "TM" / "HM" tag.
    applyOne(
        "Pokedex move-source label",
        DexMoveRowLabelPatch::isRowPainter,
        DexMoveRowLabelPatch::patch,
    )

    // The hidden-ability line is gated on a whitelist baked into the client that imported
    // species can never join; the record carries the ability, the screen just refuses to say so.
    applyOne(
        "Hidden-ability release gate removed",
        HiddenAbilityGatePatch::isDexDetailScreen,
        HiddenAbilityGatePatch::patch,
    )

    // A scissor pop with nothing pushed - a battle frame drawn while the window is minimized -
    // was a fatal render error; the guard makes it a no-op instead.
    applyOne(
        "Scissor stack pop guarded",
        ScissorGuardPatch::isScissorStack,
        ScissorGuardPatch::patch,
    )

    // The battle scene rebuilds the animation registry per battle, discarding runtime entries;
    // the builder's tail now re-applies the movevfx/moveanim fixups on every rebuild.
    applyOne(
        "anim registry rebuild hook",
        AnimRegistryHookPatch::isRegistry,
        AnimRegistryHookPatch::patch,
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

    // Party-to-PC drags reach the server as nothing at all; log the drop and the move sender.
    applyOne(
        "drag drop diagnostic",
        DragDiagnosticPatch::isPartyWindow,
        DragDiagnosticPatch::patchPartyWindow,
    )
    applyOne(
        "drag send diagnostic",
        DragDiagnosticPatch::isMoveSender,
        DragDiagnosticPatch::patchMoveSender,
    )

    // The battle text bank, dumped through the client's own accessor at the first battle event.
    applyOne(
        "battle text dump",
        BattleTextDumpPatch::isEventFactory,
        BattleTextDumpPatch::patch,
    )

    applyOne(
        "Unova map build stage markers",
        K90DiagnosticPatch::isUnovaMap,
        K90DiagnosticPatch::patch,
    )

    // Every DS map the client builds is written out with its own tile and event answers, for
    // the server's Johto/Sinnoh/Unova importer.
    // The DS map dump hook is OFF: even inert it sat inside the client's map build and Johto
    // stayed black. Re-enable only for a deliberate dump session.
    if (System.getenv("MONMMO_NDS_DUMP") == "1") {
      for (mapClass in listOf("f/Hv0", "f/k90", "f/QK")) {
        applyOne(
            "NDS map dump $mapClass",
            NdsMapDumpPatch.named(mapClass),
            NdsMapDumpPatch::patch,
        )
      }
    }

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

    // The evolution cinematic draws frame 0 of a mod GIF and never advances it; tick the frames.
    applyOne(
        "evolution scene animation",
        EvolutionAnimPatch::isEvolutionScene,
        EvolutionAnimPatch::patch,
    )

    // The sprite fetch diagnostic (SpriteFetchDiagnosticPatch) stays available but is NOT applied:
    // its probe decodes every GIF it inspects, and the Pokedex fetches every imported species on
    // open, which turned the hook into a multi-second freeze. Re-enable only to investigate.
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
    // The vfx-playing animation, compiled against the client jar (it extends f.Dm0). Registered
    // per move by the movevfx fixups; the class rides the overlay like every other helper.
    jar.putNextEntry(ZipEntry("monmmo/VfxAnim.class"))
    jar.write(compileHelperForClientRuntime("VfxAnim", client, companions = listOf("MapLog")))
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
    // Battle animations for the imported moves (operator-directed). Two tiers:
    //
    // 560-732: the client SHIPS an authored particle effect for every one of these
    // (particle/auto/<id>.vfx, through Gen 6 - Moonblast is 585) that no code ever registered.
    // movevfx plays the move's own effect through VfxAnim, the translation of the client's
    // generic timeline. Authentic visuals, zero authoring.
    //
    // 733+: no shipped effect exists, so each aliases a retail donor's animation - same type,
    // preferring the same damage category and the closest power. A placeholder by design; any
    // line can be handpicked later, and an authored .vfx can promote a move to the first tier.
    val moveVfxLines =
        parsedMoves.filter { it.id in 560..LAST_SHIPPED_VFX_MOVE }.map { "movevfx:${it.id}" }
    // Handpicked donors that beat the naive vfx playback - emitted AFTER the vfx tier so they
    // win (fixups apply in file order). Operator-curated; grow this list freely.
    val handpickedAnims =
        mapOf(
                585 to 236, // Moonblast - Moonlight's coded moon-and-glow staging
                // 594 Water Shuriken reverted to its own water vfx: the Electro Ball pick threw
                // the right SHAPE in the wrong ELEMENT - a yellow electric ball off a water move
                // (operator verdict: stupid). Structure matching must never cross elements.
                566 to 467, // Phantom Force - Shadow Force outright (script match, same move)
            )
            .map { (move, donor) -> "moveanim:$move:$donor" }
    val retailMoves = parsedMoves.filter { it.id in 1..559 }
    // Donor choice reads the Expansion's own animation scripts (goto targets and shared
    // primitives) - see AnimScriptMatcher. The stat heuristic survives only as the last fallback
    // for moves whose script matched nothing, and every choice lands in a review file.
    val structural = AnimScriptMatcher.matches(moveRoot, parsedMoves)
    val donorReview = StringBuilder("move;name;donor;donorName;how\n")
    val moveAnimLines =
        parsedMoves
            .filter { it.id in (LAST_SHIPPED_VFX_MOVE + 1)..999 }
            .mapNotNull { move ->
              val structuralChoice = structural[move.id]
              val donor: Int?
              val how: String
              if (structuralChoice != null) {
                donor = structuralChoice.donor
                how = structuralChoice.reason
              } else {
                val sameType = retailMoves.filter { it.type == move.type }
                val pool = sameType.filter { it.category == move.category }.ifEmpty { sameType }
                donor =
                    pool.minByOrNull { kotlin.math.abs(it.power - move.power) * 1000 + it.id }?.id
                how = "stat fallback"
              }
              donor?.also {
                val donorName = parsedMoves.firstOrNull { m -> m.id == it }?.name ?: "vfx tier $it"
                donorReview.append("${move.id};${move.name};$it;$donorName;$how\n")
              }
              donor?.let { "moveanim:${move.id}:$it" }
            }
    // Advisory rows for the vfx tier too: every 560-732 move's script-matched donor, so a move
    // whose own-effect playback disappoints can be flipped to its donor with one handpick line.
    parsedMoves
        .filter { it.id in 560..LAST_SHIPPED_VFX_MOVE }
        .forEach { move ->
          structural[move.id]?.let { choice ->
            val donorName = parsedMoves.firstOrNull { m -> m.id == choice.donor }?.name ?: "?"
            donorReview.append(
                "${move.id};${move.name};${choice.donor};$donorName;ADVISORY vfx tier - ${choice.reason}\n")
          }
        }
    val reviewFile = Path.of("build/expansion-client/anim-donors.csv")
    Files.createDirectories(reviewFile.parent)
    Files.writeString(reviewFile, donorReview.toString())
    println("[fairy-type] anim donors: ${moveAnimLines.size} chosen, review at $reviewFile")
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

    val dumpLines =
        listOf(
            "locations",
            "dump:7",
            "dump:133",
            "dumptools",
            "dumpitems",
            // What the client holds for an imported species once every section and fixup ran.
            "dumplabels",
            "dumpspecies:7",
            "dumpspecies:25",
            "dumpspecies:668",
            "dumpspecies:676",
            "dumpspecies:718",
        )
    // The fields only the ROM loader fills, zeroed on every imported species: gender ratio,
    // exp yield, height, weight and the per-stat EV yields the dex renders. Values come from the
    // same catalogue that generated the merged monsters.json, so the two stay identical. The id
    // window skips everything a ROM already covers: retail 1-649, the form records 650-667 and
    // the client-reserved 1000-1052.
    val heldItemNames = ItemNames(expansionRootPath)
    val speciesDataLines =
        expansion.mapNotNull { entry ->
          val wire = entry.clientWireId ?: return@mapNotNull null
          if (wire in 1..667 || wire in 1000..1052) return@mapNotNull null
          listOf(
                  "speciesdata",
                  wire,
                  entry.genderRatio,
                  entry.expYield,
                  entry.height,
                  entry.weight,
                  entry.evYieldHp,
                  entry.evYieldAttack,
                  entry.evYieldDefense,
                  entry.evYieldSpeed,
                  entry.evYieldSpAttack,
                  entry.evYieldSpDefense,
                  listOfNotNull(
                          heldItemNames.bySymbol(entry.itemCommonSymbol),
                          heldItemNames.bySymbol(entry.itemRareSymbol),
                      )
                      .map { it.first }
                      .distinct()
                      .joinToString(",")
                      .ifEmpty { "-" },
                  // Growth curve as the client's f/XB1 index (= the dump's exp_type numbering).
                  // Section 10 never sets it, and the record constructor defaults to index 1 -
                  // ERRATIC - so a Medium Slow species read its XP as below its own level and the
                  // summary/battle XP bar never moved.
                  GROWTH_ORDER.indexOf(entry.growthRateSymbol.removePrefix("GROWTH_"))
                      .coerceAtLeast(0),
              )
              .joinToString(":")
        }

    val fixups =
        retypeLines +
            moveTypeLines +
            moveVfxLines +
            handpickedAnims +
            moveAnimLines +
            evoLines +
            speciesDataLines +
            toolLines +
            dumpLines
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

    // The detail-panel texts the ROM cannot provide, one row per table/species/text. Category
    // and paragraph come from the Expansion's own species_info; height and weight are formatted
    // from the catalogue's decimeters and hectograms.
    val dexDescriptions = ExpansionDexText.parse(expansionRootPath)
    val dexTextRows = StringBuilder()
    var dexTextCount = 0
    expansion.forEach { entry ->
      val wire = entry.clientWireId ?: return@forEach
      if (wire in 1..667 || wire in 1000..1052) return@forEach
      val clean = { value: String -> value.replace("\t", " ").replace("\n", "\\n") }
      // Measured on screen: the Type row text reads table 260 (the "Ninja Pokemon" category
      // line) and the Desc row reads 235/236 (the dex-entry paragraph) - the reverse of the
      // first guess.
      if (entry.categoryName.isNotBlank()) {
        val category = clean(entry.categoryName + " Pokémon")
        dexTextRows.append("260\t").append(wire).append("\t").append(category).append("\n")
        dexTextCount++
      }
      ExpansionDexText.forSymbol(dexDescriptions, entry.symbol)?.let { paragraph ->
        dexTextRows.append("235\t").append(wire).append("\t").append(clean(paragraph)).append("\n")
        dexTextRows.append("236\t").append(wire).append("\t").append(clean(paragraph)).append("\n")
        dexTextCount++
      }
      // Retail formatting sampled off the ROM at runtime: height 1\'04\" (feet, zero-
      // padded inches), weight 13.2 lbs. - imperial, not metric.
      val totalInches = Math.round(entry.height * 3.93701).toInt()
      dexTextRows
          .append("245\t")
          .append(wire)
          .append("\t")
          .append(totalInches / 12)
          .append("'")
          .append(String.format("%02d", totalInches % 12))
          .append("\"\n")
      dexTextRows
          .append("268\t")
          .append(wire)
          .append("\t")
          .append(String.format("%.1f", entry.weight * 0.220462))
          .append(" lbs.\n")
    }

    jar.putNextEntry(ZipEntry("monmmo/dex-text.tsv"))
    jar.write(dexTextRows.toString().toByteArray())
    jar.closeEntry()
    println("[fairy-type] dex texts packed: " + dexTextCount + " category/paragraph rows")

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

/** The highest move id with a shipped particle effect: particles.pak holds auto/0-732.vfx. */
private const val LAST_SHIPPED_VFX_MOVE = 732

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
private fun compileHelperForClientRuntime(
    name: String,
    classpath: Path? = null,
    companions: List<String> = emptyList(),
): ByteArray {
  fun read(source: String): String =
      checkNotNull(object {}.javaClass.classLoader.getResourceAsStream("monmmo/$source.java")) {
            "The $source helper source is not on the launcher classpath"
          }
          .use { String(it.readBytes()) }
  val compiler =
      checkNotNull(javax.tools.ToolProvider.getSystemJavaCompiler()) {
        "A JDK is required to compile the fixup helper"
      }
  val work = Files.createTempDirectory("monmmo-helper")
  val sourceDir = work.resolve("monmmo")
  Files.createDirectories(sourceDir)
  val sources =
      (listOf(name) + companions).map { helper ->
        sourceDir.resolve("$helper.java").also { Files.writeString(it, read(helper)) }
      }
  val arguments = mutableListOf("--release", "17", "-d", work.toString())
  // Helpers that extend client classes (VfxAnim extends f.Dm0) compile against the client jar
  // itself; the reflection-only helpers need no classpath.
  classpath?.let {
    arguments += "-cp"
    arguments += it.toString()
  }
  arguments += sources.map(Path::toString)
  val result = compiler.run(null, null, null, *arguments.toTypedArray())
  check(result == 0) { "The $name helper does not compile" }
  return Files.readAllBytes(work.resolve("monmmo").resolve("$name.class"))
}
