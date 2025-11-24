package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.pelias.PeliasReverseRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotonReverseRequestTest {
    @Test
    fun `from PeliasReverseParams creates request with basic req`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertEquals(59.911491, request.latitude)
        assertEquals(10.757933, request.longitude)
        assertEquals("no", request.language)
        assertEquals(10, request.limit)
        assertNull(request.radius)
    }

    @Test
    fun `from PeliasReverseParams includes radius when provided`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                radius = 1000.0,
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertEquals(1000.0, request.radius)
    }

    @Test
    fun `from PeliasReverseParams uses custom size and language`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                size = 20,
                lang = "en",
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertEquals(20, request.limit)
        assertEquals("en", request.language)
    }

    @Test
    fun `from PeliasReverseParams always excludes OSM addresses`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                multiModal = "all",
            )

        val request = PhotonReverseRequest.from(req)

        assertTrue(request.excludes.contains("osm.public_transport.address"))
    }

    @Test
    fun `from PeliasReverseParams excludes child for parent mode`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertTrue(request.excludes.contains("osm.public_transport.address"))
        assertTrue(request.excludes.contains("multimodal.child"))
        assertEquals(2, request.excludes.size)
    }

    @Test
    fun `from PeliasReverseParams excludes parent for child mode`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                multiModal = "child",
            )

        val request = PhotonReverseRequest.from(req)

        assertTrue(request.excludes.contains("osm.public_transport.address"))
        assertTrue(request.excludes.contains("multimodal.parent"))
        assertEquals(2, request.excludes.size)
    }

    @Test
    fun `from PeliasReverseParams excludes only addresses for all mode`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                multiModal = "all",
            )

        val request = PhotonReverseRequest.from(req)

        assertTrue(request.excludes.contains("osm.public_transport.address"))
        assertEquals(1, request.excludes.size)
    }

    @Test
    fun `from PeliasReverseParams builds includes from filters`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03"),
                boundaryLocalityIds = listOf("0301"),
                sources = listOf("osm", "kartverket"),
                layers = listOf("venue"),
                categories = listOf("transport"),
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertTrue(request.includes.contains("country.no"))
        assertTrue(request.includes.contains("county_gid.03"))
        assertTrue(request.includes.contains("locality_gid.0301"))
        assertTrue(request.includes.contains("legacy.source.osm"))
        assertTrue(request.includes.contains("legacy.source.kartverket"))
        assertTrue(request.includes.contains("legacy.layer.venue"))
        assertTrue(request.includes.contains("legacy.category.transport"))
    }

    @Test
    fun `from PeliasReverseParams includes tariff zones`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                tariffZones = listOf("RUT:TariffZone:01"),
                tariffZoneAuthorities = listOf("RUT"),
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertTrue(request.includes.contains("tariff_zone_id.RUT:TariffZone:01"))
        assertTrue(request.includes.contains("tariff_zone_authority.RUT"))
    }

    @Test
    fun `from PeliasReverseParams handles empty filters`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertEquals(emptyList(), request.includes)
    }

    @Test
    fun `from PeliasReverseParams handles all boundary types`() {
        val req =
            PeliasReverseRequest(
                lat = 59.911491,
                lon = 10.757933,
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03", "18", "50"),
                boundaryLocalityIds = listOf("0301", "1804", "5001"),
                multiModal = "parent",
            )

        val request = PhotonReverseRequest.from(req)

        assertEquals(7, request.includes.size)
        assertTrue(request.includes.contains("country.no"))
        assertTrue(request.includes.contains("county_gid.03"))
        assertTrue(request.includes.contains("county_gid.18"))
        assertTrue(request.includes.contains("county_gid.50"))
        assertTrue(request.includes.contains("locality_gid.0301"))
        assertTrue(request.includes.contains("locality_gid.1804"))
        assertTrue(request.includes.contains("locality_gid.5001"))
    }
}
