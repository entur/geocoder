package no.entur.geocoder.converter.source.adresse

import no.entur.geocoder.converter.TestConfig
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class MatrikkelPopularityCalculatorTest {
    private val calculator = MatrikkelPopularityCalculator(TestConfig.config.matrikkel)

    @Test
    fun `popularity returns expected value`() {
        assertEquals(20.0, calculator.calculateAddressPopularity())
        assertEquals(20.0, calculator.calculateStreetPopularity())
    }
}
