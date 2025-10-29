package no.entur.geocoder.converter.stedsnavn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.converter.photon.NominatimPlace
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

        val objectMapper = jacksonObjectMapper()
        val lines = outputFile.readLines()
        assertTrue(lines.isNotEmpty(), "Output file should have lines")
        assertTrue(lines.size > 1, "Output file should have at least two lines (header and data)")

        val firstPlaceJson = lines[1]
        val nominatimPlace: NominatimPlace = objectMapper.readValue(firstPlaceJson)

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
        assertTrue(placeContent.categories.any { it.startsWith("place") }, "Categories should contain 'place'")
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
            assertNotNull(entry.navnerom, "navnerom should not be null")
            assertNotNull(entry.stedsnavn, "stedsnavn should not be null")
            assertNotNull(entry.kommunenummer, "kommunenummer should not be null")
            assertNotNull(entry.kommunenavn, "kommunenavn should not be null")
            assertNotNull(entry.fylkesnummer, "fylkesnummer should not be null")
            assertNotNull(entry.fylkesnavn, "fylkesnavn should not be null")
            // matrikkelId and adressekode are optional for settlement types
        }
    }

    @Test
    fun `should titleize municipality name in address`() {
        parseDefault()
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val address = nominatimPlace.content.first().address

        assertNotNull(address, "Address should not be null")
        assertNotNull(address.city, "City should not be null")

        val city = address.city
        val firstChar = city.first()
        assertTrue(firstChar.isUpperCase(), "First character of city name should be uppercase")
    }

    @Test
    fun `should parse and store skrivemåtestatus field`() {
        parseDefault()
        // All entries in test file should have spelling status since they passed filtering
        val jomna = entries.find { it.stedsnavn == "Jømna" }
        assertNotNull(jomna, "Should find Jømna entry")
        assertNotNull(jomna.skrivemåtestatus, "Should have skrivemåtestatus field")
        assertTrue(
            StedsnavnSpellingStatus.isAccepted(jomna.skrivemåtestatus),
            "Parsed entry should have accepted spelling status",
        )
    }

    @Test
    fun `should filter out entries with rejected spelling status`() {
        parseDefault()
        // All parsed entries should have accepted spelling status
        entries.forEach { entry ->
            assertTrue(
                StedsnavnSpellingStatus.isAccepted(entry.skrivemåtestatus),
                "Entry ${entry.stedsnavn} should have accepted spelling status, got: ${entry.skrivemåtestatus}",
            )
        }
    }

    @Test
    fun `should use flat popularity for all place types matching kakka`() {
        parseDefault()
        val byEntry = entries.find { it.navneobjekttype == "by" }
        val tettbebyggelseEntry = entries.find { it.navneobjekttype == "tettbebyggelse" }

        assertNotNull(byEntry, "Should have at least one 'by' entry")
        assertNotNull(tettbebyggelseEntry, "Should have at least one 'tettbebyggelse' entry")

        val byPlace = converter.convertToNominatim(byEntry)
        val tettbebyggelsePlace = converter.convertToNominatim(tettbebyggelseEntry)

        val byImportance = byPlace.content.first().importance
        val tettbebyggelseImportance = tettbebyggelsePlace.content.first().importance

        // Both should have the same importance since popularity is flat (40 for all)
        assertEquals(
            byImportance,
            tettbebyggelseImportance,
            "All place types should have same importance (matching kakka's flat placeBoost)",
        )
    }

    @Test
    fun `should use correct popularity values from calculator`() {
        parseDefault()
        val tettbebyggelseEntry = entries.find { it.navneobjekttype == "tettbebyggelse" }
        assertNotNull(tettbebyggelseEntry, "Should have tettbebyggelse entry")

        val popularity = StedsnavnPopularityCalculator.calculatePopularity(tettbebyggelseEntry.navneobjekttype)
        assertEquals(40.0, popularity, "tettbebyggelse should have popularity of 40 (matching kakka)")

        val byEntry = entries.find { it.navneobjekttype == "by" }
        if (byEntry != null) {
            val byPopularity = StedsnavnPopularityCalculator.calculatePopularity(byEntry.navneobjekttype)
            assertEquals(40.0, byPopularity, "by should also have popularity of 40 (flat value matching kakka)")
        }
    }
}
