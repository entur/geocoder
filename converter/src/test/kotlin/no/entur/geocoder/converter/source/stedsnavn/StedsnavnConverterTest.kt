package no.entur.geocoder.converter.source.stedsnavn

import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.converter.target.NominatimPlace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.test.*

class StedsnavnConverterTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var converter: StedsnavnConverter
    private lateinit var inputFile: File
    private lateinit var entries: List<StedsnavnEntry>

    @BeforeEach
    fun setup() {
        converter = StedsnavnConverter()
    }

    fun parseDefault() {
        inputFile = getTestFile("Basisdata_3420_Elverum_25833_Stedsnavn_GML.gml")
        entries = converter.parseGml(inputFile).toList()
    }

    private fun getTestFile(file: String): File {
        val resource = javaClass.classLoader.getResource(file) ?: error("Test file not found")
        return File(resource.file)
    }

    @Test
    fun `Should find Stedsnavn even though one skrivemåtestatus is historisk`() {
        inputFile = getTestFile("bydel.gml")
        entries = converter.parseGml(inputFile).toList()

        assertEquals(2, entries.size)

        val outputFile = tempDir.resolve("output.json").toFile()
        converter.convert(inputFile, outputFile, isAppending = false)

        assertTrue(outputFile.readText().contains("Grünerløkka"))
        assertEquals(outputFile.readLines().size, 3)
    }

    @Test
    fun `should convert stedsnavn GML to nominatim json`() {
        parseDefault()
        val outputFile = tempDir.resolve("output.json").toFile()

        converter.convert(inputFile, outputFile, isAppending = false)

        assertTrue(outputFile.exists(), "Output file should exist")
        assertTrue(outputFile.length() > 0, "Output file should not be empty")

        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty(), "Output file should have lines")
        assertTrue(lines.size > 1, "Output file should have at least two lines (header and data)")

        val firstPlaceJson = lines[1]
        val nominatimPlace: NominatimPlace = jacksonMapper.readValue(firstPlaceJson)

        assertNotNull(nominatimPlace.content.firstOrNull(), "Place content should not be null")
        val placeContent = nominatimPlace.content.first()

        assertNotNull(placeContent.extra, "Extra should not be null")
        val extra = placeContent.extra

        assertEquals("kartverket-stedsnavn", extra.source, "Source should be kartverket")
        assertEquals("point", extra.accuracy, "Accuracy should be point")
        assertEquals("NOR", extra.country_a, "Country code should be NOR")
        assertNotNull(extra.locality, "Locality should not be null")
        assertNotNull(extra.locality_gid, "Locality GID should not be null")
        assertNotNull(extra.county_gid, "County GID should not be null")

        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Centroid should have 2 coordinates")

        assertNotNull(placeContent.name, "Name should not be null")
        assertNotNull(placeContent.address, "Address should not be null")
        assertEquals("no", placeContent.country_code, "Country code should be 'no'")
        assertTrue(placeContent.categories.any { it.startsWith(LEGACY_CATEGORY_PREFIX) }, "Categories should contain 'place'")
        assertTrue(placeContent.rank_address <= 20, "Rank address should be appropriate for settlement")
    }

    @Test
    fun `should parse correct number of entries with target navneobjekttype`() {
        parseDefault()
        assertTrue(entries.isNotEmpty(), "Should parse at least one entry")
        // Test file contains: 1 "by" (Elverum) + 2 "tettbebyggelse" (Jmna, Hanstad) = 3 entries
        assertEquals(3, entries.size, "Should parse exactly 3 entries with target navneobjekttype")
    }

    @Test
    fun `should parse Jømna tettbebyggelse correctly`() {
        parseDefault()
        val jomna = entries.find { it.stedsnavn == "Jømna" }

        assertNotNull(jomna, "Should find Jømna tettbebyggelse")
        assertEquals("22874", jomna.lokalId)
        assertEquals("https://data.geonorge.no/sosi/stedsnavn", jomna.navnerom)
        assertEquals("3420", jomna.kommunenummer)
        assertEquals("Elverum", jomna.kommunenavn)
        assertEquals("34", jomna.fylkesnummer)
        assertEquals("Innlandet", jomna.fylkesnavn)
        assertEquals("tettbebyggelse", jomna.navneobjekttype)
        assertEquals(null, jomna.matrikkelId, "Tettbebyggelse should not have matrikkelId")
        assertEquals(null, jomna.adressekode, "Tettbebyggelse should not have adressekode")
        assertTrue(jomna.coordinates.isNotEmpty(), "Should have coordinates")
    }

    @Test
    fun `should convert coordinates from UTM33 to WGS84`() {
        parseDefault()
        val firstEntry = entries.first()
        val nominatimPlace = converter.convertToNominatim(firstEntry)
        val placeContent = nominatimPlace.content.first()

        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Should have lon and lat")

        val lon = placeContent.centroid[0]
        val lat = placeContent.centroid[1]

        assertTrue(lon > BigDecimal.ZERO, "Longitude should be positive")
        assertTrue(lat > BigDecimal.ZERO, "Latitude should be positive")
        assertTrue(lon < BigDecimal("20"), "Longitude should be reasonable for Norway")
        assertTrue(lat > BigDecimal("58"), "Latitude should be reasonable for Norway")
        assertTrue(lat < BigDecimal("72"), "Latitude should be reasonable for Norway")
    }

    @Test
    fun `should generate correct ID format`() {
        parseDefault()
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val extra = nominatimPlace.content.first().extra

        assertNotNull(extra.id, "ID should not be null")
        val id = extra.id ?: error("ID should not be null")
        assertTrue(id.toLong() > 0)
    }

    @Test
    fun `should have correct locality and county GID format`() {
        parseDefault()
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val extra = nominatimPlace.content.first().extra

        assertEquals(
            "KVE:TopographicPlace:${entry.kommunenummer}",
            extra.locality_gid,
            "Locality GID should have correct format",
        )
        assertEquals(
            "KVE:TopographicPlace:${entry.fylkesnummer}",
            extra.county_gid,
            "County GID should have correct format",
        )
    }

    @Test
    fun `should only parse entries with target navneobjekttype`() {
        parseDefault()
        val targetTypes = setOf("tettsteddel", "bydel", "by", "tettsted", "tettbebyggelse")
        entries.forEach { entry ->
            assertTrue(
                entry.navneobjekttype != null && targetTypes.contains(entry.navneobjekttype.lowercase()),
                "All entries should have a target navneobjekttype",
            )
        }
    }

    @Test
    fun `should filter out administrative types like kommune`() {
        parseDefault()
        // The test file contains navneobjekttype="kommune" (administrative division)
        // This should be filtered out since it's not in the target types
        val kommuneEntry = entries.find { it.navneobjekttype == "kommune" }
        assertEquals(null, kommuneEntry, "Administrative type 'kommune' should be filtered out")

        // Verify no administrative types are parsed
        val administrativeTypes = setOf("kommune", "fylke", "grunnkrets")
        entries.forEach { entry ->
            assertFalse(
                administrativeTypes.contains(entry.navneobjekttype?.lowercase()),
                "Administrative types should be filtered out, found: ${entry.navneobjekttype}",
            )
        }
    }

    @Test
    fun `should parse all required fields`() {
        parseDefault()
        entries.forEach { entry ->
            assertNotNull(entry.lokalId, "lokalId should not be null")
            assertNotNull(entry.stedsnavn, "stedsnavn should not be null")
            assertNotNull(entry.navneobjekttype, "navneobjekttype should not be null")
            assertNotNull(entry.kommunenummer, "kommunenummer should not be null")
            assertNotNull(entry.kommunenavn, "kommunenavn should not be null")
            assertNotNull(entry.fylkesnummer, "fylkesnummer should not be null")
            assertNotNull(entry.fylkesnavn, "fylkesnavn should not be null")
            assertTrue(entry.coordinates.isNotEmpty(), "coordinates should not be empty")
        }
    }

    @Test
    fun `converted places should have valid rank_address`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val rankAddress = nominatimPlace.content.first().rank_address

            assertTrue(rankAddress in 1..30, "rank_address should be between 1 and 30, got $rankAddress")
            assertTrue(rankAddress <= 20, "Settlement rank_address should be <= 20 for appropriate visibility")
        }
    }

    @Test
    fun `should handle special characters in place names`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val name =
                nominatimPlace.content
                    .first()
                    .name
                    ?.name

            assertNotNull(name, "Name should not be null")
            assertFalse(name.isEmpty(), "Name should not be empty")
        }
    }

    @Test
    fun `output should contain header line`() {
        parseDefault()
        val outputFile = tempDir.resolve("output_header.json").toFile()

        converter.convert(inputFile, outputFile, isAppending = false)

        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty())
        assertTrue(lines[0].contains("NominatimDumpFile"), "First line should be header")
        assertTrue(lines[0].contains("version"), "Header should contain version")
    }

    @Test
    fun `all entries should have point accuracy`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val accuracy =
                nominatimPlace.content
                    .first()
                    .extra.accuracy

            assertEquals("point", accuracy, "All stedsnavn entries should have point accuracy")
        }
    }

    @Test
    fun `all entries should have correct source`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val source =
                nominatimPlace.content
                    .first()
                    .extra.source

            assertEquals("kartverket-stedsnavn", source, "All entries should have kartverket-stedsnavn source")
        }
    }

    @Test
    fun `all entries should have country code NO`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val countryCode = nominatimPlace.content.first().country_code

            assertEquals("no", countryCode, "All Norwegian places should have country code 'no'")
        }
    }

    @Test
    fun `importance values should be within valid range`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val importance =
                nominatimPlace.content
                    .first()
                    .importance
                    .toDouble()

            assertTrue(importance > 0.0, "Importance should be positive")
            assertTrue(importance <= 1.0, "Importance should not exceed 1.0")
        }
    }

    @Test
    fun `place names should be titleized`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val cityName =
                nominatimPlace.content
                    .first()
                    .address.city

            if (cityName != null) {
                val firstChar = cityName.first()
                assertTrue(
                    firstChar.isUpperCase() || !firstChar.isLetter(),
                    "City name should be titleized: $cityName",
                )
            }
        }
    }

    @Test
    fun `should handle entries with alternative names`() {
        val testFile = getTestFile("bydel.gml")
        val entries = converter.parseGml(testFile).toList()

        val entryWithAltName = entries.find { it.annenSkrivemåte.isNotEmpty() }
        if (entryWithAltName != null) {
            val nominatimPlace = converter.convertToNominatim(entryWithAltName)
            val altName =
                nominatimPlace.content
                    .first()
                    .name
                    ?.alt_name

            assertNotNull(altName, "Entry with alternative names should have alt_name populated")
        }
    }

    @Test
    fun `bbox should contain valid coordinates for point features`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val bbox = nominatimPlace.content.first().bbox
            val centroid = nominatimPlace.content.first().centroid

            assertNotNull(bbox, "Bbox should not be null")
            if (bbox.isNotEmpty()) {
                assertEquals(4, bbox.size, "Bbox should have 4 coordinates (minLon, minLat, maxLon, maxLat)")
                assertTrue(bbox[0].toDouble() in -180.0..180.0, "Bbox minLon should be valid")
                assertTrue(bbox[1].toDouble() in -90.0..90.0, "Bbox minLat should be valid")
                assertTrue(bbox[2].toDouble() in -180.0..180.0, "Bbox maxLon should be valid")
                assertTrue(bbox[3].toDouble() in -90.0..90.0, "Bbox maxLat should be valid")
            }
            assertTrue(centroid.size == 2, "Centroid should have 2 coordinates (lon, lat)")
        }
    }

    @Test
    fun `object_type should be N for stedsnavn points`() {
        parseDefault()
        entries.forEach { entry ->
            val nominatimPlace = converter.convertToNominatim(entry)
            val objectType = nominatimPlace.content.first().object_type

            assertEquals("N", objectType, "Stedsnavn entries should have object_type 'N' (node)")
        }
    }
}
