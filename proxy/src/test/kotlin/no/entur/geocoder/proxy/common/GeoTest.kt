package no.entur.geocoder.proxy.common

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
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

    @ParameterizedTest
    @CsvSource(
        "5, 14", // very local: biasRadius ~2 km
        "10, 13", // local: biasRadius ~5 km
        "25, 12", // district: biasRadius ~11 km
        "50, 11", // biasRadius ~25 km
        "100, 10", // regional: biasRadius ~55 km
        "200, 9", // multi-region: biasRadius ~120 km
        "500, 9", // saturating: effective ~280 km -> biasRadius ~120 km
        "2500, 9", // FocusDefaults.SCALE_KM, saturated near asymptote 300 -> biasRadius ~120 km
    )
    fun `peliasScaleToPhotonZoom converts scale to zoom level`(peliasScale: Int, expectedZoom: Int) {
        val zoom = Geo.peliasScaleToPhotonZoom(peliasScale)
        assertEquals(expectedZoom, zoom)
    }

    @ParameterizedTest
    @CsvSource(
        "59.91386, 10.75224, no",
        "59.47787, 11.71967, no", // Norway–Sweden border (NO side)
        "59.49251, 11.78009, se", // Norway–Sweden border (SE side)
        "65.8481, 24.1466, fi", // Sweden–Finland border (FI side)
        "65.8350, 24.1300, se", // Sweden–Finland border (SE side)
        "55.6210, 12.6500, dk", // Denmark–Sweden Øresund (DK side)
        "55.5700, 12.9800, se", // Denmark–Sweden Øresund (SE side)
        "54.8205, 9.3980, de", // Denmark–Germany Kruså (DE side)
        "54.8675, 9.4175, dk", // Denmark–Germany Kruså (DK side)
        "59.09735, 11.25770, no", // Oscar Torp-heimen (NO)
        "59.08674, 11.24925, se", // Tull Customs, Strömstad (SE)
        "60.146179, 12.518511, no", // Østerbyvegen (NO)
    )
    fun `getCountryCode returns correct country for border coordinates`(lat: Double, lon: Double, countryCode: String) {
        val expectedCountry = Country.parse(countryCode)
        val actualCountry = Geo.getCountry(Coordinate(lat, lon))
        assertEquals(expectedCountry, actualCountry, "Expected $expectedCountry for ($lat, $lon), got $actualCountry")
    }
}
