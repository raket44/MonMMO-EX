package de.fiereu.openmmo.items

import de.fiereu.openmmo.items.generated.GeneratedItems
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ItemRegistry @Inject constructor() {

  private val byId = ConcurrentHashMap<Int, ItemDef>()
  private val idsByItem = ConcurrentHashMap<ItemDef, List<Int>>()

  private val byGbaConstant = HashMap<String, ItemDef>()

  init {
    GeneratedItems.loadInto(this)
    loadImportedItems()
    registerGbaKeyItems()
  }

  /**
   * GBA story key items the modern catalogue dropped. Their wire id addresses the client's
   * per-region item table (regionId * 1000 + the game's own item index), so the client shows the
   * real FireRed/Emerald name and description - Oak's Parcel is Oak's, not Gen 4's Twinleaf one.
   */
  private fun registerGbaKeyItems() {
    fun add(regionId: Int, constant: String, gbaId: Int, name: String) {
      val wireId = regionId * GBA_REGION_TABLE + gbaId
      if (byId.containsKey(wireId)) return
      val item = ItemDef(name, 0)
      register(item, wireId)
      byGbaConstant[constant] = item
    }
    // FireRed (region 0 table).
    add(0, "OAKS_PARCEL", 349, "Oak's Parcel")
    add(0, "POKE_FLUTE", 350, "Poké Flute")
    add(0, "SECRET_KEY", 351, "Secret Key")
    add(0, "BIKE_VOUCHER", 352, "Bike Voucher")
    add(0, "GOLD_TEETH", 353, "Gold Teeth")
    add(0, "CARD_KEY", 355, "Card Key")
    add(0, "LIFT_KEY", 356, "Lift Key")
    add(0, "SILPH_SCOPE", 359, "Silph Scope")
    add(0, "FAME_CHECKER", 363, "Fame Checker")
    add(0, "TEACHY_TV", 366, "Teachy TV")
    add(0, "TRI_PASS", 367, "Tri-Pass")
    add(0, "RAINBOW_PASS", 368, "Rainbow Pass")
    add(0, "TEA", 369, "Tea")
    add(0, "POWDER_JAR", 372, "Powder Jar")
    add(0, "RUBY", 373, "Ruby")
    add(0, "SAPPHIRE", 374, "Sapphire")
    // Emerald (region 1 table).
    add(1, "MACH_BIKE", 259, "Mach Bike")
    add(1, "ITEMFINDER", 261, "Itemfinder")
    add(1, "CONTEST_PASS", 266, "Contest Pass")
    add(1, "WAILMER_PAIL", 268, "Wailmer Pail")
    add(1, "DEVON_GOODS", 269, "Devon Goods")
    add(1, "SOOT_SACK", 270, "Soot Sack")
    add(1, "BASEMENT_KEY", 271, "Basement Key")
    add(1, "ACRO_BIKE", 272, "Acro Bike")
    add(1, "POKEBLOCK_CASE", 273, "Pokéblock Case")
    add(1, "LETTER", 274, "Letter")
    add(1, "EON_TICKET", 275, "Eon Ticket")
    add(1, "SCANNER", 278, "Scanner")
    add(1, "GO_GOGGLES", 279, "Go-Goggles")
    add(1, "METEORITE", 280, "Meteorite")
    add(1, "ROOM_1_KEY", 281, "Rm. 1 Key")
    add(1, "ROOM_2_KEY", 282, "Rm. 2 Key")
    add(1, "ROOM_4_KEY", 283, "Rm. 4 Key")
    add(1, "ROOM_6_KEY", 284, "Rm. 6 Key")
    add(1, "STORAGE_KEY", 285, "Storage Key")
    add(1, "DEVON_SCOPE", 288, "Devon Scope")
    add(1, "MYSTIC_TICKET", 370, "Mystic Ticket")
    add(1, "AURORA_TICKET", 371, "Aurora Ticket")
    add(1, "MAGMA_EMBLEM", 375, "Magma Emblem")
    add(1, "OLD_SEA_MAP", 376, "Old Sea Map")
  }

  /**
   * Items the launcher's overlay creates inside the client - imported TMs, evolution items like the
   * Ice Stone, and the Gen 6+ catalogue. The manifest is written by the client staging task
   * (id;name per line) and copied here, so the server can address exactly what the client shows.
   * Absent manifest means a build without the imported content; the retail registry stands alone.
   */
  private fun loadImportedItems() {
    val manifest = javaClass.getResourceAsStream("/monmmo/imported-items.csv") ?: return
    manifest.bufferedReader().useLines { lines ->
      lines
          .filter { ';' in it }
          .forEach { line ->
            val id = line.substringBefore(';').toIntOrNull() ?: return@forEach
            val name = line.substringAfter(';').trim()
            if (name.isNotEmpty() && byId[id] == null) register(ItemDef(name, 0), id)
          }
    }
  }

  fun register(item: ItemDef, vararg ids: Int) {
    for (id in ids) {
      val previous = byId.put(id, item)
      // Generated data, so a clash is a codegen bug rather than bad input.
      check(previous == null || previous === item) {
        "Item id $id is claimed by both ${previous?.name} and ${item.name}"
      }
    }
    idsByItem.merge(item, ids.toList()) { old, new -> (old + new).distinct().sorted() }
  }

  fun get(id: Int): ItemDef? = byId[id]

  fun idsOf(item: ItemDef): List<Int> = idsByItem[item].orEmpty()

  /** An item with several ids is named by its lowest. */
  fun idOf(item: ItemDef): Int =
      idsOf(item).firstOrNull() ?: error("Item '${item.name}' has no id in this build")

  fun idOrNull(item: ItemDef): Int? = idsOf(item).firstOrNull()

  fun all(): Collection<ItemDef> = idsByItem.keys

  fun size(): Int = idsByItem.size

  /**
   * Resolves a GBA script item constant ("ITEM_POKE_BALL") against the catalogue by mangling
   * display names the same way the generator does, plus aliases for the gen-3 spellings the scripts
   * use that the modern catalogue renamed.
   */
  fun byScriptConstant(token: String): ItemDef? {
    if (!token.startsWith("ITEM_")) return null
    val constant = token.removePrefix("ITEM_")
    return byConstantName.value[GEN3_ALIASES[constant] ?: constant] ?: byGbaConstant[constant]
  }

  // The same mangling ItemDataParser.identifierOf applies, so the script constant for a retail
  // item is exactly ITEM_ plus this.
  private val byConstantName = lazy {
    idsByItem.keys.associateBy { item ->
      java.text.Normalizer.normalize(item.name, java.text.Normalizer.Form.NFD)
          .replace(Regex("\\p{Mn}+"), "")
          .uppercase(java.util.Locale.ROOT)
          .replace(Regex("[^A-Z0-9]+"), "_")
          .trim('_')
    }
  }

  private companion object {
    const val GBA_REGION_TABLE = 1000
    val GEN3_ALIASES =
        mapOf(
            "PARLYZ_HEAL" to "PARALYZE_HEAL",
            "THUNDERSTONE" to "THUNDER_STONE",
            "X_DEFEND" to "X_DEFENSE",
            "X_SPECIAL" to "X_SP_ATK",
            "ELIXER" to "ELIXIR",
            "MAX_ELIXER" to "MAX_ELIXIR",
            "ENERGYPOWDER" to "ENERGY_POWDER",
            "TINYMUSHROOM" to "TINY_MUSHROOM",
            "DEEPSEATOOTH" to "DEEP_SEA_TOOTH",
            "DEEPSEASCALE" to "DEEP_SEA_SCALE",
            "NEVERMELTICE" to "NEVER_MELT_ICE",
            "TWISTEDSPOON" to "TWISTED_SPOON",
            "SILVERPOWDER" to "SILVER_POWDER",
            "BLACKGLASSES" to "BLACK_GLASSES",
            "BRIGHTPOWDER" to "BRIGHT_POWDER",
            "UP_GRADE" to "UPGRADE",
            "KINGS_ROCK" to "KING_S_ROCK",
            "SS_TICKET" to "S_S_TICKET",
        )
  }
}
