package no.entur.geocoder.converter.stedsnavn

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.converter.NominatimPlace
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.math.BigDecimal
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StedsnavnConverterTest {
    @TempDir
    lateinit var tempDir: Path

    private lateinit var converter: StedsnavnConverter
    private lateinit var inputFile: File
    private lateinit var entries: List<StedsnavnEntry>

    @BeforeEach
    fun setup() {
        converter = StedsnavnConverter()
        inputFile = getTestFile()
        entries = converter.parseGml(inputFile).toList()
    }

    private fun getTestFile(): File {
        val resource = javaClass.classLoader.getResource("Basisdata_3420_Elverum_25833_Stedsnavn_GML.gml")
            ?: error("Test file not found")
        return File(resource.file)
    }

    @Test
    fun `should convert stedsnavn GML to nominatim json`() {
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

        assertEquals("whosonfirst", extra.source, "Source should be whosonfirst")
        assertEquals("point", extra.accuracy, "Accuracy should be point")
        assertEquals("NOR", extra.country_a, "Country code should be NOR")
        assertNotNull(extra.locality, "Locality should not be null")
        assertNotNull(extra.locality_gid, "Locality GID should not be null")
        assertNotNull(extra.county_gid, "County GID should not be null")
        assertNotNull(extra.label, "Label should not be null")

        assertNotNull(placeContent.centroid, "Centroid should not be null")
        assertEquals(2, placeContent.centroid.size, "Centroid should have 2 coordinates")

        assertNotNull(placeContent.name, "Name should not be null")
        assertNotNull(placeContent.address, "Address should not be null")
        assertEquals("no", placeContent.country_code, "Country code should be 'no'")
        assertEquals(listOf("street"), placeContent.categories, "Categories should contain 'street'")
        assertEquals(26, placeContent.rank_address, "Rank address should be 26")
    }

    @Test
    fun `should parse correct number of entries with vegreferanse`() {
        assertTrue(entries.isNotEmpty(), "Should parse at least one entry")
        assertTrue(entries.size >= 400, "Should parse at least 400 entries with vegreferanse")
    }

    @Test
    fun `should parse Andreas Grottings veg correctly`() {
        val andreasGrottingsVeg = entries.find { it.stedsnavn.contains("Andreas") }

        assertNotNull(andreasGrottingsVeg, "Should find Andreas Grøttings veg")
        assertEquals("39418", andreasGrottingsVeg.lokalId)
        assertEquals("https://data.geonorge.no/sosi/stedsnavn", andreasGrottingsVeg.navnerom)
        assertEquals("3420", andreasGrottingsVeg.kommunenummer)
        assertEquals("Elverum", andreasGrottingsVeg.kommunenavn)
        assertEquals("34", andreasGrottingsVeg.fylkesnummer)
        assertEquals("Innlandet", andreasGrottingsVeg.fylkesnavn)
        assertEquals("225593282", andreasGrottingsVeg.matrikkelId)
        assertEquals("1050", andreasGrottingsVeg.adressekode)
        assertTrue(andreasGrottingsVeg.coordinates.isNotEmpty(), "Should have coordinates")
    }

    @Test
    fun `should convert coordinates from UTM33 to WGS84`() {
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
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val extra = nominatimPlace.content.first().extra

        assertNotNull(extra.id, "ID should not be null")
        val id = extra.id ?: error("ID should not be null")
        assertTrue(
            id.startsWith("KVE:TopographicPlace:"),
            "ID should start with KVE:TopographicPlace:"
        )
        assertTrue(
            id.contains("-"),
            "ID should contain hyphen separator"
        )
    }

    @Test
    fun `should generate correct label format`() {
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val extra = nominatimPlace.content.first().extra

        assertNotNull(extra.label, "Label should not be null")
        val label = extra.label ?: error("Label should not be null")
        assertTrue(
            label.contains(entry.stedsnavn),
            "Label should contain street name"
        )
        assertTrue(
            label.contains(entry.kommunenavn),
            "Label should contain municipality name"
        )
        assertTrue(
            label.contains(", "),
            "Label should be formatted as 'streetname, municipality'"
        )
    }

    @Test
    fun `should have correct locality and county GID format`() {
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val extra = nominatimPlace.content.first().extra

        assertEquals(
            "whosonfirst:locality:KVE:TopographicPlace:${entry.kommunenummer}",
            extra.locality_gid,
            "Locality GID should have correct format"
        )
        assertEquals(
            "whosonfirst:county:KVE:TopographicPlace:${entry.fylkesnummer}",
            extra.county_gid,
            "County GID should have correct format"
        )
    }

    @Test
    fun `should only parse entries with vegreferanse`() {
        entries.forEach { entry ->
            assertNotNull(entry.matrikkelId, "All entries should have matrikkelId from vegreferanse")
            assertNotNull(entry.adressekode, "All entries should have adressekode from vegreferanse")
        }
    }

    @Test
    fun `should parse all required fields`() {
        entries.forEach { entry ->
            assertNotNull(entry.lokalId, "lokalId should not be null")
            assertNotNull(entry.navnerom, "navnerom should not be null")
            assertNotNull(entry.stedsnavn, "stedsnavn should not be null")
            assertNotNull(entry.kommunenummer, "kommunenummer should not be null")
            assertNotNull(entry.kommunenavn, "kommunenavn should not be null")
            assertNotNull(entry.fylkesnummer, "fylkesnummer should not be null")
            assertNotNull(entry.fylkesnavn, "fylkesnavn should not be null")
            assertNotNull(entry.matrikkelId, "matrikkelId should not be null")
            assertNotNull(entry.adressekode, "adressekode should not be null")
        }
    }

    @Test
    fun `should titleize municipality name in address`() {
        val entry = entries.first()
        val nominatimPlace = converter.convertToNominatim(entry)
        val address = nominatimPlace.content.first().address

        assertNotNull(address, "Address should not be null")
        assertNotNull(address.city, "City should not be null")

        val city = address.city ?: error("City should not be null")
        val firstChar = city.first()
        assertTrue(firstChar.isUpperCase(), "First character of city name should be uppercase")
    }
}

