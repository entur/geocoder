package no.entur.geocoder.common

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportanceCalculatorTest {
    @Test
    fun `output is always within valid range`() {
        val testValues = listOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 100_000_000, 1_000_000_000)
        testValues.forEach { value ->
            val importance = ImportanceCalculator.calculateImportance(value)
            assertTrue(importance >= 0.1, "Importance $importance below floor for value $value")
            assertTrue(importance <= 1.0, "Importance $importance above 1.0 for value $value")
        }
    }

    @Test
    fun `higher popularity always gives higher importance`() {
        val values = listOf(1, 50, 500, 5_000, 50_000, 500_000, 5_000_000, 50_000_000)
        val importances = values.map { ImportanceCalculator.calculateImportance(it) }

        // Verify monotonically increasing
        importances.zipWithNext().forEach { (current, next) ->
            assertTrue(next > current, "Monotonicity violated: $current >= $next")
        }
    }

    @Test
    fun `locationBiasCalculator maps weight to scale correctly`() {
        assertEquals(1.0, ImportanceCalculator.locationBiasCalculator(0.0), 0.001)
        assertEquals(0.2, ImportanceCalculator.locationBiasCalculator(15.0), 0.001)
        assertEquals(0.0, ImportanceCalculator.locationBiasCalculator(50.0), 0.001)
        assertEquals(0.0, ImportanceCalculator.locationBiasCalculator(1000.0), 0.001)
        assertEquals(1.0, ImportanceCalculator.locationBiasCalculator(-10.0), 0.001)
    }

    @Test
    fun `locationBiasCalculator is monotonically decreasing`() {
        val scales = listOf(0, 10, 20, 30, 40, 50).map { ImportanceCalculator.locationBiasCalculator(it.toDouble()) }
        scales.zipWithNext().forEach { (curr, next) -> assertTrue(curr >= next) }
    }

    @Test
    fun `minimum value gives floor`() {
        val result = ImportanceCalculator.calculateImportance(1)
        assertEquals(0.1, result, 0.001)
    }

    @Test
    fun `maximum value gives 1_0`() {
        val result = ImportanceCalculator.calculateImportance(1_000_000_000)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun `equal logarithmic steps produce equal importance steps`() {
        // Powers of 10 should have equal spacing in importance
        val step1 = ImportanceCalculator.calculateImportance(100)
        val step2 = ImportanceCalculator.calculateImportance(1_000)
        val step3 = ImportanceCalculator.calculateImportance(10_000)
        val step4 = ImportanceCalculator.calculateImportance(100_000)

        val delta1 = step2 - step1
        val delta2 = step3 - step2
        val delta3 = step4 - step3

        // All deltas should be approximately equal (logarithmic scale)
        assertEquals(delta1, delta2, 0.01)
        assertEquals(delta2, delta3, 0.01)
    }

    @Test
    fun `preserves ordering of any input sequence`() {
        val randomValues = listOf(42, 157, 1_234, 5_678, 123_456, 9_876_543)
        val importances = randomValues.map { ImportanceCalculator.calculateImportance(it) }

        assertEquals(importances, importances.sorted())
    }

    @Test
    fun `custom range respects boundaries`() {
        val minPop = 10.0
        val maxPop = 10_000.0
        val floor = 0.2

        // Minimum should give floor
        val minResult = ImportanceCalculator.calculateImportance(10, minPop, maxPop, floor)
        assertEquals(floor, minResult, 0.001)

        // Maximum should give 1.0
        val maxResult = ImportanceCalculator.calculateImportance(10_000, minPop, maxPop, floor)
        assertEquals(1.0, maxResult, 0.001)
    }

    @Test
    fun `custom floor is respected`() {
        val customFloor = 0.3
        val result = ImportanceCalculator.calculateImportance(1, floor = customFloor)
        assertEquals(customFloor, result, 0.001)
    }
}
