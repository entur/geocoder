package no.entur.geocoder.converter.source.adresse

import no.entur.geocoder.converter.ConverterConfig
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class MatrikkelPopularityCalculatorTest {
    private val calculator = MatrikkelPopularityCalculator(ConverterConfig().matrikkel)

    @Test
    fun `popularity returns expected value`() {
        assertEquals(20.0, calculator.calculateAddressPopularity())
        assertEquals(20.0, calculator.calculateStreetPopularity())
    }
}
