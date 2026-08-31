package de.fiereu.openmmo.launcher.content

import java.nio.file.Path
import java.text.Normalizer

/**
 * The full item import: every Expansion item the client does not already have, by measured diff.
 *
 * The client's own inventory was dumped at runtime (`dumpitems` in the fixup helper wrote
 * `item-names.csv` - id;name for all 3203 registered items, now a checked-in calibration file), so
 * "missing" is computed by comparing names, not by guessing which generations PokeMMO covers. That
 * matters because its coverage is irregular: it holds an "Ability Patch" of its own, the usable
 * items live in a 5000+ block, and the low ids are mails and placeholders.
 *
 * Each missing item is created by the same donor-clone mechanism as the evolution items, with the
 * donor chosen by pocket so kind and bag placement are genuine. TMs have their own dedicated
 * pipeline and key items are story props with no behavior to inherit, so both stay out.
 */
object ItemImportPlan {
  const val FIRST_ITEM_ID = 22000
  const val NAME_STRING_BASE = 740000
  const val DESC_STRING_BASE = 750000

  /** Live client items to clone per pocket, ids verified against the calibration dump. */
  private val DONORS =
      mapOf(
          "POCKET_ITEMS" to 5234, // Leftovers - a plain held item
          "POCKET_MEDICINE" to 5017, // Potion
          "POCKET_BERRIES" to 5155, // Oran Berry
          "POCKET_POKE_BALLS" to 5004, // Poké Ball
      )

  data class Import(
      val symbol: String,
      val name: String,
      val description: String,
      val itemId: Int,
      val donorId: Int,
  )

  fun compute(expansionRoot: Path): List<Import> {
    val existing =
        javaClass.getResourceAsStream("/monmmo/item-names.csv")!!.bufferedReader().useLines { lines
          ->
          lines
              .mapNotNull { line -> line.substringAfter(';', "").takeIf(String::isNotBlank) }
              .map(::normalize)
              .toSet()
        }
    val imports = mutableListOf<Import>()
    ExpansionItems.parse(expansionRoot).values.forEach { item ->
      val donor = DONORS[item.pocket] ?: return@forEach
      if (item.name.isBlank() || item.name.startsWith("?")) return@forEach
      if (normalize(item.name) in existing) return@forEach
      if (RENAMED_IN_CLIENT[normalize(item.name)]?.let { normalize(it) in existing } == true) {
        return@forEach
      }
      imports +=
          Import(item.symbol, item.name, item.description, FIRST_ITEM_ID + imports.size, donor)
    }
    // The daycare's ability changer - not an Expansion item, but it rides the same pipeline.
    imports +=
        Import(
            symbol = "ITEM_MONMMO_ABILITY_PILL",
            name = "Ability Pill",
            description =
                "A pill that switches a Pokémon to another of its species' abilities. " +
                    "Sold by the Day Care for \$20,000.",
            itemId = FIRST_ITEM_ID + imports.size,
            donorId = DONORS.getValue("POCKET_MEDICINE"),
        )
    return imports
  }

  /**
   * Items Gen 6 renamed: the client holds them under the Gen 5 name, so a bare name diff would
   * import a duplicate. Every entry was verified against the calibration dump before listing.
   */
  private val RENAMED_IN_CLIENT =
      mapOf(
              "Paralyze Heal" to "Parlyz Heal",
              "X Defense" to "X Defend",
              "X Sp. Atk" to "X Special",
              "Leek" to "Stick",
              "Health Feather" to "Health Wing",
              "Muscle Feather" to "Muscle Wing",
              "Resist Feather" to "Resist Wing",
              "Genius Feather" to "Genius Wing",
              "Clever Feather" to "Clever Wing",
              "Swift Feather" to "Swift Wing",
              "Pretty Feather" to "Pretty Wing",
          )
          .mapKeys { (key, _) -> normalize(key) }

  /** Case, punctuation and accents removed, so "Poké Ball" and "Poke Ball" collide as intended. */
  private fun normalize(name: String): String =
      Normalizer.normalize(name, Normalizer.Form.NFD).lowercase().filter {
        it in 'a'..'z' || it in '0'..'9'
      }
}
