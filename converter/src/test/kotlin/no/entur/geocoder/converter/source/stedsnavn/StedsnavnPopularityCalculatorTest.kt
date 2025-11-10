package no.entur.geocoder.converter.source.stedsnavn

import kotlin.test.Test
import kotlin.test.assertEquals

class StedsnavnPopularityCalculatorTest {
    @Test
    fun `returns flat popularity value of 40`() {
        assertEquals(40.0, StedsnavnPopularityCalculator.calculatePopularity())
        assertEquals(40.0, StedsnavnPopularityCalculator.calculatePopularity(null))
        assertEquals(40.0, StedsnavnPopularityCalculator.calculatePopularity("by"))
        assertEquals(40.0, StedsnavnPopularityCalculator.calculatePopularity("unknown"))
    }
}
