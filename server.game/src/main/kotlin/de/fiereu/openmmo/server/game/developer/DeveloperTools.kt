package de.fiereu.openmmo.server.game.developer

import de.fiereu.openmmo.server.game.config.GameServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val log = KotlinLogging.logger {}

@Singleton
class DeveloperTools @Inject constructor(config: GameServerConfig) {
  val enabled: Boolean = config.developer.enabled
  val expansionClientContent: Boolean = enabled && config.developer.expansionClientContent

  private val interpretedOverrides = ConcurrentHashMap.newKeySet<String>()
  private val kotlinOverrides = ConcurrentHashMap.newKeySet<String>()

  @Volatile
  var interpreterVerbose: Boolean = config.developer.interpreterVerbose
    private set

  init {
    if (enabled) {
      interpretedOverrides += config.developer.interpretedOverrides
      kotlinOverrides += config.developer.kotlinOverrides
      val conflicts = interpretedOverrides intersect kotlinOverrides
      if (conflicts.isNotEmpty()) {
        interpretedOverrides.removeAll(conflicts)
        log.error { "Conflicting script overrides defaulted to Kotlin: ${conflicts.sorted()}" }
      }
      log.warn {
        "Developer tools enabled; interpreted overrides=${interpretedOverrides.sorted()} " +
            "Kotlin overrides=${kotlinOverrides.sorted()} verbose=$interpreterVerbose " +
            "expansionClientContent=$expansionClientContent"
      }
    }
  }

  fun overrides(): Set<String> = interpretedOverrides.toSet()

  fun kotlinOverrides(): Set<String> = kotlinOverrides.toSet()

  fun enableOverride(scriptIdOrLabel: String): Boolean {
    if (!enabled) return false
    kotlinOverrides.remove(scriptIdOrLabel)
    val added = interpretedOverrides.add(scriptIdOrLabel)
    if (added) log.warn { "[Developer Override] Enabled interpreted $scriptIdOrLabel" }
    return added
  }

  fun disableOverride(scriptIdOrLabel: String): Boolean {
    if (!enabled) return false
    val removed = interpretedOverrides.remove(scriptIdOrLabel)
    if (removed) log.warn { "[Developer Override] Disabled interpreted $scriptIdOrLabel" }
    return removed
  }

  fun enableKotlinOverride(scriptIdOrLabel: String): Boolean {
    if (!enabled) return false
    interpretedOverrides.remove(scriptIdOrLabel)
    val added = kotlinOverrides.add(scriptIdOrLabel)
    if (added) log.warn { "[Developer Override] Enabled Kotlin $scriptIdOrLabel" }
    return added
  }

  fun disableKotlinOverride(scriptIdOrLabel: String): Boolean {
    if (!enabled) return false
    val removed = kotlinOverrides.remove(scriptIdOrLabel)
    if (removed) log.warn { "[Developer Override] Disabled Kotlin $scriptIdOrLabel" }
    return removed
  }

  fun clearScriptOverride(scriptIdOrLabel: String): Boolean =
      disableOverride(scriptIdOrLabel) or disableKotlinOverride(scriptIdOrLabel)

  fun matchingOverride(scriptLabel: String): String? {
    if (!enabled) return null
    return interpretedOverrides.firstOrNull { matches(it, scriptLabel) }
  }

  fun matchingKotlinOverride(scriptLabel: String): String? {
    if (!enabled) return null
    return kotlinOverrides.firstOrNull { matches(it, scriptLabel) }
  }

  fun setInterpreterVerbose(enabled: Boolean) {
    if (!this.enabled) return
    interpreterVerbose = enabled
    log.warn { "Developer interpreter verbose logging ${if (enabled) "enabled" else "disabled"}" }
  }

  fun trace(message: () -> String) {
    if (enabled && interpreterVerbose) log.info { "[DevTrace] ${message()}" }
  }

  private fun matches(configured: String, scriptLabel: String): Boolean =
      configured == scriptLabel ||
          (':' in configured && configured.substringAfterLast(':') == scriptLabel)
}
