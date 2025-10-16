package no.entur.geocoder.converter.stedsnavn

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
    val coordinates: List<Pair<Double, Double>>, // UTM33 coordinates (east, north)
)

