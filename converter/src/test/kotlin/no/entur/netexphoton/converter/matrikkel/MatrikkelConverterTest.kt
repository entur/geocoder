package no.entur.netexphoton.converter.matrikkel

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.netexphoton.converter.NominatimPlace
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
        val firstPlaceJson = lines[1]
        val nominatimPlace: NominatimPlace = objectMapper.readValue(firstPlaceJson)

        assertNotNull(nominatimPlace.content.firstOrNull()?.extratags, "Extratags should not be null")
        val extra = nominatimPlace.content.first().extratags

        assertEquals("399524883", extra.id)
        assertEquals("address", extra.layer)
        assertEquals("kartverket", extra.source)
        assertEquals("399524883", extra.source_id)
        assertEquals("point", extra.accuracy)
        assertEquals("NOR", extra.country_a)
        assertEquals("KVE:TopographicPlace:34", extra.county_gid)
        assertEquals("Elverum", extra.locality)
        assertEquals("KVE:TopographicPlace:3420", extra.locality_gid)
        assertEquals("Svanåsen", extra.borough)
        assertEquals("borough:34200104", extra.borough_gid)
        assertEquals("Gobakkvegen 438, Hernes", extra.label)

        val placeContent = nominatimPlace.content.first()
        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Centroid should have 2 coordinates")
        assertEquals("438", placeContent.housenumber)
        assertEquals("Gobakkvegen", placeContent.address?.street)
        assertEquals("TODO", placeContent.address?.county)
        assertEquals("2410", placeContent.postcode)
        assertEquals("Gobakkvegen 438", placeContent.name?.name)

        assertEquals(BigDecimal("11.483291"), placeContent.centroid[0])
        assertEquals(BigDecimal("61.025715"), placeContent.centroid[1])
    }
}
