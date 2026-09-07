package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.enums.Ability

/**
 * The numeric ability ids the client speaks: pokeemerald-expansion's abilities.h numbering, which
 * is the standard one the client's own ability names sit at (string 210000 + id) and which the
 * launcher uses when it imports names for the rest. The server's [Ability] enum is ordered for
 * mechanics and drifts from those numbers after Gen 3 (the Expansion dropped Cacophony, shifting
 * Air Lock to 76; later gaps shift the rest by more), so an ordinal on the wire named the wrong
 * ability - a Neutralizing Gas popup read "Steely Spirit" - and a retail ability id resolved to
 * the wrong enum entry. Resource monmmo/ability-ids.csv is generated from the header.
 */
object AbilityWireIds {
  private val idByName: Map<String, Int>
  private val nameById: Map<Int, String>

  init {
    val rows =
        AbilityWireIds::class.java.getResourceAsStream("/monmmo/ability-ids.csv")?.bufferedReader()?.readLines()
            ?: emptyList()
    idByName =
        rows.mapNotNull { line ->
          val (name, id) = line.split(',').let { if (it.size == 2) it[0] to it[1].toIntOrNull() else null } ?: return@mapNotNull null
          id?.let { name to it }
        }.toMap()
    nameById = idByName.entries.associate { (name, id) -> id to name }
  }

  /** The client's id for [ability]; the ordinal when the header does not know it (Cacophony). */
  fun of(ability: Ability): Int = idByName[ability.name] ?: ability.ordinal

  /** The enum entry behind a client or retail ability id, or null for an id nothing here models. */
  fun ability(wireId: Int): Ability? =
      nameById[wireId]?.let { name -> Ability.entries.firstOrNull { it.name == name } }
}

fun Ability.wireId(): Int = AbilityWireIds.of(this)
