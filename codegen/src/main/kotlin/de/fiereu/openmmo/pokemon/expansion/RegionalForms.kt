package de.fiereu.openmmo.pokemon.expansion

/**
 * Regional forms (Alolan, Galarian, Hisuian, Paldean) - one rule shared by the client staging and
 * the server (project owner, 2026-09-13):
 * - each is an entry of its region's dex tab and a Next Form of its base species;
 * - it counts toward its base species' National entry, as in the games (Alolan Vulpix is #037);
 * - an evolution into one that the Expansion gates on its region happens at night.
 *
 * Totems are a size rather than a region, and Pikachu's "Alola" is a cap, so neither is regional.
 */
object RegionalForms {
  private val registry by lazy { ExpansionSpeciesRegistry() }

  /** The dex tab a regional form is listed under (Hisui sits with Galar), or null. */
  fun regionOf(entry: ExpansionSpeciesDef): Int? {
    if (!entry.isForm || isPikachuCostume(entry) || "_TOTEM" in entry.symbol) return null
    return REGION_TABS.entries.firstOrNull { (region, _) -> "_$region" in entry.symbol }?.value
  }

  /** Pikachu's caps and cosplay outfits: costumes on the client's own Pikachu, not forms. */
  fun isPikachuCostume(entry: ExpansionSpeciesDef): Boolean =
      entry.isForm &&
          entry.baseSpeciesStableId.substringAfterLast("SPECIES_").equals("PIKACHU", ignoreCase = true)

  /** The base species' client id when [clientWireId] is a regional form, otherwise null. */
  fun baseWireOf(clientWireId: Int): Int? = baseByWire[clientWireId]

  /**
   * The base species' client id when [clientWireId] is any other form - Mega, Gigantamax, gender,
   * cosmetic, Totem - otherwise null. Those are alternate forms reached through their base's form
   * toggle, never Pokedex entries of their own (project owner, 2026-09-13): the client drops a
   * hidden species from its lists only while it is unseen, so their progress counts on the base.
   */
  fun alternateFormBaseOf(clientWireId: Int): Int? = alternateBaseByWire[clientWireId]

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

  private val REGION_TABS = linkedMapOf("ALOLA" to 7, "GALAR" to 8, "HISUI" to 8, "PALDEA" to 9)
}
