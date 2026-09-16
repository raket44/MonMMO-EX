package de.fiereu.openmmo.server.game.services

import de.fiereu.openmmo.common.Pokemon
import de.fiereu.openmmo.common.PokemonMove
import de.fiereu.openmmo.common.enums.EVs
import de.fiereu.openmmo.common.enums.IVs
import de.fiereu.openmmo.common.enums.PokemonContainer
import de.fiereu.openmmo.common.enums.PokemonRarityFlag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

private fun mon(
    shiny: Boolean = false,
    secret: Boolean = false,
    alpha: Boolean = false,
    hiddenAbility: Boolean = false,
) =
    Pokemon(
        id = 1,
        ownerId = 1,
        container = PokemonContainer.PARTY,
        containerSlot = 0,
        dexId = 133,
        seed = 0,
        ot = "RaKeT",
        nickname = "",
        level = 50,
        hp = 100,
        xp = 0,
        eVs = EVs(),
        iVs = IVs(),
        moves = List(4) { PokemonMove(0, 0) },
        isShiny = shiny,
        hasHiddenAbility = hiddenAbility,
        isAlpha = alpha,
        isSecret = secret,
        isFatefulEncounter = false,
        isRaidEncounter = false,
        caughtAt = LocalDateTime.of(2026, 1, 1, 0, 0),
    )

private fun bits(flags: Short) = flags.toInt()

/**
 * The breed window compares the baby record it builds from these flags against BOTH parents and
 * announces every trait that will not be inherited (client strings 2521-2524). Sending zero made it
 * tell an alpha pair "It will not be an Alpha because one of the parents is not an Alpha"
 * (2026-09-16).
 */
class OffspringRarityTest :
    FunSpec({
      test("two alphas pass the alpha mark on") {
        val a = mon(alpha = true)
        val b = mon(alpha = true)
        PokemonRarityFlag.ALPHA.isSet(bits(offspringRarityFlags(a, b, a))) shouldBe true
      }

      test("an alpha bred with a non-alpha does not") {
        val a = mon(alpha = true)
        val b = mon()
        PokemonRarityFlag.ALPHA.isSet(bits(offspringRarityFlags(a, b, a))) shouldBe false
      }

      test("a shiny pair breeds a shiny, and secret needs both parents secret") {
        val plain = offspringRarityFlags(mon(shiny = true), mon(shiny = true), mon(shiny = true))
        PokemonRarityFlag.SHINY.isSet(bits(plain)) shouldBe true
        PokemonRarityFlag.SECRET_SHINY.isSet(bits(plain)) shouldBe false

        val secretPair = offspringRarityFlags(mon(secret = true), mon(secret = true), mon(secret = true))
        PokemonRarityFlag.SHINY.isSet(bits(secretPair)) shouldBe true
        PokemonRarityFlag.SECRET_SHINY.isSet(bits(secretPair)) shouldBe true

        val mixed = offspringRarityFlags(mon(shiny = true), mon(secret = true), mon(shiny = true))
        PokemonRarityFlag.SHINY.isSet(bits(mixed)) shouldBe true
        PokemonRarityFlag.SECRET_SHINY.isSet(bits(mixed)) shouldBe false
      }

      test("the hidden ability follows the main parent alone") {
        val withHa = mon(hiddenAbility = true)
        val without = mon()
        PokemonRarityFlag.HIDDEN_ABILITY.isSet(bits(offspringRarityFlags(withHa, without, withHa))) shouldBe true
        PokemonRarityFlag.HIDDEN_ABILITY.isSet(bits(offspringRarityFlags(withHa, without, without))) shouldBe false
      }

      test("a plain pair breeds a plain baby") {
        offspringRarityFlags(mon(), mon(), mon()) shouldBe 0.toShort()
      }
    })
