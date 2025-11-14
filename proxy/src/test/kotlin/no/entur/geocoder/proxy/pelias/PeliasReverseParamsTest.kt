package no.entur.geocoder.proxy.pelias

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PeliasReverseParamsTest {
    @Test
    fun `validates latitude in range`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasReverseParams(
                lat = BigDecimal("91.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PeliasReverseParams(
                lat = BigDecimal("-91.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )
        }
    }

    @Test
    fun `validates longitude in range`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("181.0"),
                multiModal = "parent",
            )
        }

        assertFailsWith<IllegalArgumentException> {
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("-181.0"),
                multiModal = "parent",
            )
        }
    }

    @Test
    fun `accepts valid coordinates`() {
        val params =
            PeliasReverseParams(
                lat = BigDecimal("59.911491"),
                lon = BigDecimal("10.757933"),
                multiModal = "parent",
            )

        assertEquals(BigDecimal("59.911491"), params.lat)
        assertEquals(BigDecimal("10.757933"), params.lon)
    }

    @Test
    fun `accepts extreme valid coordinates`() {
        val params1 =
            PeliasReverseParams(
                lat = BigDecimal("90.0"),
                lon = BigDecimal("180.0"),
                multiModal = "parent",
            )

        assertEquals(BigDecimal("90.0"), params1.lat)
        assertEquals(BigDecimal("180.0"), params1.lon)

        val params2 =
            PeliasReverseParams(
                lat = BigDecimal("-90.0"),
                lon = BigDecimal("-180.0"),
                multiModal = "parent",
            )

        assertEquals(BigDecimal("-90.0"), params2.lat)
        assertEquals(BigDecimal("-180.0"), params2.lon)
    }

    @Test
    fun `has correct defaults`() {
        val params =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )

        assertNull(params.radius)
        assertEquals(10, params.size)
        assertEquals("no", params.lang)
        assertNull(params.boundaryCountry)
        assertEquals(emptyList(), params.boundaryCountyIds)
        assertEquals(emptyList(), params.boundaryLocalityIds)
        assertEquals(emptyList(), params.tariffZones)
        assertEquals(emptyList(), params.tariffZoneAuthorities)
        assertEquals(emptyList(), params.sources)
        assertEquals(emptyList(), params.layers)
        assertEquals(emptyList(), params.categories)
        assertEquals("parent", params.multiModal)
    }

    @Test
    fun `accepts all parameters`() {
        val params =
            PeliasReverseParams(
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

        assertEquals(BigDecimal("59.911491"), params.lat)
        assertEquals(BigDecimal("10.757933"), params.lon)
        assertEquals(1000.0, params.radius)
        assertEquals(20, params.size)
        assertEquals("en", params.lang)
        assertEquals("NOR", params.boundaryCountry)
        assertEquals(listOf("03", "18"), params.boundaryCountyIds)
        assertEquals(listOf("0301", "1804"), params.boundaryLocalityIds)
        assertEquals(listOf("RUT:TariffZone:01", "RUT:TariffZone:02"), params.tariffZones)
        assertEquals(listOf("RUT", "ATB"), params.tariffZoneAuthorities)
        assertEquals(listOf("osm", "kartverket"), params.sources)
        assertEquals(listOf("address", "venue"), params.layers)
        assertEquals(listOf("transport", "education"), params.categories)
        assertEquals("child", params.multiModal)
    }

    @Test
    fun `accepts null radius`() {
        val params =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                radius = null,
                multiModal = "parent",
            )

        assertNull(params.radius)
    }

    @Test
    fun `accepts zero radius`() {
        val params =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                radius = 0.0,
                multiModal = "parent",
            )

        assertEquals(0.0, params.radius)
    }

    @Test
    fun `accepts large radius`() {
        val params =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                radius = 100000.0,
                multiModal = "parent",
            )

        assertEquals(100000.0, params.radius)
    }

    @Test
    fun `accepts different multiModal values`() {
        val parent =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "parent",
            )
        assertEquals("parent", parent.multiModal)

        val child =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "child",
            )
        assertEquals("child", child.multiModal)

        val all =
            PeliasReverseParams(
                lat = BigDecimal("60.0"),
                lon = BigDecimal("10.0"),
                multiModal = "all",
            )
        assertEquals("all", all.multiModal)
    }
}
