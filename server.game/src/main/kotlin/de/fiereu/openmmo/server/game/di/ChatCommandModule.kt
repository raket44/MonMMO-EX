package de.fiereu.openmmo.server.game.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import de.fiereu.openmmo.server.game.services.command.CatchCommand
import de.fiereu.openmmo.server.game.services.command.ChatCommand
import de.fiereu.openmmo.server.game.services.command.ClientCreateItemCommand
import de.fiereu.openmmo.server.game.services.command.ClientMoveTo2Command
import de.fiereu.openmmo.server.game.services.command.ClientMoveToCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperGamemodeCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperGiveExpansionCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperGiveItemCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperGmMenuCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperHealCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperRarityCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperReleaseCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperScriptCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperSeasonCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperStoryCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperTeleportCommand
import de.fiereu.openmmo.server.game.services.command.LinkProbeCommand
import de.fiereu.openmmo.server.game.services.command.DonatorCommand
import de.fiereu.openmmo.server.game.services.command.GiveMoneyCommand
import de.fiereu.openmmo.server.game.services.command.HordeCommand
import de.fiereu.openmmo.server.game.services.command.SocialTestCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperTourCommand
import de.fiereu.openmmo.server.game.services.command.DeveloperWarpCommand
import de.fiereu.openmmo.server.game.services.command.HelpCommand
import de.fiereu.openmmo.server.game.services.command.PosCommand
import de.fiereu.openmmo.server.game.services.command.StoryCommand
import de.fiereu.openmmo.server.game.services.command.TestBattleCommand

/**
 * A name must not start with a client side command. The client resolves those itself and never
 * sends them, which is why there is a /pos and no /where, which /w would have swallowed.
 */
@Module
interface ChatCommandModule {
  @Binds @IntoSet fun helpCommand(command: HelpCommand): ChatCommand

  @Binds @IntoSet fun posCommand(command: PosCommand): ChatCommand

  @Binds @IntoSet fun testBattleCommand(command: TestBattleCommand): ChatCommand

  @Binds @IntoSet fun catchCommand(command: CatchCommand): ChatCommand

  @Binds @IntoSet fun storyCommand(command: StoryCommand): ChatCommand

  @Binds @IntoSet fun developerScriptCommand(command: DeveloperScriptCommand): ChatCommand

  @Binds
  @IntoSet
  fun developerGiveExpansionCommand(command: DeveloperGiveExpansionCommand): ChatCommand

  @Binds @IntoSet fun developerStoryCommand(command: DeveloperStoryCommand): ChatCommand

  @Binds
  @IntoSet
  fun developerRaidCommand(command: de.fiereu.openmmo.server.game.services.command.DeveloperRaidCommand): ChatCommand

  @Binds @IntoSet fun developerSeasonCommand(command: DeveloperSeasonCommand): ChatCommand

  @Binds @IntoSet fun linkProbeCommand(command: LinkProbeCommand): ChatCommand

  @Binds
  @IntoSet
  fun developerProbeCommand(
      command: de.fiereu.openmmo.server.game.services.command.DeveloperProbeCommand
  ): ChatCommand

  @Binds @IntoSet fun developerTeleportCommand(command: DeveloperTeleportCommand): ChatCommand

  @Binds @IntoSet fun hordeCommand(command: HordeCommand): ChatCommand

  @Binds @IntoSet fun giveMoneyCommand(command: GiveMoneyCommand): ChatCommand

  @Binds @IntoSet fun donatorCommand(command: DonatorCommand): ChatCommand

  @Binds @IntoSet fun socialTestCommand(command: SocialTestCommand): ChatCommand

  @Binds @IntoSet fun developerReleaseCommand(command: DeveloperReleaseCommand): ChatCommand

  @Binds @IntoSet fun developerGiveItemCommand(command: DeveloperGiveItemCommand): ChatCommand

  @Binds @IntoSet fun developerHealCommand(command: DeveloperHealCommand): ChatCommand
  @Binds @IntoSet fun developerRarityCommand(command: DeveloperRarityCommand): ChatCommand

  @Binds @IntoSet fun developerGamemodeCommand(command: DeveloperGamemodeCommand): ChatCommand

  @Binds @IntoSet fun developerGmMenuCommand(command: DeveloperGmMenuCommand): ChatCommand

  @Binds @IntoSet fun developerWarpCommand(command: DeveloperWarpCommand): ChatCommand

  @Binds @IntoSet fun developerTourCommand(command: DeveloperTourCommand): ChatCommand

  @Binds @IntoSet fun clientMoveToCommand(command: ClientMoveToCommand): ChatCommand

  @Binds @IntoSet fun clientCreateItemCommand(command: ClientCreateItemCommand): ChatCommand

  @Binds @IntoSet fun clientMoveTo2Command(command: ClientMoveTo2Command): ChatCommand
}
