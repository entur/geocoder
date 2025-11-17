package no.entur.geocoder.converter.source.stopplace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NetexParserTest {
    @Test
    fun `parse that file`() {
        val parser = NetexParser()
        val stream = this::class.java.getResourceAsStream("/oslo.xml")
        assertNotNull(stream, "Test file /oslo.xml not found.")

        val places = parser.parseXml(stream).stopPlaces.toList()
        assertEquals(places.size, 1264)

        assertEquals("NSR:StopPlace:152", places[0].id)
        assertEquals("NSR:StopPlace:59651", places[1].id)
        assertEquals("NSR:StopPlace:153", places[2].id)
    }

    @Test
    fun `parse GroupOfStopPlaces from stopPlaces xml`() {
        val parser = NetexParser()
        val stream = this::class.java.getResourceAsStream("/stopPlaces.xml")
        assertNotNull(stream, "Test file /stopPlaces.xml not found.")

        val result = parser.parseXml(stream)
        val groups = result.groupOfStopPlaces.toList()

        assertEquals(2, groups.size)
        assertEquals("NSR:GroupOfStopPlaces:72", groups[0].id)
        assertEquals("Hammerfest", groups[0].name.text)
        assertEquals("NSR:GroupOfStopPlaces:1", groups[1].id)
        assertEquals("Oslo", groups[1].name.text)
    }
}
