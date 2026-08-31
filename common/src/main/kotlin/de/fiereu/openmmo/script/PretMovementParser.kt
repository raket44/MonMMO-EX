package de.fiereu.openmmo.script

/** Parses one human-readable pret movement list while retaining its original source lines. */
object PretMovementParser {
  private val labelLine = Regex("^(\\w+)::?\\s*$")

  fun parse(id: ScriptId, sourceFile: String, lines: List<String>): MovementProgram {
    val actions = mutableListOf<MovementAction>()
    var ended = false
    for (raw in lines) {
      val line = raw.substringBefore('@').trim()
      if (line.isEmpty() || labelLine.matches(line) || line.startsWith(".")) continue
      val command = line.substringBefore(' ').substringBefore('\t').trim()
      if (command == "step_end") {
        ended = true
        break
      }
      val rest = line.removePrefix(command).trim()
      actions +=
          MovementAction(
              command = command,
              args = if (rest.isEmpty()) emptyList() else rest.split(',').map(String::trim),
              sourceLine = line,
          )
    }
    require(ended) { "Movement ${id.stable} has no step_end in $sourceFile" }
    return MovementProgram(id, sourceFile, actions)
  }
}
