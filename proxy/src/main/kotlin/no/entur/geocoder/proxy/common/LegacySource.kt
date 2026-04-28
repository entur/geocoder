package no.entur.geocoder.proxy.common

enum class LegacySource {
    openstreetmap, // NSR parent stops
    openaddresses, // Addresses (with numbers) from kartverket
    ;

    fun category() = LEGACY_SOURCE_PREFIX + this.name

    companion object {
        const val LEGACY_SOURCE_PREFIX = "legacy.source."
    }
}
