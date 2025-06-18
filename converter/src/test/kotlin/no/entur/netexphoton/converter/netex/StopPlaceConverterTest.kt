package no.entur.netexphoton.converter.netex

import no.entur.netexphoton.converter.FileUtil.streamToFile
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
}
