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
    // The client rides FRLG's Bicycle (360); the generated table lists the item only under the
    // Gen 5-numbered 5450. Registering 360 on the same item makes it the id scripts hand out
    // (idOf takes the lowest), so there is one Bicycle, not two.
    register(de.fiereu.openmmo.items.generated.Items.BICYCLE, 360)
    registerClientTools()
    registerBerryFarming()
  }

  /**
   * Every tool the client defines itself (ClientTools) that the catalogue does not list - PokeMMO's
   * 1600-1782 block and its 7600 copies - as "TM <move>" / "HM <move>" at its client id, one item per
   * move. Scripts hand these out now that our duplicates are gone (Route 4's Mega Punch is 1710).
   */
  private fun registerClientTools() {
    val moveNames by lazy { de.fiereu.openmmo.moves.MoveRegistry().all().associate { it.id to it.name } }
    // ONE item per MOVE, never per number (owner, 2026-09-22): TM27 is Return in one game and
    // something else in another, so the catalogue's numbered entries ("HM01" at 5420, "TM70" at
    // 5397, "TM25") all fold onto the move's item, whatever band they came from. A grant stores
    // the lowest id, so a second Thunder from another region stacks on the first, and Unova's Cut
    // is the same HM Cut as Kanto's. Any HM id among them makes the item an HM everywhere - Flash
    // is HM Flash, not "TM Flash", TM70 included.
    ClientTools.itemToMove.entries
        .groupBy({ it.value }, { it.key })
        .forEach { (moveId, itemIds) ->
          val name = moveNames[moveId] ?: return@forEach
          val item = ItemDef("${if (itemIds.any(::isHmId)) "HM" else "TM"} $name", 0)
          for (id in itemIds.sorted()) fold(id, item)
        }
  }

  /** Move [id] onto [item], off whatever item held it; an item left with no ids is forgotten. */
  private fun fold(id: Int, item: ItemDef) {
    val previous = byId.put(id, item)
    if (previous != null && previous !== item) {
      val left = idsByItem[previous].orEmpty() - id
      if (left.isEmpty()) idsByItem.remove(previous) else idsByItem[previous] = left
    }
    idsByItem.merge(item, listOf(id)) { old, new -> (old + new).distinct().sorted() }
  }

  /**
   * PokeMMO's own berry-farming items, which no Gen 3/5 table carries: the Harvesting Tools, the
   * ten seeds and the Watering Can, at the client ids the dumped item-names.csv shows (1028-1039,
   * 4561). The client draws and stacks them; BerryPlotService plants and harvests with them.
   */
  private fun registerBerryFarming() {
    val own =
        listOf(
            1028 to "Harvesting Tool", 1029 to "Unbreakable Harvesting Tool",
            1030 to "Plain Spicy Seed", 1031 to "Very Spicy Seed", 1032 to "Plain Dry Seed", 1033 to "Very Dry Seed",
            1034 to "Plain Sweet Seed", 1035 to "Very Sweet Seed", 1036 to "Plain Bitter Seed", 1037 to "Very Bitter Seed",
            1038 to "Plain Sour Seed", 1039 to "Very Sour Seed", 4561 to "Watering Can")
    for ((id, name) in own) if (!byId.containsKey(id)) register(ItemDef(name, 0), id)
  }

  /** The client's HM blocks (f/ls0.jU0). */
  private fun isHmId(id: Int): Boolean =
      id in 339..346 || id in 5420..5425 || id in 8420..8427 || id in 1297..1298 || id in 9420..9427

  /**
   * The GBA story key items, pinned to their Gen 3 ids so a script constant resolves to the entry
   * the client names - not a Gen 4/5 namesake in the 5000 band (Oak's Parcel is Oak's, not Gen 4's
   * Twinleaf one). The DS bicycles are the one thing still minted at region * 1000 + index.
   */
  private fun registerGbaKeyItems() {
    fun add(regionId: Int, constant: String, gbaId: Int, name: String) {
      // Kanto and Hoenn share the Gen 3 index at RAW ids - the 1000 band the "Hoenn table" once
      // used is PokeMMO's own items (1259 Super Carbos). The DS bicycles keep region * 1000.
      val wireId = if (regionId <= 1) gbaId else regionId * GBA_REGION_TABLE + gbaId
      // The catalogue already lists most of these at that very id: the constant must still
      // point there, or the name index picks a Gen 5-numbered namesake (Secret Key 5467).
      byId[wireId]?.let { existing ->
        byGbaConstant.putIfAbsent(constant, existing)
        return
      }
      val item = ItemDef(name, 0)
      register(item, wireId)
      byGbaConstant[constant] = item
    }
    // The DS games' Bicycle (item 433 in DPPt, HGSS and BW): Eterna's Rad Rickshaw, Goldenrod's
    // bike shop and Unova's Route 3 day-care man hand it out; StoryPlayerService turns every
    // regional bike into the client's Bicycle (360).
    add(2, "BICYCLE_UNOVA", 433, "Bicycle")
    add(3, "BICYCLE_SINNOH", 433, "Bicycle")
    add(4, "BICYCLE_JOHTO", 433, "Bicycle")
    // The GBA HMs (339-346) are folded onto the per-move items by registerClientTools, like
    // every other band's; ITEM_HMnn resolves by the Gen 3 move (byScriptConstant), so HM05 IS
    // Flash - the catalogue's Gen 5-numbered "HM05" (5424) is Waterfall, which is what Oak's
    // aide handed out until 2026-09-08.
    // Gen 3 key items both games hand out, at the ids both games use.
    add(0, "COIN_CASE", 260, "Coin Case")
    add(0, "ITEMFINDER", 261, "Itemfinder")
    add(0, "OLD_ROD", 262, "Old Rod")
    add(0, "GOOD_ROD", 263, "Good Rod")
    add(0, "SUPER_ROD", 264, "Super Rod")
    add(0, "SS_TICKET", 265, "S.S. Ticket")
    add(0, "TOWN_MAP", 358, "Town Map")
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

  /**
   * The client lists every 5000-band item a second time at +1000 (6233 and 5233 are both Metal
   * Coat, name for name across the whole band). An id from that mirror band that nothing claims
   * resolves to the 5000-band item, so a held 6233 is a Metal Coat to battle and evolution alike.
   */
  fun get(id: Int): ItemDef? =
      byId[id]
          ?: byId[ItemIdAliases.canonical(id)].takeIf { ItemIdAliases.canonical(id) != id }
          ?: if (id in MIRROR_ITEM_BAND) byId[id - 1000] else null

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
  /**
   * [regionId] picks between namesakes: the catalogue lists "Super Rod" at 264 (Gen 3) and 5447
   * (Gen 5), and a Kanto script and a Unova script both spell it ITEM_SUPER_ROD. Kanto and Hoenn
   * take the Gen 3 entry, Unova the 5000 band, Sinnoh 8000, Johto 9000. Without a region the last
   * registered namesake wins, as it always did (the support analyzer only asks "does it exist").
   */
  fun byScriptConstant(token: String, regionId: Int? = null): ItemDef? {
    if (!token.startsWith("ITEM_")) return null
    val constant = token.removePrefix("ITEM_")
    fun named(name: String): ItemDef? = preferBand(allByConstantName.value[name].orEmpty(), regionId)
    // Machines by the Gen 3 move, never by number: TMnn is the imported "TM <move>" item, HMnn
    // the GBA-band HM registered above.
    MACHINE.matchEntire(constant)?.let { m ->
      val index = m.groupValues[2].toInt() - 1
      // HMnn by the Gen 3 move: the per-move item every band folded onto (registerClientTools).
      if (m.groupValues[1] == "HM") return GEN3_HM_MOVES.getOrNull(index)?.let(::clientTool) ?: byGbaConstant[constant]
      val move = GEN3_TM_MOVES.getOrNull(index) ?: return null
      return byConstantName.value["TM_" + mangle(move)] ?: clientTool(move) ?: byGbaConstant[constant]
    }
    // A machine named by its move (the Expansion's ITEM_TM_FACADE, Norman's gym reward): the
    // client's own tool for that move, now that our "TM <move>" duplicates are not created.
    MOVE_MACHINE.matchEntire(constant)?.let { match ->
      (byConstantName.value[constant] ?: clientTool(match.groupValues[1]))?.let {
        return it
      }
    }
    // The aliases run BOTH ways: FRLG scripts spell gen-3 (ITEM_PARLYZ_HEAL) against modern
    // catalogue names, while pret's Emerald uses modern constants (ITEM_PARALYZE_HEAL) against
    // catalogue entries that kept the gen-3 spelling. One direction stranded whole mart shelves.
    val alias = GEN3_ALIASES[constant] ?: GEN3_REVERSE[constant]
    // A GBA region's own key item first: the registry knows exactly which entry Oak's Parcel or
    // the S.S. Ticket is, and the name index would hand a Gen 5 namesake to a Kanto script.
    if (regionId != null && regionId <= 1) {
      (byGbaConstant[constant] ?: alias?.let { byGbaConstant[it] })?.let { return it }
    }
    return named(constant)
        ?: alias?.let { named(it) }
        ?: byGbaConstant[constant]
        ?: alias?.let { byGbaConstant[it] }
  }

  /** Of several catalogue namesakes, the one whose id sits in [regionId]'s band; else the last. */
  private fun preferBand(candidates: List<ItemDef>, regionId: Int?): ItemDef? {
    if (candidates.isEmpty()) return null
    val band =
        when (regionId) {
          0, 1 -> 0..999
          2 -> 5000..5999
          3 -> 8000..8999
          4 -> 9000..9999
          else -> return candidates.last()
        }
    return candidates.firstOrNull { item -> idsByItem[item].orEmpty().any { it in band } } ?: candidates.last()
  }

  /**
   * The client's own tool for the move named [moveName] (ClientTools): the retail TM we would
   * otherwise have duplicated - Water Pulse is PokeMMO's 1601, Rock Tomb Gen 5's TM39 (5366). A tool
   * the catalogue does not list is registered as "TM <move>" at its client id.
   */
  private fun clientTool(moveName: String): ItemDef? {
    val moveId = moveIdsByName.value[mangle(moveName)] ?: return null
    val toolId = ClientTools.toolFor(moveId) ?: return null
    byId[toolId]?.let {
      return it
    }
    return ItemDef("TM $moveName", 0).also { register(it, toolId) }
  }

  private val moveIdsByName = lazy {
    de.fiereu.openmmo.moves.MoveRegistry().all().associate { mangle(it.name) to it.id }
  }

  // The same mangling ItemDataParser.identifierOf applies, so the script constant for a retail
  // item is exactly ITEM_ plus this.
  private val byConstantName = lazy { idsByItem.keys.associateBy { item -> mangle(item.name) } }
  /** Every namesake under a mangled name - the same display name sits in several bands. */
  private val allByConstantName = lazy { idsByItem.keys.groupBy { item -> mangle(item.name) } }

  private fun mangle(name: String): String =
      java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
          .replace(Regex("\\p{Mn}+"), "")
          .uppercase(java.util.Locale.ROOT)
          .replace(Regex("[^A-Z0-9]+"), "_")
          .trim('_')

  private companion object {
    const val GBA_REGION_TABLE = 1000

    private val MACHINE = Regex("^(TM|HM)(\\d\\d)$")

    /** TM_FACADE / HM_SURF: a machine constant spelled with its move. */
    private val MOVE_MACHINE = Regex("^(?:TM|HM)_([A-Z0-9_]+)$")

    /** HM01-HM08 in FireRed and Emerald. */
    val GEN3_HM_MOVES = listOf("Cut", "Fly", "Surf", "Strength", "Flash", "Rock Smash", "Waterfall", "Dive")

    /** TM01-TM50 in FireRed and Emerald (the same list in both). */
    val GEN3_TM_MOVES =
        listOf(
            "Focus Punch", "Dragon Claw", "Water Pulse", "Calm Mind", "Roar", "Toxic", "Hail", "Bulk Up",
            "Bullet Seed", "Hidden Power", "Sunny Day", "Taunt", "Ice Beam", "Blizzard", "Hyper Beam",
            "Light Screen", "Protect", "Rain Dance", "Giga Drain", "Safeguard", "Frustration", "Solar Beam",
            "Iron Tail", "Thunderbolt", "Thunder", "Earthquake", "Return", "Dig", "Psychic", "Shadow Ball",
            "Brick Break", "Double Team", "Reflect", "Shock Wave", "Flamethrower", "Sludge Bomb", "Sandstorm",
            "Fire Blast", "Rock Tomb", "Aerial Ace", "Torment", "Facade", "Secret Power", "Rest", "Attract",
            "Thief", "Steel Wing", "Skill Swap", "Snatch", "Overheat",
        )

    /** The client's second copy of the 5000-band items: 5000-band id + 1000. */
    val MIRROR_ITEM_BAND = 6000..6999
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
    val GEN3_REVERSE = GEN3_ALIASES.entries.associate { (gen3, modern) -> modern to gen3 }
  }
}
