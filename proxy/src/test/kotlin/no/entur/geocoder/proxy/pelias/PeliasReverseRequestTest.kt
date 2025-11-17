package no.entur.geocoder.proxy.pelias

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PeliasReverseRequestTest {
    @Test
    fun `validates latitude in range`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasReverseRequest(
                lat = BigDecimal("91.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PeliasReverseRequest(
                lat = BigDecimal("-91.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )
        }
    }

    @Test
    fun `validates longitude in range`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("181.0"),
                multiModal = "parent",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("-181.0"),
                multiModal = "parent",
            )
        }
    }

    @Test
    fun `accepts valid coordinates`() {
        val req =
            PeliasReverseRequest(
                lat = BigDecimal("59.911491"),
                lon = BigDecimal("10.757933"),
                multiModal = "parent",
            )

        assertEquals(BigDecimal("59.911491"), req.lat)
        assertEquals(BigDecimal("10.757933"), req.lon)
    }

    @Test
    fun `accepts extreme valid coordinates`() {
        val req1 =
            PeliasReverseRequest(
                lat = BigDecimal("90.0"),
                lon = BigDecimal("180.0"),
                multiModal = "parent",
            )

        assertEquals(BigDecimal("90.0"), req1.lat)
        assertEquals(BigDecimal("180.0"), req1.lon)

        val req2 =
            PeliasReverseRequest(
                lat = BigDecimal("-90.0"),
                lon = BigDecimal("-180.0"),
                multiModal = "parent",
            )

        assertEquals(BigDecimal("-90.0"), req2.lat)
        assertEquals(BigDecimal("-180.0"), req2.lon)
    }

    @Test
    fun `has correct defaults`() {
        val req =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )

        assertNull(req.radius)
        assertEquals(10, req.size)
        assertEquals("no", req.lang)
        assertNull(req.boundaryCountry)
        assertEquals(emptyList(), req.boundaryCountyIds)
        assertEquals(emptyList(), req.boundaryLocalityIds)
        assertEquals(emptyList(), req.tariffZones)
        assertEquals(emptyList(), req.tariffZoneAuthorities)
        assertEquals(emptyList(), req.sources)
        assertEquals(emptyList(), req.layers)
        assertEquals(emptyList(), req.categories)
        assertEquals("parent", req.multiModal)
    }

    @Test
    fun `accepts all parameters`() {
        val req =
            PeliasReverseRequest(
                lat = BigDecimal("59.911491"),
                lon = BigDecimal("10.757933"),
                radius = 1000.0,
                size = 20,
                lang = "en",
                boundaryCountry = "NOR",
                boundaryCountyIds = listOf("03", "18"),
                boundaryLocalityIds = listOf("0301", "1804"),
                tariffZones = listOf("RUT:TariffZone:01", "RUT:TariffZone:02"),
                tariffZoneAuthorities = listOf("RUT", "ATB"),
                sources = listOf("osm", "kartverket"),
                layers = listOf("address", "venue"),
                categories = listOf("transport", "education"),
                multiModal = "child",
            )

        assertEquals(BigDecimal("59.911491"), req.lat)
        assertEquals(BigDecimal("10.757933"), req.lon)
        assertEquals(1000.0, req.radius)
        assertEquals(20, req.size)
        assertEquals("en", req.lang)
        assertEquals("NOR", req.boundaryCountry)
        assertEquals(listOf("03", "18"), req.boundaryCountyIds)
        assertEquals(listOf("0301", "1804"), req.boundaryLocalityIds)
        assertEquals(listOf("RUT:TariffZone:01", "RUT:TariffZone:02"), req.tariffZones)
        assertEquals(listOf("RUT", "ATB"), req.tariffZoneAuthorities)
        assertEquals(listOf("osm", "kartverket"), req.sources)
        assertEquals(listOf("address", "venue"), req.layers)
        assertEquals(listOf("transport", "education"), req.categories)
        assertEquals("child", req.multiModal)
    }

    @Test
    fun `accepts null radius`() {
        val req =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                radius = null,
                multiModal = "parent",
            )

        assertNull(req.radius)
    }

    @Test
    fun `accepts zero radius`() {
        val req =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                radius = 0.0,
                multiModal = "parent",
            )

        assertEquals(0.0, req.radius)
    }

    @Test
    fun `accepts large radius`() {
        val req =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                radius = 100000.0,
                multiModal = "parent",
            )

        assertEquals(100000.0, req.radius)
    }

    @Test
    fun `accepts different multiModal values`() {
        val parent =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )
        assertEquals("parent", parent.multiModal)

        val child =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "child",
            )
        assertEquals("child", child.multiModal)

        val all =
            PeliasReverseRequest(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "all",
            )
        assertEquals("all", all.multiModal)
    }
}
