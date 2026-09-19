@file:JvmName("ShowdownSpriteFetchMain")

package de.fiereu.openmmo.launcher.content

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesDef
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Pokemon Showdown's Gen 5-style sprite set (the Smogon Sprite Project), fetched into
 * reference/sprite-packs/showdown and served to the mod ahead of the operator's still packs.
 *
 * Ten folders matter: `gen5`, `gen5-shiny`, `gen5-back`, `gen5-back-shiny` (96x96 stills, every
 * species and form through Gen 9), `gen5ani`, `gen5ani-back` (Gen 5-style animated GIFs, which stop
 * where the Smogon project stopped and have no shinies) and `ani`, `ani-back`, `ani-shiny`,
 * `ani-back-shiny` (the XY-era animated set: every later species, the fan Megas, and real shiny
 * animations). A Gen 5-style animation is used first, the XY-era one when there is none, a still
 * only when there is no animation at all - a still with a synthesized bob was the "looks like shit"
 * the operator saw on Mega Clefable. Files are named by Showdown's species id - the
 * lower-cased name with a hyphenated form: `ninetales-alola`, `charizard-megax`, `vivillon-polar`.
 *
 * The scope is what the mod stages: everything past the client's stock Dex 1-649, plus the
 * regional, Mega, Primal, Gigantamax and Totem forms of stock species. Stock species themselves
 * and their cosmetic forms the client already draws (Unown letters, Rotom appliances...) are never
 * touched (operator-directed).
 */
class ShowdownIndex private constructor(private val byDex: Map<Int, List<Candidate>>) {

  private data class Candidate(val slug: String, val formeId: String)

  /** Showdown's id for [entry], or null when it is out of scope or has no counterpart. */
  fun slugFor(entry: ExpansionSpeciesDef, bySymbol: Map<String, ExpansionSpeciesDef>): String? {
    val dex = entry.nationalDexId ?: return null
    val candidates = byDex[dex] ?: return null
    val base = candidates.firstOrNull { it.formeId.isEmpty() } ?: return null
    val symbol = normalize(entry.symbol.removePrefix("SPECIES_"))
    val family = base.slug
    val suffix = if (symbol.startsWith(family)) symbol.removePrefix(family) else formSuffix(entry, bySymbol)
    if (dex <= LAST_STOCK_DEX && SPECIAL_FORMS.none { it in suffix }) return null
    if (suffix.isEmpty()) return base.slug
    candidates.firstOrNull { it.formeId == suffix }?.let {
      return it.slug
    }
    return candidates
        .filter { it.formeId.isNotEmpty() && (suffix.endsWith(it.formeId) || suffix.startsWith(it.formeId)) }
        .maxByOrNull { it.formeId.length }
        ?.slug ?: base.slug
  }

  private fun formSuffix(entry: ExpansionSpeciesDef, bySymbol: Map<String, ExpansionSpeciesDef>): String {
    val baseSymbol = entry.baseSpeciesStableId.substringAfter("SPECIES_")
    val own = entry.symbol.removePrefix("SPECIES_")
    return normalize(own.removePrefix(baseSymbol).trimStart('_'))
  }

  companion object {
    const val LAST_STOCK_DEX = 649
    private val SPECIAL_FORMS = listOf("alola", "galar", "hisui", "paldea", "mega", "primal", "gmax", "totem")

    /** Showdown ids: lower-case ASCII letters and digits only (Flabébé -> flabebe). */
    fun normalize(text: String): String =
        java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
            .lowercase()
            .filter { it in 'a'..'z' || it in '0'..'9' }

    /** Builds the index from Showdown's pokedex.json (play.pokemonshowdown.com/data/pokedex.json). */
    fun load(pokedexJson: Path): ShowdownIndex {
      val root = Json.parseToJsonElement(Files.readString(pokedexJson)).jsonObject
      val numByName = mutableMapOf<String, Int>()
      root.forEach { (_, value) ->
        val obj = value.jsonObject
        val num = obj["num"]?.jsonPrimitive?.intOrNull ?: return@forEach
        numByName[obj["name"]!!.jsonPrimitive.content] = num
      }
      val byDex = mutableMapOf<Int, MutableList<Candidate>>()
      root.forEach { (slug, value) ->
        val obj: JsonObject = value.jsonObject
        val name = obj["name"]!!.jsonPrimitive.content
        val cosmetic = obj["isCosmeticForme"]?.jsonPrimitive?.booleanOrNull == true
        val baseName =
            obj["baseSpecies"]?.jsonPrimitive?.content
                ?: if (cosmetic) numByName.keys.filter { name.startsWith("$it-") }.maxByOrNull { it.length }
                else name
        val num = obj["num"]?.jsonPrimitive?.intOrNull ?: baseName?.let(numByName::get) ?: return@forEach
        if (num <= 0) return@forEach
        val formeId = if (baseName == null || baseName == name) "" else normalize(name.removePrefix(baseName))
        // Sprite FILES keep a hyphen between species and form ("ninetales-alola.png") even though
        // the dex key does not ("ninetalesalola"); the key of the base entry is the species part.
        val fileSlug = if (formeId.isEmpty()) slug else "${normalize(baseName!!)}-$formeId"
        byDex.getOrPut(num) { mutableListOf() }.add(Candidate(fileSlug, formeId))
      }
      return ShowdownIndex(byDex)
    }
  }
}

/** The fetched Showdown files for one species, by side; each is null when Showdown has none. */
data class ShowdownSpriteSet(
    val aniFront: Path?,
    val aniBack: Path?,
    /** Shiny animations exist only in the XY-era set; null means recolour the normal one. */
    val aniFrontShiny: Path?,
    val aniBackShiny: Path?,
    val front: Path?,
    val frontShiny: Path?,
    val back: Path?,
    val backShiny: Path?,
)

/** Reads what the fetch left under [root] through the manifest it wrote (symbol -> slug). */
class ShowdownSprites(private val root: Path) {
  private val slugs: Map<String, String> =
      root.resolve(MANIFEST).takeIf { Files.isRegularFile(it) }?.let { manifest ->
        Files.readAllLines(manifest)
            .drop(1)
            .mapNotNull { line -> line.split(',').takeIf { it.size >= 3 }?.let { it[0] to it[2] } }
            .toMap()
      } ?: emptyMap()

  val available: Boolean
    get() = slugs.isNotEmpty()

  fun resolve(symbol: String): ShowdownSpriteSet? {
    val slug = slugs[symbol] ?: return null
    fun file(folder: String, ext: String): Path? =
        root.resolve(folder).resolve("$slug.$ext").takeIf { Files.isRegularFile(it) }
    val gen5Front = file("gen5ani", "gif")
    val gen5Back = file("gen5ani-back", "gif")
    val set =
        ShowdownSpriteSet(
            aniFront = gen5Front ?: file("ani", "gif"),
            aniBack = gen5Back ?: file("ani-back", "gif"),
            aniFrontShiny = if (gen5Front == null) file("ani-shiny", "gif") else null,
            aniBackShiny = if (gen5Back == null) file("ani-back-shiny", "gif") else null,
            front = file("gen5", "png"),
            frontShiny = file("gen5-shiny", "png"),
            back = file("gen5-back", "png"),
            backShiny = file("gen5-back-shiny", "png"),
        )
    return if (set.front == null && set.aniFront == null) null else set
  }

  companion object {
    const val MANIFEST = "manifest.csv"
    val FOLDERS =
        listOf(
            "gen5" to "png", "gen5-shiny" to "png", "gen5-back" to "png", "gen5-back-shiny" to "png",
            "gen5ani" to "gif", "gen5ani-back" to "gif",
            "ani" to "gif", "ani-back" to "gif", "ani-shiny" to "gif", "ani-back-shiny" to "gif",
        )
  }
}

/**
 * Downloads the in-scope sprites: `<showdownRoot>` must already hold pokedex.json. Existing files
 * are kept, 404s are remembered in `404.txt` so reruns only retry what changed.
 */
fun main(args: Array<String>) {
  require(args.size >= 2) { "Usage: <showdown-root> <expansion-root> [maxDownloads]" }
  val root = Path.of(args[0])
  val maxDownloads = args.getOrNull(2)?.toIntOrNull() ?: Int.MAX_VALUE
  val index = ShowdownIndex.load(root.resolve("pokedex.json"))
  val expansion = ExpansionSpeciesRegistry().all()
  val bySymbol = expansion.associateBy { it.symbol }
  val staged =
      expansion.filter {
        it.clientContentCompatible && (it.isNewToClient || (it.clientWireId ?: 0) >= FIRST_FORM_WIRE_ID)
      }
  val manifest = mutableListOf<String>()
  val slugs = linkedSetOf<String>()
  var unmapped = 0
  staged.forEach { entry ->
    val slug = index.slugFor(entry, bySymbol)
    if (slug == null) {
      unmapped++
      return@forEach
    }
    manifest += "${entry.symbol},${entry.clientWireId},$slug,${entry.nationalDexId}"
    slugs += slug
  }
  Files.createDirectories(root)
  Files.write(root.resolve(ShowdownSprites.MANIFEST), listOf("symbol,wireId,slug,nationalDex") + manifest)
  println("[showdown] ${manifest.size} staged records mapped to ${slugs.size} Showdown ids ($unmapped out of scope)")

  val missingFile = root.resolve("404.txt")
  val known404 = if (Files.isRegularFile(missingFile)) Files.readAllLines(missingFile).toMutableSet() else mutableSetOf()
  val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
  var downloaded = 0
  var skipped = 0
  var missing = 0
  var failed = 0
  loop@ for (slug in slugs) {
    for ((folder, ext) in ShowdownSprites.FOLDERS) {
      val key = "$folder/$slug.$ext"
      val target = root.resolve(folder).resolve("$slug.$ext")
      if (Files.isRegularFile(target) || key in known404) {
        skipped++
        continue
      }
      if (downloaded >= maxDownloads) break@loop
      val request = HttpRequest.newBuilder(URI.create("$BASE/$key")).GET().build()
      val response =
          try {
            client.send(request, HttpResponse.BodyHandlers.ofByteArray())
          } catch (error: Exception) {
            failed++
            println("[showdown] $key: $error")
            continue
          }
      when (response.statusCode()) {
        200 -> {
          Files.createDirectories(target.parent)
          Files.write(target, response.body())
          downloaded++
        }
        404 -> {
          known404 += key
          missing++
        }
        else -> {
          failed++
          println("[showdown] $key: HTTP ${response.statusCode()}")
        }
      }
      Thread.sleep(40)
    }
  }
  Files.write(missingFile, known404.sorted())
  println("[showdown] downloaded=$downloaded kept=$skipped absent=$missing failed=$failed")
}

private const val BASE = "https://play.pokemonshowdown.com/sprites"
