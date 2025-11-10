package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.common.Extra
import no.entur.geocoder.proxy.photon.PhotonResult
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeliasResultTransformerTest {

    @Test
    fun `transformSource extracts source from tags`() {
        val extra = Extra(tags = "legacy.source.osm,legacy.layer.venue")
        val source = PeliasResultTransformer.transformSource(extra)
        assertEquals("osm", source)
    }

    @Test
    fun `transformSource with kartverket source`() {
        val extra = Extra(tags = "legacy.source.kartverket,legacy.layer.address")
        val source = PeliasResultTransformer.transformSource(extra)
        assertEquals("kartverket", source)
    }

    @Test
    fun `transformSource returns null when no source tag present`() {
        val extra = Extra(tags = "legacy.layer.venue,legacy.category.transport")
        val source = PeliasResultTransformer.transformSource(extra)
        assertNull(source)
    }

    @Test
    fun `transformSource returns null for null extra`() {
        val source = PeliasResultTransformer.transformSource(null)
        assertNull(source)
    }

    @Test
    fun `transformLayer extracts layer from tags`() {
        val extra = Extra(tags = "legacy.source.osm,legacy.layer.venue")
        val layer = PeliasResultTransformer.transformLayer(extra)
        assertEquals("venue", layer)
    }

    @Test
    fun `transformLayer extracts address layer`() {
        val extra = Extra(tags = "legacy.source.kartverket,legacy.layer.address")
        val layer = PeliasResultTransformer.transformLayer(extra)
        assertEquals("address", layer)
    }

    @Test
    fun `transformLayer returns null when no layer tag present`() {
        val extra = Extra(tags = "legacy.source.osm,legacy.category.transport")
        val layer = PeliasResultTransformer.transformLayer(extra)
        assertNull(layer)
    }

    @Test
    fun `transformLayer returns null for null extra`() {
        val layer = PeliasResultTransformer.transformLayer(null)
        assertNull(layer)
    }

    @Test
    fun `transformCategory extracts multiple categories from tags`() {
        val extra = Extra(tags = "legacy.category.transport,legacy.category.education,legacy.source.osm")
        val categories = PeliasResultTransformer.transformCategory(extra)
        assertEquals(listOf("transport", "education"), categories)
    }

    @Test
    fun `transformCategory extracts single category`() {
        val extra = Extra(tags = "legacy.source.osm,legacy.category.transport")
        val categories = PeliasResultTransformer.transformCategory(extra)
        assertEquals(listOf("transport"), categories)
    }

    @Test
    fun `transformCategory returns empty list when no category tags present`() {
        val extra = Extra(tags = "legacy.source.osm,legacy.layer.venue")
        val categories = PeliasResultTransformer.transformCategory(extra)
        assertEquals(emptyList(), categories)
    }

    @Test
    fun `transformCategory returns empty list for null extra`() {
        val categories = PeliasResultTransformer.transformCategory(null)
        assertEquals(emptyList(), categories)
    }

    @Test
    fun `transformBoroughGid adds whosonfirst prefix`() {
        val gid = PeliasResultTransformer.transformBoroughGid("123456")
        assertEquals("whosonfirst:123456", gid)
    }

    @Test
    fun `transformBoroughGid returns null for null input`() {
        val gid = PeliasResultTransformer.transformBoroughGid(null)
        assertNull(gid)
    }

    @Test
    fun `transformCountyGid adds whosonfirst county prefix`() {
        val gid = PeliasResultTransformer.transformCountyGid("03")
        assertEquals("whosonfirst:county:03", gid)
    }

    @Test
    fun `transformCountyGid handles different county codes`() {
        assertEquals("whosonfirst:county:18", PeliasResultTransformer.transformCountyGid("18"))
        assertEquals("whosonfirst:county:50", PeliasResultTransformer.transformCountyGid("50"))
    }

    @Test
    fun `transformCountyGid returns null for null input`() {
        val gid = PeliasResultTransformer.transformCountyGid(null)
        assertNull(gid)
    }

    @Test
    fun `transformLocalityGid adds whosonfirst locality prefix`() {
        val gid = PeliasResultTransformer.transformLocalityGid("0301")
        assertEquals("whosonfirst:locality:0301", gid)
    }

    @Test
    fun `transformLocalityGid handles different locality codes`() {
        assertEquals("whosonfirst:locality:1804", PeliasResultTransformer.transformLocalityGid("1804"))
    }

    @Test
    fun `transformLocalityGid returns null for null input`() {
        val gid = PeliasResultTransformer.transformLocalityGid(null)
        assertNull(gid)
    }

    @Test
    fun `calculateDistanceKm calculates distance between two points`() {
        val geometry = PhotonResult.PhotonGeometry(
            type = "Point",
            coordinates = listOf(BigDecimal("10.757933"), BigDecimal("59.911491"))
        )
        val distance = PeliasResultTransformer.calculateDistanceKm(
            geometry,
            BigDecimal("59.912000"),
            BigDecimal("10.758000")
        )

        assertNotNull(distance)
        assertTrue(distance > BigDecimal.ZERO)
        assertTrue(distance < BigDecimal("0.1"))
    }

    @Test
    fun `calculateDistanceKm returns null for invalid geometry with one coordinate`() {
        val geometry = PhotonResult.PhotonGeometry(
            type = "Point",
            coordinates = listOf(BigDecimal("10.0"))
        )
        val distance = PeliasResultTransformer.calculateDistanceKm(
            geometry,
            BigDecimal("60.0"),
            BigDecimal("10.0")
        )

        assertNull(distance)
    }

    @Test
    fun `calculateDistanceKm returns null for empty coordinates`() {
        val geometry = PhotonResult.PhotonGeometry(
            type = "Point",
            coordinates = emptyList()
        )
        val distance = PeliasResultTransformer.calculateDistanceKm(
            geometry,
            BigDecimal("60.0"),
            BigDecimal("10.0")
        )

        assertNull(distance)
    }

    @Test
    fun `transformFeature creates complete PeliasFeature`() {
        val extra = Extra(
            id = "W123456",
            source = "osm",
            tags = "legacy.source.osm,legacy.layer.venue,legacy.category.transport",
            locality = "Oslo",
            locality_gid = "0301",
            county_gid = "03",
            country_a = "NOR",
            accuracy = "point",
            tariff_zones = "RUT:TariffZone:01,RUT:TariffZone:02",
            alt_name = "Oslo S;Oslo Central"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            type = "Feature",
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.757933"), BigDecimal("59.911491"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = "Oslo Sentralstasjon",
                street = "Jernbanetorget",
                housenumber = "1",
                postcode = "0154",
                county = "Oslo",
                extra = extra
            )
        )

        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals("Feature", peliasFeature.type)
        assertEquals("Point", peliasFeature.geometry.type)

        val props = peliasFeature.properties
        assertEquals("W123456", props.id)
        assertEquals("osm:venue:W123456", props.gid)
        assertEquals("venue", props.layer)
        assertEquals("osm", props.source)
        assertEquals("Oslo Sentralstasjon", props.name)
        assertEquals("Oslo S", props.popular_name)
        assertEquals("Jernbanetorget", props.street)
        assertEquals("1", props.housenumber)
        assertEquals("0154", props.postalcode)
        assertEquals(listOf("transport"), props.category)
        assertEquals(listOf("RUT:TariffZone:01", "RUT:TariffZone:02"), props.tariff_zones)
    }

    @Test
    fun `transformFeature includes distance when provided`() {
        val extra = Extra(
            id = "123",
            tags = "legacy.source.osm,legacy.layer.venue"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = "Test Location",
                extra = extra
            )
        )

        val distance = BigDecimal("1.234")
        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, distance)

        assertEquals(distance, peliasFeature.properties.distance)
    }

    @Test
    fun `transformFeature creates label with locality when name differs`() {
        val extra = Extra(
            id = "123",
            locality = "Oslo",
            tags = "legacy.source.osm,legacy.layer.venue"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = "Central Station",
                extra = extra
            )
        )

        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals("Central Station, Oslo", peliasFeature.properties.label)
    }

    @Test
    fun `transformFeature creates label without duplicate locality`() {
        val extra = Extra(
            id = "123",
            locality = "Oslo",
            tags = "legacy.source.osm,legacy.layer.venue"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = "Oslo",
                extra = extra
            )
        )

        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals("Oslo", peliasFeature.properties.label)
    }

    @Test
    fun `transformFeature uses locality as label when name is blank`() {
        val extra = Extra(
            id = "123",
            locality = "Oslo",
            tags = "legacy.source.osm,legacy.layer.venue"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = "",
                extra = extra
            )
        )

        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals("Oslo", peliasFeature.properties.label)
    }

    @Test
    fun `transformFeature combines street and housenumber when no name`() {
        val extra = Extra(
            id = "123",
            source = "kartverket",
            tags = "legacy.source.kartverket,legacy.layer.address"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = null,
                street = "Karl Johans gate",
                housenumber = "22",
                extra = extra
            )
        )

        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals("Karl Johans gate 22", peliasFeature.properties.name)
    }

    @Test
    fun `transformFeature uses street alone when no name and no housenumber`() {
        val extra = Extra(
            id = "123",
            tags = "legacy.source.osm,legacy.layer.street"
        )

        val photonFeature = PhotonResult.PhotonFeature(
            geometry = PhotonResult.PhotonGeometry(
                type = "Point",
                coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
            ),
            properties = PhotonResult.PhotonProperties(
                name = null,
                street = "Karl Johans gate",
                housenumber = null,
                extra = extra
            )
        )

        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals("Karl Johans gate", peliasFeature.properties.name)
    }

    @Test
    fun `parseAndTransform creates valid JSON with bbox`() {
        val photonResult = PhotonResult(
            features = listOf(
                PhotonResult.PhotonFeature(
                    geometry = PhotonResult.PhotonGeometry(
                        type = "Point",
                        coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0"))
                    ),
                    properties = PhotonResult.PhotonProperties(
                        name = "Place 1",
                        extra = Extra(
                            id = "1",
                            tags = "legacy.source.osm,legacy.layer.venue"
                        )
                    )
                ),
                PhotonResult.PhotonFeature(
                    geometry = PhotonResult.PhotonGeometry(
                        type = "Point",
                        coordinates = listOf(BigDecimal("11.0"), BigDecimal("61.0"))
                    ),
                    properties = PhotonResult.PhotonProperties(
                        name = "Place 2",
                        extra = Extra(
                            id = "2",
                            tags = "legacy.source.osm,legacy.layer.venue"
                        )
                    )
                )
            )
        )

        val json = PeliasResultTransformer.parseAndTransform(photonResult)

        assertTrue(json.contains("\"features\""))
        assertTrue(json.contains("\"bbox\""))
        assertTrue(json.contains("Place 1"))
        assertTrue(json.contains("Place 2"))
    }

    @Test
    fun `parseAndTransform calculates distances when coordinates provided`() {
        val photonResult = PhotonResult(
            features = listOf(
                PhotonResult.PhotonFeature(
                    geometry = PhotonResult.PhotonGeometry(
                        type = "Point",
                        coordinates = listOf(BigDecimal("10.757933"), BigDecimal("59.911491"))
                    ),
                    properties = PhotonResult.PhotonProperties(
                        name = "Oslo",
                        extra = Extra(
                            id = "1",
                            tags = "legacy.source.osm,legacy.layer.venue"
                        )
                    )
                )
            )
        )

        val json = PeliasResultTransformer.parseAndTransform(
            photonResult,
            BigDecimal("59.912000"),
            BigDecimal("10.758000")
        )

        assertTrue(json.contains("\"distance\""))
    }

    @Test
    fun `parseAndTransform handles empty features list`() {
        val photonResult = PhotonResult(features = emptyList())

        val json = PeliasResultTransformer.parseAndTransform(photonResult)

        assertTrue(json.contains("\"features\":[]"))
        assertTrue(!json.contains("\"bbox\""))
    }
}

