package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasReverseRequest
import no.entur.geocoder.proxy.v3.V3ReverseRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
                ) to listOf("country.no", "county_gid.KVE.TopographicPlace.03"),
                PeliasAutocompleteRequest(
                    text = "Bergen addresses",
                    boundaryCountry = "NOR",
                    boundaryCountyIds = listOf("46"),
                    boundaryLocalityIds = listOf("4601"),
                    tariffZones = emptyList(),
                    tariffZoneAuthorities = emptyList(),
                    sources = listOf("openaddresses"),
                    layers = listOf("address"),
                    categories = emptyList(),
                    multiModal = "all",
                ) to
                    listOf(
                        "country.no",
                        "county_gid.KVE.TopographicPlace.46",
                        "locality_gid.KVE.TopographicPlace.4601",
                        "legacy.source.openaddresses",
                        "legacy.layer.address",
                    ),
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
                        "country.no", "county_gid.KVE.TopographicPlace.50",
                        "tariff_zone_id.ATB.TariffZone.A,tariff_zone_id.ATB.TariffZone.B",
                        "tariff_zone_authority.ATB", "legacy.source.osm", "layer.stopPlace", "legacy.category.transport",
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
            val includes = PhotonFilterBuilder.buildIncludes(scenario.first)
            assertEquals(scenario.second.size, includes.size, "Failed for scenario: ${scenario.first.text}")
            scenario.second.forEach { expected ->
                assertTrue(includes.contains(expected), "Missing '$expected' in scenario: ${scenario.first.text}")
            }
        }
    }

    @Test
    fun `buildMultimodalExclude handles different modes`() {
        data class MultimodalTest(val mode: String, val expected: String?)

        val testCases =
            listOf(
                MultimodalTest("parent", "multimodal.child"),
                MultimodalTest("child", "multimodal.parent"),
                MultimodalTest("all", null),
                MultimodalTest("unknown", null),
                MultimodalTest("", null),
            )

        testCases.forEach { test ->
            assertEquals(test.expected, PhotonFilterBuilder.buildMultimodalExclude(test.mode))
        }
    }

    @Test
    fun `v2 tariffZones routes refs to tariff_zone_id or fare_zone_id by ref shape`() {
        val req =
            PeliasAutocompleteRequest(
                text = "mixed",
                tariffZones = listOf("RUT:TariffZone:1", "RUT:FareZone:4", "ATB:TariffZone:A"),
            )
        val includes = PhotonFilterBuilder.buildIncludes(req)
        // All three refs share one comma-separated include group so they OR within v2's tariffZones filter.
        assertTrue(
            includes.contains(
                "tariff_zone_id.RUT.TariffZone.1,fare_zone_id.RUT.FareZone.4,tariff_zone_id.ATB.TariffZone.A",
            ),
            "Got: $includes",
        )
    }

    @Test
    fun `boundary county_ids and locality_ids are converted to Photon filters`() {
        val autocomplete =
            PeliasAutocompleteRequest(
                text = "test",
                boundaryCountyIds = listOf("KVE:TopographicPlace:03", "KVE:TopographicPlace:18"),
                boundaryLocalityIds = listOf("KVE:TopographicPlace:4601", "KVE:TopographicPlace:3001"),
            )
        val autocompleteIncludes = PhotonFilterBuilder.buildIncludes(autocomplete)
        assertTrue(autocompleteIncludes.contains("county_gid.KVE.TopographicPlace.03,county_gid.KVE.TopographicPlace.18"))
        assertTrue(autocompleteIncludes.contains("locality_gid.KVE.TopographicPlace.4601,locality_gid.KVE.TopographicPlace.3001"))

        val reverse =
            PeliasReverseRequest(
                lat = 60.0,
                lon = 10.0,
                boundaryCountyIds = listOf("KVE:TopographicPlace:40"),
                boundaryLocalityIds = listOf("KVE:TopographicPlace:4005"),
                multiModal = "parent",
            )
        val reverseIncludes = PhotonFilterBuilder.buildIncludes(reverse)
        assertTrue(reverseIncludes.contains("county_gid.KVE.TopographicPlace.40"))
        assertTrue(reverseIncludes.contains("locality_gid.KVE.TopographicPlace.4005"))
    }

    @Test
    fun `v3 stopPlaceTypes become a stop_place_type include group`() {
        val req =
            V3ReverseRequest(
                lat = 59.91,
                lon = 10.75,
                stopPlaceTypes = listOf("railStation", "airport"),
            )
        val includes = PhotonFilterBuilder.buildIncludes(req)
        // One comma-separated group so the types OR with each other.
        assertTrue(includes.contains("stop_place_type.railStation,stop_place_type.airport"), "Got: $includes")
    }

    @Test
    fun `v3 layers and stopPlaceTypes compose as a union, not an exclusion`() {
        val req =
            V3ReverseRequest(
                lat = 59.91,
                lon = 10.75,
                layers = listOf("stopPlace", "groupOfStopPlaces"),
                stopPlaceTypes = listOf("railStation"),
            )
        val includes = PhotonFilterBuilder.buildIncludes(req)
        // Two groups: the requested layers, AND (other layers + types). No standalone type group -
        // that was the old exclusion that dropped requested groups.
        assertTrue(includes.contains("layer.stopPlace,layer.groupOfStopPlaces"), "Got: $includes")
        assertTrue(includes.contains("layer.groupOfStopPlaces,stop_place_type.railStation"), "Got: $includes")
        assertFalse(includes.contains("stop_place_type.railStation"), "Got: $includes")
    }

    @Test
    fun `v3 union ORs multiple stopPlaceTypes within the type group`() {
        val req =
            V3ReverseRequest(
                lat = 59.91,
                lon = 10.75,
                layers = listOf("stopPlace", "groupOfStopPlaces"),
                stopPlaceTypes = listOf("railStation", "airport"),
            )
        val includes = PhotonFilterBuilder.buildIncludes(req)
        // Types share group 2 with the other layers, so they OR together.
        assertTrue(
            includes.contains("layer.groupOfStopPlaces,stop_place_type.railStation,stop_place_type.airport"),
            "Got: $includes",
        )
    }

    @Test
    fun `v3 stopPlaceTypes are ignored when layers is given without the stopPlace layer`() {
        val req =
            V3ReverseRequest(
                lat = 59.91,
                lon = 10.75,
                layers = listOf("groupOfStopPlaces"),
                stopPlaceTypes = listOf("railStation"),
            )
        val includes = PhotonFilterBuilder.buildIncludes(req)
        // stopPlace not requested, so types has nothing to constrain - just the layer filter.
        assertTrue(includes.contains("layer.groupOfStopPlaces"), "Got: $includes")
        assertFalse(includes.any { it.contains("stop_place_type.") }, "Got: $includes")
    }
}
