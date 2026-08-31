package de.fiereu.openmmo.server.game

import de.fiereu.openmmo.common.SpeciesWireIds
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.config.ConfigLoader
import de.fiereu.openmmo.server.game.di.DaggerGameServerComponent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

private val log = KotlinLogging.logger {}

private val SHUTDOWN_FLUSH_TIMEOUT = 10.seconds

fun main() {
  val config = ConfigLoader.load()
  // Stated at boot rather than at first use, because a missing environment flag otherwise only
  // surfaces as "disabled" when someone finally types a command.
  log.info {
    "Developer tools: enabled=${config.developer.enabled} " +
        "expansionClientContent=${config.developer.expansionClientContent}"
  }
  val component = DaggerGameServerComponent.factory().create(config)
  // Client ids follow National Dex order, which only the generated catalogue knows. common sits
  // below it, so the mapping the packet codecs use is installed here before anything is served.
  installSpeciesWireIds(ExpansionSpeciesRegistry())
  component.databaseBootstrap().migrate()
  val characterStore = component.characterStore()
  characterStore.startPeriodicFlush()
  runBlocking { component.devCharacterSeeder().seed() }
  Runtime.getRuntime()
      .addShutdownHook(
          Thread {
            runCatching {
                  runBlocking { withTimeout(SHUTDOWN_FLUSH_TIMEOUT) { characterStore.shutdown() } }
                }
                .onFailure { log.warn(it) { "Final character flush did not complete" } }
          })
  component.server().start()
}

/**
 * Bridges canonical server ids to the client ids the generated catalogue assigned. Falling back to
 * the canonical id keeps retail species, which the client already owns, passing through untouched.
 */
private fun installSpeciesWireIds(expansion: ExpansionSpeciesRegistry) {
  val toClient =
      expansion.all().mapNotNull { it.clientWireId?.let { wire -> it.serverId to wire } }.toMap()
  val toCanonical = toClient.entries.associate { (canonical, wire) -> wire to canonical }
  SpeciesWireIds.install(
      toClient = { canonical -> toClient[canonical] ?: canonical },
      toCanonical = { wire -> toCanonical[wire] ?: wire },
  )
}
