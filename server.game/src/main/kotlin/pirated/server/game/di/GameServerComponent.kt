package pirated.server.game.di

import dagger.BindsInstance
import dagger.Component
import pirated.server.game.GameServer
import pirated.server.game.config.GameServerConfig
import pirated.server.game.db.DatabaseBootstrap
import pirated.server.game.handler.GameAppHandler
import pirated.server.game.session.SessionRegistry
import pirated.server.game.storage.CharacterStore
import pirated.server.game.storage.DevCharacterSeeder
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(modules = [GameServerModule::class, ChatCommandModule::class])
interface GameServerComponent {

  fun server(): GameServer

  fun handlerProvider(): Provider<GameAppHandler>

  fun sessionRegistry(): SessionRegistry

  fun databaseBootstrap(): DatabaseBootstrap

  fun characterStore(): CharacterStore

  fun incubatorService(): pirated.server.game.services.IncubatorService

  fun devCharacterSeeder(): DevCharacterSeeder

  fun testBoxSeeder(): pirated.server.game.storage.TestBoxSeeder

  @Component.Factory
  fun interface Factory {
    fun create(@BindsInstance config: GameServerConfig): GameServerComponent
  }
}
