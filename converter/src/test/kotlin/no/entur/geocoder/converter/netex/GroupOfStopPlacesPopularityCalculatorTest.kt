package no.entur.geocoder.converter.netex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupOfStopPlacesPopularityCalculatorTest {

    @Test
    fun `single member group has boosted popularity`() {
        // Group with one member stop that has popularity 60 (bus station)
        val memberPopularities = listOf(60L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 (gosBoostFactor) * 60 = 600
        assertEquals(600.0, popularity, "Single member group: 10 × 60")
    }

    @Test
    fun `two member group multiplies popularities`() {
        // Group with two members: rail station (60) and metro station (60)
        val memberPopularities = listOf(60L, 60L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * (60 * 60) = 10 * 3,600 = 36,000
        assertEquals(36000.0, popularity, "Two member group: 10 × (60 × 60)")
    }

    @Test
    fun `three member group uses product of all members`() {
        // Group with three members: each with popularity 60
        val memberPopularities = listOf(60L, 60L, 60L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * (60 * 60 * 60) = 10 * 216,000 = 2,160,000
        assertEquals(2160000.0, popularity, "Three member group: 10 × (60³)")
    }

    @Test
    fun `group with varying member popularities multiplies all`() {
        // Mixed members: basic stop (30), bus station (60), rail with interchange (180)
        val memberPopularities = listOf(30L, 60L, 180L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * (30 * 60 * 180) = 10 * 324,000 = 3,240,000
        assertEquals(3240000.0, popularity, "Varying members: 10 × (30 × 60 × 180)")
    }

    @Test
    fun `empty group returns boost factor`() {
        val memberPopularities = emptyList<Long>()
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Empty list should result in just the boost factor
        assertEquals(10.0, popularity, "Empty group should return gosBoostFactor (10)")
    }

    @Test
    fun `single member with low popularity gets boosted`() {
        // Single basic stop (popularity 30)
        val memberPopularities = listOf(30L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * 30 = 300
        assertEquals(300.0, popularity, "Single basic stop: 10 × 30")
    }

    @Test
    fun `multiplication causes exponential growth with more members`() {
        val twoMembers = GroupOfStopPlacesPopularityCalculator.calculatePopularity(listOf(60L, 60L))
        val threeMembers = GroupOfStopPlacesPopularityCalculator.calculatePopularity(listOf(60L, 60L, 60L))
        val fourMembers = GroupOfStopPlacesPopularityCalculator.calculatePopularity(listOf(60L, 60L, 60L, 60L))

        // Verify strictly increasing with exponential growth
        assertTrue(twoMembers < threeMembers, "Adding third member should significantly increase popularity")
        assertTrue(threeMembers < fourMembers, "Adding fourth member should further increase popularity")

        // Verify exponential: each addition multiplies by 60
        assertEquals(twoMembers * 60, threeMembers, 0.1, "Should grow exponentially")
        assertEquals(threeMembers * 60, fourMembers, 0.1, "Should grow exponentially")
    }

    @Test
    fun `group with very low popularity members still gets boost`() {
        // Two very basic stops: popularity 20 each (minimum from addresses)
        val memberPopularities = listOf(20L, 20L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * (20 * 20) = 10 * 400 = 4,000
        assertEquals(4000.0, popularity, "Low popularity members: 10 × (20 × 20)")
    }

    @Test
    fun `kakka formula verification - realistic scenario`() {
        // Realistic Oslo S scenario:
        // - Main rail station with preferred interchange: 30 * 2 * 10 = 600
        // - Metro station: 30 * 2 = 60
        // - Bus station: 30 * 2 = 60
        val memberPopularities = listOf(600L, 60L, 60L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * (600 * 60 * 60) = 10 * 2,160,000 = 21,600,000
        assertEquals(21600000.0, popularity, "Oslo S scenario: 10 × (600 × 60 × 60)")
    }

    @Test
    fun `boost factor of 10 is applied correctly`() {
        // Verify gosBoostFactor = 10 from production config
        val memberPopularities = listOf(100L)
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * 100 = 1000
        assertEquals(1000.0, popularity, "Boost factor of 10 should be applied")
    }

    @Test
    fun `popularity ordering preserved for increasing member count`() {
        val groups = listOf(
            listOf(60L),           // 1 member
            listOf(60L, 60L),      // 2 members
            listOf(60L, 60L, 60L), // 3 members
        )

        val popularities = groups.map { GroupOfStopPlacesPopularityCalculator.calculatePopularity(it) }

        // Verify strictly increasing
        for (i in 0 until popularities.size - 1) {
            assertTrue(popularities[i] < popularities[i + 1],
                "Popularity should increase with member count: ${popularities[i]} >= ${popularities[i + 1]}")
        }
    }

    @Test
    fun `large product handled with Double to avoid overflow`() {
        // 5 members with high popularity would overflow Long
        val memberPopularities = List(5) { 180L }
        val popularity = GroupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)

        // Expected: popularity = 10 * (180^5) = 10 * 1,889,568,000,000 = 18,895,680,000,000
        // This exceeds Long.MAX_VALUE (9,223,372,036,854,775,807) but Double handles it
        assertEquals(1.889568e12, popularity, 1e9, "Should handle large products without overflow")
    }
}
