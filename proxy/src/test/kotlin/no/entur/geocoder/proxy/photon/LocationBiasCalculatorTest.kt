package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.photon.LocationBiasCalculator.calculateLocationBias
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationBiasCalculatorTest {
    @Test
    fun `locationBiasCalculator maps weight to scale correctly`() {
        assertEquals(1.0, calculateLocationBias(0.0), 0.001)
        assertEquals(0.2, calculateLocationBias(15.0), 0.001)
        assertEquals(0.0, calculateLocationBias(50.0), 0.001)
        assertEquals(0.0, calculateLocationBias(1000.0), 0.001)
        assertEquals(1.0, calculateLocationBias(-10.0), 0.001)
    }

    @Test
    fun `locationBiasCalculator is monotonically decreasing`() {
        val scales = listOf(0, 10, 20, 30, 40, 50).map { calculateLocationBias(it.toDouble()) }
        scales.zipWithNext().forEach { (curr, next) -> assertTrue(curr >= next) }
    }
}
