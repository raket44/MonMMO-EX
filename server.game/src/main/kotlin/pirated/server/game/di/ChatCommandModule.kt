package pirated.server.game.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import pirated.server.game.services.command.CatchCommand
import pirated.server.game.services.command.ChatCommand
import pirated.server.game.services.command.ClientCreateItemCommand
import pirated.server.game.services.command.ClientMoveTo2Command
import pirated.server.game.services.command.ClientMoveToCommand
import pirated.server.game.services.command.DeveloperGiveExpansionCommand
import pirated.server.game.services.command.DeveloperGiveItemCommand
import pirated.server.game.services.command.DeveloperGmMenuCommand
import pirated.server.game.services.command.DeveloperHealCommand
import pirated.server.game.services.command.DeveloperRarityCommand
import pirated.server.game.services.command.DeveloperReleaseCommand
import pirated.server.game.services.command.DeveloperSeasonCommand
import pirated.server.game.services.command.DeveloperStoryCommand
import pirated.server.game.services.command.DeveloperWorldCommand
import pirated.server.game.services.command.DeveloperLookaheadCommand
import pirated.server.game.services.command.DeveloperTeleportCommand
import pirated.server.game.services.command.DonatorCommand
import pirated.server.game.services.command.GiveMoneyCommand
import pirated.server.game.services.command.HordeCommand
import pirated.server.game.services.command.DeveloperTourCommand
import pirated.server.game.services.command.DeveloperWarpCommand
import pirated.server.game.services.command.HelpCommand
import pirated.server.game.services.command.PosCommand
import pirated.server.game.services.command.StoryCommand

/**
 * A name must not start with a client side command. The client resolves those itself and never
 * sends them, which is why there is a /pos and no /where, which /w would have swallowed.
 */
@Module
interface ChatCommandModule {
  @Binds @IntoSet fun helpCommand(command: HelpCommand): ChatCommand

  @Binds @IntoSet fun posCommand(command: PosCommand): ChatCommand


  @Binds @IntoSet fun catchCommand(command: CatchCommand): ChatCommand

  @Binds @IntoSet fun storyCommand(command: StoryCommand): ChatCommand


  @Binds
  @IntoSet
  fun developerGiveExpansionCommand(command: DeveloperGiveExpansionCommand): ChatCommand

  @Binds @IntoSet fun developerStoryCommand(command: DeveloperStoryCommand): ChatCommand

  @Binds @IntoSet fun developerWorldCommand(command: DeveloperWorldCommand): ChatCommand

  @Binds @IntoSet fun developerLookaheadCommand(command: DeveloperLookaheadCommand): ChatCommand

  @Binds
  @IntoSet
  fun developerRaidCommand(command: pirated.server.game.services.command.DeveloperRaidCommand): ChatCommand

  @Binds @IntoSet fun developerSeasonCommand(command: DeveloperSeasonCommand): ChatCommand


  @Binds @IntoSet fun developerTeleportCommand(command: DeveloperTeleportCommand): ChatCommand

  @Binds @IntoSet fun hordeCommand(command: HordeCommand): ChatCommand

  @Binds @IntoSet fun giveMoneyCommand(command: GiveMoneyCommand): ChatCommand

  @Binds @IntoSet fun donatorCommand(command: DonatorCommand): ChatCommand


  @Binds @IntoSet fun developerReleaseCommand(command: DeveloperReleaseCommand): ChatCommand

  @Binds @IntoSet fun developerGiveItemCommand(command: DeveloperGiveItemCommand): ChatCommand

  @Binds @IntoSet fun developerHealCommand(command: DeveloperHealCommand): ChatCommand

  @Binds @IntoSet fun developerSpriteCommand(command: pirated.server.game.services.command.DeveloperSpriteCommand): ChatCommand
  @Binds @IntoSet fun developerRarityCommand(command: DeveloperRarityCommand): ChatCommand


  @Binds @IntoSet fun developerGmMenuCommand(command: DeveloperGmMenuCommand): ChatCommand

  @Binds @IntoSet fun developerWarpCommand(command: DeveloperWarpCommand): ChatCommand

  @Binds @IntoSet fun developerTourCommand(command: DeveloperTourCommand): ChatCommand

  @Binds @IntoSet fun clientMoveToCommand(command: ClientMoveToCommand): ChatCommand

  @Binds @IntoSet fun clientCreateItemCommand(command: ClientCreateItemCommand): ChatCommand

  @Binds @IntoSet fun clientMoveTo2Command(command: ClientMoveTo2Command): ChatCommand
}
