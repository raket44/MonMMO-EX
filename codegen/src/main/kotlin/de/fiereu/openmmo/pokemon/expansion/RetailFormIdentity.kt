package de.fiereu.openmmo.pokemon.expansion

/**
 * One identity per form (project owner, 2026-09-13: no duplicates). A form the retail client already
 * owns is that retail species under the client's form number - never the Expansion's copy of it.
 * Monsters created or stored under the Expansion's id for such a form (Unown B, Rotom Heat...) are
 * normalized to the base species and form number the client speaks.
 */
object RetailFormIdentity {
  private val retailFormsByServerId: Map<Int, ExpansionSpeciesDef> by lazy {
    GeneratedExpansionSpeciesCatalog.species.filter { it.isRetailForm }.associateBy { it.serverId }
  }

  private val byStableId: Map<String, ExpansionSpeciesDef> by lazy {
    GeneratedExpansionSpeciesCatalog.species.associateBy { it.stableId }
  }

  /** The (species, form) a monster stored as [dexId] with [form] really is. */
  fun normalize(dexId: Int, form: Int): Pair<Int, Int> {
    val entry = retailFormsByServerId[dexId] ?: return dexId to form
    val baseSpecies = byStableId[entry.baseSpeciesStableId]?.clientWireId ?: return dexId to form
    return baseSpecies to (entry.formIndex ?: form)
  }
}
