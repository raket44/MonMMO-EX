@file:JvmName("Main")

package de.fiereu.openmmo.codegen.move

import java.io.File

fun main(args: Array<String>) {
  require(args.size >= 4) {
    "Usage: <output-dir> <templates-dir> <class-cache-dir> <decomp-dir> got ${args.toList()}"
  }
  val outputDir = File(args[0])
  val templatesDir = File(args[1])
  val classCacheDir = File(args[2])
  val decompDir = File(args[3])

  println("[moves] parsing from $decompDir")
  // Moves come from the Expansion, not the Emerald decomp: its table stops at 354 and cannot
  // describe anything from Gen 4 on, which left imported moves with placeholder PP and no power.
  val moves = ExpansionMoveParser(decompDir).parseAll()
  println("[moves] parsed ${moves.size} moves. writing to $outputDir")
  MovesRenderer(templatesDir, outputDir, classCacheDir).render(moves)
  println("[moves] done")
}
