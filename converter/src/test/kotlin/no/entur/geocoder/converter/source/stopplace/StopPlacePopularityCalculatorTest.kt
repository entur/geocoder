package no.entur.geocoder.converter.source.stopplace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StopPlacePopularityCalculatorTest {
    @Test
    fun `basic stop place returns expected popularity`() {
        // Basic stop with default value (30)
        val stopPlace = createStopPlace(stopPlaceType = null)
        val popularity = StopPlacePopularityCalculator.calculatePopularity(stopPlace)

        assertEquals(30L, popularity, "Basic stop should have popularity 30")
    }

    @Test
    fun `bus station has higher popularity than basic stop`() {
        val basicStop = createStopPlace(stopPlaceType = "onstreetBus")
        val busStation = createStopPlace(stopPlaceType = "busStation")

        val basicPopularity = StopPlacePopularityCalculator.calculatePopularity(basicStop)
        val busStationPopularity = StopPlacePopularityCalculator.calculatePopularity(busStation)

        assertTrue(
            busStationPopularity > basicPopularity,
            "Bus station should have higher popularity than basic stop",
        )
    }

    @Test
    fun `metro station has boosted popularity`() {
        val metroStation = createStopPlace(stopPlaceType = "metroStation")
        val popularity = StopPlacePopularityCalculator.calculatePopularity(metroStation)

        // Expected: popularity = 30 * 2 = 60
        assertEquals(60L, popularity, "Metro station should have popularity 60")
    }

    @Test
    fun `rail station has boosted popularity`() {
        val railStation = createStopPlace(stopPlaceType = "railStation")
        val popularity = StopPlacePopularityCalculator.calculatePopularity(railStation)

        // Expected: popularity = 30 * 2 = 60
        assertEquals(60L, popularity, "Rail station should have popularity 60")
    }

    @Test
    fun `recommended interchange multiplies popularity`() {
        val stopWithInterchange =
            createStopPlace(
                stopPlaceType = "railStation",
                weighting = "recommendedInterchange",
            )
        val popularity = StopPlacePopularityCalculator.calculatePopularity(stopWithInterchange)

        // Expected: popularity = 30 * 2 (rail) * 3 (interchange) = 180
        assertEquals(
            180L, popularity,
            "Rail station with recommended interchange should have popularity 180",
        )
    }

    @Test
    fun `preferred interchange gives high popularity`() {
        val stopWithInterchange =
            createStopPlace(
                stopPlaceType = "railStation",
                weighting = "preferredInterchange",
            )
        val popularity = StopPlacePopularityCalculator.calculatePopularity(stopWithInterchange)

        // Expected: popularity = 30 * 2 (rail) * 10 (interchange) = 600
        assertEquals(
            600L, popularity,
            "Rail station with preferred interchange should have popularity 600",
        )
    }

    @Test
    fun `popularity values are strictly ordered`() {
        val stops =
            listOf(
                createStopPlace(stopPlaceType = null), // 30
                createStopPlace(stopPlaceType = "busStation"), // 60
                createStopPlace(stopPlaceType = "railStation", weighting = "recommendedInterchange"), // 180
                createStopPlace(stopPlaceType = "railStation", weighting = "preferredInterchange"), // 600
            )

        val popularities = stops.map { StopPlacePopularityCalculator.calculatePopularity(it) }

        // Verify strictly increasing
        for (i in 0 until popularities.size - 1) {
            assertTrue(
                popularities[i] < popularities[i + 1],
                "Popularity should increase: ${popularities[i]} >= ${popularities[i + 1]}",
            )
        }
    }

    // ===== Multimodal Parent Station Tests =====
    // Parent stops NEVER have their own stopPlaceType - popularity derived from children
    // Formula: popularity = defaultValue * (SUM of child type factors) * interchange factor

    @Test
    fun `multimodal parent uses sum of child types`() {
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation")

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2) = 120
        assertEquals(120L, popularity, "Multimodal parent (rail+metro) should have popularity 120")
    }

    @Test
    fun `multimodal parent with three child types`() {
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation", "busStation")

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2 + 2) = 180
        assertEquals(180L, popularity, "Multimodal parent (rail+metro+bus) should sum all child factors")
    }

    @Test
    fun `multimodal parent sums factors not multiplies them`() {
        // rail + metro + bus = 30 * (2 + 2 + 2) = 180, not 30 * 2 * 2 * 2 = 240
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation", "busStation")

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        assertEquals(180L, popularity, "Should sum factors (2+2+2=6), not multiply (2*2*2=8)")
    }

    @Test
    fun `multimodal parent with unconfigured child types defaults to factor 1`() {
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("ferryStop", "tramStation") // Neither configured

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        // Expected: popularity = 30 * (1 + 1) = 60
        assertEquals(60L, popularity, "Unconfigured stop types should default to factor 1.0")
    }

    @Test
    fun `multimodal parent with interchange applies to total`() {
        val parentStop =
            createStopPlace(
                stopPlaceType = null,
                weighting = "preferredInterchange",
            )
        val childTypes = listOf("railStation", "metroStation")

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2) * 10 = 1200
        assertEquals(
            1200L, popularity,
            "Interchange factor should apply after summing stop type factors",
        )
    }

    @Test
    fun `duplicate child types are summed not deduplicated`() {
        // Parent with THREE rail station children (same type)
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "railStation", "railStation")

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2 + 2) = 180
        // NOT: 30 * 2 = 60 (if deduplicated)
        assertEquals(180L, popularity, "Duplicate types should be summed, not deduplicated (3 × 2 = 6)")
    }

    @Test
    fun `many children of same type produce high popularity`() {
        // Parent with 5 bus station children
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = List(5) { "busStation" } // 5 identical entries

        val popularity = StopPlacePopularityCalculator.calculatePopularity(parentStop, childTypes)

        // Expected: popularity = 30 * (2+2+2+2+2) = 300
        assertEquals(300L, popularity, "5 bus stations should contribute 5 × 2 = 10 to factor")
    }

    // Helper function to create test StopPlace instances
    private fun createStopPlace(
        id: String = "NSR:StopPlace:1",
        stopPlaceType: String? = null,
        weighting: String? = null,
        transportMode: String? = null,
    ): StopPlace =
        StopPlace(
            id = id,
            version = "1",
            modification = null,
            created = null,
            changed = null,
            name =
                StopPlace.LocalizedText().apply {
                    lang = "no"
                    text = "Test Stop"
                },
            centroid =
                StopPlace.Centroid(
                    location =
                        StopPlace.Location(
                            longitude = 10.746.toBigDecimal(),
                            latitude = 59.911.toBigDecimal(),
                        ),
                ),
            stopPlaceType = stopPlaceType,
            weighting = weighting,
            transportMode = transportMode,
        )
}
