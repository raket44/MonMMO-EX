package de.fiereu.openmmo.pokemon.expansion

import javax.inject.Inject
import javax.inject.Singleton

/** One row of an Expansion form-change table: the trigger kind, the form it turns into, its params. */
data class FormChange(val kind: String, val targetSymbol: String, val params: List<String>)

/**
 * The Expansion form-change tables, keyed by the table symbol a species' data names. Targets are
 * species symbols resolved through the expansion catalogue, so a form is only reachable when the
 * catalogue carries it.
 */
@Singleton
class FormChangeRegistry
@Inject
constructor(private val expansion: ExpansionSpeciesRegistry = ExpansionSpeciesRegistry()) {
  private val tables: Map<String, List<FormChange>> =
      GeneratedFormChanges.rows
          .map { it.split('\t') }
          .filter { it.size >= 3 }
          .groupBy({ it[0] }, { FormChange(it[1], it[2], it.drop(3).filter { p -> p.isNotEmpty() }) })

  /**
   * The table of the species with server id [serverId]; every form of a family shares it. Retail
   * species keep their dex id as server id, which the catalogue knows as the client wire id.
   */
  fun of(serverId: Int): List<FormChange> =
      (expansion.getByServerId(serverId) ?: expansion.getByClientWireId(serverId))?.formChangeTableSymbol?.takeIf { it.isNotEmpty() }?.let { tables[it] }.orEmpty()

  fun targetServerId(change: FormChange): Int? = expansion.get(change.targetSymbol)?.serverId

  fun tableCount(): Int = tables.size
}
