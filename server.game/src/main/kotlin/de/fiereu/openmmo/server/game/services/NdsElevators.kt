package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.enums.Direction
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

/**
 * Unova's elevator floor tables, from `nds-elevators-2.txt` (tools/nds/Elevators5.java; layout and
 * verification in docs/gen5-elevators.md).
 *
 * In Black/White a panel script (engine ids 10405-10418 = script file 888) points opcode 0x1CB at
 * a floor table inside its own script file: 10-byte rows {label, destination header, x, height,
 * row} ending at 0xFFFF. 0x1CC lists every row whose header is not the current map (value = row
 * index), and 0x1CD warps to the chosen row. A table is identified here by (script file, byte
 * offset of the table in that file), the same pair the corpus passes to `ds_elevatortable`.
 */
@Singleton
class NdsElevators @Inject constructor() {
  /** One row of a floor table. [textId] is the server's DS text id of the label ("1F", "47F"). */
  data class Floor(
      val index: Int,
      val textId: Int,
      val destHeader: Int,
      val destBank: Int,
      val destMap: Int,
      val x: Int,
      val y: Int,
      val facing: Direction,
      val label: String,
  )

  /** A panel as the ROM places it: the bg event on a map and the table its script points at. */
  data class Panel(val region: Int, val bank: Int, val map: Int, val script: Int, val file: Int, val table: Int, val x: Int, val y: Int)

  private class Data(val tables: Map<Long, List<Floor>>, val panels: List<Panel>)

  @Volatile private var loadedStamp = Long.MIN_VALUE
  @Volatile private var data = Data(emptyMap(), emptyList())

  /** The rows of the table at [tableOffset] in script file [file], in ROM order (index = row). */
  fun floors(region: Int, file: Int, tableOffset: Int): List<Floor> = current().tables[key(region, file, tableOffset)] ?: emptyList()

  /** What 0x1CC lists on the map [currentHeader] (bank | map << 8): every row but that map's own. */
  fun menu(region: Int, file: Int, tableOffset: Int, currentHeader: Int): List<Floor> =
      floors(region, file, tableOffset).filter { it.destHeader != currentHeader }

  fun panels(region: Int): List<Panel> = current().panels.filter { it.region == region }

  private fun key(region: Int, file: Int, table: Int): Long = (region.toLong() shl 40) or (file.toLong() shl 20) or table.toLong()

  private fun current(): Data {
    val file = dataFile() ?: return data
    val stamp = file.lastModified()
    if (stamp != loadedStamp) {
      synchronized(this) {
        if (stamp != loadedStamp) {
          data = load(file)
          loadedStamp = stamp
        }
      }
    }
    return data
  }

  private fun load(file: File): Data {
    val tables = HashMap<Long, java.util.TreeMap<Int, Floor>>()
    val panels = LinkedHashSet<Panel>()
    file.forEachLine { line ->
      if (!line.startsWith("elevator;")) return@forEachLine
      val p = line.split(';')
      if (p.size < 24) return@forEachLine
      val region = p[1].toInt()
      val scriptFile = p[19].toInt()
      val table = p[21].toInt()
      val floor =
          Floor(
              index = p[5].toInt(),
              textId = p[6].toInt(),
              destHeader = p[7].toInt(),
              destBank = p[8].toInt(),
              destMap = p[9].toInt(),
              x = p[10].toInt(),
              y = p[11].toInt(),
              facing = dsFacing(p[14].toInt()),
              label = p[18],
          )
      tables.getOrPut(key(region, scriptFile, table)) { java.util.TreeMap() }[floor.index] = floor
      panels += Panel(region, p[2].toInt(), p[3].toInt(), p[4].toInt(), scriptFile, table, p[22].toInt(), p[23].toInt())
    }
    log.info { "Loaded ${tables.size} Unova elevator tables, ${panels.size} panels from ${file.path}" }
    return Data(tables.mapValues { it.value.values.toList() }, panels.toList())
  }

  private fun dataFile(): File? =
      listOf(File("nds-elevators-2.txt"), File("server.game/nds-elevators-2.txt")).firstOrNull { it.isFile }

  companion object {
    /** The Gen 4/5 direction enum the ROM passes (0 north, 1 south, 2 west, 3 east). */
    fun dsFacing(code: Int): Direction =
        when (code) {
          0 -> Direction.UP
          2 -> Direction.LEFT
          3 -> Direction.RIGHT
          else -> Direction.DOWN
        }
  }
}
