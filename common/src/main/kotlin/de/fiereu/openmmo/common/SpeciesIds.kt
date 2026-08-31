package de.fiereu.openmmo.common

/** Canonical server ids for Expansion records live outside every retail National Dex range. */
const val EXPANSION_SERVER_SPECIES_BASE = 0x10000

/**
 * The managed client reserves 1000-1052 for its own custom records. Expansion records use a
 * separate deterministic range, based on the configured source id rather than catalogue order.
 */
const val EXPANSION_CLIENT_SPECIES_BASE = 2000

private const val MAX_UNSIGNED_SHORT = 0xFFFF

fun expansionServerSpeciesId(originalId: Int): Int {
  require(originalId > 0) { "Expansion species id must be positive: $originalId" }
  return EXPANSION_SERVER_SPECIES_BASE + originalId
}

fun expansionClientSpeciesId(originalId: Int): Int {
  require(originalId > 0) { "Expansion species id must be positive: $originalId" }
  val wireId = EXPANSION_CLIENT_SPECIES_BASE + originalId
  require(wireId <= MAX_UNSIGNED_SHORT) {
    "Expansion species $originalId exceeds the client's unsigned 16-bit species field"
  }
  return wireId
}

/**
 * How canonical ids become client ids. Since Dex ordering, the rule depends on National Dex numbers
 * and form ordinals that live in the generated catalogue, which this module sits below. The server
 * installs the real mapping at startup; the fallback below is the plain arithmetic used before Dex
 * ordering, which keeps standalone tools and protocol tests working on their own.
 */
object SpeciesWireIds {
  @Volatile private var toClient: ((Int) -> Int)? = null
  @Volatile private var toCanonical: ((Int) -> Int)? = null

  fun install(toClient: (Int) -> Int, toCanonical: (Int) -> Int) {
    this.toClient = toClient
    this.toCanonical = toCanonical
  }

  internal fun mapToClient(canonicalId: Int): Int? = toClient?.invoke(canonicalId)

  internal fun mapToCanonical(clientId: Int): Int? = toCanonical?.invoke(clientId)
}

/** Translate persistent canonical identity to the id understood by the managed client. */
fun clientSpeciesId(canonicalId: Int): Int =
    SpeciesWireIds.mapToClient(canonicalId) ?: fallbackClientSpeciesId(canonicalId)

private fun fallbackClientSpeciesId(canonicalId: Int): Int =
    if (canonicalId >= EXPANSION_SERVER_SPECIES_BASE) {
      val originalId = canonicalId - EXPANSION_SERVER_SPECIES_BASE
      if (originalId in 1..649) originalId else expansionClientSpeciesId(originalId)
    } else {
      require(canonicalId in 0..MAX_UNSIGNED_SHORT) {
        "Species $canonicalId cannot be serialized to the client's unsigned 16-bit species field"
      }
      canonicalId
    }

/** Reverse only the range explicitly reserved for generated Expansion client records. */
fun canonicalSpeciesId(clientId: Int): Int =
    SpeciesWireIds.mapToCanonical(clientId) ?: fallbackCanonicalSpeciesId(clientId)

private fun fallbackCanonicalSpeciesId(clientId: Int): Int =
    if (clientId > EXPANSION_CLIENT_SPECIES_BASE) {
      expansionServerSpeciesId(clientId - EXPANSION_CLIENT_SPECIES_BASE)
    } else {
      clientId
    }
