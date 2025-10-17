package no.entur.geocoder.converter.importance

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ImportanceCalculatorTest {

    @Test
    fun `test minimum value returns floor`() {
        val result = ImportanceCalculator.calculateImportance(20)
        assertEquals(0.1, result, 0.001)
    }

    @Test
    fun `test maximum value returns 1_0`() {
        val result = ImportanceCalculator.calculateImportance(1_000_000_000)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun `test typical stop values`() {
        assertEquals(0.116, ImportanceCalculator.calculateImportance(30), 0.01)
        assertEquals(0.158, ImportanceCalculator.calculateImportance(60), 0.01)
        assertEquals(0.266, ImportanceCalculator.calculateImportance(600), 0.01)
    }

    @Test
    fun `test popular stops in middle range`() {
        assertEquals(0.415, ImportanceCalculator.calculateImportance(10_000), 0.01)
        assertEquals(0.649, ImportanceCalculator.calculateImportance(1_000_000), 0.01)
    }

    @Test
    fun `test mega groups near maximum`() {
        assertEquals(0.883, ImportanceCalculator.calculateImportance(100_000_000), 0.01)
    }

    @Test
    fun `test distribution preserves order`() {
        val values = listOf(20, 60, 600, 10_000, 1_000_000, 100_000_000, 1_000_000_000)
        val importances = values.map { ImportanceCalculator.calculateImportance(it) }

        // Verify strictly increasing
        for (i in 0 until importances.size - 1) {
            assert(importances[i] < importances[i + 1]) {
                "Importance not increasing: ${importances[i]} >= ${importances[i + 1]}"
            }
        }
    }
}
