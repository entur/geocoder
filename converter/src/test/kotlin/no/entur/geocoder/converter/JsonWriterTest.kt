package no.entur.geocoder.converter

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.common.Extra
import no.entur.geocoder.converter.target.NominatimHeader
import no.entur.geocoder.converter.target.NominatimPlace
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path

class JsonWriterTest {
    private val jsonWriter = JsonWriter()
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `export creates output file with header when not appending`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = false)

        assertTrue(Files.exists(outputPath))
        val lines = Files.readAllLines(outputPath)
        assertEquals(1, lines.size)

        val header = objectMapper.readValue<NominatimHeader>(lines[0])
        assertEquals("NominatimDumpFile", header.type)
        assertEquals("0.1.0", header.content.version)
        assertEquals("geocoder", header.content.generator)
    }

    @Test
    fun `export creates parent directories if needed`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("subdir/nested/output.ndjson")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = false)

        assertTrue(Files.exists(outputPath))
        assertTrue(Files.exists(outputPath.parent))
    }

    @Test
    fun `export writes entries without header when appending`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        Files.writeString(outputPath, "existing line\n")

        val place = createTestPlace(1L)
        val entries = sequenceOf(place)

        jsonWriter.export(entries, outputPath, isAppending = true)

        val lines = Files.readAllLines(outputPath)
        assertEquals(2, lines.size)
        assertEquals("existing line", lines[0])
    }

    @Test
    fun `export writes multiple entries`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val places =
            sequenceOf(
                createTestPlace(1L),
                createTestPlace(2L),
                createTestPlace(3L),
            )

        jsonWriter.export(places, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        assertEquals(4, lines.size)

        val header = objectMapper.readValue<NominatimHeader>(lines[0])
        assertEquals("NominatimDumpFile", header.type)

        val place1 = objectMapper.readValue<NominatimPlace>(lines[1])
        assertEquals(1L, place1.content[0].place_id)

        val place2 = objectMapper.readValue<NominatimPlace>(lines[2])
        assertEquals(2L, place2.content[0].place_id)

        val place3 = objectMapper.readValue<NominatimPlace>(lines[3])
        assertEquals(3L, place3.content[0].place_id)
    }

    @Test
    fun `export omits null fields`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val place = createTestPlace(1L, includeOptional = false)
        val entries = sequenceOf(place)

        jsonWriter.export(entries, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        val placeJson = lines[1]

        assertFalse(placeJson.contains("\"parent_place_id\""))
        assertFalse(placeJson.contains("\"name\""))
        assertFalse(placeJson.contains("\"housenumber\""))
    }

    @Test
    fun `export includes non-null fields`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val place = createTestPlace(1L, includeOptional = true)
        val entries = sequenceOf(place)

        jsonWriter.export(entries, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        val placeJson = lines[1]

        assertTrue(placeJson.contains("\"parent_place_id\""))
        assertTrue(placeJson.contains("\"name\""))
        assertTrue(placeJson.contains("\"housenumber\""))
    }

    @Test
    fun `export writes valid JSON for each line`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val places =
            sequenceOf(
                createTestPlace(1L),
                createTestPlace(2L),
            )

        jsonWriter.export(places, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        lines.forEach { line ->
            assertDoesNotThrow {
                objectMapper.readTree(line)
            }
        }
    }

    @Test
    fun `header contains correct feature flags`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        val header = objectMapper.readValue<NominatimHeader>(lines[0])

        assertTrue(header.content.features.sorted_by_country)
        assertFalse(header.content.features.has_addresslines)
    }

    @Test
    fun `header contains timestamp`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        val header = objectMapper.readValue<NominatimHeader>(lines[0])

        assertNotNull(header.content.data_timestamp)
        assertTrue(header.content.data_timestamp.isNotEmpty())
    }

    @Test
    fun `header contains database version`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        val header = objectMapper.readValue<NominatimHeader>(lines[0])

        assertEquals("0.3.6-1", header.content.database_version)
    }

    @Test
    fun `export handles empty sequence when not appending`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = false)

        val lines = Files.readAllLines(outputPath)
        assertEquals(1, lines.size)
    }

    @Test
    fun `export handles empty sequence when appending`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")
        Files.writeString(outputPath, "existing line\n")
        val entries = emptySequence<NominatimPlace>()

        jsonWriter.export(entries, outputPath, isAppending = true)

        val lines = Files.readAllLines(outputPath)
        assertEquals(1, lines.size)
        assertEquals("existing line", lines[0])
    }

    @Test
    fun `export appends to file correctly`(
        @TempDir tempDir: Path,
    ) {
        val outputPath = tempDir.resolve("output.ndjson")

        val firstBatch = sequenceOf(createTestPlace(1L))
        jsonWriter.export(firstBatch, outputPath, isAppending = false)

        val secondBatch = sequenceOf(createTestPlace(2L), createTestPlace(3L))
        jsonWriter.export(secondBatch, outputPath, isAppending = true)

        val lines = Files.readAllLines(outputPath)
        assertEquals(4, lines.size)

        val place1 = objectMapper.readValue<NominatimPlace>(lines[1])
        assertEquals(1L, place1.content[0].place_id)

        val place2 = objectMapper.readValue<NominatimPlace>(lines[2])
        assertEquals(2L, place2.content[0].place_id)

        val place3 = objectMapper.readValue<NominatimPlace>(lines[3])
        assertEquals(3L, place3.content[0].place_id)
    }

    private fun createTestPlace(id: Long, includeOptional: Boolean = false): NominatimPlace =
        NominatimPlace(
            type = "place",
            content =
                listOf(
                    NominatimPlace.PlaceContent(
                        place_id = id,
                        object_type = "N",
                        object_id = id,
                        categories = listOf("place", "city"),
                        rank_address = 16,
                        importance = 0.5.toBigDecimal(),
                        parent_place_id = if (includeOptional) 999L else null,
                        name = if (includeOptional) NominatimPlace.Name("Test Place", "Alt Name") else null,
                        address =
                            NominatimPlace.Address(
                                street = if (includeOptional) "Test Street" else null,
                                city = "Oslo",
                                county = "Oslo",
                            ),
                        housenumber = if (includeOptional) "42" else null,
                        postcode = "0001",
                        country_code = "NO",
                        centroid = listOf(BigDecimal("10.7522"), BigDecimal("59.9139")),
                        bbox = emptyList(),
                        extra =
                            Extra(
                                source = "place:city",
                            ),
                    ),
                ),
        )
}
