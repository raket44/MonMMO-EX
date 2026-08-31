package de.fiereu.openmmo.pokemon

import de.fiereu.openmmo.common.enums.PokemonType
import de.fiereu.openmmo.pokemon.expansion.EXPANSION_SERVER_ID_BASE
import de.fiereu.openmmo.pokemon.expansion.ExpansionSpeciesRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeUnique
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ExpansionSpeciesCatalogTest :
    FunSpec({
      val expansion = ExpansionSpeciesRegistry()

      test("configured Expansion catalog is complete and stable") {
        val report = expansion.report()
        report.totalSpeciesAndForms shouldBe 1571
        report.baseSpecies shouldBe 1025
        report.forms shouldBe 546
        report.highestOriginalId shouldBe 1572
        report.duplicateOriginalIds shouldBe emptySet()
        report.knownClientMappings shouldBe 1571
        report.requiringClientMapping shouldBe 0
        report.stockClientMappings shouldBe 649
        report.generatedClientMappings shouldBe 675
        expansion.all().map { it.stableId }.shouldBeUnique()
        expansion.all().map { it.serverId }.shouldBeUnique()
      }

      test("retail Emerald registry remains unchanged") {
        val registry = SpeciesRegistry(expansion)
        registry.size() shouldBe 386
        registry.all().map { it.id }.toSet() shouldBe (1..386).toSet()
        registry.get(1)!!.name shouldBe "BULBASAUR"
      }

      test("Sylveon and Sprigatito preserve Expansion identity and can form runtime definitions") {
        val sylveon = expansion.get("SPECIES_SYLVEON")!!
        sylveon.stableId shouldBe "expansion:SPECIES_SYLVEON"
        sylveon.originalId shouldBe 700
        sylveon.serverId shouldBe EXPANSION_SERVER_ID_BASE + 700
        sylveon.typeSymbols shouldBe listOf("TYPE_FAIRY", "TYPE_FAIRY")
        sylveon.clientWireId shouldBe 700
        sylveon.runtimeDefinition()!!.type1 shouldBe PokemonType.FAIRY

        val sprigatito = expansion.get("SPECIES_SPRIGATITO")!!
        sprigatito.originalId shouldBe 1289
        sprigatito.clientWireId shouldBe 906
        sprigatito.runtimeDefinition() shouldNotBe null
      }

      test("Tyrunt keeps Expansion stats abilities learnset evolution and stable wire identity") {
        val tyrunt = expansion.get("SPECIES_TYRUNT")!!
        tyrunt.originalId shouldBe 696
        tyrunt.serverId shouldBe EXPANSION_SERVER_ID_BASE + 696
        tyrunt.clientWireId shouldBe 696
        tyrunt.typeSymbols shouldBe listOf("TYPE_ROCK", "TYPE_DRAGON")
        tyrunt.abilitySymbols shouldBe
            listOf("ABILITY_STRONG_JAW", "ABILITY_NONE", "ABILITY_STURDY")
        tyrunt.levelUpLearnset.take(4).map { it.moveSymbol } shouldBe
            listOf("MOVE_TACKLE", "MOVE_TAIL_WHIP", "MOVE_ROAR", "MOVE_ANCIENT_POWER")
        tyrunt.evolutionTargetStableIds shouldBe listOf("expansion:SPECIES_TYRANTRUM")
        tyrunt.abilityMechanicsSupported shouldBe false
        tyrunt.clientContentCompatible shouldBe true
      }

      test("known Gen 5 control has a verified client mapping") {
        val snivy = expansion.get("SPECIES_SNIVY")!!
        snivy.clientWireId shouldBe 495
        expansion.runtimeDefinition(495)!!.id shouldBe 495
      }

      test("namespaced lookup is stable and separate from the bare symbol alias") {
        expansion.get("expansion:SPECIES_SYLVEON") shouldBe expansion.get("SPECIES_SYLVEON")
      }
      test("client asset sources resolve to real files in the Expansion tree") {
        val tyrunt = expansion.get("SPECIES_TYRUNT")!!
        tyrunt.assets.frontPicPath shouldBe "graphics/pokemon/tyrunt/anim_front.png"
        tyrunt.assets.backPicPath shouldBe "graphics/pokemon/tyrunt/back.png"
        tyrunt.assets.iconPath shouldBe "graphics/pokemon/tyrunt/icon.png"
        tyrunt.assets.normalPalettePath shouldBe "graphics/pokemon/tyrunt/normal.pal"
        tyrunt.assets.shinyPalettePath shouldBe "graphics/pokemon/tyrunt/shiny.pal"
        tyrunt.assets.cryPath shouldBe "sound/direct_sound_samples/cries/tyrunt.wav"
        tyrunt.assets.iconPalIndex shouldBe 2
        tyrunt.assetSourcesResolved shouldBe true
      }

      test("every client-compatible species can be rendered from Expansion sources") {
        val staged = expansion.all().filter { it.isNewToClient && it.clientContentCompatible }
        staged.count { !it.assetSourcesResolved } shouldBe 0
      }
      test("cry sources survive the decomp inconsistent CamelCase cry labels") {
        val cry = { symbol: String -> expansion.get(symbol)!!.assets.cryPath }
        cry("SPECIES_PORYGON2") shouldBe "sound/direct_sound_samples/cries/porygon2.wav"
        cry("SPECIES_LOPUNNY_MEGA") shouldBe "sound/direct_sound_samples/cries/lopunny_mega.wav"
        // Cry_Zygarde50 splits where Cry_Porygon2 does not, so a name rule alone cannot match both.
        cry("SPECIES_ZYGARDE_50") shouldBe "sound/direct_sound_samples/cries/zygarde_50.wav"

        val staged = expansion.all().filter { it.isNewToClient && it.clientContentCompatible }
        staged.count { it.assets.cryPath.isEmpty() } shouldBe 1
      }
    })
