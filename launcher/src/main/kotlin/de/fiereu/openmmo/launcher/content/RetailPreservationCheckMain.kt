package de.fiereu.openmmo.launcher.content

import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.system.exitProcess
import org.w3c.dom.Element
import org.xml.sax.InputSource

/**
 * Proves a staged build leaves retail alone.
 *
 * Every record the stock data.pak and string table carry must come through the staged build
 * unchanged, or changed only in a way the project owner approved (2026-09-12):
 * - learnsets: retail plus additions. A retail list may gain moves, never lose or reorder one.
 * - species detail: the Fairy retypes may change a retail species' egg groups, nothing else.
 * - moves: Sweet Kiss, Charm and Moonlight may become Fairy, nothing else (2026-09-13).
 * - strings: the rewordings named in [APPROVED_STRING_CHANGES].
 *
 * Anything else that differs fails. Records the build adds are counted, never judged. A section
 * with no codec is compared byte for byte.
 *
 * Usage: `<stock data.pak> <staged data.pak> <stock strings_en.xml> <staged strings_en.xml>`. Exits 1
 * when any retail record changed in an unapproved way or was removed.
 */
fun main(args: Array<String>) {
  require(args.size == 4) {
    "usage: <stock data.pak> <staged data.pak> <stock strings_en.xml> <staged strings_en.xml>"
  }
  val stock = ClientDataPak.parse(File(args[0]).readBytes())
  val staged = ClientDataPak.parse(File(args[1]).readBytes())

  val keyed =
      listOf(
          compareKeyed(SpeciesCodec, stock, staged, key = { it.speciesId.toString() }),
          compareKeyed(LevelUpLearnsetCodec, stock, staged, key = { it.speciesId.toString() }) { b, a ->
            pairwise(b, a) { retail, built -> isSubsequence(retail.moves, built.moves) }
          },
          compareKeyed(ExtraLearnsetCodec, stock, staged, key = { "${it.speciesId}/cat${it.category}" }) {
              b,
              a ->
            pairwise(b, a) { retail, built -> isSubsequence(retail.moves, built.moves) }
          },
          compareKeyed(SpeciesDetailCodec, stock, staged, key = { it.speciesId.toString() }) { b, a ->
            pairwise(b, a, ::onlyAdditions)
          },
          compareKeyed(RegionalDexCodec, stock, staged, key = { "region${it.regionId}" }),
          compareKeyed(MoveCodec, stock, staged, key = { it.moveId.toString() }) { b, a ->
            pairwise(b, a) { retail, built -> built == retail || isApprovedFairyRetype(retail, built) }
          },
          compareKeyed(MoveExtraCodec, stock, staged, key = { it.moveId.toString() }),
      )
  val codecTypes = setOf(10, 1, 2, 6, 11, 4, 12)
  val raw =
      stock.sectionTypes.filter { it !in codecTypes }.map { type ->
        if (type == ITEM_TABLE_SECTION) compareItemTable(stock, staged) else compareRaw(type, stock, staged)
      }
  val strings = compareStrings(File(args[2]).readText(), File(args[3]).readText())

  val results = keyed + raw + strings + romSpeciesAdditions(stock, staged)
  results.forEach { println(it.render()) }
  val broken = results.filter { !it.clean }
  if (broken.isEmpty()) {
    println("[retail-check] OK: every retail record is unchanged or changed only as approved")
  } else {
    println("[retail-check] FAILED: retail changed in ${broken.joinToString { it.label }}")
    exitProcess(1)
  }
}

/** Retail strings deliberately reworded: PC menu, the two battle-end lines, the ball line. */
private val APPROVED_STRING_CHANGES = setOf("2351", "5017", "5018", "200532")

private class Result(
    val label: String,
    val kept: Int,
    val approved: List<String>,
    val changed: List<String>,
    val removed: List<String>,
    val added: Int,
    val samples: List<String> = emptyList(),
) {
  val clean
    get() = changed.isEmpty() && removed.isEmpty()

  fun render(): String = buildString {
    append("[retail-check] ${if (clean) "ok  " else "FAIL"} $label: ")
    append(
        "retail kept=$kept approved=${approved.size} changed=${changed.size} " +
            "removed=${removed.size}; added=$added")
    listOf("approved" to approved, "changed" to changed, "removed" to removed).forEach { (name, ids) ->
      if (ids.isEmpty()) return@forEach
      append("\n    $name: ${ids.take(LIST_LIMIT).joinToString()}")
      if (ids.size > LIST_LIMIT) append(" ...")
    }
    samples.forEach { append("\n    $it") }
  }
}

private const val LIST_LIMIT = 40
private const val SAMPLE_COUNT = 10
private const val SAMPLE_WIDTH = 400
private const val SAMPLE_LEAD = 80

/**
 * Both renderings, starting a little before the first character where they differ. Non-ASCII is
 * escaped: the Windows console prints it as `?`, which reads like corruption that is not there.
 */
private fun sample(key: String, before: Any?, after: Any?): List<String> {
  fun ascii(value: Any?) =
      value.toString().map { if (it.code in 32..126) "$it" else "\\u%04x".format(it.code) }.joinToString("")
  val a = ascii(before)
  val b = ascii(after)
  val start = (a.commonPrefixWith(b).length - SAMPLE_LEAD).coerceAtLeast(0)
  val lead = if (start > 0) "..." else ""
  return listOf(
      "$key stock : $lead${a.drop(start).take(SAMPLE_WIDTH)}",
      "$key staged: $lead${b.drop(start).take(SAMPLE_WIDTH)}",
  )
}

/**
 * A retail detail record changed only by additions (project owner, 2026-09-12/13): egg groups, and
 * types only ever to Fairy (the retypes), evolutions after retail's, form entries after retail's.
 */
private fun onlyAdditions(retail: SpeciesDetail, built: SpeciesDetail): Boolean {
  val additive = RetailMerge.ADDITIVE_BITS
  val rest =
      built.copy(
          flags = (built.flags and additive.inv()) or (retail.flags and additive),
          eggGroups = retail.eggGroups,
          types = retail.types,
          evolutions = retail.evolutions,
          specialVariants = retail.specialVariants,
      )
  val typesOk = built.types == retail.types || built.types?.toList()?.contains(FAIRY_TYPE) == true
  val evolutionsKept = built.evolutions.orEmpty().take(retail.evolutions.orEmpty().size) == retail.evolutions.orEmpty()
  val retailVariants = retail.specialVariants
  val variantsKept =
      retailVariants == null ||
          built.specialVariants?.drop(1)?.take(retailVariants.size - 1) == retailVariants.drop(1)
  return rest == retail && typesOk && evolutionsKept && variantsKept
}

/**
 * A detail record the build ADDS for a species the ROM defines (retail had no record for it) is held
 * to the same additive fields - nothing else about a ROM species may change through data.
 */
private fun romSpeciesAdditions(stock: ClientDataPak, staged: ClientDataPak): Result {
  val label = "species detail added for ROM species (section 6)"
  val retailKeys =
      stock.payloadOf(SpeciesDetailCodec.type)?.let(SpeciesDetailCodec::decode).orEmpty().map { it.speciesId }.toSet()
  val added =
      staged.payloadOf(SpeciesDetailCodec.type)
          ?.let(SpeciesDetailCodec::decode)
          .orEmpty()
          .filter { it.speciesId in 1..LAST_ROM_SPECIES_ID && it.speciesId !in retailKeys }
  val (approved, changed) = added.partition { it.flags and RetailMerge.ADDITIVE_BITS.inv() == 0 }
  return Result(
      label,
      kept = 0,
      approved = approved.map { it.speciesId.toString() },
      changed = changed.map { it.speciesId.toString() },
      removed = emptyList(),
      added = 0,
      samples = changed.take(SAMPLE_COUNT).map { "${it.speciesId} flags=0x%04x".format(it.flags) },
  )
}

private const val FAIRY_TYPE = RetailMerge.FAIRY_TYPE

/**
 * A retail move record changed only by the approved Fairy retype (project owner, 2026-09-13): the id
 * is one of [RetailMerge.APPROVED_FAIRY_MOVE_RETYPES], retail's type was the ROM's (unflagged) or
 * Normal, and the staged record is retail's with bit 0x8 and type 19 - category, every other flag,
 * payload and effect unchanged. Any other move change still fails.
 */
internal fun isApprovedFairyRetype(retail: MoveRecord, built: MoveRecord): Boolean =
    retail.moveId in RetailMerge.APPROVED_FAIRY_MOVE_RETYPES &&
        (retail.type == null || retail.type == RetailMerge.NORMAL_TYPE) &&
        built == retail.copy(flags = retail.flags or MoveRecord.TYPE, type = FAIRY_TYPE)

/** The ROM defines species 1-649 and the form records 650-667. */
private const val LAST_ROM_SPECIES_ID = 667

/** Same number of records under the key, and each staged one relates to its retail one by [ok]. */
private fun <T> pairwise(retail: List<T>, built: List<T>, ok: (T, T) -> Boolean): Boolean =
    retail.size == built.size && retail.indices.all { ok(retail[it], built[it]) }

/** Every element of [small], in order, appears in [big]. */
private fun <E> isSubsequence(small: List<E>, big: List<E>): Boolean {
  var matched = 0
  for (element in big) if (matched < small.size && small[matched] == element) matched++
  return matched == small.size
}

private fun <T> compareKeyed(
    codec: SectionCodec<T>,
    stock: ClientDataPak,
    staged: ClientDataPak,
    key: (T) -> String,
    approve: (List<T>, List<T>) -> Boolean = { _, _ -> false },
): Result {
  val label = "${codec.name} (section ${codec.type})"
  val before = stock.payloadOf(codec.type)?.let(codec::decode)?.groupBy(key)
  val after = staged.payloadOf(codec.type)?.let(codec::decode)?.groupBy(key) ?: emptyMap()
  if (before == null) return Result(label, 0, emptyList(), emptyList(), emptyList(), after.size)
  val differing = before.keys.filter { it in after && after[it] != before[it] }
  val (approved, changed) = differing.partition { approve(before.getValue(it), after.getValue(it)) }
  val removed = before.keys.filter { it !in after }
  return Result(
      label,
      kept = before.size - differing.size - removed.size,
      approved = approved,
      changed = changed,
      removed = removed,
      added = after.keys.count { it !in before },
      samples = changed.take(SAMPLE_COUNT).flatMap { k -> sample(k, before[k], after[k]) },
  )
}

private const val ITEM_TABLE_SECTION = 3

/**
 * The item table (section 3) may only gain records after retail's (project owner, 2026-09-13): the
 * staged payload is retail's records byte for byte under a larger count, then records for new ids.
 */
private fun compareItemTable(stock: ClientDataPak, staged: ClientDataPak): Result {
  val label = "item table (section 3)"
  val before = stock.payloadOf(ITEM_TABLE_SECTION)!!
  val after =
      staged.payloadOf(ITEM_TABLE_SECTION)
          ?: return Result(label, 0, emptyList(), emptyList(), listOf("all"), 0)
  val retailCount = Reader(before).short()
  val stagedCount = Reader(after).short()
  val retailBytes = before.copyOfRange(2, before.size)
  val kept = after.size >= before.size && after.copyOfRange(2, before.size).contentEquals(retailBytes)
  if (!kept || stagedCount < retailCount) {
    return Result(label, 0, emptyList(), listOf("retail records changed"), emptyList(), 0)
  }
  // Appended records start with their u16 id; every one must be in the block for new items.
  val tail = Reader(after.copyOfRange(before.size, after.size))
  val added = stagedCount - retailCount
  val outside = mutableListOf<String>()
  repeat(added) {
    val id = tail.short()
    val flags = tail.int()
    if (id < ClientItemRecords.FIRST_NEW_ITEM_ID) outside += "$id"
    tail.short() // donor
    if (flags and ClientItemRecords.TAUGHT_MOVE != 0) tail.short()
    if (flags and ClientItemRecords.TEXT_AND_ICON != 0) {
      tail.int()
      tail.int()
      tail.short()
    }
  }
  check(tail.exhausted()) { "item table has ${tail.remaining()} bytes past its records" }
  return Result(label, retailCount, emptyList(), outside, emptyList(), added)
}

private fun compareRaw(type: Int, stock: ClientDataPak, staged: ClientDataPak): Result {
  val label = "section $type (bytes)"
  val before = stock.payloadOf(type)!!
  val after =
      staged.payloadOf(type)
          ?: return Result(label, 0, emptyList(), emptyList(), listOf("all"), 0)
  return if (before.contentEquals(after)) Result(label, 1, emptyList(), emptyList(), emptyList(), 0)
  else Result(label, 0, emptyList(), listOf("${before.size} -> ${after.size} bytes"), emptyList(), 0)
}

/**
 * Decoded, not raw: the staging writer re-serializes the whole file, turning `&#x2022;` into a
 * literal bullet and `>` into `&gt;`. Those are the same string to the client, so only the decoded
 * text and the non-id attributes are compared.
 */
private fun parseStrings(xml: String): Map<String, List<String>> {
  val factory =
      DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
      }
  val nodes =
      factory
          .newDocumentBuilder()
          .parse(InputSource(StringReader(xml)))
          .documentElement
          .getElementsByTagName("string")
  return (0 until nodes.length)
      .map { nodes.item(it) as Element }
      .groupBy(
          { it.getAttribute("id") },
          { element ->
            val attributes =
                (0 until element.attributes.length)
                    .map(element.attributes::item)
                    .filter { it.nodeName != "id" }
                    .sortedBy { it.nodeName }
                    .joinToString(" ") { "${it.nodeName}=${it.nodeValue}" }
            "$attributes|${element.textContent}"
          },
      )
}

private fun compareStrings(stockXml: String, stagedXml: String): Result {
  val before = parseStrings(stockXml)
  val after = parseStrings(stagedXml)
  val differing = before.keys.filter { it in after && after[it] != before[it] }
  val (approved, changed) = differing.partition { it in APPROVED_STRING_CHANGES }
  val removed = before.keys.filter { it !in after }
  return Result(
      "strings_en.xml",
      kept = before.size - differing.size - removed.size,
      approved = approved,
      changed = changed,
      removed = removed,
      added = after.keys.count { it !in before },
      samples = changed.take(SAMPLE_COUNT).flatMap { k -> sample(k, before[k], after[k]) },
  )
}
