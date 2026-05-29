package no.entur.geocoder.proxy.v3

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Extra
import no.entur.geocoder.proxy.common.Source
import no.entur.geocoder.proxy.photon.PhotonResult
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonGeometry
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonProperties
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

    @Test
    fun `display name appends locality`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, locality = "Oslo"))
        assertEquals("Test, Oslo", place.name.display)
    }

    @Test
    fun `label is null when same as default name`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, alt_name = "Test"))
        assertNull(place.name.label)
    }

    @Test
    fun `label is set when different from default name`() {
        val place = transformSingle(Extra(id = "OSM:TopographicPlace:42", source = Source.OSM, alt_name = "Alias"))
        assertEquals("Alias", place.name.label)
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
                            properties = PhotonProperties(
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
        assertEquals(java.math.BigDecimal("1.001"), place.distance)
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
