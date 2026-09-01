package de.fiereu.openmmo.net.game.packets

import de.fiereu.bytecodec.*

/**
 * One possible IV outcome for a stat, client class f/dm (parser sB1.qP1, renderer pM1.Cm0, both
 * bytecode-walked): [value] is the IV number (dm.k41 - the renderer's TreeSet of these makes the
 * "min - max" range and equal values merge), [percent] is the chance ALREADY SCALED to 0-100 (dm.hS
 * is formatted with a "%" suffix as-is), and [labelStringId] is an optional client string id
 * rendered as the row's tooltip label (dm.rA0; <= 0 falls back to string 2546 "{value}:
 * {percent}%", retail uses ids like 2541 "High pass" / 2542 "Low pass" / 2544 "Average").
 */
data class BreedingStatContribution(
    val value: Byte,
    val percent: Float,
    val labelStringId: Int,
)

/**
 * Per-stat forecast row, client class f/Cp. The stat itself is the ARRAY POSITION (the renderer
 * indexes statEntries[stat.Df0]); [braceItemId] (Cp.FC1) is the held Power item causing a
 * guaranteed pass - any value > 0 renders "Guaranteed inheritance due to {item}", so it MUST stay 0
 * on unbraced rows (stat indices sent here once rendered as Poke Ball names).
 */
data class BreedingStatEntry(
    val guaranteed: Boolean,
    val braceItemId: Short,
    val contributions: List<BreedingStatContribution>,
)

data class BreedingForecastPacket(
    val parentA: Long,
    val parentB: Long,
    val hasPreview: Boolean,
    val species: Short,
    val form: Byte,
    val statEntries: List<BreedingStatEntry>,
    /**
     * The offspring's possible NATURES, as f/ns0 ids 0-24 (each enum constant carries a raised and
     * lowered RC0 stat and two flavors - it was misread as shininess variants for a while). EMPTY
     * means a random roll: the window shows "???" (tooltip string 2532). A non-empty list renders
     * the names at 100/n% each with "Guaranteed inheritance due to Everstone" (the client hardcodes
     * item 195 for the label), so only fill it when a parent actually holds one. The client shows a
     * mon's nature as (seed & 0xFFFFFFFF) % 25 (k91.xH), the same derivation as Pokemon.nature.
     */
    val possibleNatures: List<Byte>,
    val valueIds: List<Short>,
    val valueSources: List<Byte>,
    val gender: Byte,
    val nature: Short,
    val shiny: Boolean,
    val cost: Int,
    val secondaryCost: Int,
)

private object BreedingStatContributionCodec : PacketCodec<BreedingStatContribution>() {
  override fun CodecScope<BreedingStatContribution>.body(): BreedingStatContribution {
    val value = field(S8, BreedingStatContribution::value)
    val percent = field(F32LE, BreedingStatContribution::percent)
    val labelStringId = field(S32LE, BreedingStatContribution::labelStringId)
    return BreedingStatContribution(value, percent, labelStringId)
  }
}

private val BreedingStatContributionListPrefixedU8: Codec<List<BreedingStatContribution>> =
    object : Codec<List<BreedingStatContribution>> {
      override fun read(buf: ReadBuffer): List<BreedingStatContribution> {
        val n = U8.read(buf)
        return List(n) { BreedingStatContributionCodec.read(buf) }
      }

      override fun write(buf: WriteBuffer, value: List<BreedingStatContribution>) {
        U8.write(buf, value.size)
        value.forEach { BreedingStatContributionCodec.write(buf, it) }
      }
    }

private object BreedingStatEntryCodec : PacketCodec<BreedingStatEntry>() {
  override fun CodecScope<BreedingStatEntry>.body(): BreedingStatEntry {
    val guaranteed = field(Bool, BreedingStatEntry::guaranteed)
    val braceItemId = field(S16LE, BreedingStatEntry::braceItemId)
    val contributions =
        field(BreedingStatContributionListPrefixedU8, BreedingStatEntry::contributions)
    return BreedingStatEntry(guaranteed, braceItemId, contributions)
  }
}

object BreedingForecastPacketCodec : PacketCodec<BreedingForecastPacket>() {
  override fun CodecScope<BreedingForecastPacket>.body(): BreedingForecastPacket {
    val parentA = field(S64LE, BreedingForecastPacket::parentA)
    val parentB = field(S64LE, BreedingForecastPacket::parentB)
    val hasPreview = field(Bool, BreedingForecastPacket::hasPreview)
    if (!hasPreview) {
      return BreedingForecastPacket(
          parentA,
          parentB,
          hasPreview,
          0,
          0,
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
          0,
          0,
          false,
          0,
          0,
      )
    }
    val species = field(S16LE, BreedingForecastPacket::species)
    val form = field(S8, BreedingForecastPacket::form)
    val statCount = field(U8) { it.statEntries.size }
    val statEntries = List(statCount) { i -> field(BreedingStatEntryCodec) { it.statEntries[i] } }
    val natureCount = field(U8) { it.possibleNatures.size }
    val possibleNatures = List(natureCount) { i -> field(S8) { it.possibleNatures[i] } }
    val valueCount = field(U8) { it.valueIds.size }
    val valueIds = List(valueCount) { i -> field(S16LE) { it.valueIds[i] } }
    val valueSources = List(valueCount) { i -> field(S8) { it.valueSources[i] } }
    val gender = field(S8, BreedingForecastPacket::gender)
    val nature = field(S16LE, BreedingForecastPacket::nature)
    val shiny = field(Bool, BreedingForecastPacket::shiny)
    val cost = field(S32LE, BreedingForecastPacket::cost)
    val secondaryCost = field(S32LE, BreedingForecastPacket::secondaryCost)
    return BreedingForecastPacket(
        parentA,
        parentB,
        hasPreview,
        species,
        form,
        statEntries,
        possibleNatures,
        valueIds,
        valueSources,
        gender,
        nature,
        shiny,
        cost,
        secondaryCost,
    )
  }
}
