package no.entur.geocoder.proxy.pelias

import no.entur.geocoder.proxy.pelias.PeliasAutocompleteParams.FocusParams
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import java.math.BigDecimal
import kotlin.test.Test

class PeliasResultTransformerTest {
    @Test
    fun `should allow for pelias distance fudge`() {
        // Oslo
        val geometry =
            PhotonGeometry(
                type = "Point",
                coordinates =
                    listOf(
                        BigDecimal("10.7522"), // lon
                        BigDecimal("59.9139"), // lat
                    ),
            )

        // Bergen
        val focus =
            FocusParams(
                lat = BigDecimal("60.39299"),
                lon = BigDecimal("5.32415"),
                null,
                null,
            )

        val distance = PeliasResultTransformer.calculateDistanceKm(geometry, focus.lat, focus.lon)

        if (distance != null) {
            assertEquals(305.322.toBigDecimal(), distance)
        } else {
            fail("Distance should not be null")
        }
    }
}
