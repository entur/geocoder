package no.entur.geocoder.proxy.v3

import io.ktor.http.*

/**
 * Shared filter fields between v3 autocomplete and reverse requests. Implemented by both
 * `V3AutocompleteRequest` and `V3ReverseRequest` so the Photon include builder and the
 * result-side filter echo can take one type.
 */
interface V3FilterParams {
    val countries: List<String>
    val counties: List<String>
    val localities: List<String>
    val fareZones: List<String>
    val fareZoneAuthorities: List<String>
    val sources: List<String>
    val layers: List<String>
    val multimodal: String
}

/** Parse a comma-separated query parameter, dropping blank entries. */
internal fun Parameters.csv(name: String): List<String> =
    this[name]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
