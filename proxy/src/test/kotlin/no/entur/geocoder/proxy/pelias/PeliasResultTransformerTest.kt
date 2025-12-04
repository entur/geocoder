package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.common.Coordinate
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.proxy.pelias.PeliasAutocompleteRequest.FocusParams
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.*

class PeliasResultTransformerTest {
    @ParameterizedTest
    @CsvSource(
        "legacy.source.osm,legacy.layer.venue | osm",
        "legacy.source.kartverket,legacy.layer.address | kartverket",
        "legacy.layer.venue,legacy.category.transport |", // null expected
        delimiter = '|',
    )
    fun `transformSource extracts source from tags`(tags: String, expectedSource: String?) {
        val extra = Extra(tags = tags)
        assertEquals(expectedSource, PeliasResultTransformer.transformSource(extra))
    }

    @Test
    fun `transformSource returns null for null extra`() {
        assertNull(PeliasResultTransformer.transformSource(null))
    }

    @ParameterizedTest
    @CsvSource(
        "legacy.source.osm,legacy.layer.venue | venue",
        "legacy.source.kartverket,legacy.layer.address | address",
        "legacy.source.osm,legacy.category.transport |", // null expected
        delimiter = '|',
    )
    fun `transformLayer extracts layer from tags`(tags: String, expectedLayer: String?) {
        val extra = Extra(tags = tags)
        assertEquals(expectedLayer, PeliasResultTransformer.transformLayer(extra))
    }

    @Test
    fun `transformLayer returns null for null extra`() {
        assertNull(PeliasResultTransformer.transformLayer(null))
    }

    @ParameterizedTest
    @CsvSource(
        "legacy.category.transport,legacy.category.education,legacy.source.osm | transport;education",
        "legacy.source.osm,legacy.category.transport | transport",
        "legacy.source.osm,legacy.layer.venue | ", // empty list
        delimiter = '|',
    )
    fun `transformCategory extracts categories from tags`(tags: String, expectedCategoriesStr: String?) {
        val extra = Extra(tags = tags)
        val expected = if (expectedCategoriesStr.isNullOrEmpty()) emptyList() else expectedCategoriesStr.split(";")
        assertEquals(expected, PeliasResultTransformer.transformCategory(extra))
    }

    @Test
    fun `transformCategory returns empty list for null extra`() {
        assertEquals(emptyList(), PeliasResultTransformer.transformCategory(null))
    }

    @ParameterizedTest
    @CsvSource(
        "borough, 123456, whosonfirst:123456",
        "county, 03, whosonfirst:county:03",
        "county, 18, whosonfirst:county:18",
        "locality, 0301, whosonfirst:locality:0301",
        "locality, 1804, whosonfirst:locality:1804",
    )
    fun `transform gid functions add appropriate prefixes`(type: String, input: String, expected: String) {
        val result =
            when (type) {
                "borough" -> PeliasResultTransformer.transformBoroughGid(input)
                "county" -> PeliasResultTransformer.transformCountyGid(input)
                "locality" -> PeliasResultTransformer.transformLocalityGid(input)
                else -> null
            }
        assertEquals(expected, result)
    }

    @Test
    fun `transform gid functions return null for null input`() {
        assertNull(PeliasResultTransformer.transformBoroughGid(null))
        assertNull(PeliasResultTransformer.transformCountyGid(null))
        assertNull(PeliasResultTransformer.transformLocalityGid(null))
    }

    @ParameterizedTest
    @CsvSource(
        "10.757933;59.911491 | true",
        "10.0 | false",
        " | false", // empty coordinates
        delimiter = '|',
    )
    fun `calculateDistanceKm handles various coordinate cases`(coordinatesStr: String?, shouldCalculate: Boolean) {
        val coordinates = if (coordinatesStr.isNullOrBlank()) emptyList() else coordinatesStr.split(";").map { it.toDouble() }
        val geometry = PhotonGeometry(type = "Point", coordinates = coordinates)
        val distance = PeliasResultTransformer.calculateDistanceKm(geometry, Coordinate(59.912000, 10.758000))

        if (shouldCalculate) {
            assertNotNull(distance)
            assertTrue(distance > 0.0)
        } else {
            assertNull(distance)
        }
    }

    @Test
    fun `transformFeature creates complete PeliasFeature`() {
        val extra =
            Extra(
                id = "W123456",
                source = "osm",
                tags = "legacy.source.osm,legacy.layer.venue,legacy.category.transport",
                locality = "Oslo",
                locality_gid = "0301",
                county_gid = "03",
                country_a = "NOR",
                accuracy = "point",
                tariff_zones = "RUT:TariffZone:01,RUT:TariffZone:02",
                alt_name = "Oslo S;Oslo Central",
            )

        val photonFeature =
            PhotonFeature(
                type = "Feature",
                geometry =
                    PhotonGeometry(
                        type = "Point",
                        coordinates = listOf(10.757933, 59.911491),
                    ),
                properties =
                    PhotonProperties(
                        name = "Oslo Sentralstasjon",
                        street = "Jernbanetorget",
                        housenumber = "1",
                        postcode = "0154",
                        county = "Oslo",
                        extra = extra,
                    ),
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
        val photonFeature = createPhotonFeature(name = "Test Location")
        val distance = 1.234
        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, distance)

        assertEquals(distance, peliasFeature.properties.distance?.toDouble())
    }

    @ParameterizedTest
    @CsvSource(
        "Central Station | Oslo | Central Station, Oslo",
        "Oslo | Oslo | Oslo",
        " | Oslo | Oslo", // empty/null name
        delimiter = '|',
    )
    fun `transformFeature creates label correctly`(name: String?, locality: String, expectedLabel: String) {
        val extra = Extra(id = "123", locality = locality, tags = "legacy.source.osm,legacy.layer.venue")
        val photonFeature = createPhotonFeature(name = name?.takeIf { it.isNotBlank() }, extra = extra)
        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals(expectedLabel, peliasFeature.properties.label)
    }

    @ParameterizedTest
    @CsvSource(
        " | Karl Johans gate | 22 | Karl Johans gate 22",
        " | Karl Johans gate |  | Karl Johans gate",
        "My Place | Karl Johans gate | 22 | My Place",
        delimiter = '|',
    )
    fun `transformFeature creates name correctly from street and housenumber`(
        name: String?,
        street: String,
        housenumber: String?,
        expectedName: String,
    ) {
        val extra = Extra(id = "123", tags = "legacy.source.osm,legacy.layer.venue")
        val photonFeature =
            createPhotonFeature(
                name = name?.takeIf { it.isNotBlank() },
                street = street,
                housenumber = housenumber?.takeIf { it.isNotBlank() },
                extra = extra,
            )
        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertEquals(expectedName, peliasFeature.properties.name)
    }

    @Test
    fun `parseAndTransform creates valid JSON with bbox`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry =
                                PhotonGeometry(
                                    type = "Point",
                                    coordinates = listOf(10.0, 60.0),
                                ),
                            properties =
                                PhotonProperties(
                                    name = "Place 1",
                                    extra =
                                        Extra(
                                            id = "1",
                                            tags = "legacy.source.osm,legacy.layer.venue",
                                            description = "foo bar",
                                        ),
                                ),
                        ),
                        PhotonFeature(
                            geometry =
                                PhotonGeometry(
                                    type = "Point",
                                    coordinates = listOf(11.0, 61.0),
                                ),
                            properties =
                                PhotonProperties(
                                    name = "Place 2",
                                    extra =
                                        Extra(
                                            id = "2",
                                            tags = "legacy.source.osm,legacy.layer.venue",
                                        ),
                                ),
                        ),
                    ),
            )
        val request = PeliasAutocompleteRequest("foo")

        val result = PeliasResultTransformer.parseAndTransform(photonResult, request)

        assertEquals(2, result.features.size)
        assertTrue(result.features.any { it.properties.name == "Place 2" })
        assertTrue(result.features.any { it.properties.name == "Place 1" })
        assertTrue { result.features.any { it.properties.description?.first() == mapOf("nor" to "foo bar") } }
    }

    @Test
    fun `parseAndTransform calculates distances when coordinates provided`() {
        val photonResult =
            createPhotonResult(
                name = "Oslo",
                coordinates = listOf(10.757933, 59.911491),
            )
        val request =
            PeliasAutocompleteRequest(
                text = "foo",
                focus = FocusParams(lat = 59.912000, lon = 10.758000),
            )
        val result = PeliasResultTransformer.parseAndTransform(photonResult, request)

        assertEquals(
            0.057.toBigDecimal(),
            result.features
                .first()
                .properties.distance,
        )
    }

    @Test
    fun `parseAndTransform handles empty features list`() {
        val photonResult = PhotonResult(features = emptyList())

        val request = PeliasAutocompleteRequest("foo")
        val result = PeliasResultTransformer.parseAndTransform(photonResult, request)

        assertTrue(result.features.isEmpty())
        assertNull(result.bbox)
    }

    @Test
    fun `parseAndTransform includes description and verifies JSON with Jackson`() {
        val extra = Extra(id = "1", tags = "legacy.source.osm,legacy.layer.venue", description = "foran Oslo S")
        val photonResult = createPhotonResult(name = "Oslo S", extra = extra)
        val result = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("foo"))
        val json = jacksonMapper.writeValueAsString(result)

        assertContains(json, """"description":[{"nor":"foran Oslo S"}]""")
    }

    @Test
    fun `transformDescription handles null description`() {
        val extra = Extra(id = "1", tags = "legacy.source.osm,legacy.layer.venue", description = null)
        val photonResult = createPhotonResult(extra = extra)
        val result = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("foo"))

        assertNull(
            result.features
                .first()
                .properties.description,
        )
    }

    @ParameterizedTest
    @CsvSource(
        "norsk beskrivelse | nor=norsk beskrivelse",
        "nor:norsk beskrivelse | nor=norsk beskrivelse",
        "nor:norsk beskrivelse;eng:english description | nor=norsk beskrivelse,eng=english description",
        "nor:norsk;eng:english;swe:svenska | nor=norsk,eng=english,swe=svenska",
        delimiter = '|',
    )
    fun `transformDescription handles various formats`(description: String, expectedEntriesStr: String) {
        val extra = Extra(id = "1", tags = "legacy.source.osm,legacy.layer.venue", description = description)
        val photonResult = createPhotonResult(extra = extra)
        val result = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("foo"))
        val actualDescription =
            result.features
                .first()
                .properties.description

        assertNotNull(actualDescription)

        val expectedEntries = expectedEntriesStr.split(",")
        assertEquals(expectedEntries.size, actualDescription.size)

        expectedEntries.forEachIndexed { index, expected ->
            val (lang, text) = expected.split("=", limit = 2)
            assertEquals(mapOf(lang to text), actualDescription[index])
        }
    }

    private fun createPhotonFeature(
        name: String? = "Test",
        coordinates: List<Double> = listOf(10.0, 60.0),
        extra: Extra? = Extra(id = "1", tags = "legacy.source.osm,legacy.layer.venue"),
        street: String? = null,
        housenumber: String? = null,
    ) = PhotonFeature(
        geometry = PhotonGeometry(type = "Point", coordinates = coordinates),
        properties =
            PhotonProperties(
                name = name,
                street = street,
                housenumber = housenumber,
                extra = extra,
            ),
    )

    private fun createPhotonResult(
        name: String? = "Test",
        coordinates: List<Double> = listOf(10.0, 60.0),
        extra: Extra? = Extra(id = "1", tags = "legacy.source.osm,legacy.layer.venue"),
        street: String? = null,
        housenumber: String? = null,
        postcode: String? = null,
        county: String? = null,
    ) = PhotonResult(
        features =
            listOf(
                PhotonFeature(
                    geometry =
                        PhotonGeometry(
                            type = "Point",
                            coordinates = coordinates,
                        ),
                    properties =
                        PhotonProperties(
                            name = name,
                            street = street,
                            housenumber = housenumber,
                            postcode = postcode,
                            county = county,
                            extra = extra,
                        ),
                ),
            ),
    )
}
