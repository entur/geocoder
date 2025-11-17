package no.entur.geocoder.proxy.pelias

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PeliasResultTest {
    @Test
    fun `PeliasResult creates with default values`() {
        val result =
            PeliasResponse(
                features = emptyList(),
            )

        assertEquals("FeatureCollection", result.type)
        assertEquals(emptyList(), result.features)
        assertEquals(null, result.bbox)
        assertNotNull(result.geocoding)
    }

    @Test
    fun `PeliasResult creates with features and bbox`() {
        val feature =
            PeliasResponse.PeliasFeature(
                geometry =
                    PeliasResponse.PeliasGeometry(
                        type = "Point",
                        coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0")),
                    ),
                properties =
                    PeliasResponse.PeliasProperties(
                        id = "123",
                        name = "Test Place",
                    ),
            )

        val result =
            PeliasResponse(
                features = listOf(feature),
                bbox = listOf(BigDecimal("10.0"), BigDecimal("60.0"), BigDecimal("11.0"), BigDecimal("61.0")),
            )

        assertEquals(1, result.features.size)
        assertEquals(4, result.bbox?.size)
    }

    @Test
    fun `PeliasFeature has correct default type`() {
        val feature =
            PeliasResponse.PeliasFeature(
                geometry =
                    PeliasResponse.PeliasGeometry(
                        type = "Point",
                        coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0")),
                    ),
                properties = PeliasResponse.PeliasProperties(),
            )

        assertEquals("PhotonFeature", feature.type)
    }

    @Test
    fun `PeliasProperties creates with all fields`() {
        val props =
            PeliasResponse.PeliasProperties(
                id = "W123456789",
                gid = "osm:venue:W123456789",
                layer = "venue",
                source = "osm",
                source_id = "W123456789",
                name = "Oslo Sentralstasjon",
                popular_name = "Oslo S",
                housenumber = "1",
                street = "Jernbanetorget",
                distance = BigDecimal("1.234"),
                postalcode = "0154",
                accuracy = "point",
                country_a = "NOR",
                county = "Oslo",
                county_gid = "whosonfirst:county:03",
                locality = "Oslo",
                locality_gid = "whosonfirst:locality:0301",
                borough = "Sentrum",
                borough_gid = "whosonfirst:123",
                label = "Oslo Sentralstasjon, Oslo",
                category = listOf("transport", "station"),
                tariff_zones = listOf("RUT:TariffZone:01", "RUT:TariffZone:02"),
            )

        assertEquals("W123456789", props.id)
        assertEquals("osm:venue:W123456789", props.gid)
        assertEquals("venue", props.layer)
        assertEquals("osm", props.source)
        assertEquals("W123456789", props.source_id)
        assertEquals("Oslo Sentralstasjon", props.name)
        assertEquals("Oslo S", props.popular_name)
        assertEquals("1", props.housenumber)
        assertEquals("Jernbanetorget", props.street)
        assertEquals(BigDecimal("1.234"), props.distance)
        assertEquals("0154", props.postalcode)
        assertEquals("point", props.accuracy)
        assertEquals("NOR", props.country_a)
        assertEquals("Oslo", props.county)
        assertEquals("whosonfirst:county:03", props.county_gid)
        assertEquals("Oslo", props.locality)
        assertEquals("whosonfirst:locality:0301", props.locality_gid)
        assertEquals("Sentrum", props.borough)
        assertEquals("whosonfirst:123", props.borough_gid)
        assertEquals("Oslo Sentralstasjon, Oslo", props.label)
        assertEquals(listOf("transport", "station"), props.category)
        assertEquals(listOf("RUT:TariffZone:01", "RUT:TariffZone:02"), props.tariff_zones)
    }

    @Test
    fun `GeocodingMetadata has correct defaults`() {
        val metadata = PeliasResponse.GeocodingMetadata()

        assertEquals("0.2", metadata.version)
        assertEquals("http://pelias.mapzen.com/v1/attribution", metadata.attribution)
        assertEquals(null, metadata.query)
        assertNotNull(metadata.engine)
        assertNotNull(metadata.timestamp)
    }

    @Test
    fun `EngineMetadata has correct defaults`() {
        val engine = PeliasResponse.GeocodingMetadata.EngineMetadata()

        assertEquals("Photon", engine.name)
        assertEquals("Komoot", engine.author)
        assertEquals("0.7.0", engine.version)
    }

    @Test
    fun `QueryMetadata creates with all fields`() {
        val query =
            PeliasResponse.GeocodingMetadata.QueryMetadata(
                text = "Oslo",
                parser = "addressit",
                tokens = listOf("oslo"),
                size = 20,
                layers = listOf("address", "venue"),
                sources = listOf("osm", "kartverket"),
                private = false,
                lang = PeliasResponse.GeocodingMetadata.LangMetadata(),
                querySize = 50,
            )

        assertEquals("Oslo", query.text)
        assertEquals("addressit", query.parser)
        assertEquals(listOf("oslo"), query.tokens)
        assertEquals(20, query.size)
        assertEquals(listOf("address", "venue"), query.layers)
        assertEquals(listOf("osm", "kartverket"), query.sources)
        assertEquals(false, query.private)
        assertNotNull(query.lang)
        assertEquals(50, query.querySize)
    }

    @Test
    fun `LangMetadata has correct defaults`() {
        val lang = PeliasResponse.GeocodingMetadata.LangMetadata()

        assertEquals("Norwegian Bokmål", lang.name)
        assertEquals("nb", lang.iso6391)
        assertEquals("nob", lang.iso6393)
        assertEquals(false, lang.defaulted)
    }

    @Test
    fun `PeliasGeometry stores coordinates correctly`() {
        val geometry =
            PeliasResponse.PeliasGeometry(
                type = "Point",
                coordinates =
                    listOf(
                        BigDecimal("10.757933"),
                        BigDecimal("59.911491"),
                    ),
            )

        assertEquals("Point", geometry.type)
        assertEquals(2, geometry.coordinates.size)
        assertEquals(BigDecimal("10.757933"), geometry.coordinates[0])
        assertEquals(BigDecimal("59.911491"), geometry.coordinates[1])
    }

    @Test
    fun `PeliasResult with multiple features`() {
        val features =
            listOf(
                PeliasResponse.PeliasFeature(
                    geometry =
                        PeliasResponse.PeliasGeometry(
                            type = "Point",
                            coordinates = listOf(BigDecimal("10.0"), BigDecimal("60.0")),
                        ),
                    properties = PeliasResponse.PeliasProperties(name = "Place 1"),
                ),
                PeliasResponse.PeliasFeature(
                    geometry =
                        PeliasResponse.PeliasGeometry(
                            type = "Point",
                            coordinates = listOf(BigDecimal("11.0"), BigDecimal("61.0")),
                        ),
                    properties = PeliasResponse.PeliasProperties(name = "Place 2"),
                ),
                PeliasResponse.PeliasFeature(
                    geometry =
                        PeliasResponse.PeliasGeometry(
                            type = "Point",
                            coordinates = listOf(BigDecimal("12.0"), BigDecimal("62.0")),
                        ),
                    properties = PeliasResponse.PeliasProperties(name = "Place 3"),
                ),
            )

        val result = PeliasResponse(features = features)

        assertEquals(3, result.features.size)
        assertEquals("Place 1", result.features[0].properties.name)
        assertEquals("Place 2", result.features[1].properties.name)
        assertEquals("Place 3", result.features[2].properties.name)
    }
}
