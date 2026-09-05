package de.fiereu.openmmo.server.game.developer

import de.fiereu.openmmo.server.game.testsupport.testOcarinas
import de.fiereu.openmmo.common.CharacterPermissions
import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.enums.CharacterGender
import de.fiereu.openmmo.common.enums.Region
import de.fiereu.openmmo.items.ItemRegistry
import de.fiereu.openmmo.moves.MoveRegistry
import de.fiereu.openmmo.pokemon.LearnsetRegistry
import de.fiereu.openmmo.pokemon.SpeciesRegistry
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import de.fiereu.openmmo.server.game.battle.WildMonFactory
import de.fiereu.openmmo.server.game.config.DeveloperToolsConfig
import de.fiereu.openmmo.server.game.config.GameServerConfig
import de.fiereu.openmmo.server.game.services.DexProgressService
import de.fiereu.openmmo.server.game.services.StoryPlayerService
import de.fiereu.openmmo.server.game.services.WorldStateService
import de.fiereu.openmmo.server.game.services.command.CommandContext
import de.fiereu.openmmo.server.game.services.command.DeveloperGiveExpansionCommand
import de.fiereu.openmmo.server.game.storage.CharacterStore
import de.fiereu.openmmo.server.game.storage.EntityIdService
import de.fiereu.openmmo.server.game.testsupport.FakeCharacterRepository
import de.fiereu.openmmo.server.game.testsupport.FakeSession
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperGiveExpansionCommandTest :
    FunSpec({
      suspend fun fixture(
          enabled: Boolean,
          scope: CoroutineScope,
          expansionClientContent: Boolean = false,
      ): Triple<DeveloperGiveExpansionCommand, CommandContext, CharacterStore> {
        val store = CharacterStore(FakeCharacterRepository(), EntityIdService(), scope)
        val created = store.createCharacter(1, "Red", CharacterGender.MALE, Region.KANTO)
        store.updateCharacter(
            created.info.copy(
                permissions = created.info.permissions or CharacterPermissions.DEVELOPER))
        val character = store.getCharacter(created.info.id)!!
        val session = FakeSession(characterId = character.info.id, regionId = 0)
        val expansion = ExpansionSpeciesRegistry()
        val species = SpeciesRegistry(expansion)
        val moves = MoveRegistry()
        val factory = WildMonFactory(species, moves, LearnsetRegistry(), EntityIdService())
        val player =
            StoryPlayerService(
                store,
                factory,
                species,
                moves,
                ItemRegistry(),
                DexProgressService(store),
                WorldStateService(DexProgressService(store)),
                testOcarinas(store))
        val tools =
            DeveloperTools(
                GameServerConfig(
                    "127.0.0.1",
                    0,
                    2,
                    "game.private.pem",
                    byteArrayOf(1),
                    developer =
                        DeveloperToolsConfig(
                            enabled = enabled,
                            expansionClientContent = expansionClientContent,
                        ),
                ))
        val command =
            DeveloperGiveExpansionCommand(
                tools, expansion, factory, player, de.fiereu.openmmo.moves.MoveRegistry())
        val context =
            CommandContext(session, session.state(), character, emptyList(), listOf(command))
        return Triple(command, context, store)
      }

      test("mapped Gen 5 control uses the normal party and persistence path") {
        runTest {
          val (command, context, store) = fixture(enabled = true, backgroundScope)
          command.run(context.withArgs("SPECIES_SNIVY", "10"))

          val pokemon = store.getCharacter(context.characterId)!!.pokemon.single()
          // One identity per species: dex 1-649 collapses from the expansion-offset id to the
          // plain canonical id at creation (WildMonFactory), so a given Snivy and a caught one
          // are the same monster server-side.
          pokemon.dexId shouldBe 495
          pokemon.moves.map { it.id.toInt() } shouldBe listOf(33, 43, 22, 35)
        }
      }

      test("a Fairy species is accepted now that the client enum carries the type") {
        runTest {
          val (command, context, store) =
              fixture(enabled = true, backgroundScope, expansionClientContent = true)
          command.run(context.withArgs("SPECIES_SYLVEON", "25"))

          // Fairy is ordinal 19 in the patched client enum, so Sylveon is no longer refused.
          store.getCharacter(context.characterId)!!.pokemon shouldNotBe emptyList<Pokemon>()
        }
      }

      test("generated Expansion content must be explicitly enabled") {
        runTest {
          val (command, context, store) = fixture(enabled = true, backgroundScope)
          command.run(context.withArgs("SPECIES_TYRUNT", "10"))
          store.getCharacter(context.characterId)!!.pokemon shouldBe emptyList()
        }
      }

      test("normal and shiny Tyrunt preserve canonical identity through the same creation path") {
        runTest {
          val (normalCommand, normalContext, normalStore) =
              fixture(enabled = true, backgroundScope, expansionClientContent = true)
          normalCommand.run(normalContext.withArgs("SPECIES_TYRUNT", "10"))
          val normal = normalStore.getCharacter(normalContext.characterId)!!.pokemon.single()
          normal.dexId shouldBe 0x10000 + 696
          normal.isShiny shouldBe false
          normal.moves.map { it.id.toInt() } shouldBe listOf(33, 39, 46, 246)

          val (shinyCommand, shinyContext, shinyStore) =
              fixture(enabled = true, backgroundScope, expansionClientContent = true)
          shinyCommand.run(shinyContext.withArgs("SPECIES_TYRUNT", "10", "shiny"))
          val shiny = shinyStore.getCharacter(shinyContext.characterId)!!.pokemon.single()
          shiny.dexId shouldBe normal.dexId
          shiny.isShiny shouldBe true
        }
      }

      test("command is disabled outside developer configuration") {
        runTest {
          val (command, context, store) = fixture(enabled = false, backgroundScope)
          command.run(context.withArgs("SPECIES_SNIVY", "10"))

          store.getCharacter(context.characterId)!!.pokemon shouldBe emptyList()
        }
      }
    })

private fun CommandContext.withArgs(vararg args: String): CommandContext =
    CommandContext(session, state, character, args.toList(), commands)
