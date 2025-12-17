package no.entur.geocoder.converter

import no.entur.geocoder.converter.target.NominatimId
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test

class NominatimIdTest {
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
        val nominatimId = NominatimId.valueOf(type)
        val id = nominatimId.create(123L)
        assertTrue(id.toString().startsWith(expectedPrefix.toString()))
        assertEquals(expectedPrefix, nominatimId.prefix)
    }

    @Test
    fun `different prefixes create different IDs`() {
        val ids = NominatimId.entries.map { it.create(100L) }.toSet()
        assertEquals(7, ids.size)
    }

    @Test
    fun `negative IDs use absolute value`() {
        assertEquals(NominatimId.address.create(123L), NominatimId.address.create(-123L))
    }

    @Test
    fun `string hashCode is consistent`() {
        val id1 = NominatimId.stedsnavn.create("Oslo")
        val id2 = NominatimId.stedsnavn.create("Oslo")
        val id3 = NominatimId.stedsnavn.create("Bergen")

        assertEquals(id1, id2)
        assertNotEquals(id1, id3)
    }
}
