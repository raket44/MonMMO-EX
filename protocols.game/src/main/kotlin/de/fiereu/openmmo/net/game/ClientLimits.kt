package de.fiereu.openmmo.net.game

import de.fiereu.bytecodec.Codec
import de.fiereu.bytecodec.GrowableWriteBuffer

/**
 * The r32645 client inflates each compressed packet with ONE Inflater.inflate call into a fixed
 * 30000-byte buffer (f/od4.oP, allocated in od4's static init; the compressed input buffer R80 is
 * the same size). A payload longer than that leaves the rest inside the connection's inflater, so
 * every later compressed packet decodes as garbage ("Undefined hz1 -14", "Undefined xz7 -1") and
 * the bag window dies on its missing stack list. A 217-monster PC (about 38 KB of records with the
 * UTF-16 names) did exactly that at login on 2026-09-16.
 */
const val CLIENT_INFLATE_BUFFER = 30_000

/** Payload budget for one packet of a splittable list: headroom under [CLIENT_INFLATE_BUFFER]. */
const val CLIENT_CHUNK_BUDGET = 24_000

/**
 * Splits [items] into runs whose encoded size (per [codec]) stays under [budget] bytes, at most
 * [maxCount] items per run and always at least one. An empty list gives no runs.
 */
fun <T> chunkByWireSize(
    items: List<T>,
    codec: Codec<T>,
    budget: Int = CLIENT_CHUNK_BUDGET,
    maxCount: Int = Int.MAX_VALUE,
): List<List<T>> {
  val runs = ArrayList<List<T>>()
  var run = ArrayList<T>()
  var bytes = 0
  for (item in items) {
    val size = GrowableWriteBuffer().also { codec.write(it, item) }.size()
    if (run.isNotEmpty() && (bytes + size > budget || run.size >= maxCount)) {
      runs += run
      run = ArrayList()
      bytes = 0
    }
    run += item
    bytes += size
  }
  if (run.isNotEmpty()) runs += run
  return runs
}
