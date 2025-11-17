package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.pelias.PeliasAutocompleteParams
import no.entur.geocoder.proxy.pelias.PeliasPlaceParams
import no.entur.geocoder.proxy.pelias.PeliasResultTransformer.CITY_AND_GOSP_LIST_HEADROOM
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotonAutocompleteRequestTest {
    @Test
    fun `from PeliasAutocompleteParams creates request with basic params`() {
        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                size = 20,
                lang = "en",
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertEquals("Oslo", request.query)
        assertEquals(20 + CITY_AND_GOSP_LIST_HEADROOM, request.limit)
        assertEquals("en", request.language)
        assertEquals(emptyList(), request.includes)
        assertEquals(listOf("multimodal.child", "osm.public_transport.address"), request.excludes)
        assertNull(request.lat)
        assertNull(request.lon)
    }

    @Test
    fun `from PeliasAutocompleteParams includes focus parameters`() {
        val focus =
            PeliasAutocompleteParams.FocusParams(
                lat = BigDecimal("59.911491"),
                lon = BigDecimal("10.757933"),
                scale = 50,
                weight = 15.0,
            )

        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertEquals(BigDecimal("59.911491"), request.lat)
        assertEquals(BigDecimal("10.757933"), request.lon)
        assertTrue(request.zoom != null)
        assertTrue(request.weight != null)
        assertTrue(request.weight < 1.0)
        assertTrue(request.weight > 0.0)
    }

    @Test
    fun `from PeliasAutocompleteParams builds includes from filters`() {
        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03"),
                sources = listOf("osm"),
                layers = listOf("venue"),
                categories = listOf("transport"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertTrue(request.includes.contains("country.NOR"))
        assertTrue(request.includes.contains("county_gid.03"))
        assertTrue(request.includes.contains("legacy.source.osm"))
        assertTrue(request.includes.contains("legacy.layer.venue"))
        assertTrue(request.includes.contains("legacy.category.transport"))
    }

    @Test
    fun `from PeliasAutocompleteParams builds excludes based on multiModal`() {
        val paramsParent =
            PeliasAutocompleteParams(
                text = "Oslo gate 1",
                multiModal = "parent",
            )
        assertEquals(listOf("multimodal.child"), PhotonAutocompleteRequest.from(paramsParent).excludes)

        val paramsChild =
            PeliasAutocompleteParams(
                text = "Oslo gate 1",
                multiModal = "child",
            )
        assertEquals(listOf("multimodal.parent"), PhotonAutocompleteRequest.from(paramsChild).excludes)

        val paramsAll =
            PeliasAutocompleteParams(
                text = "Oslo gate 1",
                multiModal = "all",
            )
        assertEquals(listOf(), PhotonAutocompleteRequest.from(paramsAll).excludes)
    }

    @Test
    fun `from PeliasAutocompleteParams includes tariff zones`() {
        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                tariffZones = listOf("RUT:TariffZone:01", "RUT:TariffZone:02"),
                tariffZoneAuthorities = listOf("RUT"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertTrue(request.includes.contains("tariff_zone_id.RUT:TariffZone:01"))
        assertTrue(request.includes.contains("tariff_zone_id.RUT:TariffZone:02"))
        assertTrue(request.includes.contains("tariff_zone_authority.RUT"))
    }

    @Test
    fun `from PeliasPlaceParams creates requests for each id`() {
        val params =
            PeliasPlaceParams(
                ids = listOf("osm:venue:123", "kartverket:address:456", "whosonfirst:locality:789"),
            )

        val requests = PhotonAutocompleteRequest.from(params)

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
            assertNull(request.weight)
        }
    }

    @Test
    fun `from PeliasAutocompleteParams calculates zoom from scale`() {
        val focus =
            PeliasAutocompleteParams.FocusParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                scale = 100,
                weight = null,
            )

        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertTrue(request.zoom != null)
        assertTrue(request.zoom > 0)
    }

    @Test
    fun `from PeliasAutocompleteParams uses default zoom when no scale`() {
        val focus =
            PeliasAutocompleteParams.FocusParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                scale = null,
                weight = null,
            )

        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                focus = focus,
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertTrue(request.zoom != null)
    }

    @Test
    fun `from PeliasAutocompleteParams converts weight correctly`() {
        val focus1 =
            PeliasAutocompleteParams.FocusParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                scale = null,
                weight = 0.1,
            )

        val params1 =
            PeliasAutocompleteParams(
                text = "Oslo",
                focus = focus1,
                multiModal = "parent",
            )

        val request1 = PhotonAutocompleteRequest.from(params1)
        assertNotNull(request1.weight)
        assertTrue(request1.weight <= 1.0)

        val focus2 =
            PeliasAutocompleteParams.FocusParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                scale = null,
                weight = 100.0,
            )

        val params2 =
            PeliasAutocompleteParams(
                text = "Oslo",
                focus = focus2,
                multiModal = "parent",
            )

        val request2 = PhotonAutocompleteRequest.from(params2)
        assertNotNull(request2.weight)
        assertTrue(request2.weight >= 0.0)
    }

    @Test
    fun `from PeliasAutocompleteParams includes all boundary filters`() {
        val params =
            PeliasAutocompleteParams(
                text = "Oslo",
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03", "18"),
                boundaryLocalityIds = listOf("0301", "1804"),
                multiModal = "parent",
            )

        val request = PhotonAutocompleteRequest.from(params)

        assertTrue(request.includes.contains("country.NOR"))
        assertTrue(request.includes.contains("county_gid.03"))
        assertTrue(request.includes.contains("county_gid.18"))
        assertTrue(request.includes.contains("locality_gid.0301"))
        assertTrue(request.includes.contains("locality_gid.1804"))
    }
}
