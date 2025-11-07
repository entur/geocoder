package no.entur.geocoder.converter.source.matrikkel

object MatrikkelPopularityCalculator {
    private const val DEFAULT_VALUE = 20.0

    fun calculateAddressPopularity(): Double = DEFAULT_VALUE

    fun calculateStreetPopularity(): Double = DEFAULT_VALUE
}
