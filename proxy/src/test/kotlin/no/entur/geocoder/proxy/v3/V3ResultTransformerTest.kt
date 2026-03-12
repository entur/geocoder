package no.entur.geocoder.proxy.v3

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Source
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
        "nsr, , stop_position, , stopPlace",
        "nsr, , railway_station, , stopPlace",
        "nsr, , , ${Category.OSM_GOSP}, groupOfStopPlaces",
        "nsr, , other, , poi",
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
        val place = transformSingle(
            extra = Extra(id = "OSM:PointOfInterest:42", source = source, tags = tags),
            osmKey = osmKey,
            osmValue = osmValue,
        )
        assertEquals(V3Result.Layer.valueOf(expectedLayer), place.layer)
    }

    @Test
    fun `display name appends locality`() {
        val place = transformSingle(Extra(id = "OSM:PointOfInterest:42", source = Source.OSM, locality = "Oslo"))
        assertEquals("Test, Oslo", place.name.display)
    }

    @Test
    fun `label is null when same as default name`() {
        val place = transformSingle(Extra(id = "OSM:PointOfInterest:42", source = Source.OSM, alt_name = "Test"))
        assertNull(place.name.label)
    }

    @Test
    fun `label is set when different from default name`() {
        val place = transformSingle(Extra(id = "OSM:PointOfInterest:42", source = Source.OSM, alt_name = "Alias"))
        assertEquals("Alias", place.name.label)
    }

    @Test
    fun `transport modes are parsed`() {
        val place = transformSingle(Extra(id = "OSM:PointOfInterest:42", source = Source.NSR, transport_mode = "bus:localBus;rail"))
        val modes = place.transportModes
        assertEquals(2, modes?.size)
        assertEquals("bus", modes?.get(0)?.mode)
        assertEquals("localBus", modes?.get(0)?.subMode)
        assertEquals("rail", modes?.get(1)?.mode)
        assertNull(modes?.get(1)?.subMode)
    }

    @Test
    fun `address is null when no address fields present`() {
        val place = transformSingle(Extra(id = "OSM:PointOfInterest:42", source = Source.OSM))
        assertNull(place.address)
    }

    @Test
    fun `address is built from photon properties and extra`() {
        val place = transformSingle(
            extra = Extra(id = "OSM:PointOfInterest:42", source = Source.OSM, locality = "Oslo", county_gid = "03"),
            street = "Storgata",
            housenumber = "1",
        )
        assertEquals("Storgata", place.address?.streetName)
        assertEquals("1", place.address?.houseNumber)
        assertEquals("Oslo", place.address?.locality)
        assertEquals("03", place.address?.countyId)
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
        val req = V3AutocompleteRequest(query = "test")
        return V3ResultTransformer.parseAndTransform(photonResult, req).features.first().properties
    }
}
