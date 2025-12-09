package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasPlaceRequest
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest.Companion.RESULT_PRUNING_HEADROOM
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.*

class PhotonAutocompleteRequestTest {
    @Test
    fun `from PeliasAutocompleteParams creates request with basic req`() {
        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                size = 20,
                lang = "en",
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertEquals("Oslo", request.query)
        assertEquals(20 + RESULT_PRUNING_HEADROOM, request.limit)
        assertEquals("en", request.language)
        assertEquals(emptyList(), request.includes)
        assertEquals(listOf("multimodal.child", "osm.public_transport.address"), request.excludes)
        assertNull(request.lat)
        assertNull(request.lon)
    }

    @Test
    fun `from PeliasAutocompleteParams includes focus parameters`() {
        val focus =
            PeliasAutocompleteRequest.FocusParams(
                lat = 59.911491,
                lon = 10.757933,
                scale = 50,
                weight = 15.0,
            )

        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertEquals(59.911491, request.lat)
        assertEquals(10.757933, request.lon)
        assertEquals(11, request.zoom)
        assertNotNull(request.locationBiasScale)
        // Weight of 15.0 produces scale of ~0.2
        assertEquals(0.2, request.locationBiasScale, 0.01)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "country.no", "county_gid.KVE.TopographicPlace.03", "legacy.source.osm", "legacy.layer.venue", "legacy.category.transport",
        ],
    )
    fun `from PeliasAutocompleteParams builds includes from filters`(expectedInclude: String) {
        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03"),
                sources = listOf("osm"),
                layers = listOf("venue"),
                categories = listOf("transport"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertEquals(expectedInclude, request.includes.find { it == expectedInclude })
    }

    @Test
    fun `from PeliasAutocompleteParams builds excludes based on multiModal`() {
        val reqParent =
            PeliasAutocompleteRequest(
                text = "Oslo gate 1",
                multiModal = "parent",
            )
        assertEquals(listOf("multimodal.child"), PhotonAutocompleteRequest.from(reqParent).excludes)

        val reqChild =
            PeliasAutocompleteRequest(
                text = "Oslo gate 1",
                multiModal = "child",
            )
        assertEquals(listOf("multimodal.parent"), PhotonAutocompleteRequest.from(reqChild).excludes)

        val reqAll =
            PeliasAutocompleteRequest(
                text = "Oslo gate 1",
                multiModal = "all",
            )
        assertEquals(listOf(), PhotonAutocompleteRequest.from(reqAll).excludes)
    }

    @ParameterizedTest
    @ValueSource(strings = ["tariff_zone_id.RUT.TariffZone.01,tariff_zone_id.RUT.TariffZone.02", "tariff_zone_authority.RUT"])
    fun `from PeliasAutocompleteParams includes tariff zones`(expectedInclude: String) {
        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                tariffZones = listOf("RUT:TariffZone:01", "RUT:TariffZone:02"),
                tariffZoneAuthorities = listOf("RUT"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertEquals(expectedInclude, request.includes.find { it == expectedInclude })
    }

    @Test
    fun `from PeliasPlaceParams creates requests for each id`() {
        val req =
            PeliasPlaceRequest(
                ids = listOf("osm:venue:123", "kartverket:address:456", "whosonfirst:locality:789"),
            )

        val requests = PhotonAutocompleteRequest.from(req)

        assertEquals(3, requests.size)
        assertEquals("osm:venue:123", requests[0].query)
        assertEquals("kartverket:address:456", requests[1].query)
        assertEquals("whosonfirst:locality:789", requests[2].query)

        requests.forEach { request ->
            assertEquals(1, request.limit)
            assertEquals("no", request.language)
            assertNull(request.lat)
            assertNull(request.lon)
            assertNull(request.zoom)
            assertNull(request.locationBiasScale)
        }
    }

    @Test
    fun `from PeliasAutocompleteParams calculates zoom from scale`() {
        val focus =
            PeliasAutocompleteRequest.FocusParams(
                lat = 60.0,
                lon = 10.0,
                scale = 100,
                weight = null,
            )

        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        // Scale of 100 should map to zoom level 10
        assertEquals(10, request.zoom)
    }

    @Test
    fun `from PeliasAutocompleteParams converts weight of 0_1 to scale`() {
        val focus =
            PeliasAutocompleteRequest.FocusParams(
                lat = 60.0,
                lon = 10.0,
                scale = null,
                weight = 0.1,
            )

        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        // Weight 0.1 produces scale of ~0.683
        assertNotNull(request.locationBiasScale)
        assertEquals(0.683, request.locationBiasScale, 0.001)
    }

    @Test
    fun `from PeliasAutocompleteParams converts weight of 100 to scale`() {
        val focus =
            PeliasAutocompleteRequest.FocusParams(
                lat = 60.0,
                lon = 10.0,
                scale = null,
                weight = 100.0,
            )

        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        // Weight 100.0 should produce scale close to 0.0 (high weight = low scale)
        assertNotNull(request.locationBiasScale)
        assertEquals(0.0, request.locationBiasScale, 0.01)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "country.no",
            "county_gid.KVE.TopographicPlace.03,county_gid.KVE.TopographicPlace.18",
            "locality_gid.KVE.TopographicPlace.0301,locality_gid.KVE.TopographicPlace.1804",
        ],
    )
    fun `from PeliasAutocompleteParams includes all boundary filters`(expectedInclude: String) {
        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03", "18"),
                boundaryLocalityIds = listOf("0301", "1804"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertEquals(expectedInclude, request.includes.find { it == expectedInclude })
    }
}
