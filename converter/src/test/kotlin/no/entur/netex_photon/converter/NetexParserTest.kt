package no.entur.netex_photon.converter

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
}
