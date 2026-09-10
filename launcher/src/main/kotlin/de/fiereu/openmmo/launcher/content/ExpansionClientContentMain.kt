@file:JvmName("ExpansionClientContentMain")

package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/** Builds inactive client content under launcher/build. It never writes to an installed client. */
fun main(args: Array<String>) {
  require(args.size >= 5) {
    "Usage: <stock-data.pak> <output-data.pak> <stock-strings-en.xml> <output-strings-en.xml> " +
        "<expansion-root> [<showdown-sprite-root>]"
  }
  val stockData = Path.of(args[0])
  val outputData = Path.of(args[1])
  val stockStrings = Path.of(args[2])
  val outputStrings = Path.of(args[3])
  val expansionRoot = Path.of(args[4])
  require(Files.isRegularFile(stockData)) { "Stock data.pak not found: $stockData" }
  require(Files.isRegularFile(stockStrings)) { "Stock English strings not found: $stockStrings" }
  require(Files.isDirectory(expansionRoot)) { "Expansion root not found: $expansionRoot" }

  val expansion = ExpansionSpeciesRegistry().all()
  // Every species the client does not already own: new Dex entries AND the forms the catalogue
  // mapped into the 1079+ wire block - Alolans, Galarians, Megas and the rest. The give command
  // trusted those wire ids while only the Dex-650+ records were installed, so an Alolan Vulpix
  // arrived as species 1134 with no client record behind it: reported as given, rendered as
  // nothing. The invariant is that no species carries a wire id without a record to back it.
  // The client's own Dex 1-649 records stay untouched.
  val selected =
      expansion.filter {
        it.clientContentCompatible &&
            (it.isNewToClient || (it.clientWireId ?: 0) >= FIRST_FORM_WIRE_ID)
      }
  val stagedIds = selected.mapTo(HashSet()) { it.stableId }

  // Every species the client can address, not just the new ones.
  //
  // The client applies a learnset by assignment - section 1 does `species.moves = list` and section
  // 2 stores into a per-category slot - and our records are appended after the stock ones, so a
  // record written here replaces what the client shipped. Restricting this to new species left
  // Jigglypuff and the rest of Gen 1-5 on the client's Gen 5 lists, which is exactly backwards: the
  // Expansion is meant to be the source of truth for all of them.
  val authoritative =
      selected + expansion.filter { !it.isNewToClient && !it.isForm && it.clientWireId != null }
  val records = selected.map(::record)
  // Species the client already has, but whose typing the Expansion changed - the Gen 6 Fairy
  // retypes. Their learnsets and details are still written from here, but the type change itself
  // rides in the overlay's fixup helper: replacing the record wholesale created a fresh object
  // after the ROM loader had linked evolution chains, and Jigglypuff arrived with no evolution.
  val retyped =
      expansion.filter { !it.isNewToClient && !it.isForm && "TYPE_FAIRY" in it.typeSymbols }
  Files.createDirectories(outputData.parent)

  // Moves first: which ones exist decides which learnset entries can survive. The client defines
  // 1-559 from the ROMs, and section 4 is where everything past that has to be written.
  val moveIds = MoveText.ids(expansionRoot)
  val moveData = MoveText.parse(expansionRoot, moveIds).associateBy { it.id }
  // The per-game dumps keep learn methods apart, so tools, egg and prevo each land in the
  // category the client means. The old single-file source lumped them together, which is how a
  // pre-evolution level-up move ended up in the tools tab as a nameless TM.
  val gameLists = PorymovesLearnsets.parse(expansionRoot, moveIds)
  val teachable = gameLists.taught
  val eggMoves = gameLists.egg
  val prevoMoves = gameLists.prevo
  // Teachable and egg lists are keyed by family names like XERNEAS or EEVEE, while a species.
  // symbol may carry form suffixes - SPECIES_XERNEAS_NEUTRAL - and an evolved form inherits egg
  // moves from its family root. Walking the name back one underscore at a time finds the owning
  // key: XERNEAS_NEUTRAL -> XERNEAS. Without this, Xerneas shipped with no TM list at all and
  // Sylveon with no egg moves.
  fun familyLookup(table: Map<String, List<Int>>, entry: ExpansionSpeciesDef): List<Int>? {
    val candidates =
        listOf(
            entry.symbol.removePrefix("SPECIES_"),
            entry.baseSpeciesStableId.substringAfter("SPECIES_"),
        )
    candidates.forEach { name ->
      var current = name
      while (current.isNotEmpty()) {
        table[current]?.let {
          return it
        }
        val cut = current.lastIndexOf(0x5f.toChar())
        if (cut <= 0) break
        current = current.substring(0, cut)
      }
    }
    return null
  }
  val usedMoveIds =
      (authoritative.flatMap { it.levelUpLearnset }.map { it.originalMoveId } +
              authoritative.flatMap { entry ->
                (familyLookup(teachable, entry) ?: emptyList()) +
                    (familyLookup(eggMoves, entry) ?: emptyList()) +
                    (familyLookup(prevoMoves, entry) ?: emptyList())
              })
          .toSet()
  // A move the ROMs never had needs a full record: accuracy, power, PP and type. Ids stay below the
  // 1000 block the client keeps for its own event moves.
  val newMoves =
      usedMoveIds
          .filter { it > LAST_CLIENT_MOVE_ID }
          .sorted()
          .mapNotNull { id -> moveData[id] }
          .filter { it.id < FIRST_CLIENT_OWN_MOVE_ID }
  val moveRecords = newMoves.map(::moveRecord)
  // Anything we could not define would render as a blank entry, so it stays out of the learnsets.
  val definedMoves = (1..LAST_CLIENT_MOVE_ID).toSet() + newMoves.map { it.id }

  // Nothing should end up here; if it does, say which move and why rather than dropping it quietly.
  val undefined = usedMoveIds.filter { it > LAST_CLIENT_MOVE_ID && it !in definedMoves }.sorted()
  if (undefined.isNotEmpty()) {
    println(
        "[expansion-client] moves left undefined: " +
            undefined.joinToString { id -> "$id ${moveData[id]?.name ?: "(not in moves_info)"}" })
  }

  val learnsets =
      authoritative.mapNotNull { entry ->
        val moves =
            entry.levelUpLearnset
                .filter { it.originalMoveId in definedMoves }
                .sortedBy { it.level }
                .take(LevelUpLearnset.MAX_MOVES)
                .map { LevelUpMove(it.originalMoveId, it.level.coerceIn(1, 100)) }
        if (moves.isEmpty()) null else LevelUpLearnset(checkNotNull(entry.clientWireId), moves)
      }

  // A species with no dex entry cannot be opened in the Pokedex at all. The client keeps six
  // per-region slots and the stock file fills five, so everything new shares the last one, listed
  // in National Dex order.
  // One Pokedex region per national dex range the imported species fall into. The client shipped
  // with five and its species record only had six slots for region numbers; the classpath overlay
  // widens both, and a tab's name is string id 250000 + region, so nothing else is needed here.
  //
  // A species is a member of a region precisely when its number there is at least 1 - the list is
  // built by filtering on that and sorting by it - so writing the number is writing the membership.
  val dexRegions =
      DEX_REGIONS.map { (region, range) ->
        RegionalDex(
            region,
            selected
                // Forms sit above the last contiguous id, so the Pokedex copy never reaches them.
                .filter { !it.isForm && (it.nationalDexId ?: 0) in range }
                .sortedBy { it.nationalDexId }
                .mapNotNull { it.clientWireId },
        )
      }
  // Egg, taught and pre-evolution moves, each in the category the client means. The tools tab is
  // built from the tool registry against the tools category; the tutor category is the client's
  // home for moves a pre-evolution knew - exactly the set the per-game dumps list as PreEvoMoves.
  // Canonical species keep their retail lists untouched.
  val extra =
      authoritative.flatMap { entry ->
        val wireId = entry.clientWireId ?: return@flatMap emptyList()
        val egg = familyLookup(eggMoves, entry)?.filter { it in definedMoves }?.distinct()
        val taught =
            if (entry.stableId !in stagedIds) null
            else
                familyLookup(teachable, entry)
                    ?.filter { it in definedMoves && it !in (egg ?: emptyList()) }
                    ?.distinct()
        val prevo =
            if (entry.stableId !in stagedIds) null
            else
                familyLookup(prevoMoves, entry)
                    ?.filter {
                      it in definedMoves &&
                          it !in (egg ?: emptyList()) &&
                          it !in (taught ?: emptyList())
                    }
                    ?.distinct()
        listOfNotNull(
                taught?.takeIf { it.isNotEmpty() }?.let { ExtraLearnset.MOVE_LEARNER_TOOLS to it },
                prevo?.takeIf { it.isNotEmpty() }?.let { ExtraLearnset.PREVO_MOVES to it },
                egg?.takeIf { it.isNotEmpty() }?.let { ExtraLearnset.EGG_MOVES to it },
            )
            .map { (category, moves) -> ExtraLearnset(wireId, category, moves) }
      }

  // The optional-field section: egg groups, plus the payload-free flags stock species carry.
  // The replaced species need their detail record too. Section 10 runs first and rebuilds the
  // species object, so egg groups and rarity are whatever the record leaves behind unless they are
  // written here; 7 of the 22 had no stock entry to re-apply.
  val details =
      (selected + retyped).mapNotNull { entry ->
        val wireId = entry.clientWireId ?: return@mapNotNull null
        val groups = entry.eggGroupSymbols.map(::eggGroup)
        detail(
            speciesId = wireId,
            eggGroup1 = groups.getOrElse(0) { 0 },
            eggGroup2 = groups.getOrElse(1) { groups.getOrElse(0) { 0 } },
            rarity = entry.rarity,
        )
      }

  // One pass over the file. Each section is decoded in full, appended to, and re-encoded; a codec
  // that cannot reproduce the stock bytes exactly aborts the build before anything is written.
  //
  // Sections are processed by the client in file order and the later ones attach to species already
  // in its registry - the learnset section does registry.get(id)?.moves = list, null-safe, so an
  // unknown id is skipped in silence. Editing sections in place keeps that order intact.
  val built =
      ClientDataPak.parse(Files.readAllBytes(stockData))
          .edit(SpeciesCodec) { stock -> stock + checkNoCollisions(stock, records) }
          .edit(LevelUpLearnsetCodec) { stock -> stock + learnsets }
          .edit(ExtraLearnsetCodec) { stock -> stock + extra }
          .edit(MoveCodec) { stock ->
            val occupied = stock.map { it.moveId }.toSet()
            val clashes = moveRecords.map { it.moveId }.filter(occupied::contains)
            require(clashes.isEmpty()) { "Move ids already defined by the client: $clashes" }
            stock + moveRecords
          }
          .edit(RegionalDexCodec) { stock ->
            val taken = stock.map { it.regionId }.toSet()
            val clashes = dexRegions.map { it.regionId }.filter(taken::contains)
            require(clashes.isEmpty()) { "The client already uses dex regions $clashes" }
            stock + dexRegions
          }
          .edit(SpeciesDetailCodec) { stock -> stock + details }
  // The retail wild-locations rebuild (WildLocationsSection) is parked: replacing section 5
  // wholesale changed the dex in ways the project owner rejected - species with retail-only data
  // rendered empty, and season rows-in-the-list is not the wanted design. The stock table stays
  // until the dex grows real season and form controls via the overlay.
  Files.write(outputData, built.compress())
  println(
      "[expansion-client] extra learnsets: ${extra.size} lists, " +
          "${extra.sumOf { it.moves.size }} moves")
  println("[expansion-client] egg groups and flags set on ${details.size} species")
  dexRegions.forEach {
    println("[expansion-client] dex region ${it.regionId} lists ${it.speciesIds.size} species")
  }
  println(
      "[expansion-client] ${retyped.size} retypes ride in the overlay fixups: " +
          retyped.joinToString { it.symbol.removePrefix("SPECIES_").lowercase() })
  println(
      "[expansion-client] move definitions written: ${moveRecords.size} " +
          "(ids ${newMoves.firstOrNull()?.id}-${newMoves.lastOrNull()?.id})")

  val totalMoves = authoritative.sumOf { it.levelUpLearnset.size }
  val keptMoves = learnsets.sumOf { it.moves.size }
  println(
      "[expansion-client] learnsets=${learnsets.size} species, $keptMoves of $totalMoves moves " +
          "(${totalMoves - keptMoves} need client move definitions)")
  patchNames(stockStrings, outputStrings, selected, expansionRoot, newMoves)

  // Metadata registers a species; without converted assets the client would draw it blank.
  val withAssets = selected.filter { it.assetSourcesResolved }
  // Icons for every created item, from the Expansion's own art (operator-directed).
  val (itemIcons, iconSummary) =
      ItemIconStaging.build(
          expansionRoot,
          TmPlan.taughtMoves(expansionRoot),
          MoveText.parse(expansionRoot, moveIds).associate { it.id to it.type },
      )
  println(
      "[expansion-client] item icons staged=${iconSummary.staged}" +
          if (iconSummary.missing.isEmpty()) ""
          else " missing=${iconSummary.missing.take(10).joinToString()}")
  // Pokemon Showdown's Gen 5-style set (optional sixth argument, fetched by
  // :launcher:fetchShowdownSprites): animated where it has animation, stills otherwise, for every
  // species and form the mod stages. The Expansion's converted GBA art is only used where Showdown
  // has nothing (operator-directed: one source, no fallback chain).
  val showdown =
      args.getOrNull(5)?.let { Path.of(it) }?.let { ShowdownSprites(it) }?.takeIf { it.available }
  val assets =
      ExpansionAssetStaging(expansionRoot, showdown = showdown)
          .stage(
              withAssets,
              outputData.parent.resolve("mods/monmmo-lost-knights.zip"),
              itemIcons,
              allSpecies = expansion,
          )
  Files.newBufferedWriter(outputData.parent.parent.resolve("sprite-pack.csv")).use { writer ->
    writer.appendLine("symbol,wireId,front,frontShiny,back,backShiny")
    assets.packReport.forEach { writer.appendLine(it) }
  }
  Files.newBufferedWriter(outputData.parent.parent.resolve("sprite-pack-missing.csv")).use { writer ->
    writer.appendLine("symbol,wireId,name,nationalDex,missing")
    assets.packMissing.forEach { writer.appendLine(it) }
  }
  println(
      "[expansion-client] gen5-style sprites: ${assets.packSprites} species/forms from Showdown " +
          "(animated fronts=${assets.showdownAnimated}; showdown " +
          "${if (showdown == null) "absent" else "present"})")

  // The manifest of every item the overlay creates in the client, as id;name. The server's
  // ItemRegistry loads this from its classpath so the same items are addressable server-side -
  // without it, /giveitem could never hand over an Ice Stone the client displays perfectly well.
  val itemManifest = outputData.parent.parent.resolve("imported-items.csv")
  Files.newBufferedWriter(itemManifest).use { writer ->
    val moveNames = MoveText.parse(expansionRoot, MoveText.ids(expansionRoot)).associateBy { it.id }
    TmPlan.taughtMoves(expansionRoot).forEachIndexed { index, moveId ->
      // Numberless (operator-directed): the name is "TM <move>" everywhere a name renders.
      val name = "TM ${moveNames[moveId]?.name ?: ""}".trim()
      writer.appendLine("${TmPlan.FIRST_ITEM_ID + index};$name")
    }
    EvoItemPlan.ITEMS.forEachIndexed { index, (_, name) ->
      writer.appendLine("${EvoItemPlan.FIRST_ITEM_ID + index};$name")
    }
    ItemImportPlan.compute(expansionRoot).forEach { item ->
      writer.appendLine("${item.itemId};${item.name}")
    }
  }

  val report = outputData.parent.parent.resolve("compatibility.csv")
  Files.newBufferedWriter(report).use { writer ->
    writer.appendLine("stableId,wireId,compatible,reason")
    expansion
        .filter { it.isNewToClient }
        .forEach { entry ->
          val reason =
              when {
                entry.usesClientUnsupportedType -> "unsupported client type"
                !entry.clientContentCompatible -> "missing required source asset"
                else -> "metadata ready; assets require conversion"
              }
          writer.appendLine(
              "${entry.stableId},${entry.clientWireId},${entry.clientContentCompatible},$reason")
        }
  }
  println(
      "[expansion-client] staged=${selected.size} of ${expansion.count { it.isNewToClient }} new species " +
          "dataPak=$outputData strings=$outputStrings")
  println(
      "[expansion-client] assets staged=${assets.staged} cries=${assets.cries} " +
          "animated=${assets.animated} followers=${assets.followers} " +
          "followerFallbacks=${assets.followerFallbacks} " +
          "noSource=${selected.size - withAssets.size} failed=${assets.failures.size}")
  assets.failures.entries.take(5).forEach {
    println("[expansion-client] asset failure ${it.key}: ${it.value}")
  }
  println("[expansion-client] output is inactive; follower conversion is still required")
}

/**
 * The stat order the client reads: HP, Attack, Defense, Speed, Special Attack, Special Defense.
 * Speed sits third from the end, not last as in the Expansion headers.
 */
private fun record(entry: ExpansionSpeciesDef): SpeciesRecord {
  val types = entry.typeSymbols.map(::clientType)
  val record =
      SpeciesRecord(
          speciesId = checkNotNull(entry.clientWireId),
          type1 = types[0],
          type2 = types.getOrElse(1) { types[0] },
          stats =
              listOf(
                  entry.baseHp,
                  entry.baseAttack,
                  entry.baseDefense,
                  entry.baseSpeed,
                  entry.baseSpAttack,
                  entry.baseSpDefense,
              ),
          abilities =
              listOf(
                  entry.abilityIds[0],
                  entry.abilityIds.getOrElse(1) { 0 },
                  entry.abilityIds.getOrElse(2) { 0 },
              ),
          trailer = SPECIES_TRAILER,
      )
  val shorts = listOf(record.speciesId) + record.stats + record.abilities + record.trailer
  require(shorts.all { it in 0..0xffff }) {
    "Species record exceeds an unsigned 16-bit field: $record"
  }
  // 19 is Fairy, added to the client type enum by the classpath overlay patch.
  require(record.type1 in 0..MAX_CLIENT_TYPE && record.type2 in 0..MAX_CLIENT_TYPE) {
    "Species ${record.speciesId} uses a type the patched client cannot represent"
  }
  return record
}

/** Refuses to add a species id the stock file already uses, or to add one twice. */
private fun checkNoCollisions(
    stock: List<SpeciesRecord>,
    additions: List<SpeciesRecord>,
): List<SpeciesRecord> {
  val duplicates =
      additions.map { it.speciesId }.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
  require(duplicates.isEmpty()) { "Duplicate generated client species ids: ${duplicates.sorted()}" }
  val occupied = stock.map { it.speciesId }.toSet()
  val collisions = additions.map { it.speciesId }.filter(occupied::contains).toSet()
  require(collisions.isEmpty()) {
    "Generated client species ids already exist: ${collisions.sorted()}"
  }
  return additions
}

/**
 * Builds one optional-field record. Only the bits with a payload we can fill are set, alongside the
 * payload-free flags every stock species carries.
 */
private fun detail(speciesId: Int, eggGroup1: Int, eggGroup2: Int, rarity: Int): SpeciesDetail {
  require(eggGroup1 in 0..MAX_EGG_GROUP && eggGroup2 in 0..MAX_EGG_GROUP) {
    "Species $speciesId has an egg group outside the client range"
  }
  val rare = rarity != 0
  return SpeciesDetail(
      speciesId = speciesId,
      flags =
          SpeciesDetail.EGG_GROUPS or
              // Deliberately not HIDDEN_FROM_DEX: section 10 sets that on every species it
              // creates, and the Pokedex skips any entry carrying it.
              // Nor EXCLUDED_FROM_DEX, the second flag the screen checks alongside it.
              SpeciesDetail.FLAG_200 or
              (if (rare) SpeciesDetail.RARITY else 0),
      eggGroups = eggGroup1 to eggGroup2,
      rarity = if (rare) rarity else null,
  )
}

/**
 * A move the ROMs never had, written out in full.
 *
 * Only the four fields whose meaning is settled are flagged: accuracy, power, PP and type. The
 * others stay unset rather than being filled with zeroes we cannot justify. The attribute lines the
 * client prints beneath a move come from [MoveAttributes], which is measured against the 559 moves
 * the client already ships rather than assumed.
 */
private fun moveRecord(move: MoveText.Move): MoveRecord =
    MoveRecord(
        moveId = move.id,
        category = moveCategory(move.category),
        flags =
            MoveRecord.FIELD_001 or MoveRecord.FIELD_002 or MoveRecord.FIELD_004 or MoveRecord.TYPE,
        // The client spells a never-miss move 101; the decomp spells it 0.
        field001 = if (move.accuracy == 0) NEVER_MISS_ACCURACY else move.accuracy,
        field002 = move.power,
        field004 = move.pp,
        type = clientType(move.type),
        effects = MoveAttributes.of(move),
    )

/** `f/wF`, the client's damage category enum. */
private fun moveCategory(symbol: String): Int =
    when (symbol) {
      "DAMAGE_CATEGORY_PHYSICAL" -> 0
      "DAMAGE_CATEGORY_SPECIAL" -> 1
      else -> 2
    }

private fun clientType(symbol: String): Int =
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
      "TYPE_NONE" -> 18
      // Added by patchClientTypes; matches PokemonType.FAIRY on the server.
      "TYPE_FAIRY" -> 19
      else -> error("Client revision 31914 cannot represent Expansion type $symbol")
    }

private fun patchNames(
    source: Path,
    output: Path,
    species: List<ExpansionSpeciesDef>,
    expansionRoot: Path,
    importedMoves: List<MoveText.Move>,
    locationNames: List<Pair<Int, String>> = emptyList(),
) {
  val factory =
      DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        isExpandEntityReferences = false
      }
  val document = factory.newDocumentBuilder().parse(source.toFile())
  val root = document.documentElement
  val stockStrings =
      (0 until root.childNodes.length)
          .mapNotNull { index ->
            val node = root.childNodes.item(index)
            val id = node.attributes?.getNamedItem("id")?.nodeValue?.toIntOrNull()
            id?.let { it to node.textContent }
          }
          .toMap()
  val occupied = stockStrings.keys
  // An ability with no name renders as a raw placeholder, so every ability the staged species use
  // gets its name and description. Ids the ROM already covers are left alone rather than shadowed.
  val abilityIds = AbilityText.ids(expansionRoot)
  val abilityText = AbilityText.parse(expansionRoot, abilityIds).associateBy { it.id }
  species
      .flatMap { it.abilityIds }
      .filter { it > 0 }
      .distinct()
      .sorted()
      .forEach { id ->
        val ability = abilityText[id] ?: return@forEach
        listOf(
                AbilityText.NAME_BASE + id to ability.name,
                AbilityText.DESCRIPTION_BASE + id to ability.description,
            )
            .forEach { (stringId, value) ->
              if (stringId in occupied) return@forEach
              root.appendChild(
                  document.createElement("string").apply {
                    setAttribute("id", stringId.toString())
                    textContent = value
                  })
            }
      }

  // Name strings for the TM items the overlay creates - numberless "TM <move>" (operator-directed:
  // the same name renders in pickups, dialogs, the bag and the dex; a bare number tells nobody
  // anything and regional numbering cannot drift when there is no number).
  val tmMoves = TmPlan.taughtMoves(expansionRoot)
  val moveNamesById =
      MoveText.parse(expansionRoot, MoveText.ids(expansionRoot)).associateBy { it.id }
  tmMoves.forEachIndexed { index, moveId ->
    val stringId = TmPlan.NAME_STRING_BASE + index
    if (stringId in occupied) return@forEachIndexed
    // The CLIENT's move name wins when it differs - PokeMMO renames Hail to Snowscape at
    // string 110258, and the tool must carry the name the player sees on the move.
    val clientMoveName = stockStrings[110000 + moveId]
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          textContent = "TM ${clientMoveName ?: moveNamesById[moveId]?.name ?: ""}".trim()
        })
  }

  // The RETAIL tools get the same treatment: their names live at string 240000 + itemId
  // (measured - f/Gc0.fb on the live client), and a strings_en.xml entry at that id overrides
  // the name everywhere it renders. "TM24" becomes "TM Thunderbolt", "HM03" becomes "HM Surf".
  // TM and HM lists are separate because item id no longer decides the class: the ids right
  // after TM92 are the Unova HM block, and the higher regions keep HM blocks of their own.
  (RetailTools.renames().map { it to "TM" } + RetailTools.hmRenames().map { it to "HM" }).forEach {
      (rename, prefix) ->
    val (stringId, moveId) = rename
    if (stringId in occupied) return@forEach
    val moveName = moveNamesById[moveId]?.name ?: return@forEach
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          // The block's first slot is the bag pocket's class tag (RetailTools.CLASS_NAME_SLOTS).
          textContent = if (stringId in RetailTools.CLASS_NAME_SLOTS) prefix else "$prefix $moveName"
        })
  }

  // Location labels for the dex Wild Locations rows - string 140000 + region * 1000 + index.
  // Season-restricted spawns carry the season in the label, which is how this client generation
  // expresses seasons at all.
  locationNames.forEach { (stringId, label) ->
    if (stringId in occupied) return@forEach
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          textContent = label
        })
  }

  // Names and descriptions for every item the full import creates - the measured diff of the
  // Expansion's item table against the client's dumped inventory.
  ItemImportPlan.compute(expansionRoot).forEachIndexed { index, item ->
    listOf(
            ItemImportPlan.NAME_STRING_BASE + index to item.name,
            ItemImportPlan.DESC_STRING_BASE + index to item.description,
        )
        .forEach { (stringId, value) ->
          if (stringId in occupied || value.isEmpty()) return@forEach
          root.appendChild(
              document.createElement("string").apply {
                setAttribute("id", stringId.toString())
                textContent = value
              })
        }
  }

  // Names and descriptions for the imported evolution items; descriptions come from the
  // Expansion's own item table so the bag text is the real one, not the donor stone's.
  val expansionItems = ExpansionItems.parse(expansionRoot)
  EvoItemPlan.ITEMS.forEachIndexed { index, (symbol, name) ->
    val nameId = EvoItemPlan.NAME_STRING_BASE + index
    val descriptionId = EvoItemPlan.DESC_STRING_BASE + index
    val description = expansionItems[symbol]?.description.orEmpty()
    listOf(nameId to name, descriptionId to description).forEach { (stringId, value) ->
      if (stringId in occupied || value.isEmpty()) return@forEach
      root.appendChild(
          document.createElement("string").apply {
            setAttribute("id", stringId.toString())
            textContent = value
          })
    }
  }

  // A Pokedex tab takes its name from string id 250000 + region, and the client checks the string
  // exists before using it - so a region with no name here renders no tab at all.
  DEX_REGION_NAMES.forEach { (region, name) ->
    val stringId = REGION_NAME_STRING_BASE + region
    require(stringId !in occupied) {
      "String $stringId is already used; region $region cannot be named"
    }
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          textContent = name
        })
  }

  // The client renders a type badge from a string id. Fairy is patched into the type enum with an
  // id of its own because the stock ids come from the ROM, which has no Fairy to name.
  require(FairyTypePatch.FAIRY_NAME_STRING_ID !in occupied) {
    "Client string ${FairyTypePatch.FAIRY_NAME_STRING_ID} is taken; pick another for the Fairy type"
  }
  root.appendChild(
      document.createElement("string").apply {
        setAttribute("id", FairyTypePatch.FAIRY_NAME_STRING_ID.toString())
        textContent = "Fairy"
      })

  // A move with no name renders blank in a learnset, so every imported move gets its name and
  // description. The client reads both from the string table, which is how PokeMMO renames Hail.
  importedMoves.forEach { move ->
    listOf(
            MoveText.NAME_BASE + move.id to move.name,
            MoveText.DESCRIPTION_BASE + move.id to move.description,
        )
        .forEach { (stringId, value) ->
          if (stringId in occupied) return@forEach
          root.appendChild(
              document.createElement("string").apply {
                setAttribute("id", stringId.toString())
                textContent = value
              })
        }
  }

  // Category is the line under the species name in the dex ("Life Pokemon"). The reader
  // (zK0.qH1, bytecode) is NOT a plain 155000 + speciesId: for any species id past 493 it adds
  // 16 before the lookup - a gap in PokeMMO's own string table - and renders empty when the
  // string is missing. Writing at the unshifted id is why every imported species showed no
  // description line (operator-reported).
  species.forEach { entry ->
    val wireId = entry.clientWireId ?: return@forEach
    if (entry.categoryName.isBlank()) return@forEach
    val shifted = if (wireId > CATEGORY_SHIFT_THRESHOLD) wireId + CATEGORY_SHIFT else wireId
    val stringId = CATEGORY_STRING_BASE + shifted
    if (stringId in occupied) return@forEach
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          textContent = entry.categoryName
        })
  }

  species.forEach { entry ->
    val stringId = 150000 + checkNotNull(entry.clientWireId)
    require(stringId !in occupied) { "Client string $stringId already exists (${entry.stableId})" }
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          textContent = clientDisplayName(entry)
        })
  }

  // Stock rewordings. The battle-end lines 5017/5018 are written for a team ("{00} have defeated
  // {01}!"), which reads as a grammar slip for one trainer, and 5018 breaks the page before the
  // names; "{00} defeated {01}!" on one line fits both.
  (0 until root.childNodes.length)
      .map(root.childNodes::item)
      .filter { it.attributes?.getNamedItem("id")?.nodeValue?.toIntOrNull() in DEFEATED_STRING_IDS }
      .forEach { it.textContent = it.textContent.replace("have defeated", "defeated").replace("has defeated", "defeated").replace("\\n\\n", " ") }

  // The Safari Game's lines. The client's safari packet (s2c 0x3A, f/L6) prints the ROM's lines
  // in the DS regions and, in the GBA regions, six strings of its own table that this build's
  // strings_en.xml no longer carries - so they are staged here in FireRed's words
  // (src/battle_message.c), with the placeholders the client fills: {06} the monster, {23} the
  // trainer, {0F} the monster that fled.
  SAFARI_STRINGS.forEach { (stringId, value) ->
    require(stringId !in occupied) { "Client string $stringId is taken; pick another for the Safari line" }
    root.appendChild(
        document.createElement("string").apply {
          setAttribute("id", stringId.toString())
          textContent = value
        })
  }
  (0 until root.childNodes.length)
      .map(root.childNodes::item)
      .forEach { node ->
        val reworded = (PC_MENU_REWORDS + BALL_LINE_REWORDS)[node.attributes?.getNamedItem("id")?.nodeValue?.toIntOrNull()] ?: return@forEach
        node.textContent = reworded
      }

  Files.createDirectories(output.parent)
  TransformerFactory.newInstance()
      .newTransformer()
      .apply {
        setOutputProperty(OutputKeys.INDENT, "yes")
        setOutputProperty(OutputKeys.ENCODING, "UTF-8")
      }
      .transform(DOMSource(document), StreamResult(output.toFile()))
}

/** "{00} have defeated {01}!" (single line and boxed): reworded to "{00} defeated {01}!". */
private val DEFEATED_STRING_IDS = setOf(5017, 5018)

/**
 * The GBA-region strings of the client's safari packet (f/L6 fallbacks; see SafariEventPacket),
 * in FireRed's words: {23} = the trainer, {06} = the monster, {0F} = the monster that fled.
 */
private val SAFARI_STRINGS =
    mapOf(
        // The rock slot carries the Safari Ball throw: the rock itself goes through the bait toss.
        200271 to "{23} used\\nSafari Ball!",
        200272 to "{23} threw some BAIT\\nat the {06}!",
        200273 to "{06} is watching\\ncarefully!",
        200274 to "{06} is angry!",
        200275 to "{06} is eating!",
        200148 to "Wild {0F} fled!",
    )

/** The PC menu's first entry: stock "{01}'s PC" reads oddly beside GTL and Mail; MonMMO names the function (was hand-edited in the client before 2026-09-09). */
private val PC_MENU_REWORDS = mapOf(2351 to "Storage System")

/**
 * The bait event's toss template (kind -33, kinds 3..5: "{00} tossed a {01} to {02}!", {00} = the
 * thrower string from the packet). As a bare "{00}" the server prints any sentence through it -
 * FireRed's "{player} used {ball}!" before a throw. The bait throws themselves go through the
 * safari packet, so nothing else reads this template.
 */
private val BALL_LINE_REWORDS = mapOf(200532 to "{00}")

/** The dex category line sits 5000 above the species name in the string table. */
private const val CATEGORY_STRING_BASE = 155000

/** zK0.qH1: category lookups for species ids past 493 add 16 - PokeMMO's own table gap. */
private const val CATEGORY_SHIFT_THRESHOLD = 493
private const val CATEGORY_SHIFT = 16

/** The wire block the catalogue generator assigns to forms, after the last Dex-numbered slot. */
internal const val FIRST_FORM_WIRE_ID = 1079

/**
 * Forms share their base species' display name in the Expansion, which would put several identical
 * "Vulpix" entries in the client. The form suffix from the symbol becomes a parenthesised label:
 * SPECIES_VULPIX_ALOLA renders as "Vulpix (Alola)".
 */
private fun clientDisplayName(entry: ExpansionSpeciesDef): String {
  if (!entry.isForm) return entry.displayName
  val base = entry.baseSpeciesStableId.substringAfterLast("SPECIES_")
  val suffix = entry.symbol.removePrefix("SPECIES_").removePrefix(base).trim('_')
  if (suffix.isEmpty()) return entry.displayName
  val label =
      suffix.split('_').joinToString(" ") { part ->
        part.lowercase().replaceFirstChar(Char::uppercase)
      }
  return "${entry.displayName} ($label)"
}

/** The client's move table covers this many ids; beyond it a move has no definition. */
private const val LAST_CLIENT_MOVE_ID = 559

/** The last free per-region dex slot in the client's six-wide array. */
/**
 * The single Pokedex region the imported species are filed under.
 *
 * One, not one per generation: a species stores its per-region number in a six-slot array and the
 * client fills five, so slot 5 is the only one that exists. Widening that array is not enough,
 * because both the reader and data.pak's writer clamp an out-of-range region to the array's last
 * slot instead of rejecting it - so every extra region aliases onto the same entry and the tabs
 * show each other's contents.
 */
private val DEX_REGIONS =
    listOf(
        6 to 650..721,
        7 to 722..809,
        8 to 810..905,
        9 to 906..1025,
    )

/** Fairy is 19 once the classpath overlay patches the type enum. */
private const val MAX_CLIENT_TYPE = 19

/** The client keeps egg groups in a 0-16 range. */
private const val MAX_EGG_GROUP = 16

/** Every stock species record ends with this value; its meaning is not yet pinned down. */
private const val SPECIES_TRAILER = 255

/** The client and the decomp number egg groups identically, so the symbol maps straight across. */
private fun eggGroup(symbol: String): Int =
    when (symbol.removePrefix("EGG_GROUP_")) {
      "MONSTER" -> 1
      "WATER_1" -> 2
      "BUG" -> 3
      "FLYING" -> 4
      "FIELD" -> 5
      "FAIRY" -> 6
      "GRASS" -> 7
      "HUMAN_LIKE" -> 8
      "WATER_3" -> 9
      "MINERAL" -> 10
      "AMORPHOUS" -> 11
      "WATER_2" -> 12
      "DITTO" -> 13
      "DRAGON" -> 14
      "NO_EGGS_DISCOVERED" -> 15
      else -> 0
    }

/** The client keeps move ids from here up for its own event moves, so ours stay below it. */
private const val FIRST_CLIENT_OWN_MOVE_ID = 1000

/** The client spells a move that cannot miss as accuracy 101. */
private const val NEVER_MISS_ACCURACY = 101

/** A Pokedex tab's name lives at this id plus the region number. */
private const val REGION_NAME_STRING_BASE = 250000

/** Names for the regions added above, in the same order the client will show them. */
private val DEX_REGION_NAMES =
    listOf(
        6 to "Kalos",
        7 to "Alola",
        8 to "Galar",
        9 to "Paldea",
    )

/**
 * The shared TM plan: which taught moves get a TM item, in which order.
 *
 * The dex parses a row's TM number out of the item's name string, so the name and the item must
 * agree; this single deterministic list is used both by the string pass and by the overlay's item
 * creation so they cannot drift. Numbering starts above PokeMMO's own tools.
 */
object TmPlan {
  const val FIRST_NUMBER = 101
  const val NAME_STRING_BASE = 700000
  const val FIRST_ITEM_ID = 20000

  fun taughtMoves(expansionRoot: java.nio.file.Path): List<Int> =
      PorymovesLearnsets.parse(expansionRoot, MoveText.ids(expansionRoot))
          .taught
          .values
          .flatten()
          .toSortedSet()
          .toList()
}

/**
 * The evolution items the client never had, imported as real items.
 *
 * Each rides the proven tool pipeline: a live donor item is field-copied so kind, pocket and icon
 * stay valid, then the name, id and identity are ours. The donor is a real evolution stone, because
 * every one of these is a use-on-a-monster evolution item. Ids continue above the TM block.
 *
 * The evolution parameter the client stores is the item id minus 5000 - its loader shifts item
 * params by +5000 into the custom space, and injected entries take the identical path.
 */
object EvoItemPlan {
  const val FIRST_ITEM_ID = 21000
  const val NAME_STRING_BASE = 710000
  const val DESC_STRING_BASE = 730000

  /** A real stone to copy: Water Stone, a ROM item the registry always holds. */
  const val DONOR_ITEM_ID = 84

  val ITEMS =
      listOf(
          "ITEM_ICE_STONE" to "Ice Stone",
          "ITEM_SACHET" to "Sachet",
          "ITEM_WHIPPED_DREAM" to "Whipped Dream",
          "ITEM_TART_APPLE" to "Tart Apple",
          "ITEM_SWEET_APPLE" to "Sweet Apple",
          "ITEM_SYRUPY_APPLE" to "Syrupy Apple",
          "ITEM_CRACKED_POT" to "Cracked Pot",
          "ITEM_CHIPPED_POT" to "Chipped Pot",
          "ITEM_GALARICA_CUFF" to "Galarica Cuff",
          "ITEM_GALARICA_WREATH" to "Galarica Wreath",
          "ITEM_LINKING_CORD" to "Linking Cord",
          "ITEM_BLACK_AUGURITE" to "Black Augurite",
          "ITEM_PEAT_BLOCK" to "Peat Block",
          "ITEM_MALICIOUS_ARMOR" to "Malicious Armor",
          "ITEM_AUSPICIOUS_ARMOR" to "Auspicious Armor",
          "ITEM_MASTERPIECE_TEACUP" to "Masterpiece Teacup",
          "ITEM_UNREMARKABLE_TEACUP" to "Unremarkable Teacup",
          "ITEM_SCROLL_OF_DARKNESS" to "Scroll of Darkness",
          "ITEM_SCROLL_OF_WATERS" to "Scroll of Waters",
          "ITEM_METAL_ALLOY" to "Metal Alloy",
      )

  fun itemId(symbol: String): Int? =
      ITEMS.indexOfFirst { it.first == symbol }.takeIf { it >= 0 }?.let { FIRST_ITEM_ID + it }

  /** The value an evolution entry carries for this item, before the client's +5000 shift. */
  fun evolutionParam(symbol: String): Int? = itemId(symbol)?.minus(5000)
}
