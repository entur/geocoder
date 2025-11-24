package no.entur.geocoder.converter.source.adresse

import no.entur.geocoder.converter.ConverterConfig.MatrikkelConfig

class MatrikkelPopularityCalculator(private val config: MatrikkelConfig) {
    fun calculateAddressPopularity(): Double = config.addressPopularity

    fun calculateStreetPopularity(): Double = config.streetPopularity
}
