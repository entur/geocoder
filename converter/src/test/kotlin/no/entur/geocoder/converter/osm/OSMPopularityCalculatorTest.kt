package no.entur.geocoder.converter.osm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OSMPopularityCalculatorTest {
    @Test
    fun `calculates popularity as base times priority`() {
        // Test that the formula is: popularity = DEFAULT_VALUE × priority
        val hospitalTags = mapOf("amenity" to "hospital") // Priority 9
        val cinemaTags = mapOf("amenity" to "cinema") // Priority 1

        val hospitalPop = OSMPopularityCalculator.calculatePopularity(hospitalTags)
        val cinemaPop = OSMPopularityCalculator.calculatePopularity(cinemaTags)

        // Both should be non-zero and hospital should be exactly 9x cinema
        assertTrue(hospitalPop > 0, "Hospital should have positive popularity")
        assertTrue(cinemaPop > 0, "Cinema should have positive popularity")
        assertEquals(hospitalPop / cinemaPop, 9.0, "Hospital should be 9x more popular than cinema")
    }

    @Test
    fun `multiple matching tags use highest priority not sum or average`() {
        val highOnly = mapOf("amenity" to "hospital") // Priority 9
        val lowOnly = mapOf("amenity" to "cinema") // Priority 1
        val both =
            mapOf(
                "amenity" to "hospital", // Priority 9
                "tourism" to "attraction", // Priority 1
            )

        val highPop = OSMPopularityCalculator.calculatePopularity(highOnly)
        val lowPop = OSMPopularityCalculator.calculatePopularity(lowOnly)
        val bothPop = OSMPopularityCalculator.calculatePopularity(both)

        // Should use max, not sum or average
        assertEquals(highPop, bothPop, "Should use highest priority, not sum or average")
        assertTrue(bothPop != highPop + lowPop, "Should not sum priorities")
        assertTrue(bothPop != (highPop + lowPop) / 2, "Should not average priorities")
    }

    @Test
    fun `unmatched tags return zero popularity`() {
        val bench = mapOf("amenity" to "bench")
        val convenience = mapOf("shop" to "convenience")
        val randomTag = mapOf("foo" to "bar")

        assertEquals(0.0, OSMPopularityCalculator.calculatePopularity(bench))
        assertEquals(0.0, OSMPopularityCalculator.calculatePopularity(convenience))
        assertEquals(0.0, OSMPopularityCalculator.calculatePopularity(randomTag))
    }

    @Test
    fun `empty tags return zero popularity`() {
        assertEquals(0.0, OSMPopularityCalculator.calculatePopularity(emptyMap()))
    }

    @Test
    fun `hasFilter requires exact key and value match`() {
        // Test that it's not just checking keys or doing partial matches
        assertTrue(OSMPopularityCalculator.hasFilter("amenity", "hospital"))
        assertFalse(OSMPopularityCalculator.hasFilter("amenity", "bench"))
        assertFalse(OSMPopularityCalculator.hasFilter("amenity", "hospitals")) // Plural
        assertFalse(OSMPopularityCalculator.hasFilter("building", "hospital")) // Wrong key
    }

    @Test
    fun `getFilterKeys returns non-empty set`() {
        val keys = OSMPopularityCalculator.getFilterKeys()

        assertTrue(keys.isNotEmpty(), "Should have configured filter keys")
        assertTrue(keys.contains("amenity"), "Should include common OSM keys")
        assertTrue(keys.contains("tourism"), "Should include common OSM keys")
    }

    @Test
    fun `different POI types have different priorities`() {
        // Test that the system differentiates between POI types
        val hospital = mapOf("amenity" to "hospital")
        val hotel = mapOf("tourism" to "hotel")
        val cinema = mapOf("amenity" to "cinema")

        val hospitalPop = OSMPopularityCalculator.calculatePopularity(hospital)
        val hotelPop = OSMPopularityCalculator.calculatePopularity(hotel)
        val cinemaPop = OSMPopularityCalculator.calculatePopularity(cinema)

        // Verify they're all different and all positive
        assertTrue(hospitalPop > 0)
        assertTrue(hotelPop > 0)
        assertTrue(cinemaPop > 0)
        assertTrue(hospitalPop != hotelPop)
        assertTrue(hotelPop != cinemaPop)
        assertTrue(hospitalPop != cinemaPop)
    }
}
