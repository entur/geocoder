package no.entur.geocoder.converter.source.netex

import no.entur.geocoder.converter.FileUtil.streamToFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class StopPlaceConverterTest {
    @Test
    fun `convert stopPlaces xml to nominatimDumpFile json`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream) { "stopPlaces.xml not found in test resources" }

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("out", ".tmp")
        converter.convert(input, output)

        assertTrue(output.exists(), "Output file was not created")

        val lines = output.readLines()
        assertTrue(lines.size > 10, "Output file is empty")
        assertTrue(lines[0].contains("NominatimDumpFile"))
        assertTrue(lines[1].contains("NSR:StopPlace:152"))
    }

    @Test
    fun `convert GroupOfStopPlaces to nominatim format`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/stopPlaces.xml")
        requireNotNull(xmlStream) { "stopPlaces.xml not found in test resources" }

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("groupOfStopPlaces", ".json")
        converter.convert(input, output)

        assertTrue(output.exists(), "Output file was not created")

        val content = output.readText()
        assertTrue(content.contains("NSR:GroupOfStopPlaces:1"), "Should contain Oslo GroupOfStopPlaces")
        assertTrue(content.contains("NSR:GroupOfStopPlaces:72"), "Should contain Hammerfest GroupOfStopPlaces")
        assertTrue(content.contains("\"name\":\"Oslo\""), "Should contain Oslo name")
        assertTrue(
            content.contains("\"osm.public_transport.group_of_stop_places\""),
            "Should contain GroupOfStopPlaces category",
        )
    }
}
