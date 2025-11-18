package no.entur.geocoder.converter.source.stedsnavn

import no.entur.geocoder.common.UtmCoordinate

/**
 * Data class representing a place name entry from Kartverket's stedsnavn GML file.
 *
 * Captures essential fields for geocoding including:
 * - Identity: lokalId, navnerom, versjonId
 * - Name: stedsnavn (primary), annenSkrivemåte (alternatives)
 * - Type: navneobjekttype (e.g., "by", "tettsted")
 * - Quality: skrivemåtestatus (spelling approval status)
 * - Location: kommunenummer/navn, fylkesnummer/navn, coordinates
 * - Optional references: matrikkelId, adressekode
 */
data class StedsnavnEntry(
    val lokalId: String,
    val navnerom: String,
    val versjonId: String?,
    val oppdateringsdato: String?,
    val stedsnavn: String,
    val kommunenummer: String,
    val kommunenavn: String,
    val fylkesnummer: String,
    val fylkesnavn: String,
    val matrikkelId: String?,
    val adressekode: String?,
    val navneobjekttype: String?,
    val skrivemåtestatus: String?, // Spelling status (e.g., "vedtatt", "godkjent")
    val coordinates: List<UtmCoordinate>, // UTM33 coordinates (east, north)
    val annenSkrivemåte: List<String> = emptyList(), // Alternative spellings
)
