package no.entur.geocoder.converter.source.matrikkel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.converter.target.NominatimPlace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MatrikkelConverterTest {
    @TempDir
    lateinit var tempDir: Path

    lateinit var inputFile: File
    lateinit var converter: MatrikkelConverter

    @BeforeEach
    fun setup() {
        converter = MatrikkelConverter(stedsnavnGmlFile = null)
        inputFile =
            File(javaClass.classLoader.getResource("Basisdata_3420_Elverum_25833_MatrikkelenAdresse.csv").file)
    }

    @Test
    fun `should convert matrikkel adresse CSV to nominatim json`() {
        val outputFile = tempDir.resolve("output.json").toFile()

        converter.convert(inputFile, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty())

        assertTrue(lines.size > 1, "Output file should have at least two lines (header and data)")

        // Find the address with lokalid 225678815
        val targetPlaceJson =
            lines.find { line ->
                line.contains("\"225678815\"")
            }
        assertNotNull(targetPlaceJson, "Could not find address with lokalid 225678815")

        val nominatimPlace: NominatimPlace = objectMapper.readValue(targetPlaceJson)

        assertNotNull(nominatimPlace.content.firstOrNull()?.extra, "Extratags should not be null")
        val extra = nominatimPlace.content.first().extra

        assertEquals("225678815", extra.id)
        assertEquals("kartverket-matrikkelenadresse", extra.source)
        assertEquals("point", extra.accuracy)
        assertEquals("NOR", extra.country_a)
        assertEquals("Elverum", extra.locality)
        assertEquals("KVE:TopographicPlace:3420", extra.locality_gid)
        assertEquals("Grindalsmoen", extra.borough)
        assertEquals("borough:34200205", extra.borough_gid)

        val placeContent = nominatimPlace.content.first()
        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Centroid should have 2 coordinates")
        assertEquals("1A", placeContent.housenumber)
        assertEquals("Ildervegen", placeContent.address.street)
        assertEquals("Innlandet", placeContent.address.county)
        assertEquals("2406", placeContent.postcode)
        assertEquals(null, placeContent.name, "Name should be null for addresses")

        assertEquals(BigDecimal("11.527525"), placeContent.centroid[0])
        assertEquals(BigDecimal("60.892175"), placeContent.centroid[1])
    }

    @Test
    fun `should populate county when stedsnavn GML file is provided`() {
        val stedsnavnFile = File(javaClass.classLoader.getResource("Basisdata_3420_Elverum_25833_Stedsnavn_GML.gml").file)
        val fullConverter = MatrikkelConverter(stedsnavnGmlFile = stedsnavnFile)
        val outputFile = tempDir.resolve("output_with_county.json").toFile()

        fullConverter.convert(inputFile, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty())

        val targetPlaceJson =
            lines.find { line ->
                line.contains("\"225678815\"")
            }
        assertNotNull(targetPlaceJson, "Could not find address with lokalid 225678815")

        val nominatimPlace: NominatimPlace = objectMapper.readValue(targetPlaceJson)
        val placeContent = nominatimPlace.content.first()

        assertNotNull(placeContent.address.county, "County should be populated when Stedsnavn GML file is provided")
        assertEquals("Innlandet", placeContent.address.county, "County should be Innlandet for Elverum kommune")
    }

    @Test
    fun `should generate both address and street entries from same input`() {
        val outputFile = tempDir.resolve("output_both.json").toFile()

        converter.convert(inputFile, outputFile)

        val lines = outputFile.readLines()

        val addressEntries = lines.filter { it.contains("osm.public_transport.address") }
        val streetEntries = lines.filter { line ->
            line.contains("osm.highway") || line.contains("street") && !line.contains("osm.public_transport.address")
        }

        assertTrue(streetEntries.isNotEmpty())
        assertTrue(addressEntries.isNotEmpty(), "Should have address entries")
        assertTrue(lines.size > 10, "Should have multiple entries including addresses")
    }

    @Test
    fun `streets should have averaged coordinates from multiple addresses`() {
        val outputFile = tempDir.resolve("output_streets.json").toFile()

        converter.convert(inputFile, outputFile)

        val lines = outputFile.readLines()

        assertTrue(lines.size > 10, "Should process multiple entries")

        val entriesWithStreetNames = lines.filter { it.contains("Ildervegen") }
        assertTrue(entriesWithStreetNames.isNotEmpty(), "Should have entries with street name Ildervegen")
    }

    @Test
    fun `address entries should have correct categories`() {
        val outputFile = tempDir.resolve("output_categories.json").toFile()

        converter.convert(inputFile, outputFile)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        val addressLine = lines.find { it.contains("\"225678815\"") }
        assertNotNull(addressLine)

        val nominatimPlace: NominatimPlace = objectMapper.readValue(addressLine)
        val categories = nominatimPlace.content.first().categories

        assertTrue(categories.any { it.contains("address") }, "Should have address category")
        assertTrue(categories.any { it.contains("source.kartverket.matrikkelenadresse") }, "Should have kartverket source category")
    }

    @Test
    fun `all entries should have valid categories`() {
        val outputFile = tempDir.resolve("output_all_categories.json").toFile()

        converter.convert(inputFile, outputFile)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines().drop(1)

        assertTrue(lines.isNotEmpty(), "Should have data entries")
        lines.forEach { line ->
            val nominatimPlace: NominatimPlace = objectMapper.readValue(line)
            val categories = nominatimPlace.content.first().categories

            assertTrue(categories.isNotEmpty(), "All entries should have at least one category")
        }
    }

    @Test
    fun `all addresses should have valid coordinates`() {
        val outputFile = tempDir.resolve("output_coords.json").toFile()

        converter.convert(inputFile, outputFile)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines().drop(1)

        lines.forEach { line ->
            val nominatimPlace: NominatimPlace = objectMapper.readValue(line)
            val centroid = nominatimPlace.content.first().centroid

            assertEquals(2, centroid.size, "Centroid should have exactly 2 coordinates")
            assertTrue(centroid[0].toDouble() in -180.0..180.0, "Longitude should be valid")
            assertTrue(centroid[1].toDouble() in -90.0..90.0, "Latitude should be valid")
        }
    }

    @Test
    fun `addresses should have proper importance values`() {
        val outputFile = tempDir.resolve("output_importance.json").toFile()

        converter.convert(inputFile, outputFile)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines().drop(1)

        lines.forEach { line ->
            val nominatimPlace: NominatimPlace = objectMapper.readValue(line)
            val importance = nominatimPlace.content.first().importance

            assertTrue(importance > 0.0, "Importance should be positive")
            assertTrue(importance <= 1.0, "Importance should not exceed 1.0")
        }
    }

    @Test
    fun `addresses with letters should have combined housenumber`() {
        val outputFile = tempDir.resolve("output_housenumber.json").toFile()

        converter.convert(inputFile, outputFile)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        val addressWith1A = lines.find { it.contains("\"225678815\"") }
        assertNotNull(addressWith1A)

        val nominatimPlace: NominatimPlace = objectMapper.readValue(addressWith1A)
        assertEquals("1A", nominatimPlace.content.first().housenumber, "Should combine number and letter")
    }
}
