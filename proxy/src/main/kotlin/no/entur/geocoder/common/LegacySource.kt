package no.entur.geocoder.common

enum class LegacySource {
    whosonfirst,
    openstreetmap, // NSR parent stops
    openaddresses, // Addresses (with numbers) from kartverket
    geonames, // NSR child stops
    ;

    fun category() = LEGACY_SOURCE_PREFIX + this.name

    companion object {
        const val LEGACY_SOURCE_PREFIX = "legacy.source."
    }
}
