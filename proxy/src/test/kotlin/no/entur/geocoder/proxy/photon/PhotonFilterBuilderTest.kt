package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotonFilterBuilderTest {
    @Test
    fun `buildIncludes creates filters for real Norwegian search scenarios`() {
        val scenarios =
            listOf(
                PeliasAutocompleteRequest(
                    text = "Empty filters",
                    boundaryCountry = null,
                    boundaryCountyIds = emptyList(),
                    boundaryLocalityIds = emptyList(),
                    tariffZones = emptyList(),
                    tariffZoneAuthorities = emptyList(),
                    sources = emptyList(),
                    layers = emptyList(),
                    categories = emptyList(),
                    multiModal = "all",
                ) to emptyList(),
                PeliasAutocompleteRequest(
                    text = "Oslo only",
                    boundaryCountry = "NOR",
                    boundaryCountyIds = listOf("03"),
                    boundaryLocalityIds = emptyList(),
                    tariffZones = emptyList(),
                    tariffZoneAuthorities = emptyList(),
                    sources = emptyList(),
                    layers = emptyList(),
                    categories = emptyList(),
                    multiModal = "all",
                ) to listOf("country.no", "county_gid.03"),
                PeliasAutocompleteRequest(
                    text = "Bergen addresses",
                    boundaryCountry = "NOR",
                    boundaryCountyIds = listOf("46"),
                    boundaryLocalityIds = listOf("4601"),
                    tariffZones = emptyList(),
                    tariffZoneAuthorities = emptyList(),
                    sources = listOf("kartverket"),
                    layers = listOf("address"),
                    categories = emptyList(),
                    multiModal = "all",
                ) to listOf("country.no", "county_gid.46", "locality_gid.4601", "legacy.source.kartverket", "legacy.layer.address"),
                PeliasAutocompleteRequest(
                    text = "Trondheim transit",
                    boundaryCountry = "NOR",
                    boundaryCountyIds = listOf("50"),
                    boundaryLocalityIds = emptyList(),
                    tariffZones = listOf("ATB:TariffZone:A", "ATB:TariffZone:B"),
                    tariffZoneAuthorities = listOf("ATB"),
                    sources = listOf("osm"),
                    layers = listOf("venue"),
                    categories = listOf("transport"),
                    multiModal = "all",
                )
                    to
                    listOf(
                        "country.no", "county_gid.50", "tariff_zone_id.ATB:TariffZone:A", "tariff_zone_id.ATB:TariffZone:B",
                        "tariff_zone_authority.ATB", "legacy.source.osm", "legacy.layer.venue", "legacy.category.transport",
                    ),
                PeliasAutocompleteRequest(
                    text = "NO_FILTER bypass",
                    boundaryCountry = null,
                    boundaryCountyIds = emptyList(),
                    boundaryLocalityIds = emptyList(),
                    tariffZones = emptyList(),
                    tariffZoneAuthorities = emptyList(),
                    sources = emptyList(),
                    layers = emptyList(),
                    categories = listOf("transport", "NO_FILTER"),
                    multiModal = "all",
                ) to emptyList(),
            )

        scenarios.forEach { scenario ->
            val includes =
                PhotonFilterBuilder.buildIncludes(scenario.first)
            assertEquals(scenario.second.size, includes.size, "Failed for scenario: ${scenario.first.text}")
            scenario.second.forEach { expected ->
                assertTrue(includes.contains(expected), "Missing '$expected' in scenario: ${scenario.first.text}")
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
