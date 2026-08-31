package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

/**
 * Proves every codec against the client's own data.pak.
 *
 * A codec earns the right to rewrite a section only by decoding the stock payload and re-encoding
 * it to identical bytes. That is what stops a guessed record layout from reaching the client: the
 * build calls the same check before it edits anything, so a wrong header fails here or in staging
 * rather than as a blank Pokedex or a client that will not boot.
 */
class ClientDataPakTest :
    FunSpec({
      val stock = stockDataPak()

      test("the stock file parses and re-serialises to identical bytes") {
        stock ?: return@test
        val pak = ClientDataPak.parse(Files.readAllBytes(stock))
        // gzip settings differ, so compare the decoded structure rather than the compressed file.
        val roundTripped = ClientDataPak.parse(pak.compress())
        roundTripped.version shouldBe pak.version
        CODECS.forEach { codec ->
          roundTripped.payloadOf(codec.type)!!.contentEquals(pak.payloadOf(codec.type)!!) shouldBe
              true
        }
      }

      CODECS.forEach { codec ->
        test("${codec.name} round-trips the stock section byte for byte") {
          stock ?: return@test
          val payload =
              ClientDataPak.parse(Files.readAllBytes(stock)).payloadOf(codec.type)
                  ?: error("The stock data.pak has no ${codec.name} section (${codec.type})")
          // Throws with the record and byte counts if the layout is wrong anywhere.
          codec.verify(payload)
          codec.decode(payload).size shouldBeGreaterThan 0
        }
      }

      test("the species section holds only the ids the ROMs do not supply") {
        stock ?: return@test
        val species =
            SpeciesCodec.decode(
                ClientDataPak.parse(Files.readAllBytes(stock)).payloadOf(SpeciesCodec.type)!!)
        // Dex 1-649 come from the ROMs, so this section carries only the client's reserved
        // 1000-1052 block. New species join it there, which is why our wire ids step over 1052.
        species.map { it.speciesId }.toSet() shouldBe (1000..1052).toSet()
        val record = species.single { it.speciesId == 1050 }
        record.stats.size shouldBe SpeciesRecord.STAT_COUNT
        record.abilities.size shouldBe SpeciesRecord.ABILITY_COUNT
      }

      test("the learnset section does cover every canonical species") {
        stock ?: return@test
        val pak = ClientDataPak.parse(Files.readAllBytes(stock))
        val ids =
            LevelUpLearnsetCodec.decode(pak.payloadOf(LevelUpLearnsetCodec.type)!!).map {
              it.speciesId
            }
        // Unlike the species section, moves are not read from the ROMs: 1-649 are all listed here.
        ids.toSet() shouldContainAll (1..649).toSet()
      }

      test("the detail section is sparse, so it overrides rather than defines") {
        stock ?: return@test
        val pak = ClientDataPak.parse(Files.readAllBytes(stock))
        val details = SpeciesDetailCodec.decode(pak.payloadOf(SpeciesDetailCodec.type)!!)
        // Only a few hundred species appear, and egg groups on fewer still: the ROM supplies the
        // rest. New species are absent from the ROM, so they need every field written here.
        details.size shouldBeLessThan 649
        details.count { it.eggGroups != null } shouldBeGreaterThan 0
      }

      test("the move extra section covers every move the client has, defining none of them") {
        stock ?: return@test
        val extra =
            MoveExtraCodec.decode(
                ClientDataPak.parse(Files.readAllBytes(stock)).payloadOf(MoveExtraCodec.type)!!)
        // 559 moves, no gaps, descending. Type, power and accuracy are not here; they are in the
        // move table, section 4, which holds 842 records and is still to be decoded.
        extra.map { it.moveId } shouldContainExactly (559 downTo 1).toList()
        extra.count { it.pairs.isNotEmpty() } shouldBe 33
        extra.filter { it.pairs.isNotEmpty() }.forEach { it.pairs.size shouldBe 2 }
      }

      test("the stock file fills five of the six dex slots, leaving the last free") {
        stock ?: return@test
        val regions =
            RegionalDexCodec.decode(
                ClientDataPak.parse(Files.readAllBytes(stock)).payloadOf(RegionalDexCodec.type)!!)
        regions.size shouldBe 5
        regions.map { it.regionId } shouldContainExactly listOf(0, 1, 2, 3, 4)
      }

      test("editing a section leaves every other section untouched") {
        stock ?: return@test
        val pak = ClientDataPak.parse(Files.readAllBytes(stock))
        val added =
            SpeciesRecord(
                speciesId = 1500,
                type1 = 19, // Fairy, which the classpath overlay adds to the client type enum
                type2 = 19,
                stats = listOf(126, 131, 95, 99, 131, 98),
                abilities = listOf(187, 0, 0),
                trailer = 255,
            )
        val edited = pak.edit(SpeciesCodec) { it + added }

        SpeciesCodec.decode(edited.payloadOf(SpeciesCodec.type)!!).last() shouldBe added
        CODECS.filter { it.type != SpeciesCodec.type }
            .forEach { codec ->
              edited.payloadOf(codec.type)!!.contentEquals(pak.payloadOf(codec.type)!!) shouldBe
                  true
            }
      }
    })

private val CODECS =
    listOf(
        SpeciesCodec,
        LevelUpLearnsetCodec,
        ExtraLearnsetCodec,
        SpeciesDetailCodec,
        RegionalDexCodec,
        MoveExtraCodec,
        MoveCodec,
    )

/**
 * The pristine client file, kept beside the install so staging stays repeatable. It is not in the
 * repository, so these tests stand down where it is absent rather than failing a clean checkout.
 */
internal fun stockDataPak(): Path? {
  val local = System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home")
  return listOf(
          "$local/MonMMO-EX/Client-31914/stock-backup/data/data.pak",
          "$local/MonMMO-EX/Client-31914/data/data.pak",
      )
      .map(Path::of)
      .firstOrNull(Files::isRegularFile)
}
