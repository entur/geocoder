package no.entur.geocoder.proxy.pelias

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PeliasAutocompleteParamsTest {

    @Test
    fun `FocusParams validates coordinate boundaries`() {
        data class CoordinateTest(val lat: String, val lon: String, val shouldFail: Boolean)

        val testCases = listOf(
            CoordinateTest("91.0", "10.757933", true),
            CoordinateTest("-91.0", "10.757933", true),
            CoordinateTest("59.911491", "181.0", true),
            CoordinateTest("59.911491", "-181.0", true),
            CoordinateTest("90.0", "180.0", false),
            CoordinateTest("-90.0", "-180.0", false),
            CoordinateTest("59.911491", "10.757933", false),
            CoordinateTest("69.649208", "18.955324", false),
            CoordinateTest("58.969976", "5.733107", false)
        )

        testCases.forEach { test ->
            if (test.shouldFail) {
                assertFailsWith<IllegalArgumentException> {
                    PeliasAutocompleteParams.FocusParams(
                        lat = BigDecimal(test.lat),
                        lon = BigDecimal(test.lon),
                        scale = null,
                        weight = null
                    )
                }
            } else {
                val focus = PeliasAutocompleteParams.FocusParams(
                    lat = BigDecimal(test.lat),
                    lon = BigDecimal(test.lon),
                    scale = null,
                    weight = null
                )
                assertEquals(BigDecimal(test.lat), focus.lat)
                assertEquals(BigDecimal(test.lon), focus.lon)
            }
        }
    }

    @Test
    fun `FocusParams validates scale and weight are positive`() {
        data class ParamTest(val scale: Int?, val weight: Double?, val shouldFail: Boolean)

        val testCases = listOf(
            ParamTest(0, null, true),
            ParamTest(-5, null, true),
            ParamTest(null, 0.0, true),
            ParamTest(null, -1.5, true),
            ParamTest(50, 2.5, false),
            ParamTest(100, 15.0, false),
            ParamTest(1, 0.1, false)
        )

        testCases.forEach { test ->
            if (test.shouldFail) {
                assertFailsWith<IllegalArgumentException> {
                    PeliasAutocompleteParams.FocusParams(
                        lat = BigDecimal("59.911491"),
                        lon = BigDecimal("10.757933"),
                        scale = test.scale,
                        weight = test.weight
                    )
                }
            } else {
                val focus = PeliasAutocompleteParams.FocusParams(
                    lat = BigDecimal("59.911491"),
                    lon = BigDecimal("10.757933"),
                    scale = test.scale,
                    weight = test.weight
                )
                assertEquals(test.scale, focus.scale)
                assertEquals(test.weight, focus.weight)
            }
        }
    }

    @Test
    fun `FocusParams from parses various input formats`() {
        data class ParseTest(
            val lat: String, val lon: String, val scale: String?, val weight: String?,
            val expectedLat: String, val expectedLon: String, val expectedScale: Int?, val expectedWeight: Double?,
            val shouldFail: Boolean = false
        )

        val testCases = listOf(
            ParseTest("59.911491", "10.757933", null, null, "59.911491", "10.757933", null, null),
            ParseTest("69.649208", "18.955324", "50km", null, "69.649208", "18.955324", 50, null),
            ParseTest("58.969976", "5.733107", "100", "2.5", "58.969976", "5.733107", 100, 2.5),
            ParseTest("63.430515", "10.395053", "25km", "15.0", "63.430515", "10.395053", 25, 15.0),
            ParseTest("invalid", "10.0", null, null, "", "", null, null, true),
            ParseTest("60.0", "invalid", null, null, "", "", null, null, true),
            ParseTest("60.0", "10.0", "invalid", null, "", "", null, null, true),
            ParseTest("60.0", "10.0", null, "invalid", "", "", null, null, true)
        )

        testCases.forEach { test ->
            if (test.shouldFail) {
                assertFailsWith<IllegalArgumentException> {
                    PeliasAutocompleteParams.FocusParams.from(test.lat, test.lon, test.scale, test.weight)
                }
            } else {
                val focus = PeliasAutocompleteParams.FocusParams.from(test.lat, test.lon, test.scale, test.weight)
                assertEquals(BigDecimal(test.expectedLat), focus.lat)
                assertEquals(BigDecimal(test.expectedLon), focus.lon)
                assertEquals(test.expectedScale, focus.scale)
                assertEquals(test.expectedWeight, focus.weight)
            }
        }
    }

    @Test
    fun `PeliasAutocompleteParams constructs with real-world Norwegian search scenarios`() {
        data class SearchScenario(
            val text: String, val country: String?, val countyIds: List<String>,
            val sources: List<String>, val multiModal: String
        )

        val scenarios = listOf(
            SearchScenario("Oslo S", "NOR", listOf("03"), listOf("osm", "kartverket"), "parent"),
            SearchScenario("Bergen stasjon", "NOR", listOf("46"), listOf("osm"), "child"),
            SearchScenario("Trondheim", "NOR", listOf("50"), emptyList(), "all"),
            SearchScenario("Jernbanetorget 1", "NOR", listOf("03"), listOf("kartverket"), "parent")
        )

        scenarios.forEach { scenario ->
            val params = PeliasAutocompleteParams(
                text = scenario.text,
                boundaryCountry = scenario.country,
                boundaryCountyIds = scenario.countyIds,
                sources = scenario.sources,
                multiModal = scenario.multiModal
            )

            assertEquals(scenario.text, params.text)
            assertEquals(scenario.country, params.boundaryCountry)
            assertEquals(scenario.countyIds, params.boundaryCountyIds)
            assertEquals(scenario.sources, params.sources)
            assertEquals(scenario.multiModal, params.multiModal)
        }
    }
}

