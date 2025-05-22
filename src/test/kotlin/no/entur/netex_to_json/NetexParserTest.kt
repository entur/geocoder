package no.entur.netex_to_json

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class NetexParserTest {
    @Test
    fun parseThatFile() {
        val netexParser = NetexParser()
        val fileStream = this::class.java.getResourceAsStream("/stopPlaces.xml")
        assertNotNull(fileStream, "Test file /stopPlaces.xml not found.")

        val stopPlacesSeq = netexParser.parseXml(fileStream)
        val iter = stopPlacesSeq.iterator()

        assertEquals("NSR:StopPlace:56697", iter.next().id)
        assertEquals("NSR:StopPlace:56769", iter.next().id)
        assertEquals("NSR:StopPlace:63329", iter.next().id)
        assertFalse(iter.hasNext(), "Expected only three StopPlaces.")
    }
}
