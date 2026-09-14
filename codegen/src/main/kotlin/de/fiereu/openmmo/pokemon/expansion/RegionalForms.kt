package de.fiereu.openmmo.pokemon.expansion

/**
 * Regional forms (Alolan, Galarian, Paldean) - one rule shared by the client staging and the server
 * (project owner, 2026-09-13/14):
 * - each takes its species' place in its region's dex tab, in that dex's own order (Alola's Raichu
 *   slot is Alolan Raichu), and stays out of the National list;
 * - it also counts toward its base species' National entry, as in the games (Alolan Vulpix is #037);
 * - an evolution into one that the Expansion gates on its region happens at night.
 *
 * One regional entry per species per region: the first form (Tauros' Combat Breed, Darmanitan's
 * Standard Mode); the other breeds and modes are alternate forms of the base. Hisui has no tab here
 * (project owner, 2026-09-14), so Hisuian forms are alternate forms too. Totems are a size rather
 * than a region, and Pikachu's "Alola" is a cap, so neither is regional.
 */
object RegionalForms {
  private val registry by lazy { ExpansionSpeciesRegistry() }

  /** The client's new dex tabs: Kalos, Alola, Galar, Paldea. */
  val DEX_TABS = listOf(6, 7, 8, 9)

  /** The dex tab a regional form is listed under, or null for anything that is not one. */
  fun regionOf(entry: ExpansionSpeciesDef): Int? = regionalTabByStableId[entry.stableId]

  /** National dex numbers in [tab]'s own dex order (monmmo/regional-dexes.csv, from PokeAPI). */
  fun dexOrder(tab: Int): List<Int> = dexOrders[tab].orEmpty()

  /** Pikachu's caps and cosplay outfits: costumes on the client's own Pikachu, not forms. */
  fun isPikachuCostume(entry: ExpansionSpeciesDef): Boolean =
      entry.isForm &&
          entry.baseSpeciesStableId.substringAfterLast("SPECIES_").equals("PIKACHU", ignoreCase = true)

  /** The base species' client id when [clientWireId] is a regional form, otherwise null. */
  fun baseWireOf(clientWireId: Int): Int? = baseByWire[clientWireId]

  /**
   * The base species' client id when [clientWireId] is any other form - Mega, Gigantamax, gender,
   * cosmetic, Totem, Hisuian, a further breed or mode - otherwise null. Those are alternate forms
   * reached through their base's form toggle, never Pokedex entries of their own (project owner,
   * 2026-09-13): the client drops a hidden species from its lists only while it is unseen, so their
   * progress counts on the base.
   */
  fun alternateFormBaseOf(clientWireId: Int): Int? = alternateBaseByWire[clientWireId]

  private fun candidateTab(entry: ExpansionSpeciesDef): Int? {
    if (!entry.isForm || isPikachuCostume(entry) || "_TOTEM" in entry.symbol) return null
    return REGION_TABS.entries.firstOrNull { (region, _) -> "_$region" in entry.symbol }?.value
  }

  private val regionalTabByStableId: Map<String, Int> by lazy {
    registry
        .all()
        .filter { it.clientWireId != null }
        .mapNotNull { entry -> candidateTab(entry)?.let { entry to it } }
        .groupBy { (entry, tab) -> entry.baseSpeciesStableId to tab }
        .values
        .map { forms -> forms.minBy { (entry, _) -> checkNotNull(entry.clientWireId) } }
        .associate { (entry, tab) -> entry.stableId to tab }
  }

  private val alternateBaseByWire: Map<Int, Int> by lazy {
    val byStableId = registry.all().associateBy { it.stableId }
    registry
        .all()
        .filter { it.isForm && regionOf(it) == null }
        .mapNotNull { form ->
          val wire = form.clientWireId ?: return@mapNotNull null
          val base = byStableId[form.baseSpeciesStableId]?.clientWireId ?: return@mapNotNull null
          (wire to base).takeIf { wire != base }
        }
        .toMap()
  }

  private val baseByWire: Map<Int, Int> by lazy {
    val byStableId = registry.all().associateBy { it.stableId }
    registry
        .all()
        .filter { regionOf(it) != null }
        .mapNotNull { form ->
          val wire = form.clientWireId ?: return@mapNotNull null
          val base = byStableId[form.baseSpeciesStableId]?.clientWireId ?: return@mapNotNull null
          wire to base
        }
        .toMap()
  }

  private val dexOrders: Map<Int, List<Int>> by lazy {
    val stream =
        RegionalForms::class.java.getResourceAsStream("/monmmo/regional-dexes.csv")
            ?: error("monmmo/regional-dexes.csv is missing from the classpath")
    stream.bufferedReader().useLines { lines ->
      lines
          .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("tab,") }
          .map { it.split(",") }
          .groupBy({ it[0].trim().toInt() }, { it[1].trim().toInt() })
    }
  }

  private val REGION_TABS = linkedMapOf("ALOLA" to 7, "GALAR" to 8, "PALDEA" to 9)
}
