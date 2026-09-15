@file:JvmName("Main")

package de.fiereu.openmmo.codegen.trainer

import java.io.File

fun main(args: Array<String>) {
  require(args.size >= 4) {
    "Usage: <output-dir> <templates-dir> <class-cache-dir> <region|decomp>... got ${args.toList()}"
  }
  val outputDir = File(args[0])
  val templatesDir = File(args[1])
  val classCacheDir = File(args[2])

  val trades = linkedMapOf<String, List<de.fiereu.openmmo.trainer.NpcTradeRow>>()
  for (spec in args.drop(3)) {
    val (region, decomp) = spec.split("|")
    val dir = File(decomp)
    val trainers = if (dir.name == "pokeplatinum" || dir.name == "pokeheartgold" || dir.name == "rom-data") NdsTrainerParser(dir).parseAll() else TrainerParser(dir).parseAll()
    println("[trainer] $region: parsed ${trainers.size} trainers from $decomp")
    TrainerRenderer(region, templatesDir, outputDir, classCacheDir).render(trainers)
    // The DS games' in-game trade npcs ride along: the same decomps, the same generated package.
    if (dir.name == "pokeplatinum" || dir.name == "pokeheartgold") {
      trades[region] = NdsNpcTradeParser(dir).parseAll().also { println("[trainer] $region: parsed ${it.size} npc trades") }
    }
  }
  if (trades.isNotEmpty()) renderNpcTrades(outputDir, trades)
  println("[trainer] done")
}

/** `GeneratedNdsNpcTrades.SINNOH` / `.JOHTO`: plain rows, no template needed. */
private fun renderNpcTrades(outputDir: File, trades: Map<String, List<de.fiereu.openmmo.trainer.NpcTradeRow>>) {
  val file = File(outputDir, "de/fiereu/openmmo/trainer/generated/GeneratedNdsNpcTrades.kt")
  file.parentFile.mkdirs()
  val out = StringBuilder()
  out.append("package de.fiereu.openmmo.trainer.generated\n\n")
  out.append("import de.fiereu.openmmo.trainer.NpcTradeRow\n\n")
  out.append("/** Generated from the DS decomps' in-game trade tables; see NdsNpcTradeParser. */\n")
  out.append("object GeneratedNdsNpcTrades {\n")
  for ((region, rows) in trades) {
    out.append("  val ${region.uppercase()}: List<NpcTradeRow> =\n      listOf(\n")
    for (r in rows) {
      out.append("          NpcTradeRow(\"${r.constant}\", \"${r.nickname.replace("\"", "\\\"")}\", ${r.dexId}, intArrayOf(${r.ivs.joinToString(", ")}), \"${r.otName.replace("\"", "\\\"")}\", ${r.personality}, ${r.requestedDexId}, ${r.heldItem?.let { "\"$it\"" } ?: "null"}),\n")
    }
    out.append("      )\n")
  }
  out.append("}\n")
  file.writeText(out.toString())
}
