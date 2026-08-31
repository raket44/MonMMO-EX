package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.pokemon.generated.GeneratedSpecies
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeciesRegistry
@Inject
constructor(
    private val expansion: ExpansionSpeciesRegistry = ExpansionSpeciesRegistry(),
) {

  private val species = ConcurrentHashMap<Int, SpeciesDef>()

  init {
    GeneratedSpecies.loadInto(this)
  }

  fun register(def: SpeciesDef) {
    species[def.id] = def
  }

  fun get(id: Int): SpeciesDef? = species[id] ?: expansion.runtimeDefinition(id)

  fun all(): Collection<SpeciesDef> = species.values

  fun size(): Int = species.size
}
