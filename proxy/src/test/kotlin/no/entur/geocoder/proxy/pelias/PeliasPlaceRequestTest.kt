package no.entur.geocoder.proxy.pelias

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeliasPlaceRequestTest {
    @Test
    fun `validates ids is not empty`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasPlaceRequest(ids = emptyList())
        }
    }

    @Test
    fun `validates ids have correct format with three colon-separated parts`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasPlaceRequest(ids = listOf("invalid"))
        }

        assertFailsWith<IllegalArgumentException> {
            PeliasPlaceRequest(ids = listOf("one:two"))
        }
    }

    @Test
    fun `accepts valid ids with three colon-separated parts`() {
        val req = PeliasPlaceRequest(ids = listOf("source:layer:id"))
        assertEquals(listOf("source:layer:id"), req.ids)
    }

    @Test
    fun `accepts multiple valid ids`() {
        val req =
            PeliasPlaceRequest(
                ids =
                    listOf(
                        "osm:venue:123456",
                        "kartverket:address:789012",
                        "whosonfirst:locality:345678",
                    ),
            )

        assertEquals(3, req.ids.size)
        assertEquals("osm:venue:123456", req.ids[0])
        assertEquals("kartverket:address:789012", req.ids[1])
        assertEquals("whosonfirst:locality:345678", req.ids[2])
    }

    @Test
    fun `rejects list with mix of valid and invalid ids`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasPlaceRequest(
                ids =
                    listOf(
                        "osm:venue:123456",
                        "invalid",
                    ),
            )
        }
    }

    @Test
    fun `validates all ids in the list`() {
        assertFailsWith<IllegalArgumentException> {
            PeliasPlaceRequest(
                ids =
                    listOf(
                        "valid:format:here",
                        "also:valid:format",
                        "not-valid",
                    ),
            )
        }
    }

    @Test
    fun `accepts real world example ids`() {
        val req =
            PeliasPlaceRequest(
                ids =
                    listOf(
                        "openstreetmap:venue:W123456789",
                        "kartverket:address:0301-12345",
                        "whosonfirst:locality:85922437",
                    ),
            )

        assertEquals(3, req.ids.size)
    }
}
