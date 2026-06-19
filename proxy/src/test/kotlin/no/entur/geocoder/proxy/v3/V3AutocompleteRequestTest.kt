package no.entur.geocoder.proxy.v3

import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        // 50km -> 18 - log_2.2(500) ≈ 10
        assertEquals(10, req.photonZoom())
    }

    @Test
    fun `photonZoom with explicit radius`() {
        // 4km -> 18 - log_2.2(40) ≈ 13
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 4.0)
        assertEquals(13, req.photonZoom())
    }

    @Test
    fun `photonZoom with small radius`() {
        // 0.25km -> 18 - log_2.2(2.5) ≈ 17
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 0.25)
        assertEquals(17, req.photonZoom())
    }

    @Test
    fun `photonZoom clamps to valid range`() {
        val large = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 100000.0)
        assertEquals(0, large.photonZoom())

        val tiny = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7, radius = 0.001)
        assertEquals(18, tiny.photonZoom())
    }

    @Test
    fun `photonLocationBiasScale uses default weight of 0_5`() {
        val req = V3AutocompleteRequest(q = "oslo", lat = 59.9, lon = 10.7)
        // weight 0.5 -> location_bias_scale = 1 - 0.5 = 0.5
        assertEquals(0.5, req.photonLocationBiasScale()!!, 0.001)
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

    @Test
    fun `focus parameters require both lat and lon`() {
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", lat = 59.9) }
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", lon = 10.7) }
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", radius = 50.0) }
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", weight = 0.5) }
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", lat = 59.9, radius = 50.0) }
    }

    @Test
    fun `limit must be between 1 and 100`() {
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", limit = 0) }
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", limit = 101) }
        assertEquals(100, V3AutocompleteRequest(q = "oslo", limit = 100).limit)

        assertFailsWith<IllegalArgumentException> { V3ReverseRequest(lat = 59.9, lon = 10.7, limit = 0) }
        assertFailsWith<IllegalArgumentException> { V3ReverseRequest(lat = 59.9, lon = 10.7, limit = 101) }
        assertEquals(100, V3ReverseRequest(lat = 59.9, lon = 10.7, limit = 100).limit)
    }

    @Test
    fun `bbox is parsed from comma-separated string`() {
        val req = V3AutocompleteRequest.from(parametersOf("q" to listOf("oslo"), "bbox" to listOf("10.5,59.8,10.9,60.0")))
        assertEquals(listOf(10.5, 59.8, 10.9, 60.0), req.bbox)
    }

    @Test
    fun `bbox rejects malformed input`() {
        // wrong arity
        assertFailsWith<IllegalArgumentException> {
            V3AutocompleteRequest.from(parametersOf("q" to listOf("oslo"), "bbox" to listOf("10.5,59.8,10.9")))
        }
        // non-numeric
        assertFailsWith<IllegalArgumentException> {
            V3AutocompleteRequest.from(parametersOf("q" to listOf("oslo"), "bbox" to listOf("a,b,c,d")))
        }
        // min >= max
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", bbox = listOf(10.9, 59.8, 10.5, 60.0)) }
        // out of range
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", bbox = listOf(-190.0, 59.8, 10.9, 60.0)) }
        assertFailsWith<IllegalArgumentException> { V3AutocompleteRequest(q = "oslo", bbox = listOf(10.5, 59.8, 10.9, 95.0)) }
    }
}
