package no.entur.geocoder.proxy.v3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class V3AutocompleteRequestTest {
    @Test
    fun `photonZoom returns null when no focus point`() {
        val req = V3AutocompleteRequest(q = "oslo")
        assertNull(req.photonZoom())
        assertNull(req.photonLocationBiasScale())
    }

    @Test
    fun `photonZoom uses default radius of 50km`() {
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7)
        // 50km -> zoom = 18 - log2(50/0.25) = 18 - log2(200) ≈ 18 - 7.64 = 10
        assertEquals(10, req.photonZoom())
    }

    @Test
    fun `photonZoom with explicit radius`() {
        // 4km -> zoom = 18 - log2(4/0.25) = 18 - 4 = 14
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 4.0)
        assertEquals(14, req.photonZoom())
    }

    @Test
    fun `photonZoom with small radius`() {
        // 0.25km -> zoom = 18 - log2(1) = 18
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 0.25)
        assertEquals(18, req.photonZoom())
    }

    @Test
    fun `photonZoom clamps to valid range`() {
        val large = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 100000.0)
        assertEquals(0, large.photonZoom())

        val tiny = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 0.001)
        assertEquals(18, tiny.photonZoom())
    }

    @Test
    fun `photonLocationBiasScale uses default weight of 0_8`() {
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7)
        // weight 0.8 -> location_bias_scale = 1 - 0.8 = 0.2
        assertEquals(0.2, req.photonLocationBiasScale()!!, 0.001)
    }

    @Test
    fun `photonLocationBiasScale with explicit weight`() {
        val noFocus = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, weight = 0.0)
        assertEquals(1.0, noFocus.photonLocationBiasScale())

        val maxFocus = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, weight = 1.0)
        assertEquals(0.0, maxFocus.photonLocationBiasScale())

        val half = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, weight = 0.5)
        assertEquals(0.5, half.photonLocationBiasScale())
    }

    @Test
    fun `photonLocationBiasScale clamps weight`() {
        val over = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, weight = 2.0)
        assertEquals(0.0, over.photonLocationBiasScale())

        val under = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, weight = -1.0)
        assertEquals(1.0, under.photonLocationBiasScale())
    }
}
