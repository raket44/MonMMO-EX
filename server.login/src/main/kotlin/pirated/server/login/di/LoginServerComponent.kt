package pirated.server.login.di

import dagger.BindsInstance
import dagger.Component
import pirated.server.login.LoginServer
import pirated.server.login.auth.AdminAccountBootstrap
import pirated.server.login.config.LoginServerConfig
import pirated.server.login.db.DatabaseBootstrap
import pirated.server.login.handler.LoginAppHandler
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(modules = [LoginServerModule::class])
interface LoginServerComponent {

  fun server(): LoginServer

  fun handlerProvider(): Provider<LoginAppHandler>

  fun databaseBootstrap(): DatabaseBootstrap

  fun adminAccountBootstrap(): AdminAccountBootstrap

  @Component.Factory
  fun interface Factory {
    fun create(@BindsInstance config: LoginServerConfig): LoginServerComponent
  }
}
