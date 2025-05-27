package no.entur.netex_to_json

import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertTrue

class ConverterTest {
    @Test
    fun `convert stopPlaces xml to nominatimDumpFile json`() {
        val parser = NetexParser()
        val converter = Converter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
//        val xmlStream = this::class.java.getResourceAsStream("/finnmark.xml")
//        val xmlStream = this::class.java.getResourceAsStream("/stopPlaces.xml")
        requireNotNull(xmlStream) { "stopPlaces.xml not found in test resources" }

        val result: NetexParser.ParseResult = parser.parseXml(xmlStream)
        val entries: Sequence<NominatimPlace> = converter.convertAll(result)

        val outputPath = Paths.get("build/test-output/nominatimDumpFile.ndjson")
        JsonWriter().export(entries, outputPath)

        assertTrue(Files.exists(outputPath), "Output file was not created")
        assertTrue(Files.size(outputPath) > 0, "Output file is empty")
    }
}

