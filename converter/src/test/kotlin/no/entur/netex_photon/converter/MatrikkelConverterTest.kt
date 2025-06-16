package no.entur.netex_photon.converter


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
            File(javaClass.classLoader.getResource("Basisdata_3420_Elverum_25833_MatrikkelenAdresse.csv")!!.file)
        val outputFile = tempDir.resolve("output.json").toFile()

        converter.convertCsv(inputFile, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty())

        assertTrue(lines.size > 1, "Output file should have at least two lines (header and data)")
        val firstPlaceJson = lines[1]
        val nominatimPlace: NominatimPlace = objectMapper.readValue(firstPlaceJson)

        assertNotNull(nominatimPlace.content.firstOrNull()?.extratags, "Extratags should not be null")
        val properties = nominatimPlace.content.first().extratags

        assertEquals("399524883", properties["id"])
        assertEquals("address", properties["layer"])
        assertEquals("kartverket", properties["source"])
        assertEquals("399524883", properties["source_id"])
        assertEquals("Gobakkvegen 438", properties["name"])
        assertEquals("438", properties["housenumber"])
        assertEquals("Gobakkvegen", properties["street"])
        assertEquals("2410", properties["postalcode"])
        assertEquals("point", properties["accuracy"])
        assertEquals("NOR", properties["country_a"])
        assertEquals("TODO", properties["county"])
        assertEquals("KVE:TopographicPlace:34", properties["county_gid"])
        assertEquals("Elverum", properties["locality"])
        assertEquals("KVE:TopographicPlace:3420", properties["locality_gid"])
        assertEquals("Svanåsen", properties["borough"])
        assertEquals("borough:34200104", properties["borough_gid"])
        assertEquals("Gobakkvegen 438, Hernes", properties["label"])

        val categoryFromExtratags = properties["category"]
        assertNotNull(categoryFromExtratags)
        assertTrue(categoryFromExtratags.contains("vegadresse"))

        val placeContent = nominatimPlace.content.first()
        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Centroid should have 2 coordinates")

        assertEquals(BigDecimal("11.483291"), placeContent.centroid[0])
        assertEquals(BigDecimal("61.025715"), placeContent.centroid[1])
    }
}
