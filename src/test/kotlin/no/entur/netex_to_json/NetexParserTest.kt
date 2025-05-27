package no.entur.netex_to_json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class NetexParserTest {
    @Test
    fun `parse that file`() {
        val parser = NetexParser()
        val stream = this::class.java.getResourceAsStream("/stopPlaces.xml")
        assertNotNull(stream, "Test file /stopPlaces.xml not found.")

        val places = parser.parseXml(stream).stopPlaces.iterator()

        assertEquals("NSR:StopPlace:56697", places.next().id)
        assertEquals("NSR:StopPlace:56769", places.next().id)
        assertEquals("NSR:StopPlace:63329", places.next().id)
        places.next()
        places.next()
        places.next()
        assertFalse(places.hasNext(), "Expected only five StopPlaces.")
    }
}
