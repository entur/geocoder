package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.converter.TestConfig
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests for OsmConverter using real PBF test files.
 */
class OsmConverterIntegrationTest {
    @Test
    fun `should convert oslo-opera PBF file to NDJSON`(
        @TempDir tempDir: Path,
    ) {
        val inputFile = File(javaClass.classLoader.getResource("oslo-opera.osm.pbf")!!.file)
        val outputFile = tempDir.resolve("output.ndjson").toFile()

        val converter = OsmConverter(TestConfig.config)
        converter.convert(inputFile, outputFile, isAppending = false)

        assertTrue(outputFile.exists(), "Output file should be created")
        assertTrue(outputFile.length() > 0, "Output file should have content")

        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty(), "Should have at least one output line")

        // Verify each line is valid JSON
        lines.forEach { line ->
            assertTrue(line.startsWith("{"), "Each line should be valid JSON: $line")
        }

        println("Converted ${lines.size} POIs from oslo-opera.osm.pbf")
    }

    @Test
    fun `should convert oslo-center PBF file to NDJSON`(
        @TempDir tempDir: Path,
    ) {
        val inputFile = File(javaClass.classLoader.getResource("oslo-center.osm.pbf")!!.file)
        val outputFile = tempDir.resolve("output.ndjson").toFile()

        val converter = OsmConverter(TestConfig.config)
        converter.convert(inputFile, outputFile, isAppending = false)

        assertTrue(outputFile.exists(), "Output file should be created")
        assertTrue(outputFile.length() > 0, "Output file should have content")

        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty(), "Should have at least one output line")

        println("Converted ${lines.size} POIs from oslo-center.osm.pbf")
    }
}
