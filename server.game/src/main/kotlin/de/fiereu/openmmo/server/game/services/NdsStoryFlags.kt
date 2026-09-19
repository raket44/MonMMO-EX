package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.story.generated.johto.JohtoFlags
import de.fiereu.openmmo.story.generated.sinnoh.SinnohFlags

/**
 * The DS npc records carry their hide flag as the ROM's numeric id; the story store keys flags by
 * NAME (johto/FLAG_HIDE_NEW_BARK_RIVAL) for the decomp'd games and by number (unova/FLAG_657) for
 * White, whose scripts name every flag that way. This turns an npc's numeric flag into the key
 * the store would hold, once per region, from the generated constants' own name -> id tables.
 */
object NdsStoryFlags {
  private const val UNOVA = 2
  private const val SINNOH = 3
  private const val JOHTO = 4

  private val johtoById: Map<Int, String> by lazy { reverse(JohtoFlags::class.java, JohtoFlags::numericId) }
  private val sinnohById: Map<Int, String> by lazy { reverse(SinnohFlags::class.java, SinnohFlags::numericId) }

  /** The story-store key of a DS region's numeric flag, null when the region has no such flag. */
  fun key(regionId: Int, flagId: Int): String? =
      when (regionId) {
        UNOVA -> "unova/FLAG_$flagId"
        SINNOH -> sinnohById[flagId]
        JOHTO -> johtoById[flagId]
        else -> null
      }

  /** True when the npc's hide flag (0 = none) is set for this character. */
  fun isHidden(regionId: Int, flagId: Int, storyFlags: Set<String>): Boolean {
    if (flagId == 0) return false
    val key = key(regionId, flagId) ?: return false
    return key in storyFlags
  }

  /** Every `const val FLAG_*` of the generated object, keyed by the id its numericId() gives. */
  private fun reverse(constants: Class<*>, numericId: (String) -> Int?): Map<Int, String> {
    val out = HashMap<Int, String>()
    for (field in constants.declaredFields) {
      if (!field.name.startsWith("FLAG_") || field.type != String::class.java) continue
      field.isAccessible = true
      val key = field.get(null) as? String ?: continue
      val id = numericId(key) ?: continue
      out.putIfAbsent(id, key)
    }
    return out
  }
}
