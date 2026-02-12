package no.entur.geocoder.converter.source.lantmateriet

import no.entur.geocoder.converter.ConverterConfig.LantmaterietConfig

class LantmaterietPopularityCalculator(private val config: LantmaterietConfig) {
    fun calculateAddressPopularity(): Double = config.addressPopularity

    fun calculateStreetPopularity(): Double = config.streetPopularity
}
