package no.entur.geocoder.converter.importance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ImportanceCalculatorTest {

    @Test
    fun `test minimum value returns floor`() {
        // With MIN_POPULARITY = 1.0, value of 1 should give exactly the floor
        val result = ImportanceCalculator.calculateImportance(1)
        assertEquals(0.1, result, 0.001)
    }

    @Test
    fun `test maximum value returns 1_0`() {
        val result = ImportanceCalculator.calculateImportance(1_000_000_000)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun `test typical stop values`() {
        // With MIN_POPULARITY = 1.0: scaled = 0.1 + (log10(pop) / 9) * 0.9
        // 30: log10(30) = 1.477, normalized = 1.477/9 = 0.164, scaled = 0.1 + 0.164*0.9 = 0.248
        assertEquals(0.248, ImportanceCalculator.calculateImportance(30), 0.01)
        // 60: log10(60) = 1.778, normalized = 1.778/9 = 0.198, scaled = 0.1 + 0.198*0.9 = 0.278
        assertEquals(0.278, ImportanceCalculator.calculateImportance(60), 0.01)
        // 600: log10(600) = 2.778, normalized = 2.778/9 = 0.309, scaled = 0.1 + 0.309*0.9 = 0.378
        assertEquals(0.378, ImportanceCalculator.calculateImportance(600), 0.01)
    }

    @Test
    fun `test popular stops in middle range`() {
        // 10,000: log10(10000) = 4, normalized = 4/9 = 0.444, scaled = 0.1 + 0.444*0.9 = 0.500
        assertEquals(0.500, ImportanceCalculator.calculateImportance(10_000), 0.01)
        // 1,000,000: log10(1000000) = 6, normalized = 6/9 = 0.667, scaled = 0.1 + 0.667*0.9 = 0.700
        assertEquals(0.700, ImportanceCalculator.calculateImportance(1_000_000), 0.01)
    }

    @Test
    fun `test mega groups near maximum`() {
        // 100,000,000: log10(100000000) = 8, normalized = 8/9 = 0.889, scaled = 0.1 + 0.889*0.9 = 0.900
        assertEquals(0.900, ImportanceCalculator.calculateImportance(100_000_000), 0.01)
    }

    @Test
    fun `test distribution preserves order`() {
        // Updated to start from 1 (new MIN_POPULARITY)
        val values = listOf(1, 60, 600, 10_000, 1_000_000, 100_000_000, 1_000_000_000)
        val importances = values.map { ImportanceCalculator.calculateImportance(it) }

        // Verify strictly increasing
        for (i in 0 until importances.size - 1) {
            assert(importances[i] < importances[i + 1]) {
                "Importance not increasing: ${importances[i]} >= ${importances[i + 1]}"
            }
        }
    }
}
