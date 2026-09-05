package de.fiereu.openmmo.codegen.trainer

/** A monster on a trainer's team. Empty [moveIds] means the level up moveset is used. */
data class ParsedTrainerMon(
    val dexId: Int,
    val level: Int,
    val iv: Int,
    val heldItem: Int,
    val moveIds: List<Int>,
)

data class ParsedTrainer(
    val id: Int,
    val constant: String,
    val name: String,
    val trainerClass: Int,
    val doubleBattle: Boolean,
    val prizeRate: Int,
    val party: List<ParsedTrainerMon>,
    val rematchIds: List<Int?>,
    /** DS trainer speech: message kind (intro 0, in-battle defeat 1, post-battle 2, doubles 3-10, rematch 17-19) to the client text id. */
    val messages: Map<Int, Int> = emptyMap(),
)
