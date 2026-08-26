package no.entur.geocoder.proxy.v3

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Extra
import no.entur.geocoder.proxy.common.Source
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonGeometry
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonProperties
import java.math.BigDecimal
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class V3ResultTransformerTest {
    @ParameterizedTest
    @CsvSource(
        "kartverket-matrikkelenadresse, , , , address",
        "kartverket-stedsnavn, , , , place",
        "nsr, , stop_position, , stopPlace",
        "nsr, , railway_station, , stopPlace",
        "nsr, , yes, legacy.layer.venue, stopPlace",
        "nsr, , , ${Category.LAYER_GOSP}, groupOfStopPlaces",
        "openstreetmap, highway, , , street",
        "openstreetmap, amenity, , , poi",
    )
    fun `determineLayer maps source and osm tags to correct layer`(
        source: String,
        osmKey: String?,
        osmValue: String?,
        tags: String?,
        expectedLayer: String,
    ) {
        val place =
            transformSingle(
                extra = Extra(id = "OSM:TopographicPlace:42", source = source, tags = tags),
                osmKey = osmKey,
                osmValue = osmValue,
            )
        assertEquals(V3Result.Layer.valueOf(expectedLayer), place.layer)
    }

    @ParameterizedTest
    @CsvSource(
        "parent, parent",
        "child, child",
        "standalone, standalone",
    )
    fun `stopPlaceRole is read from the extra stop_place_role field`(
        raw: String,
        expected: String,
    ) {
        val place = transformSingle(Extra(id = "NSR:StopPlace:1", source = Source.NSR, stop_place_role = raw))
        assertEquals(V3Result.StopPlaceRole.valueOf(expected), place.stopPlaceRole)
    }

    @Test
    fun `stopPlaceRole is omitted when extra has no stop_place_role (e g pre-reindex documents)`() {
        val place = transformSingle(Extra(id = "NSR:StopPlace:1", source = Source.NSR, stop_place_role = null))
        assertNull(place.stopPlaceRole)
    }

    @Test
    fun `unknown stop_place_role value maps to null rather than throwing`() {
        val place = transformSingle(Extra(id = "NSR:StopPlace:1", source = Source.NSR, stop_place_role = "bogus"))
        assertNull(place.stopPlaceRole)
    }

    @Test
    fun `StopPlaceRole enum names match the converter's emitted strings`() {
        // Cross-repo contract: must match what the converter writes (StopPlaceRole::as_str);
        // a rename on either side would silently null every role.
        assertEquals(
            listOf("parent", "child", "standalone"),
            V3Result.StopPlaceRole.entries.map { it.name },
        )
    }

    @Test
    fun `display name appends locality`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, locality = "Oslo"))
        assertEquals("Test, Oslo", place.names.display)
    }

    @Test
    fun `label is null when same as default name`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, alt_name = "Test"))
        assertNull(place.names.label)
    }

    @Test
    fun `label is set when different from default name`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, alt_name = "Alias"))
        assertEquals("Alias", place.names.label)
    }

    @Test
    fun `transport modes are parsed`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.NSR, transport_mode = "bus:localBus;rail"))
        val modes = place.transportModes
        assertEquals(2, modes?.size)
        assertEquals("bus", modes?.get(0)?.mode)
        assertEquals("localBus", modes?.get(0)?.subMode)
        assertEquals("rail", modes?.get(1)?.mode)
        assertNull(modes?.get(1)?.subMode)
    }

    @Test
    fun `address is null when no address fields present`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM))
        assertNull(place.address)
    }

    @Test
    fun `interim OSM PointOfInterest id is canonicalised to TopographicPlace`() {
        // Interim: index docs from a previous converter run carry OSM:PointOfInterest:N.
        val place = transformSingle(Extra(id = "OSM:PointOfInterest:42", source = Source.OSM))
        assertEquals("OSM:TopographicPlace:42", place.id)
    }

    @Test
    fun `KVE PlaceName id is passed through unchanged`() {
        val place =
            transformSingle(
                Extra(id = "KVE:PlaceName:434810", source = Source.KARTVERKET_STEDSNAVN),
            )
        assertEquals("KVE:PlaceName:434810", place.id)
    }

    @Test
    fun `address is built from photon properties and extra`() {
        val place =
            transformSingle(
                extra = Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, locality = "Oslo", county_gid = "03"),
                street = "Storgata",
                housenumber = "1",
            )
        assertEquals("Storgata", place.address?.streetName)
        assertEquals("1", place.address?.houseNumber)
        assertEquals("Oslo", place.address?.locality)
        assertEquals("03", place.address?.countyId)
    }

    @Test
    fun `reverse fills distance in km from query point`() {
        // Feature at (60.0, 10.0), query point ~1 km north at (60.009, 10.0)
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.0, 60.0)),
                            properties =
                                PhotonProperties(
                                    name = "Test",
                                    extra = Extra(id = "OSM:TopographicPlace:42", source = Source.OSM),
                                ),
                        ),
                    ),
            )
        val req = V3ReverseRequest(lat = 60.009, lon = 10.0)
        val place =
            V3ResultTransformer
                .parseAndTransform(photonResult, req)
                .features
                .first()
                .properties
        assertEquals(BigDecimal("1.001"), place.distance)
    }

    @Test
    fun `autocomplete omits distance`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM))
        assertNull(place.distance)
    }

    @Test
    fun `description parses lang-prefixed entries into a map`() {
        val place =
            transformSingle(
                Extra(
                    id = "OSM:TopographicPlace:42",
                    source = Source.OSM,
                    description = "nor:Nasjonalteater;eng:National theatre",
                ),
            )
        assertEquals(mapOf("nor" to "Nasjonalteater", "eng" to "National theatre"), place.description)
    }

    @Test
    fun `description without lang prefix defaults to nor`() {
        val place =
            transformSingle(
                Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, description = "Bare tekst"),
            )
        assertEquals(mapOf("nor" to "Bare tekst"), place.description)
    }

    @Test
    fun `description with stray colon does not get mis-parsed as lang prefix`() {
        // Free text mentioning "tel:" or "www:" must not flip the parser into lang-prefix mode.
        val place =
            transformSingle(
                Extra(
                    id = "OSM:TopographicPlace:42",
                    source = Source.OSM,
                    description = "Kafe ved tel:12345678",
                ),
            )
        assertEquals(mapOf("nor" to "Kafe ved tel:12345678"), place.description)
    }

    @Test
    fun `description omitted when absent`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM))
        assertNull(place.description)
    }

    @Test
    fun `boroughId is passed through unchanged`() {
        val place =
            transformSingle(
                Extra(
                    id = "KVE:PostalAddress:42",
                    source = Source.KARTVERKET_ADRESSE,
                    locality = "Oslo",
                    borough_gid = "KVE:Borough:34200205",
                ),
            )
        assertEquals("KVE:Borough:34200205", place.address?.boroughId)
    }

    @Test
    fun `interim bare borough id is canonicalised to KVE Borough`() {
        // Interim: index docs from a previous converter run carry borough:N.
        val place =
            transformSingle(
                Extra(
                    id = "KVE:PostalAddress:42",
                    source = Source.KARTVERKET_ADRESSE,
                    locality = "Oslo",
                    borough_gid = "borough:34200205",
                ),
            )
        assertEquals("KVE:Borough:34200205", place.address?.boroughId)
    }

    @Test
    fun `interim bare numeric borough id is canonicalised to KVE Borough`() {
        // Interim: oldest index docs carry the pre-namespacing bare numeric.
        val place =
            transformSingle(
                Extra(
                    id = "KVE:PostalAddress:42",
                    source = Source.KARTVERKET_ADRESSE,
                    locality = "Oslo",
                    borough_gid = "34200205",
                ),
            )
        assertEquals("KVE:Borough:34200205", place.address?.boroughId)
    }

    @Test
    fun `stopPlaceTypes are deduplicated`() {
        // The converter emits one entry per child stop, so a parent with several
        // bus children repeats onstreetBus.
        val place =
            transformSingle(
                Extra(
                    id = "NSR:StopPlace:1",
                    source = Source.NSR,
                    stop_place_type = "onstreetBus;onstreetBus;railStation;onstreetBus",
                ),
            )
        assertEquals(listOf("onstreetBus", "railStation"), place.stopPlaceTypes)
    }

    @Test
    fun `categories are deduplicated`() {
        val place =
            transformSingle(
                Extra(
                    id = "OSM:TopographicPlace:42",
                    source = Source.OSM,
                    // amenity=hospital + healthcare=hospital both reach the index as
                    // legacy.category.hospital.
                    tags = "legacy.source.whosonfirst,legacy.category.poi,legacy.category.hospital,legacy.category.hospital",
                ),
            )
        assertEquals(listOf("poi", "hospital"), place.categories)
    }

    @Test
    fun `stedsnavn place is dropped when a GOSP shares its name and municipality`() {
        // Sogndal and Asker are "tettsted", not "by", so a category-specific filter missed them.
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospFeature("Sogndal"),
                        placeFeature("Sogndal", "tettsted"),
                        placeFeature("Sogndalsfjøra", "tettsted"),
                    ),
            )
        val features = transformAutocomplete(photonResult)
        assertEquals(listOf("Sogndal", "Sogndalsfjøra"), features.map { it.properties.names.default })
        assertEquals(V3Result.Layer.groupOfStopPlaces, features.first().properties.layer)
    }

    @Test
    fun `stedsnavn place is dropped when only the locality names differ`() {
        // Bilingual kommunenavn: the place says "Harstad - Hárstták" where the GOSP says "Harstad",
        // so the municipality has to be compared by id.
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospFeature("Harstad", locality = "Harstad", localityGid = "KVE:TopographicPlace:5503"),
                        placeFeature("Harstad", "by", locality = "Harstad - Hárstták", localityGid = "KVE:TopographicPlace:5503"),
                    ),
            )
        assertEquals(1, transformAutocomplete(photonResult).size)
    }

    @Test
    fun `stedsnavn place is kept when the GOSP is in another municipality`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospFeature("Sandvika", localityGid = "KVE:TopographicPlace:3024"),
                        placeFeature("Sandvika", "tettsted", localityGid = "KVE:TopographicPlace:1868"),
                    ),
            )
        assertEquals(2, transformAutocomplete(photonResult).size)
    }

    @Test
    fun `stedsnavn place is kept when no GOSP shares its name`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospFeature("Bergen"),
                        placeFeature("Sogndal", "tettsted"),
                    ),
            )
        assertEquals(2, transformAutocomplete(photonResult).size)
    }

    @Test
    fun `a stop place sharing the GOSP name is kept`() {
        // Only stedsnavn places are duplicates of a GOSP; its member stops are not.
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospFeature("Sogndal"),
                        stopPlaceFeature("Sogndal"),
                        placeFeature("Sogndal", "tettsted"),
                    ),
            )
        val features = transformAutocomplete(photonResult)
        assertEquals(
            listOf(V3Result.Layer.groupOfStopPlaces, V3Result.Layer.stopPlace),
            features.map { it.properties.layer },
        )
    }

    @Test
    fun `stedsnavn place is dropped even when it outranks the GOSP`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        placeFeature("Sogndal", "tettsted"),
                        gospFeature("Sogndal"),
                    ),
            )
        val features = transformAutocomplete(photonResult)
        assertEquals(V3Result.Layer.groupOfStopPlaces, features.single().properties.layer)
    }

    @Test
    fun `dropping a duplicate makes room for the next result within the limit`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        gospFeature("Sogndal"),
                        placeFeature("Sogndal", "tettsted"),
                        placeFeature("Sogndalsfjøra", "tettsted"),
                    ),
            )
        val features = V3ResultTransformer.parseAndTransform(photonResult, V3AutocompleteRequest(q = "sogndal", limit = 2)).features
        assertEquals(listOf("Sogndal", "Sogndalsfjøra"), features.map { it.properties.names.default })
    }

    private fun transformAutocomplete(photonResult: PhotonResult) =
        V3ResultTransformer.parseAndTransform(photonResult, V3AutocompleteRequest(q = "x")).features

    @Test
    fun `filters echo includes stopPlaceTypes and omits empty filters`() {
        val photonResult = PhotonResult(features = emptyList())

        val withFilter =
            V3ResultTransformer.parseAndTransform(
                photonResult,
                V3ReverseRequest(lat = 59.91, lon = 10.75, stopPlaceTypes = listOf("railStation")),
            )
        assertEquals(
            listOf("railStation"),
            withFilter.metadata.query.filters
                ?.stopPlaceTypes,
        )

        val withoutFilters =
            V3ResultTransformer.parseAndTransform(
                photonResult,
                V3ReverseRequest(lat = 59.91, lon = 10.75),
            )
        assertNull(withoutFilters.metadata.query.filters)
    }

    @Test
    fun `feature bbox is mapped from photon extent`() {
        // Photon extent order is [minLon, maxLat, maxLon, minLat] (NW + SE);
        // GeoJSON bbox is [minLon, minLat, maxLon, maxLat].
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.75, 59.91)),
                            properties =
                                PhotonProperties(
                                    name = "Karl Johans gate",
                                    extent = listOf(10.73, 59.92, 10.76, 59.91),
                                    extra = Extra(id = "KVE:TopographicPlace:0301-Karl Johans gate", source = Source.KARTVERKET_ADRESSE),
                                ),
                        ),
                    ),
            )
        val feature = V3ResultTransformer.parseAndTransform(photonResult, V3AutocompleteRequest(q = "x")).features.first()
        assertEquals(
            listOf(BigDecimal("10.730000"), BigDecimal("59.910000"), BigDecimal("10.760000"), BigDecimal("59.920000")),
            feature.bbox,
        )
    }

    @Test
    fun `feature bbox is absent without photon extent`() {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.0, 60.0)),
                            properties =
                                PhotonProperties(
                                    name = "Test",
                                    extra = Extra(id = "OSM:TopographicPlace:42", source = Source.OSM),
                                ),
                        ),
                    ),
            )
        val feature = V3ResultTransformer.parseAndTransform(photonResult, V3AutocompleteRequest(q = "x")).features.first()
        assertNull(feature.bbox)
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
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(-21.9, 64.1)),
                            properties = PhotonProperties(name = "A", extra = Extra(id = "OSM:TopographicPlace:1", source = Source.OSM)),
                        ),
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(-18.1, 65.7)),
                            properties = PhotonProperties(name = "B", extra = Extra(id = "OSM:TopographicPlace:2", source = Source.OSM)),
                        ),
                    ),
            )
        val bbox = V3ResultTransformer.parseAndTransform(photonResult, V3AutocompleteRequest(q = "x")).bbox
        requireNotNull(bbox)
        assertEquals(BigDecimal("-21.900000"), bbox[0]) // minLon
        assertEquals(BigDecimal("64.100000"), bbox[1]) // minLat
        assertEquals(BigDecimal("-18.100000"), bbox[2]) // maxLon
        assertEquals(BigDecimal("65.700000"), bbox[3]) // maxLat
    }

    private fun gospFeature(
        name: String,
        locality: String = "Sogndal",
        localityGid: String = "KVE:TopographicPlace:4640",
    ) = feature(
        name,
        Extra(
            id = "NSR:GroupOfStopPlaces:1",
            source = Source.NSR,
            locality = locality,
            locality_gid = localityGid,
            tags = "${Category.LAYER_GOSP},legacy.category.GroupOfStopPlaces",
        ),
    )

    private fun stopPlaceFeature(name: String, localityGid: String = "KVE:TopographicPlace:4640") =
        feature(
            name,
            Extra(
                id = "NSR:StopPlace:1",
                source = Source.NSR,
                locality = "Sogndal",
                locality_gid = localityGid,
                tags = "${Category.LAYER_STOP_PLACE},legacy.layer.venue",
            ),
        )

    private fun placeFeature(
        name: String,
        type: String,
        locality: String = "Sogndal",
        localityGid: String = "KVE:TopographicPlace:4640",
    ) = feature(
        name,
        Extra(
            id = "KVE:PlaceName:1",
            source = Source.KARTVERKET_STEDSNAVN,
            locality = locality,
            locality_gid = localityGid,
            tags = "legacy.category.$type,legacy.source.whosonfirst,legacy.layer.address",
        ),
    )

    private fun feature(name: String, extra: Extra) =
        PhotonFeature(
            geometry = PhotonGeometry(type = "Point", coordinates = listOf(7.1, 61.2)),
            properties = PhotonProperties(name = name, extra = extra),
        )

    private fun transformSingle(
        extra: Extra,
        osmKey: String? = null,
        osmValue: String? = null,
        street: String? = null,
        housenumber: String? = null,
    ): V3Result.Place {
        val photonResult =
            PhotonResult(
                features =
                    listOf(
                        PhotonFeature(
                            geometry = PhotonGeometry(type = "Point", coordinates = listOf(10.0, 60.0)),
                            properties =
                                PhotonProperties(
                                    name = "Test",
                                    osm_key = osmKey,
                                    osm_value = osmValue,
                                    street = street,
                                    housenumber = housenumber,
                                    extra = extra,
                                ),
                        ),
                    ),
            )
        val req = V3AutocompleteRequest(q = "test")
        return V3ResultTransformer
            .parseAndTransform(photonResult, req)
            .features
            .first()
            .properties
    }
}
