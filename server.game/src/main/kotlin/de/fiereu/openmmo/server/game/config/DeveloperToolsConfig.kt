package de.fiereu.openmmo.server.game.config

data class DeveloperToolsConfig(
    val enabled: Boolean = false,
    val interpretedOverrides: Set<String> = emptySet(),
    val kotlinOverrides: Set<String> = emptySet(),
    val interpreterVerbose: Boolean = false,
    val expansionClientContent: Boolean = false,
)
