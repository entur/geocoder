package no.entur.geocoder.proxy.v3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class V3PlaceRequestTest {
    @Test
    fun `accepts up to MAX_IDS ids`() {
        val ids = (1..V3PlaceRequest.MAX_IDS).map { "NSR:StopPlace:$it" }
        assertEquals(V3PlaceRequest.MAX_IDS, V3PlaceRequest(ids = ids).ids.size)
    }

    @Test
    fun `rejects more than MAX_IDS ids`() {
        val ids = (1..V3PlaceRequest.MAX_IDS + 1).map { "NSR:StopPlace:$it" }
        assertFailsWith<IllegalArgumentException> { V3PlaceRequest(ids = ids) }
    }

    @Test
    fun `rejects empty ids`() {
        assertFailsWith<IllegalArgumentException> { V3PlaceRequest(ids = emptyList()) }
    }
}
