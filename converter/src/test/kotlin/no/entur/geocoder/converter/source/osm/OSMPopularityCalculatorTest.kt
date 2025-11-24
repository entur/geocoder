package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.converter.ConverterConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OSMPopularityCalculatorTest {
    private val calculator = OSMPopularityCalculator(ConverterConfig().osm)

    @Test
    fun `calculates popularity as base times priority`() {
        // Test that the formula is: popularity = DEFAULT_VALUE × priority
        val hospitalTags = mapOf("amenity" to "hospital") // Priority 9
        val cinemaTags = mapOf("amenity" to "cinema") // Priority 1

        val hospitalPop = calculator.calculatePopularity(hospitalTags)
        val cinemaPop = calculator.calculatePopularity(cinemaTags)

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

        val highPop = calculator.calculatePopularity(highOnly)
        val lowPop = calculator.calculatePopularity(lowOnly)
        val bothPop = calculator.calculatePopularity(both)

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

        assertEquals(0.0, calculator.calculatePopularity(bench))
        assertEquals(0.0, calculator.calculatePopularity(convenience))
        assertEquals(0.0, calculator.calculatePopularity(randomTag))
    }

    @Test
    fun `empty tags return zero popularity`() {
        assertEquals(0.0, calculator.calculatePopularity(emptyMap()))
    }

    @Test
    fun `hasFilter requires exact key and value match`() {
        // Test that it's not just checking keys or doing partial matches
        assertTrue(calculator.hasFilter("amenity", "hospital"))
        assertFalse(calculator.hasFilter("amenity", "bench"))
        assertFalse(calculator.hasFilter("amenity", "hospitals")) // Plural
        assertFalse(calculator.hasFilter("building", "hospital")) // Wrong key
    }

    @Test
    fun `getFilterKeys returns non-empty set`() {
        val keys = calculator.getFilterKeys()

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

        val hospitalPop = calculator.calculatePopularity(hospital)
        val hotelPop = calculator.calculatePopularity(hotel)
        val cinemaPop = calculator.calculatePopularity(cinema)

        // Verify they're all different and all positive
        assertTrue(hospitalPop > 0)
        assertTrue(hotelPop > 0)
        assertTrue(cinemaPop > 0)
        assertTrue(hospitalPop != hotelPop)
        assertTrue(hotelPop != cinemaPop)
        assertTrue(hospitalPop != cinemaPop)
    }
}
