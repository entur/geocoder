package no.entur.geocoder.converter

import no.entur.geocoder.converter.source.PlaceId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test

class PlaceIdTest {
    @ParameterizedTest
    @CsvSource(
        "address, 100",
        "street, 200",
        "stedsnavn, 300",
        "stopplace, 400",
        "gosp, 450",
        "osm, 500",
        "poi, 600",
    )
    fun `all prefixes create correct IDs`(type: String, expectedPrefix: Int) {
        val placeId = PlaceId.valueOf(type)
        val id = placeId.create(123L)
        assertTrue(id.toString().startsWith(expectedPrefix.toString()))
        assertEquals(expectedPrefix, placeId.prefix)
    }

    @Test
    fun `different prefixes create different IDs`() {
        val ids = PlaceId.entries.map { it.create(100L) }.toSet()
        assertEquals(7, ids.size)
    }

    @Test
    fun `negative IDs use absolute value`() {
        assertEquals(PlaceId.address.create(123L), PlaceId.address.create(-123L))
    }

    @Test
    fun `string hashCode is consistent`() {
        val id1 = PlaceId.stedsnavn.create("Oslo")
        val id2 = PlaceId.stedsnavn.create("Oslo")
        val id3 = PlaceId.stedsnavn.create("Bergen")

        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
    }
}
