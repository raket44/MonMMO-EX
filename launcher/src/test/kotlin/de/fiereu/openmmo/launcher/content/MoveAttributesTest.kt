package de.fiereu.openmmo.launcher.content

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import java.nio.file.Path

/**
 * Measures the attribute mapping against the client's own file.
 *
 * The client ships these lines for moves 1-559. Deriving them from the Expansion and diffing tells
 * us whether the mapping is right, rather than leaving it to look plausible - the same standard the
 * section codecs are held to, applied to meaning instead of layout.
 */
class MoveAttributesTest :
    FunSpec({
      val root = Path.of("../../pokeemerald-expansion")
      val stock = stockDataPak()

      fun shipped(): Map<Int, List<MoveEffect>>? {
        val file = stock ?: return null
        return MoveCodec.decode(
                ClientDataPak.parse(Files.readAllBytes(file)).payloadOf(MoveCodec.type)!!)
            .filter { it.moveId in 1..559 }
            .associate { it.moveId to it.effects }
      }

      test("derived attributes match the ones the client already ships") {
        val client = shipped() ?: return@test
        val ids = MoveText.ids(root)
        val moves = MoveText.parse(root, ids).associateBy { it.id }
        val compared = client.keys.filter { moves.containsKey(it) }
        val exact =
            compared.count { id -> MoveAttributes.of(moves.getValue(id)) == client.getValue(id) }
        val mismatches =
            compared.filter { id -> MoveAttributes.of(moves.getValue(id)) != client.getValue(id) }
        mismatches.take(12).forEach { id ->
          val ours =
              MoveAttributes.of(moves.getValue(id)).map {
                it.effectId to it.params.map { p -> p.shorts }
              }
          val theirs = client.getValue(id).map { it.effectId to it.params.map { p -> p.shorts } }
          println("[attributes] move $id ${moves.getValue(id).name}")
          println("[attributes]   ours   $ours")
          println("[attributes]   theirs $theirs")
        }
        val rate = exact.toDouble() / compared.size
        println("[attributes] exact match on $exact of ${compared.size} client moves")
        // The lines the client derives from mechanics we do not model will never match, so this is
        // a floor that catches a broken mapping, not a claim of perfection.
        rate shouldBeGreaterThan 0.85
      }

      test("a contact move with a secondary effect comes out right") {
        val client = shipped() ?: return@test
        val ids = MoveText.ids(root)
        val moves = MoveText.parse(root, ids).associateBy { it.id }
        // Fire Punch: contact, protectable, copyable, 10% burn.
        MoveAttributes.of(moves.getValue(7)) shouldBe client.getValue(7)
        // Pound, the simplest damaging move there is.
        MoveAttributes.of(moves.getValue(1)) shouldBe client.getValue(1)
      }
    })
