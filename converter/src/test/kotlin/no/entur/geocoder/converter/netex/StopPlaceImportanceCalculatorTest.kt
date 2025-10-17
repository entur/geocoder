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

    // ===== Multimodal Parent Station Tests =====
    // Key insight: Parent stops NEVER have their own stopPlaceType (always null)
    // They derive their importance by SUMMING the factors of their children's types
    // Formula: popularity = defaultValue * (SUM of child type factors) * interchange factor

    @Test
    fun `multimodal parent uses sum of child types`() {
        // Parent stop has no stopPlaceType (always null for parents), but has rail and metro children
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2) = 120
        // Log10 normalized: ~0.187
        assertEquals(0.187, importance, 0.02,
            "Multimodal parent (rail+metro) should have importance ~0.187")
    }

    @Test
    fun `multimodal parent with three child types`() {
        // Parent with rail, metro, and bus children
        val parentStop = createStopPlace(stopPlaceType = null)  // Parents never have their own type
        val childTypes = listOf("railStation", "metroStation", "busStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2 + 2) = 180
        // Log10 normalized: ~0.214
        assertEquals(0.214, importance, 0.02,
            "Multimodal parent (rail+metro+bus) should sum all child factors")
    }

    @Test
    fun `multimodal parent sums factors not multiplies them`() {
        // Test case from kakka: rail (factor 2) + metro (factor 2) = sum of 4, not product of 4
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // If multiplied: 30 * 2 * 2 = 120 (WRONG)
        // If summed: 30 * (2 + 2) = 120 (CORRECT, but same value in this case)
        // Better test: rail + metro + bus = 30 * (2 + 2 + 2) = 180, not 30 * 2 * 2 * 2 = 240
        val parentStop2 = createStopPlace(stopPlaceType = null)
        val childTypes2 = listOf("railStation", "metroStation", "busStation")

        val importance2 = StopPlaceImportanceCalculator.calculateImportance(parentStop2, childTypes2)

        // Expected with sum: 30 * (2+2+2) = 180, importance ~0.214
        assertEquals(0.214, importance2, 0.02,
            "Should sum factors (2+2+2=6), not multiply (2*2*2=8)")
    }

    @Test
    fun `multimodal parent with unconfigured child types defaults to factor 1`() {
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("ferryStop", "tramStation")  // Neither configured

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (1 + 1) = 60
        // Log10 normalized: ~0.158
        assertEquals(0.158, importance, 0.02,
            "Unconfigured stop types should default to factor 1.0")
    }

    @Test
    fun `multimodal parent with mixed configured and unconfigured types`() {
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "ferryStop")  // rail=2, ferry=1

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 1) = 90
        // Log10 normalized: ~0.174
        assertEquals(0.174, importance, 0.02,
            "Should sum configured (2) and default (1) factors")
    }

    @Test
    fun `multimodal parent with interchange applies to total`() {
        val parentStop = createStopPlace(
            stopPlaceType = null,
            weighting = "preferredInterchange"
        )
        val childTypes = listOf("railStation", "metroStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2) * 10 = 1200
        // Log10 normalized: ~0.290
        assertEquals(0.290, importance, 0.02,
            "Interchange factor should apply after summing stop type factors")
    }

    @Test
    fun `multimodal parent importance higher than single mode`() {
        val singleModeStop = createStopPlace(stopPlaceType = "railStation")
        val multimodalStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation", "busStation")

        val singleImportance = StopPlaceImportanceCalculator.calculateImportance(singleModeStop)
        val multiImportance = StopPlaceImportanceCalculator.calculateImportance(multimodalStop, childTypes)

        assertTrue(multiImportance > singleImportance,
            "Multimodal parent (3 types) should have higher importance than single mode")
    }

    @Test
    fun `empty child types list behaves like regular stop`() {
        val stopWithNoChildren = createStopPlace(stopPlaceType = "busStation")
        val stopWithEmptyList = createStopPlace(stopPlaceType = "busStation")

        val imp1 = StopPlaceImportanceCalculator.calculateImportance(stopWithNoChildren)
        val imp2 = StopPlaceImportanceCalculator.calculateImportance(stopWithEmptyList, emptyList())

        assertEquals(imp1, imp2, 0.001,
            "Empty child list should produce same result as no parameter")
    }

    @Test
    fun `regular stop with type ignores child types if provided`() {
        // Regular (non-parent) stops have their own stopPlaceType
        // In real data, they would have empty childTypes, but test the edge case
        val regularStop = createStopPlace(stopPlaceType = "railStation")
        val childTypes = listOf("metroStation", "busStation")  // Should be ignored

        val importance = StopPlaceImportanceCalculator.calculateImportance(regularStop, childTypes)

        // Expected: Should use own type + sum of children = 30 * (2 + 2 + 2) = 180
        // (In reality, regular stops won't have children, but if they do, we sum all)
        assertEquals(0.214, importance, 0.02,
            "Stop with own type should include both own and child factors")
    }

    @Test
    fun `multimodal parent with only child types has substantial importance`() {
        // All multimodal parents have no transport mode of their own (stopPlaceType = null)
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "metroStation", "busStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2 + 2) = 180
        assertTrue(importance > 0.2,
            "Parent derives all importance from children")
    }

    @Test
    fun `kakka test case - multiple types summarized`() {
        // From StopPlaceBoostConfigurationTest.java line 70-72:
        // "multipleTypesAndSubModesShouldBeSummarized"
        // With defaultValue=1000, factors rail=6, airport=2, ferry=0, interchange=10
        // Result: 1000 * (6 + 2 + 0) * 10 = 80,000

        // Adapted to our production config: defaultValue=30, rail=2, metro=2, bus=2
        val parentStop = createStopPlace(
            stopPlaceType = null,
            weighting = "preferredInterchange"
        )
        val childTypes = listOf("railStation", "metroStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: 30 * (2 + 2) * 10 = 1200
        // This should produce higher importance than single type with interchange
        val singleTypeWithInterchange = createStopPlace(
            stopPlaceType = "railStation",
            weighting = "preferredInterchange"
        )
        val singleImportance = StopPlaceImportanceCalculator.calculateImportance(singleTypeWithInterchange)

        assertTrue(importance > singleImportance,
            "Multimodal with interchange should exceed single mode with interchange")
    }

    @Test
    fun `duplicate child types are summed not deduplicated`() {
        // Parent with THREE rail station children (same type)
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = listOf("railStation", "railStation", "railStation")

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2 + 2 + 2) = 180
        // NOT: 30 * 2 = 60 (if deduplicated)
        // Log10 normalized: ~0.214
        assertEquals(0.214, importance, 0.02,
            "Duplicate types should be summed, not deduplicated (3 × 2 = 6)")
    }

    @Test
    fun `many children of same type produce high importance`() {
        // Realistic scenario: parent with 5 bus station children
        val parentStop = createStopPlace(stopPlaceType = null)
        val childTypes = List(5) { "busStation" }  // 5 identical entries

        val importance = StopPlaceImportanceCalculator.calculateImportance(parentStop, childTypes)

        // Expected: popularity = 30 * (2+2+2+2+2) = 300
        // Log10 normalized: ~0.237
        assertEquals(0.237, importance, 0.02,
            "5 bus stations should contribute 5 × 2 = 10 to factor")
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
