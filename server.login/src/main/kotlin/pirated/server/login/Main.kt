package pirated.server.login

import pirated.server.login.config.ConfigLoader
import pirated.server.login.di.DaggerLoginServerComponent
import kotlinx.coroutines.runBlocking

fun main() {
  val config = ConfigLoader.load()
  val component = DaggerLoginServerComponent.factory().create(config)
  component.databaseBootstrap().migrate()
  runBlocking { component.adminAccountBootstrap().ensureAdmin() }
  component.server().start()
}
