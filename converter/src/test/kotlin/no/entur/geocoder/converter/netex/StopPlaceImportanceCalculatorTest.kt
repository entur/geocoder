package no.entur.geocoder.converter.netex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopPlaceImportanceCalculatorTest {

    @Test
    fun `basic stop place returns expected importance`() {
        // Basic stop with default value (30)
        val stopPlace = createStopPlace(stopPlaceType = null)
        val importance = StopPlaceImportanceCalculator.calculateImportance(stopPlace)

        // Expected: ~0.116 based on log10 normalization of popularity=30
        assertEquals(0.116, importance, 0.01,
            "Basic stop (popularity 30) should have importance ~0.116")
    }

    @Test
    fun `bus station has higher importance than basic stop`() {
        val basicStop = createStopPlace(stopPlaceType = "onstreetBus")
        val busStation = createStopPlace(stopPlaceType = "busStation")

        val basicImportance = StopPlaceImportanceCalculator.calculateImportance(basicStop)
        val busStationImportance = StopPlaceImportanceCalculator.calculateImportance(busStation)

        assertTrue(busStationImportance > basicImportance,
            "Bus station should have higher importance than basic stop")
    }

    @Test
    fun `metro station has boosted importance`() {
        val metroStation = createStopPlace(stopPlaceType = "metroStation")
        val importance = StopPlaceImportanceCalculator.calculateImportance(metroStation)

        // Expected: popularity = 30 * 2 = 60, importance ~0.158
        assertEquals(0.158, importance, 0.01,
            "Metro station (popularity 60) should have importance ~0.158")
    }

    @Test
    fun `rail station has boosted importance`() {
        val railStation = createStopPlace(stopPlaceType = "railStation")
        val importance = StopPlaceImportanceCalculator.calculateImportance(railStation)

        // Expected: popularity = 30 * 2 = 60, importance ~0.158
        assertEquals(0.158, importance, 0.01,
            "Rail station (popularity 60) should have importance ~0.158")
    }

    @Test
    fun `recommended interchange multiplies importance`() {
        val stopWithInterchange = createStopPlace(
            stopPlaceType = "railStation",
            weighting = "recommendedInterchange"
        )
        val importance = StopPlaceImportanceCalculator.calculateImportance(stopWithInterchange)

        // Expected: popularity = 30 * 2 (rail) * 3 (interchange) = 180
        // Log10 normalized: ~0.214
        assertEquals(0.214, importance, 0.02,
            "Rail station with recommended interchange should have importance ~0.214")
    }

    @Test
    fun `preferred interchange gives high importance`() {
        val stopWithInterchange = createStopPlace(
            stopPlaceType = "railStation",
            weighting = "preferredInterchange"
        )
        val importance = StopPlaceImportanceCalculator.calculateImportance(stopWithInterchange)

        // Expected: popularity = 30 * 2 (rail) * 10 (interchange) = 600
        // Log10 normalized: ~0.266
        assertEquals(0.266, importance, 0.02,
            "Rail station with preferred interchange should have importance ~0.266")
    }

    @Test
    fun `importance values are strictly ordered by popularity`() {
        val stops = listOf(
            createStopPlace(stopPlaceType = null), // 30
            createStopPlace(stopPlaceType = "busStation"), // 60
            createStopPlace(stopPlaceType = "railStation", weighting = "recommendedInterchange"), // 180
            createStopPlace(stopPlaceType = "railStation", weighting = "preferredInterchange"), // 600
        )

        val importances = stops.map { StopPlaceImportanceCalculator.calculateImportance(it) }

        // Verify strictly increasing
        for (i in 0 until importances.size - 1) {
            assertTrue(importances[i] < importances[i + 1],
                "Importance should increase with popularity: ${importances[i]} >= ${importances[i + 1]}")
        }
    }

    @Test
    fun `all importance values are within valid range`() {
        val testCases = listOf(
            createStopPlace(stopPlaceType = null),
            createStopPlace(stopPlaceType = "busStation"),
            createStopPlace(stopPlaceType = "metroStation"),
            createStopPlace(stopPlaceType = "railStation"),
            createStopPlace(stopPlaceType = "railStation", weighting = "preferredInterchange"),
        )

        testCases.forEach { stop ->
            val importance = StopPlaceImportanceCalculator.calculateImportance(stop)
            assertTrue(importance >= 0.1, "Importance should be >= 0.1 (floor), got $importance")
            assertTrue(importance <= 1.0, "Importance should be <= 1.0, got $importance")
        }
    }

    @Test
    fun `importance never returns zero`() {
        val basicStop = createStopPlace(stopPlaceType = null)
        val importance = StopPlaceImportanceCalculator.calculateImportance(basicStop)

        assertTrue(importance >= 0.1,
            "Importance should never be below floor value (0.1)")
    }

    @Test
    fun `airport stop has appropriate importance`() {
        val airport = createStopPlace(stopPlaceType = "airport")
        val importance = StopPlaceImportanceCalculator.calculateImportance(airport)

        assertTrue(importance > 0.1,
            "Airport should have importance above minimum")
    }

    @Test
    fun `ferry stop has appropriate importance`() {
        val ferry = createStopPlace(stopPlaceType = "ferryStop")
        val importance = StopPlaceImportanceCalculator.calculateImportance(ferry)

        assertTrue(importance > 0.1,
            "Ferry stop should have importance above minimum")
    }

    @Test
    fun `tram station has appropriate importance`() {
        val tram = createStopPlace(stopPlaceType = "tramStation")
        val importance = StopPlaceImportanceCalculator.calculateImportance(tram)

        assertTrue(importance > 0.1,
            "Tram station should have importance above minimum")
    }

    @Test
    fun `different interchange levels produce different importance`() {
        val railStation = createStopPlace(stopPlaceType = "railStation")
        val recommendedInterchange = createStopPlace(
            stopPlaceType = "railStation",
            weighting = "recommendedInterchange"
        )
        val preferredInterchange = createStopPlace(
            stopPlaceType = "railStation",
            weighting = "preferredInterchange"
        )

        val imp1 = StopPlaceImportanceCalculator.calculateImportance(railStation)
        val imp2 = StopPlaceImportanceCalculator.calculateImportance(recommendedInterchange)
        val imp3 = StopPlaceImportanceCalculator.calculateImportance(preferredInterchange)

        assertTrue(imp1 < imp2, "Recommended interchange should boost importance")
        assertTrue(imp2 < imp3, "Preferred interchange should boost more than recommended")
    }

    // Helper function to create test StopPlace instances
    private fun createStopPlace(
        id: String = "NSR:StopPlace:1",
        stopPlaceType: String? = null,
        weighting: String? = null,
        transportMode: String? = null
    ): StopPlace {
        return StopPlace(
            id = id,
            version = "1",
            modification = null,
            created = null,
            changed = null,
            name = StopPlace.LocalizedText().apply {
                lang = "no"
                text = "Test Stop"
            },
            centroid = StopPlace.Centroid(
                location = StopPlace.Location(
                    longitude = 10.746.toBigDecimal(),
                    latitude = 59.911.toBigDecimal()
                )
            ),
            stopPlaceType = stopPlaceType,
            weighting = weighting,
            transportMode = transportMode
        )
    }
}
