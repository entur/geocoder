package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.proxy.common.Coordinate
import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Category.GOSP
import no.entur.geocoder.proxy.common.Extra
import no.entur.geocoder.proxy.common.Source
import no.entur.geocoder.proxy.common.JsonMapper.jacksonMapper
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
        val extra = Extra(id = "OSM:TopographicPlace:100", tags = tags)
        assertEquals(expectedSource, PeliasResultTransformer.transformSource(extra))
    }

    @ParameterizedTest
    @CsvSource(
        "legacy.source.osm,legacy.layer.venue | venue",
        "legacy.source.kartverket,legacy.layer.address | address",
        "legacy.source.osm,legacy.category.transport |", // null expected
        delimiter = '|',
    )
    fun `transformLayer extracts layer from tags`(tags: String, expectedLayer: String?) {
        val extra = Extra(id = "OSM:TopographicPlace:100", tags = tags)
        assertEquals(expectedLayer, PeliasResultTransformer.transformLayer(extra))
    }

    @ParameterizedTest
    @CsvSource(
        "legacy.category.transport;legacy.category.education;legacy.source.osm | transport;education",
        "legacy.source.osm,legacy.category.transport | transport",
        "legacy.source.osm,legacy.layer.venue | ", // empty list
        delimiter = '|',
    )
    fun `transformCategory extracts categories from tags`(tags: String, expectedCategoriesStr: String?) {
        val extra = Extra(id = "OSM:TopographicPlace:100", tags = tags)
        val expected = if (expectedCategoriesStr.isNullOrEmpty()) emptyList() else expectedCategoriesStr.split(";")
        assertEquals(expected, PeliasResultTransformer.transformCategory(extra))
    }

    @Test
    fun `transformTransportExtra parses mode with submode`() {
        val extra = Extra(id = "OSM:TopographicPlace:100", transport_mode = "bus:localBus")
        assertEquals(listOf("bus" to "localBus"), PeliasResultTransformer.transformTransportExtra(extra))
    }

    @Test
    fun `transformTransportExtra parses mode without submode`() {
        val extra = Extra(id = "OSM:TopographicPlace:100", transport_mode = "rail")
        assertEquals(listOf("rail" to null), PeliasResultTransformer.transformTransportExtra(extra))
    }

    @Test
    fun `transformTransportExtra parses multiple modes`() {
        val extra = Extra(id = "OSM:TopographicPlace:100", transport_mode = "bus:localBus;rail;metro:urbanRail")
        assertEquals(
            listOf("bus" to "localBus", "rail" to null, "metro" to "urbanRail"),
            PeliasResultTransformer.transformTransportExtra(extra),
        )
    }

    @Test
    fun `transformTransportExtra preserves duplicate mode keys with different submodes`() {
        val extra = Extra(id = "OSM:TopographicPlace:100", transport_mode = "tram:cityTram;tram")
        assertEquals(
            listOf("tram" to "cityTram", "tram" to null),
            PeliasResultTransformer.transformTransportExtra(extra),
        )
    }

    @Test
    fun `transformTransportExtra serializes as list of objects via PeliasProperties`() {
        val extra = Extra(id = "OSM:TopographicPlace:100", transport_mode = "tram:cityTram;tram")
        val props = PeliasResult.PeliasProperties(mode = PeliasResultTransformer.transformTransportExtra(extra))
        val json = jacksonMapper.writeValueAsString(props)
        assertContains(json, """[{"tram":"cityTram"},{"tram":null}]""")
    }

    @Test
    fun `transformTransportExtra returns null when no transport data`() {
        val extra = Extra(id = "OSM:TopographicPlace:123", tags = "legacy.source.osm")
        assertNull(PeliasResultTransformer.transformTransportExtra(extra))
    }

    @ParameterizedTest
    @CsvSource(
        "NSR:StopPlace:337 | nsr | NSR:StopPlace:337",
        "KVE:PostalAddress:225678815 | kartverket-matrikkelenadresse | 225678815",
        "KVE:PlaceName:434810 | kartverket-stedsnavn | 434810",
        "KVE:TopographicPlace:0301-Karl Johans gate | kartverket-matrikkelenadresse | KVE:TopographicPlace:0301-Karl Johans gate",
        "OSM:TopographicPlace:100 | openstreetmap | OSM:TopographicPlace:100",
        "OSM:PointOfInterest:100 | openstreetmap | OSM:TopographicPlace:100",
        delimiter = '|',
    )
    fun `transformFeature normalizes v3 ids back to v2 shape`(inputId: String, source: String, expectedId: String) {
        val photonFeature =
            PhotonFeature(
                type = "Feature",
                geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.7, 59.9)),
                properties = PhotonProperties(extra = Extra(id = inputId, source = source)),
            )
        val result = PeliasResultTransformer.transformFeature(photonFeature, null)
        assertEquals(expectedId, result.properties.id)
    }

    @ParameterizedTest
    @CsvSource(
        "borough, KVE:Borough:34200205, whosonfirst:borough:34200205",
        "borough, borough:123456, whosonfirst:borough:123456",
        "borough, 34200205, whosonfirst:borough:34200205",
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

    @Test
    fun `malformed borough gid is dropped rather than emitted`() {
        assertNull(PeliasResultTransformer.transformBoroughGid("KVE:Foo:1"))
        assertNull(PeliasResultTransformer.transformBoroughGid("123abc"))
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
                id = "OSM:TopographicPlace:123456",
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
        assertEquals("OSM:TopographicPlace:123456", props.id)
        assertEquals("osm:venue:OSM:TopographicPlace:123456", props.gid)
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

    @Test
    fun `transformFeature includes extra with transport data for stop places`() {
        val extra =
            Extra(
                id = "NSR:StopPlace:123",
                tags = "legacy.source.nsr,legacy.layer.venue",
                transport_mode = "bus:localBus",
            )
        val photonFeature = createPhotonFeature(name = "Bus Stop", extra = extra)
        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        val mode = peliasFeature.properties.mode
        assertNotNull(mode)
        assertEquals(listOf("bus" to "localBus"), mode)
    }

    @Test
    fun `transformFeature excludes extra when no transport data`() {
        val extra =
            Extra(
                id = "OSM:TopographicPlace:123",
                tags = "legacy.source.osm,legacy.layer.venue",
            )
        val photonFeature = createPhotonFeature(name = "POI", extra = extra)
        val peliasFeature = PeliasResultTransformer.transformFeature(photonFeature, null)

        assertNull(peliasFeature.properties.mode)
    }

    @ParameterizedTest
    @CsvSource(
        "Central Station | Oslo | Central Station, Oslo",
        "Oslo | Oslo | Oslo",
        " | Oslo | Oslo", // empty/null name
        delimiter = '|',
    )
    fun `transformFeature creates label correctly`(name: String?, locality: String, expectedLabel: String) {
        val extra = Extra(id = "OSM:TopographicPlace:123", locality = locality, tags = "legacy.source.osm,legacy.layer.venue")
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
        val extra = Extra(id = "OSM:TopographicPlace:123", tags = "legacy.source.osm,legacy.layer.venue")
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
    fun `bbox spans the returned features only, not the pruning headroom`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.0, 60.0)),
                            properties = PhotonProperties(name = "A", extra = Extra(id = "1")),
                        ),
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.1, 60.1)),
                            properties = PhotonProperties(name = "B", extra = Extra(id = "2")),
                        ),
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(30.0, 70.0)),
                            properties = PhotonProperties(name = "beyond size", extra = Extra(id = "3")),
                        ),
                    ),
            )

        val result = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("x", size = 2))

        assertEquals(2, result.features.size)
        assertEquals(
            listOf("10.000000", "60.000000", "10.100000", "60.100000"),
            result.bbox?.map { it.toPlainString() },
        )
    }

    @Test
    fun `bbox handles negative coordinates`() {
        // Regression: the old Double.MIN_VALUE sentinel broke max-calculation for
        // negative lon/lat (Jan Mayen, lon ~-8.5, is in the Norway OSM extract).
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(-9.1, 70.9)),
                            properties = PhotonProperties(name = "A", extra = Extra(id = "1")),
                        ),
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(-8.2, 71.1)),
                            properties = PhotonProperties(name = "B", extra = Extra(id = "2")),
                        ),
                    ),
            )

        val bbox = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("x")).bbox

        requireNotNull(bbox)
        assertEquals(0, java.math.BigDecimal("-9.1").compareTo(bbox[0])) // minLon
        assertEquals(0, java.math.BigDecimal("70.9").compareTo(bbox[1])) // minLat
        assertEquals(0, java.math.BigDecimal("-8.2").compareTo(bbox[2])) // maxLon
        assertEquals(0, java.math.BigDecimal("71.1").compareTo(bbox[3])) // maxLat
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
        val extra = Extra(id = "OSM:TopographicPlace:42", tags = "legacy.source.osm,legacy.layer.venue", description = "foran Oslo S")
        val photonResult = createPhotonResult(name = "Oslo S", extra = extra)
        val result = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("foo"))
        val json = jacksonMapper.writeValueAsString(result)

        assertContains(json, """"description":[{"nor":"foran Oslo S"}]""")
    }

    @Test
    fun `transformDescription handles null description`() {
        val extra = Extra(id = "OSM:TopographicPlace:42", tags = "legacy.source.osm,legacy.layer.venue", description = null)
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
        val extra = Extra(id = "OSM:TopographicPlace:42", tags = "legacy.source.osm,legacy.layer.venue", description = description)
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

    @Test
    fun `street falls back to the NOT_AN_ADDRESS sentinel when the doc has no street`() {
        val feature =
            createPhotonFeature(
                extra =
                    Extra(
                        id = "OSM:TopographicPlace:113972189",
                        tags = "legacy.source.osm,legacy.layer.venue",
                    ),
            )

        val props = PeliasResultTransformer.transformFeature(feature, null).properties

        assertEquals("NOT_AN_ADDRESS-OSM:TopographicPlace:113972189", props.street)
    }

    private fun createPhotonFeature(
        name: String? = "Test",
        coordinates: List<Double> = listOf(10.0, 60.0),
        extra: Extra = Extra(id = "OSM:TopographicPlace:42", tags = "legacy.source.osm,legacy.layer.venue"),
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

    @Test
    fun `parseAndTransform includes error message when PhotonResult has message`() {
        val photonResult =
            PhotonResult(
                message = "Unknown query parameter 'suggest_addresses'. Allowed parameters are: [include, location_bias_scale, ...]",
                features = emptyList(),
            )
        val request = PeliasAutocompleteRequest("foo")

        val result = PeliasResultTransformer.parseAndTransform(photonResult, request)

        assertNotNull(result.geocoding.errors)
        assertEquals(1, result.geocoding.errors.size)
        assertEquals(
            "Unknown query parameter 'suggest_addresses'. Allowed parameters are: [include, location_bias_scale, ...]",
            result.geocoding.errors.first(),
        )
    }

    @Test
    fun `parseAndTransform has no errors when PhotonResult has no message`() {
        val photonResult = createPhotonResult(name = "Oslo")
        val request = PeliasAutocompleteRequest("foo")

        val result = PeliasResultTransformer.parseAndTransform(photonResult, request)

        assertNull(result.geocoding.errors)
    }

    @ParameterizedTest
    @CsvSource(
        // Old Photon format (no prefix) → unchanged
        "12345, 12345",
        // New KVE prefix → stripped
        "KVE:PostalAddress:12345, 12345",
        // Old OSM format → unchanged
        "OSM:TopographicPlace:123, OSM:TopographicPlace:123",
        // New OSM format → reverted to old
        "OSM:TopographicPlace:123, OSM:TopographicPlace:123",
        // NSR ids → unchanged
        "RUT:StopPlace:337, RUT:StopPlace:337",
        delimiter = ',',
    )
    fun `v2 IDs are normalized for backward compatibility`(photonId: String, expectedId: String) {
        val extra = Extra(id = photonId.trim(), source = Source.OSM, tags = "legacy.source.osm,legacy.layer.venue")
        val photonResult = createPhotonResult(extra = extra)
        val request = PeliasAutocompleteRequest("test")

        val result = PeliasResultTransformer.parseAndTransform(photonResult, request)

        val props = result.features.first().properties
        assertEquals(expectedId.trim(), props.id)
        assertEquals(expectedId.trim(), props.source_id)
    }

    @Test
    fun `stedsnavn place is dropped when a GOSP shares its name and municipality`() {
        // Sogndal is a "tettsted", not a "by", so a category-specific filter missed it.
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospPhotonFeature("Sogndal"),
                        stedsnavnPhotonFeature("Sogndal", "tettsted"),
                        stedsnavnPhotonFeature("Sogndalsfjøra", "tettsted"),
                    ),
            )
        val features = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("sogndal")).features
        assertEquals(listOf("Sogndal", "Sogndalsfjøra"), features.map { it.properties.name })
        assertEquals(listOf(GOSP), features.first().properties.category)
        // The v2 wire fields cannot tell the two apart, hence the Photon-side source check.
        assertEquals(listOf("address", "address"), features.map { it.properties.layer })
    }

    @Test
    fun `stedsnavn place is dropped when only the locality names differ`() {
        // Bilingual kommunenavn: the place says "Harstad - Hárstták" where the GOSP says "Harstad".
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospPhotonFeature("Harstad", locality = "Harstad", localityGid = "KVE:TopographicPlace:5503"),
                        stedsnavnPhotonFeature(
                            "Harstad",
                            "by",
                            locality = "Harstad - Hárstták",
                            localityGid = "KVE:TopographicPlace:5503",
                        ),
                    ),
            )
        val features = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("harstad")).features
        assertEquals(1, features.size)
    }

    @Test
    fun `stedsnavn place is kept when the GOSP is in another municipality`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospPhotonFeature("Sandvika", localityGid = "KVE:TopographicPlace:3024"),
                        stedsnavnPhotonFeature("Sandvika", "tettsted", localityGid = "KVE:TopographicPlace:1868"),
                    ),
            )
        val features = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("sandvika")).features
        assertEquals(2, features.size)
    }

    @Test
    fun `dropping a duplicate makes room for the next result within size`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospPhotonFeature("Sogndal"),
                        stedsnavnPhotonFeature("Sogndal", "tettsted"),
                        stedsnavnPhotonFeature("Sogndalsfjøra", "tettsted"),
                    ),
            )
        val features = PeliasResultTransformer.parseAndTransform(photonResult, PeliasAutocompleteRequest("sogndal", size = 2)).features
        assertEquals(listOf("Sogndal", "Sogndalsfjøra"), features.map { it.properties.name })
    }

    @Test
    fun `place and reverse keep the duplicate - only autocomplete prunes`() {
        // An id lookup must return every id it was asked for, and reverse has no headroom to backfill from.
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospPhotonFeature("Sogndal"),
                        stedsnavnPhotonFeature("Sogndal", "tettsted"),
                    ),
            )
        val place =
            PeliasResultTransformer.parseAndTransform(
                photonResult,
                PeliasPlaceRequest(ids = listOf("NSR:GroupOfStopPlaces:1", "whosonfirst:address:1")),
            )
        assertEquals(2, place.features.size)

        val reverse = PeliasResultTransformer.parseAndTransform(photonResult, PeliasReverseRequest(lat = 61.2, lon = 7.1, size = 2, multiModal = "parent"))
        assertEquals(2, reverse.features.size)
    }

    private fun gospPhotonFeature(
        name: String,
        locality: String = "Sogndal",
        localityGid: String = "KVE:TopographicPlace:4640",
    ) = createPhotonFeature(
        name = name,
        extra =
            Extra(
                id = "NSR:GroupOfStopPlaces:1",
                source = Source.NSR,
                locality = locality,
                locality_gid = localityGid,
                tags = "${Category.LAYER_GOSP},legacy.category.$GOSP,legacy.source.whosonfirst,legacy.layer.address",
            ),
    )

    private fun stedsnavnPhotonFeature(
        name: String,
        type: String,
        locality: String = "Sogndal",
        localityGid: String = "KVE:TopographicPlace:4640",
    ) = createPhotonFeature(
        name = name,
        extra =
            Extra(
                id = "KVE:PlaceName:1",
                source = Source.KARTVERKET_STEDSNAVN,
                locality = locality,
                locality_gid = localityGid,
                tags = "legacy.category.$type,legacy.source.whosonfirst,legacy.layer.address",
            ),
    )

    private fun createPhotonResult(
        name: String? = "Test",
        coordinates: List<Double> = listOf(10.0, 60.0),
        extra: Extra = Extra(id = "OSM:TopographicPlace:42", tags = "legacy.source.osm,legacy.layer.venue"),
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
