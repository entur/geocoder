package no.entur.geocoder.common

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals

class GeoTest {
    @Test
    fun `convert UTM33 to LatLon`() {
        val utm = UtmCoordinate(310012.98, 6770754.69)

        val coord = Geo.convertUtm33ToLatLon(utm)

        assertEquals(61.025715, coord.lat, 0.00001)
        assertEquals(11.483291, coord.lon, 0.00001)
    }

    @Test
    fun `haversine distance calculation`() {
        val coord1 = Coordinate(60.39126, 5.32205)
        val coord2 = Coordinate(59.91386, 10.75224)

        val distance = Geo.haversineDistance(coord1, coord2)

        assertEquals(305072.3952385879, distance) // Distance in meters
    }

    @Test
    fun `peliasScaleToPhotonZoom with very local scale`() {
        // Pelias scale 5km → targetRadius = (5+1)/2.5 = 2.4km → zoom 14
        val zoom = Geo.peliasScaleToPhotonZoom(5)
        assertEquals(14, zoom) // Photon radius ~4km
    }

    @Test
    fun `peliasScaleToPhotonZoom with local scale`() {
        // Pelias scale 10km → targetRadius = (10+1)/2.5 = 4.4km → zoom 13
        val zoom = Geo.peliasScaleToPhotonZoom(10)
        assertEquals(13, zoom) // Photon radius ~8km
    }

    @Test
    fun `peliasScaleToPhotonZoom with district scale`() {
        // Pelias scale 25km → targetRadius = (25+1)/2.5 = 10.4km → zoom 12
        val zoom = Geo.peliasScaleToPhotonZoom(25)
        assertEquals(12, zoom) // Photon radius ~16km
    }

    @Test
    fun `peliasScaleToPhotonZoom with default Pelias scale`() {
        // Pelias default scale 50km → targetRadius = (50+1)/2.5 = 20.4km → zoom 11
        val zoom = Geo.peliasScaleToPhotonZoom(50)
        assertEquals(11, zoom) // Photon radius ~32km
    }

    @Test
    fun `peliasScaleToPhotonZoom with regional scale`() {
        // Pelias scale 100km → targetRadius = (100+1)/2.5 = 40.4km → zoom 10
        val zoom = Geo.peliasScaleToPhotonZoom(100)
        assertEquals(10, zoom) // Photon radius ~64km
    }

    @Test
    fun `peliasScaleToPhotonZoom with multi-region scale`() {
        // Pelias scale 200km → targetRadius = (200+1)/2.5 = 80.4km → zoom 9
        val zoom = Geo.peliasScaleToPhotonZoom(200)
        assertEquals(9, zoom) // Photon radius ~128km
    }

    @Test
    fun `peliasScaleToPhotonZoom with national scale`() {
        // Pelias scale 500km → targetRadius = (500+1)/2.5 = 200.4km → zoom 8
        val zoom = Geo.peliasScaleToPhotonZoom(500)
        assertEquals(8, zoom) // Photon radius ~256km
    }

    @Test
    fun `peliasScaleToPhotonZoom with explicit Entur default scale`() {
        // Entur's Pelias default: scale=2500km (very broad, minimal location bias)
        // targetRadius = (2500+1)/2.5 = 1000.4km → zoom = 6
        val zoom = Geo.peliasScaleToPhotonZoom(2500)
        assertEquals(6, zoom) // Photon radius ~512km - appropriately broad
    }

    @Test
    fun `peliasScaleToPhotonZoom produces reasonable Photon radius`() {
        // Verify that the conversion produces Photon radii in the expected ballpark
        // For Pelias scale=50km, we expect Photon radius to be larger (due to exponential vs linear decay)
        val zoom = Geo.peliasScaleToPhotonZoom(50)
        val photonRadius = (1 shl (18 - zoom)) * 0.25

        // Photon radius should be larger than Pelias scale but not excessively so
        // For scale=50km, we expect radius around 20-40km
        assert(photonRadius in 20.0..40.0) {
            "For Pelias scale=50km, expected Photon radius in [20,40]km range, got ${photonRadius}km"
        }
    }

    @ParameterizedTest
    @MethodSource("borderCountryCoordinates")
    fun `getCountryCode returns correct country for border coordinates`(testCase: Triple<Double, Double, Country>) {
        val (lat, lon, expectedCode) = testCase
        val actualCode = Geo.getCountry(Coordinate(lat, lon))
        assertEquals(expectedCode, actualCode, "Expected $expectedCode for ($lat, $lon), got $actualCode")
    }

    companion object {
        @JvmStatic
        fun borderCountryCoordinates() =
            listOf(
                Triple(59.91386, 10.75224, Country.no),
                // Norway–Sweden
                Triple(59.47787, 11.71967, Country.no),
                Triple(59.49251, 11.78009, Country.se),
                // Sweden–Finland
                Triple(65.8481, 24.1466, Country.fi),
                Triple(65.8350, 24.1300, Country.se),
                // Denmark–Sweden near Øresund Bridge
                Triple(55.6210, 12.6500, Country.dk),
                Triple(55.5700, 12.9800, Country.se),
                // Denmark–Germany near Kruså/Padborg
                Triple(54.8205, 9.3980, Country.de),
                Triple(54.8675, 9.4175, Country.dk),
                // Oscar Torp-heimen
                Triple(59.09735, 11.25770, Country.no),
                // Tull Customs, Strömstad
                Triple(59.08674, 11.24925, Country.se),
                // Østerbyvegen
                Triple(60.146179, 12.518511, Country.no)
            )
    }
}
