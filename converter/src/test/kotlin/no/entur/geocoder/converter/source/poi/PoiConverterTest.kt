package no.entur.geocoder.converter.source.poi

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.entur.geocoder.common.Category.OSM_CUSTOM_POI
import no.entur.geocoder.common.Source.CUSTOM_POI
import no.entur.geocoder.converter.TestConfig
import no.entur.geocoder.converter.target.NominatimPlace
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PoiConverterTest {
    private val converter = PoiConverter(TestConfig.config)
    private val mapper = jacksonObjectMapper()

    private fun convertTestFile(): List<NominatimPlace> {
        val inputStream = this::class.java.getResourceAsStream("/poi-test.xml")
        assertNotNull(inputStream)

        val inputFile =
            File.createTempFile("poi-test", ".xml").apply {
                deleteOnExit()
                writeBytes(inputStream.readBytes())
            }
        val outputFile = File.createTempFile("poi-output", ".ndjson").apply { deleteOnExit() }

        converter.convert(inputFile, outputFile, isAppending = false)

        return outputFile
            .readLines()
            .filter { it.isNotBlank() }
            .drop(1) // skip header
            .map { mapper.readValue(it, NominatimPlace::class.java) }
    }

    @Test
    fun `converts POI with correct coordinates, categories, and source`() {
        val places = convertTestFile()

        // Date filtering: include valid (1), always-valid (4), open-ended (5); exclude expired (2), future (3)
        assertEquals(3, places.size)

        val poi =
            places.find {
                it.content
                    .first()
                    .extra.id == "TEST:TopographicPlace:1"
            }
        assertNotNull(poi)

        val content = poi.content.first()

        // Centroid is [lon, lat]
        assertEquals(10.75, content.centroid[0].toDouble(), 0.001)
        assertEquals(59.91, content.centroid[1].toDouble(), 0.001)

        assertTrue(content.categories.contains(OSM_CUSTOM_POI))
        assertEquals(CUSTOM_POI, content.extra.source)
    }

    @Test
    fun `filters out expired and future POIs`() {
        val places = convertTestFile()

        assertNull(
            places.find {
                it.content
                    .first()
                    .extra.id == "TEST:TopographicPlace:2"
            },
            "expired",
        )
        assertNull(
            places.find {
                it.content
                    .first()
                    .extra.id == "TEST:TopographicPlace:3"
            },
            "future",
        )
        assertNotNull(
            places.find {
                it.content
                    .first()
                    .extra.id == "TEST:TopographicPlace:4"
            },
            "no validity",
        )
    }

    @Test
    fun `ndjson contains lon lat coordinates`() {
        val inputStream = this::class.java.getResourceAsStream("/poi-test.xml")
        assertNotNull(inputStream)

        val inputFile =
            File.createTempFile("poi-test", ".xml").apply {
                deleteOnExit()
                writeBytes(inputStream.readBytes())
            }
        val outputFile = File.createTempFile("poi-output", ".ndjson").apply { deleteOnExit() }

        converter.convert(inputFile, outputFile, isAppending = false)

        val ndjson = outputFile.readText()

        // Verify coordinates appear in the raw JSON (lon=10.75, lat=59.91 for test place 1)
        assertTrue(ndjson.contains("10.75"), "longitude should be in ndjson")
        assertTrue(ndjson.contains("59.91"), "latitude should be in ndjson")
    }
}
