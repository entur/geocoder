package no.entur.geocoder.converter.matrikkel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.converter.NominatimPlace
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

    @Test
    fun `should convert matrikkel adresse CSV to nominatim json`() {
        val converter = MatrikkelConverter() // Changed from Converter to MatrikkelConverter
        val inputFile =
            File(javaClass.classLoader.getResource("Basisdata_3420_Elverum_25833_MatrikkelenAdresse.csv").file)
        val outputFile = tempDir.resolve("output.json").toFile()

        converter.convert(inputFile, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty())

        assertTrue(lines.size > 1, "Output file should have at least two lines (header and data)")

        // Find the address with lokalid 225678815
        val targetPlaceJson = lines.find { line ->
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
        assertEquals("KVE:TopographicPlace:34", extra.county_gid)
        assertEquals("Elverum", extra.locality)
        assertEquals("KVE:TopographicPlace:3420", extra.locality_gid)
        assertEquals("Grindalsmoen", extra.borough)
        assertEquals("borough:34200205", extra.borough_gid)
        assertEquals("Ildervegen 1A, Elverum", extra.label)

        val placeContent = nominatimPlace.content.first()
        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Centroid should have 2 coordinates")
        assertEquals("1A", placeContent.housenumber)
        assertEquals("Ildervegen", placeContent.address?.street)
        assertEquals("TODO", placeContent.address?.county)
        assertEquals("2406", placeContent.postcode)
        assertEquals(null, placeContent.name, "Name should be null for addresses")

        assertEquals(BigDecimal("11.527525"), placeContent.centroid[0])
        assertEquals(BigDecimal("60.892175"), placeContent.centroid[1])
    }
}
