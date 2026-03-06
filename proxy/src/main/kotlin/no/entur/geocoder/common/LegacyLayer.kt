package no.entur.geocoder.common

enum class LegacyLayer {
    venue,
    address,
    ;

    fun category() = LEGACY_LAYER_PREFIX + this.name

    companion object {
        const val LEGACY_LAYER_PREFIX = "legacy.layer."
    }
}
