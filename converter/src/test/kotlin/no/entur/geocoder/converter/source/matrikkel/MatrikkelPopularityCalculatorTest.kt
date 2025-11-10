package no.entur.geocoder.converter.source.matrikkel

import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class MatrikkelPopularityCalculatorTest {
    @Test
    fun `popularity returns expected value`() {
        assertEquals(20.0, MatrikkelPopularityCalculator.calculateAddressPopularity())
        assertEquals(20.0, MatrikkelPopularityCalculator.calculateStreetPopularity())
    }
}

