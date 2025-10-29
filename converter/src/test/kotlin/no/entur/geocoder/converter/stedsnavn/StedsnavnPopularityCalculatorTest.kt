package no.entur.geocoder.converter.stedsnavn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for StedsnavnPopularityCalculator matching kakka's flat popularity value (40).
 *
 * Kakka uses a flat placeBoost of 40 for all PLACE types (but not administrative units).
 * This implementation matches that behavior exactly.
 *
 * Formula: popularity = DEFAULT_VALUE (40.0 for all target types)
 */
class StedsnavnPopularityCalculatorTest {
    @Test
    fun `all target types return same popularity`() {
        val byPop = StedsnavnPopularityCalculator.calculatePopularity("by")
        val bydelPop = StedsnavnPopularityCalculator.calculatePopularity("bydel")
        val tettstedPop = StedsnavnPopularityCalculator.calculatePopularity("tettsted")
        val tettsteddelPop = StedsnavnPopularityCalculator.calculatePopularity("tettsteddel")
        val tettbebyggelsePop = StedsnavnPopularityCalculator.calculatePopularity("tettbebyggelse")

        // All should return 40.0 (matching kakka's flat placeBoost)
        assertEquals(40.0, byPop, "by should have popularity 40")
        assertEquals(40.0, bydelPop, "bydel should have popularity 40")
        assertEquals(40.0, tettstedPop, "tettsted should have popularity 40")
        assertEquals(40.0, tettsteddelPop, "tettsteddel should have popularity 40")
        assertEquals(40.0, tettbebyggelsePop, "tettbebyggelse should have popularity 40")
    }

    @Test
    fun `unknown type returns default popularity`() {
        val popularity = StedsnavnPopularityCalculator.calculatePopularity("grend")
        assertEquals(40.0, popularity, "unknown type should return default popularity 40")
    }

    @Test
    fun `null type returns default popularity`() {
        val popularity = StedsnavnPopularityCalculator.calculatePopularity(null)
        assertEquals(40.0, popularity, "null type should return default popularity 40")
    }

    @Test
    fun `empty type returns default popularity`() {
        val popularity = StedsnavnPopularityCalculator.calculatePopularity("")
        assertEquals(40.0, popularity, "empty type should return default popularity 40")
    }

    @Test
    fun `popularity is case insensitive`() {
        assertEquals(
            StedsnavnPopularityCalculator.calculatePopularity("by"),
            StedsnavnPopularityCalculator.calculatePopularity("BY"),
            "Case should not affect popularity",
        )
        assertEquals(
            StedsnavnPopularityCalculator.calculatePopularity("tettsted"),
            StedsnavnPopularityCalculator.calculatePopularity("TETTSTED"),
            "Case should not affect popularity",
        )
    }

    @Test
    fun `all popularities are positive`() {
        val types = listOf("by", "bydel", "tettsted", "tettsteddel", "tettbebyggelse")
        types.forEach { type ->
            val popularity = StedsnavnPopularityCalculator.calculatePopularity(type)
            assertTrue(popularity > 0, "$type should have positive popularity")
        }
    }

    @Test
    fun `popularity matches kakka placeBoost value`() {
        // Kakka's placeBoost configuration: 40
        val popularity = StedsnavnPopularityCalculator.calculatePopularity("tettsted")
        assertEquals(40.0, popularity, "Popularity should be 40.0 (matching kakka's placeBoost)")
    }

    @Test
    fun `all target types have same popularity`() {
        val types = listOf("by", "bydel", "tettsted", "tettsteddel", "tettbebyggelse")
        val popularities = types.map { StedsnavnPopularityCalculator.calculatePopularity(it) }

        // All popularities should be the same (flat value)
        assertEquals(1, popularities.distinct().size, "All target types should have same popularity (flat value)")
        assertEquals(40.0, popularities.first(), "All types should have popularity 40.0")
    }

    @Test
    fun `no parameter defaults to same value`() {
        val withoutParam = StedsnavnPopularityCalculator.calculatePopularity()
        val withNull = StedsnavnPopularityCalculator.calculatePopularity(null)

        assertEquals(withNull, withoutParam, "No parameter should default to same value as null parameter")
        assertEquals(40.0, withoutParam, "Should return 40.0")
    }
}
