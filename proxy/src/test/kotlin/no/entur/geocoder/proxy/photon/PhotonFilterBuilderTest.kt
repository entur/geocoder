package no.entur.geocoder.proxy.photon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotonFilterBuilderTest {
    @Test
    fun `buildIncludes creates filters for real Norwegian search scenarios`() {
        data class FilterScenario(
            val name: String,
            val country: String?,
            val countyIds: List<String>,
            val localityIds: List<String>,
            val tariffZones: List<String>,
            val authorities: List<String>,
            val sources: List<String>,
            val layers: List<String>,
            val categories: List<String>,
            val expectedIncludes: List<String>,
        )

        val scenarios =
            listOf(
                FilterScenario(
                    "Empty filters", null, emptyList(), emptyList(), emptyList(), emptyList(),
                    emptyList(), emptyList(), emptyList(), emptyList(),
                ),
                FilterScenario(
                    "Oslo only", "NOR", listOf("03"), emptyList(), emptyList(), emptyList(),
                    emptyList(), emptyList(), emptyList(),
                    listOf("country.NOR", "county_gid.03"),
                ),
                FilterScenario(
                    "Bergen addresses", "NOR", listOf("46"), listOf("4601"), emptyList(), emptyList(),
                    listOf("kartverket"), listOf("address"), emptyList(),
                    listOf("country.NOR", "county_gid.46", "locality_gid.4601", "legacy.source.kartverket", "legacy.layer.address"),
                ),
                FilterScenario(
                    "Trondheim transit", "NOR", listOf("50"), emptyList(),
                    listOf("ATB:TariffZone:A", "ATB:TariffZone:B"), listOf("ATB"),
                    listOf("osm"), listOf("venue"), listOf("transport"),
                    listOf(
                        "country.NOR", "county_gid.50", "tariff_zone_id.ATB:TariffZone:A", "tariff_zone_id.ATB:TariffZone:B",
                        "tariff_zone_authority.ATB", "legacy.source.osm", "legacy.layer.venue", "legacy.category.transport",
                    ),
                ),
                FilterScenario(
                    "NO_FILTER bypass", null, emptyList(), emptyList(), emptyList(), emptyList(),
                    emptyList(), emptyList(), listOf("transport", "NO_FILTER"), emptyList(),
                ),
            )

        scenarios.forEach { scenario ->
            val includes =
                PhotonFilterBuilder.buildIncludes(
                    boundaryCountry = scenario.country,
                    boundaryCountyIds = scenario.countyIds,
                    boundaryLocalityIds = scenario.localityIds,
                    tariffZones = scenario.tariffZones,
                    tariffZoneAuthorities = scenario.authorities,
                    sources = scenario.sources,
                    layers = scenario.layers,
                    categories = scenario.categories,
                )

            assertEquals(scenario.expectedIncludes.size, includes.size, "Failed for scenario: ${scenario.name}")
            scenario.expectedIncludes.forEach { expected ->
                assertTrue(includes.contains(expected), "Missing '$expected' in scenario: ${scenario.name}")
            }
        }
    }

    @Test
    fun `buildMultiModalExclude handles different modes`() {
        data class MultiModalTest(val mode: String, val expected: String?)

        val testCases =
            listOf(
                MultiModalTest("parent", "multimodal.child"),
                MultiModalTest("child", "multimodal.parent"),
                MultiModalTest("all", null),
                MultiModalTest("unknown", null),
                MultiModalTest("", null),
            )

        testCases.forEach { test ->
            assertEquals(test.expected, PhotonFilterBuilder.buildMultiModalExclude(test.mode))
        }
    }
}
