package de.fiereu.openmmo.script

/** Stable source identity for a parsed GBA overworld script. */
data class ScriptId(
    val platform: String,
    val source: String,
    val gameCode: String,
    val label: String,
) {
  val stable: String
    get() = "$platform:$source:$gameCode:$label"
}

/** A small, source-preserving representation of one script program. */
data class ScriptProgram(
    val id: ScriptId,
    val storyNamespace: String,
    val sourceFile: String,
    val labels: Map<String, Int>,
    val instructions: List<ScriptInstruction>,
    /** Map-local pret object constants normalized to MonMMO's zero-based npc indexes. */
    val objectIds: Map<String, Int> = emptyMap(),
)

/** One parsed pret command. [sourceLine] is kept so runtime failures point back to the decomp. */
data class ScriptInstruction(
    val command: String,
    val args: List<ScriptArg>,
    val sourceLine: String,
)

sealed interface ScriptArg {
  val token: String
}

data class LabelArg(override val token: String) : ScriptArg

data class FlagArg(override val token: String) : ScriptArg

data class VarArg(override val token: String) : ScriptArg

data class TextArg(override val token: String) : ScriptArg

data class TrainerArg(override val token: String) : ScriptArg

data class ObjectArg(override val token: String) : ScriptArg

data class MovementArg(override val token: String) : ScriptArg

data class IntArg(val value: Int, override val token: String) : ScriptArg

data class SymbolArg(override val token: String) : ScriptArg

/** A source-preserving pret movement list. The source action names are platform-independent. */
data class MovementProgram(
    val id: ScriptId,
    val sourceFile: String,
    val actions: List<MovementAction>,
)

/** One action in a pret movement list. */
data class MovementAction(
    val command: String,
    val args: List<String>,
    val sourceLine: String,
)
