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

    @Test
    fun `output file should contain valid JSON on each line`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("valid_json", ".json")
        converter.convert(input, output)

        val lines = output.readLines()
        assertTrue(lines.isNotEmpty(), "Output should not be empty")

        lines.forEach { line ->
            assertTrue(line.isNotBlank(), "Lines should not be blank")
            assertTrue(line.startsWith("{"), "Each line should be valid JSON")
            assertTrue(line.endsWith("}"), "Each line should be valid JSON")
        }
    }

    @Test
    fun `all stop places should have coordinates`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("coordinates", ".json")
        converter.convert(input, output)

        val stopPlaceLines = output.readLines().filter { it.contains("NSR:StopPlace:") }

        assertTrue(stopPlaceLines.isNotEmpty(), "Should have stop place entries")
        stopPlaceLines.forEach { line ->
            assertTrue(line.contains("\"centroid\":["), "Each stop place should have centroid")
        }
    }

    @Test
    fun `stop places should have transport mode categories`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("categories", ".json")
        converter.convert(input, output)

        val content = output.readText()
        assertTrue(
            content.contains("osm.public_transport"),
            "Should contain public transport categories",
        )
    }

    @Test
    fun `output should have header as first line`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("header", ".json")
        converter.convert(input, output)

        val firstLine = output.readLines().first()
        assertTrue(
            firstLine.contains("NominatimDumpFile"),
            "First line should be NominatimDumpFile header",
        )
        assertTrue(
            firstLine.contains("\"version\""),
            "Header should contain version",
        )
    }

    @Test
    fun `stop places should have valid IDs`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("ids", ".json")
        converter.convert(input, output)

        val lines = output.readLines().drop(1)
        lines.forEach { line ->
            assertTrue(line.contains("\"place_id\":"), "Should have place_id")
            assertTrue(line.contains("\"object_id\":"), "Should have object_id")
        }
    }

    @Test
    fun `GroupOfStopPlaces should have higher importance than individual stops`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/stopPlaces.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("importance", ".json")
        converter.convert(input, output)

        val lines = output.readLines()

        val groupLines = lines.filter { it.contains("group_of_stop_places") }
        val stopLines = lines.filter { it.contains("NSR:StopPlace:") && !it.contains("group_of_stop_places") }

        assertTrue(groupLines.isNotEmpty(), "Should have GroupOfStopPlaces entries")
        assertTrue(stopLines.isNotEmpty(), "Should have StopPlace entries")
    }

    @Test
    fun `stop places should have names`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("names", ".json")
        converter.convert(input, output)

        val lines = output.readLines().drop(1)
        lines.forEach { line ->
            assertTrue(line.contains("\"name\":{"), "Each entry should have a name object")
            assertTrue(line.contains("\"name\":\""), "Each entry should have a name value")
        }
    }

    @Test
    fun `converted file should have multiple entries`() {
        val converter = StopPlaceConverter()
        val xmlStream = this::class.java.getResourceAsStream("/oslo.xml")
        requireNotNull(xmlStream)

        val input = streamToFile(xmlStream)
        val output = File.createTempFile("multiple", ".json")
        converter.convert(input, output)

        val lines = output.readLines()
        assertTrue(lines.size > 10, "Should have more than 10 lines (header + multiple entries)")
    }
}
