package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec

class ScratchProbeTest :
    FunSpec({
      test("probe jigglypuff sources") {
        val expansion = ExpansionSpeciesRegistry()
        val byWire = expansion.getByClientWireId(39)
        val byServer = expansion.getByServerId(0x10000 + 39)
        println("PROBE byWire39: ${byWire?.symbol} ${byWire?.displayName} server=${byWire?.serverId}")
        println("PROBE byServer: ${byServer?.symbol} ${byServer?.displayName}")
        val entry = byServer ?: byWire
        println("PROBE types: ${entry?.typeSymbols}")
        println(
            "PROBE learnset: ${entry?.levelUpLearnset?.map { "L${it.level}:${it.originalMoveId}" }}")
        val learnsets = LearnsetRegistry()
        println("PROBE registry.get(39): ${learnsets.get(39).map { "L${it.level}:${it.moveId}" }}")
        val def = SpeciesRegistry().get(39)
        println("PROBE speciesDef: ${def?.name} ${def?.type1}/${def?.type2} yields=${def?.expYield}")
      }
    })
