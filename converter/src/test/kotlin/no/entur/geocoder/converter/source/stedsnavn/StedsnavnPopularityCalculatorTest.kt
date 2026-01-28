package no.entur.geocoder.converter.source.stedsnavn

import no.entur.geocoder.converter.TestConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class StedsnavnPopularityCalculatorTest {
    private val calculator = StedsnavnPopularityCalculator(TestConfig.config.stedsnavn)

    @Test
    fun `returns flat popularity value of 40`() {
        assertEquals(40.0, calculator.calculatePopularity())
        assertEquals(40.0, calculator.calculatePopularity(null))
        assertEquals(40.0, calculator.calculatePopularity("by"))
        assertEquals(40.0, calculator.calculatePopularity("unknown"))
    }
}
