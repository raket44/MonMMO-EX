package de.fiereu.openmmo.server.game.battle

import de.fiereu.openmmo.common.enums.PokemonNature

/**
 * What the lead party monster's ability asks of a wild roll (OverworldAbilities): a nature to
 * match (Synchronize), a gender to land on (Cute Charm), and Compound Eyes' held-item odds.
 */
data class WildRollHints(
    val nature: PokemonNature? = null,
    val gender: Byte? = null,
    val compoundEyes: Boolean = false,
)
