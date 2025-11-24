package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest
import no.entur.geocoder.proxy.pelias.PeliasPlaceRequest
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest.Companion.RESULT_PRUNING_HEADROOM
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
        assertTrue(request.zoom != null)
        assertTrue(request.locationBiasScale != null)
        assertTrue(request.locationBiasScale < 1.0)
        assertTrue(request.locationBiasScale > 0.0)
    }

    @Test
    fun `from PeliasAutocompleteParams builds includes from filters`() {
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

        assertTrue(request.includes.contains("country.no"))
        assertTrue(request.includes.contains("county_gid.03"))
        assertTrue(request.includes.contains("legacy.source.osm"))
        assertTrue(request.includes.contains("legacy.layer.venue"))
        assertTrue(request.includes.contains("legacy.category.transport"))
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

    @Test
    fun `from PeliasAutocompleteParams includes tariff zones`() {
        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                tariffZones = listOf("RUT:TariffZone:01", "RUT:TariffZone:02"),
                tariffZoneAuthorities = listOf("RUT"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertTrue(request.includes.contains("tariff_zone_id.RUT:TariffZone:01"))
        assertTrue(request.includes.contains("tariff_zone_id.RUT:TariffZone:02"))
        assertTrue(request.includes.contains("tariff_zone_authority.RUT"))
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

        assertTrue(request.zoom != null)
        assertTrue(request.zoom > 0)
    }

    @Test
    fun `from PeliasAutocompleteParams converts weight correctly`() {
        val focus1 =
            PeliasAutocompleteRequest.FocusParams(
                lat = 60.0,
                lon = 10.0,
                scale = null,
                weight = 0.1,
            )

        val req1 =
            PeliasAutocompleteRequest(
                text = "Oslo",
                focus = focus1,
                multiModal = "parent",
            )

        val request1 = PhotonAutocompleteRequest.from(req1)
        assertNotNull(request1.locationBiasScale)
        assertTrue(request1.locationBiasScale <= 1.0)

        val focus2 =
            PeliasAutocompleteRequest.FocusParams(
                lat = 60.0,
                lon = 10.0,
                scale = null,
                weight = 100.0,
            )

        val req2 =
            PeliasAutocompleteRequest(
                text = "Oslo",
                focus = focus2,
                multiModal = "parent",
            )

        val request2 = PhotonAutocompleteRequest.from(req2)
        assertNotNull(request2.locationBiasScale)
        assertTrue(request2.locationBiasScale >= 0.0)
    }

    @Test
    fun `from PeliasAutocompleteParams includes all boundary filters`() {
        val req =
            PeliasAutocompleteRequest(
                text = "Oslo",
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03", "18"),
                boundaryLocalityIds = listOf("0301", "1804"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(req)

        assertTrue(request.includes.contains("country.no"))
        assertTrue(request.includes.contains("county_gid.03"))
        assertTrue(request.includes.contains("county_gid.18"))
        assertTrue(request.includes.contains("locality_gid.0301"))
        assertTrue(request.includes.contains("locality_gid.1804"))
    }
}
