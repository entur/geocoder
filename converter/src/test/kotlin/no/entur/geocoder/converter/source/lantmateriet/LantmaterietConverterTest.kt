package no.entur.geocoder.converter.source.lantmateriet

import com.fasterxml.jackson.module.kotlin.readValue
import no.entur.geocoder.common.Category
import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.converter.TestConfig
import no.entur.geocoder.converter.target.NominatimPlace
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.locationtech.jts.geom.CoordinateXY
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.ByteOrderValues
import org.locationtech.jts.io.WKBWriter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LantmaterietConverterTest {
    @TempDir
    lateinit var tempDir: Path

    lateinit var gpkgFile: File
    lateinit var converter: LantmaterietConverter

    @BeforeAll
    fun setup(
        @TempDir dir: Path,
    ) {
        tempDir = dir
        converter = LantmaterietConverter(TestConfig.config)
        gpkgFile = tempDir.resolve("test.gpkg").toFile()
        createTestGeoPackage(gpkgFile)
    }

    @Test
    fun `should convert GeoPackage to nominatim NDJSON`() {
        val outputFile = tempDir.resolve("output.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)
        val lines = outputFile.readLines()
        assertTrue(lines.size > 1, "Should have header + data lines")
    }

    @Test
    fun `should generate both address and street entries`() {
        val outputFile = tempDir.resolve("output_both.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val addressEntries = lines.filter { it.contains("osm.public_transport.address") }
        val streetEntries = lines.filter { it.contains("osm.public_transport.street") }

        assertTrue(addressEntries.isNotEmpty(), "Should have address entries")
        assertTrue(streetEntries.isNotEmpty(), "Should have street entries")
    }

    @Test
    fun `street address type should map to street`() {
        val outputFile = tempDir.resolve("output_street_type.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val streetAddressLine = lines.find { it.contains("uuid-gatu-1") }
        assertNotNull(streetAddressLine, "Should find Gatuadressplats entry")

        val place: NominatimPlace = jacksonMapper.readValue(streetAddressLine)
        val content = place.content.first()
        assertEquals("Storgatan", content.address.street)
    }

    @Test
    fun `by address type should use place name`() {
        val outputFile = tempDir.resolve("output_by_type.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val byAddressLine = lines.find { it.contains("uuid-by-1") }
        assertNotNull(byAddressLine, "Should find Byadressplats entry")

        val place: NominatimPlace = jacksonMapper.readValue(byAddressLine)
        val content = place.content.first()
        // Byadressplats is not a street address, so street should be null
        assertEquals(null, content.address.street)
    }

    @Test
    fun `gard address type should use farm name`() {
        val outputFile = tempDir.resolve("output_gard_type.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val gardAddressLine = lines.find { it.contains("uuid-gard-1") }
        assertNotNull(gardAddressLine, "Should find Gardsadressplats entry")

        val place: NominatimPlace = jacksonMapper.readValue(gardAddressLine)
        val content = place.content.first()
        // Gardsadressplats is not a street address
        assertEquals(null, content.address.street)
    }

    @Test
    fun `house number assembly with letter`() {
        val outputFile = tempDir.resolve("output_housenumber.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val addressLine = lines.find { it.contains("uuid-gatu-2") }
        assertNotNull(addressLine, "Should find address with letter")

        val place: NominatimPlace = jacksonMapper.readValue(addressLine)
        assertEquals("5B", place.content.first().housenumber)
    }

    @Test
    fun `house number with entrance code`() {
        val outputFile = tempDir.resolve("output_entrance.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val addressLine = lines.find { it.contains("uuid-gatu-3") }
        assertNotNull(addressLine, "Should find address with entrance")

        val place: NominatimPlace = jacksonMapper.readValue(addressLine)
        assertEquals("10 LGH12", place.content.first().housenumber)
    }

    @Test
    fun `non-standard address uses avvikande beteckning`() {
        val outputFile = tempDir.resolve("output_avvikande.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val addressLine = lines.find { it.contains("uuid-avvik-1") }
        assertNotNull(addressLine, "Should find non-standard address")

        val place: NominatimPlace = jacksonMapper.readValue(addressLine)
        assertEquals("S:t Eriksgatan 99X", place.content.first().housenumber)
    }

    @Test
    fun `non-Gallande records should be filtered out`() {
        val outputFile = tempDir.resolve("output_filtered.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val avregistreradLine = lines.find { it.contains("uuid-avregistrerad") }
        assertEquals(null, avregistreradLine, "Should not contain non-Gallande records")
    }

    @Test
    fun `coordinates should be valid WGS84`() {
        val outputFile = tempDir.resolve("output_coords.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        lines.forEach { line ->
            val place: NominatimPlace = jacksonMapper.readValue(line)
            val centroid = place.content.first().centroid
            assertEquals(2, centroid.size, "Centroid should have 2 coordinates")
            assertTrue(centroid[0].toDouble() in -180.0..180.0, "Longitude should be valid")
            assertTrue(centroid[1].toDouble() in -90.0..90.0, "Latitude should be valid")
        }
    }

    @Test
    fun `country code should be se and country_a SWE`() {
        val outputFile = tempDir.resolve("output_country.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        assertTrue(lines.isNotEmpty())

        val place: NominatimPlace = jacksonMapper.readValue(lines.first())
        val content = place.content.first()
        assertEquals("se", content.country_code)
        assertEquals("SWE", content.extra.country_a)
    }

    @Test
    fun `categories should include SOURCE_LANTMATERIET`() {
        val outputFile = tempDir.resolve("output_categories.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        lines.forEach { line ->
            val place: NominatimPlace = jacksonMapper.readValue(line)
            val categories = place.content.first().categories
            assertTrue(
                categories.contains(Category.SOURCE_LANTMATERIET),
                "Should contain SOURCE_LANTMATERIET category",
            )
        }
    }

    @Test
    fun `should handle directory input with multiple gpkg files`() {
        val dir = tempDir.resolve("gpkg_dir").toFile()
        dir.mkdirs()
        val file1 = dir.resolve("file1.gpkg")
        val file2 = dir.resolve("file2.gpkg")
        createTestGeoPackage(file1)
        createTestGeoPackage(file2)

        val outputFile = tempDir.resolve("output_dir.ndjson").toFile()
        converter.convert(dir, outputFile)

        assertTrue(outputFile.exists())
        val lines = outputFile.readLines().drop(1)
        // Should have entries from both files (though streets may be deduplicated)
        assertTrue(lines.size > 2, "Should have entries from multiple files")
    }

    @Test
    fun `should have county name from lanskod`() {
        val outputFile = tempDir.resolve("output_county.ndjson").toFile()

        converter.convert(gpkgFile, outputFile)

        val lines = outputFile.readLines().drop(1)
        val addressLine = lines.find { it.contains("uuid-gatu-1") }
        assertNotNull(addressLine)

        val place: NominatimPlace = jacksonMapper.readValue(addressLine)
        val county =
            place.content
                .first()
                .address.county
        assertEquals("Stockholms län", county)
    }

    private fun createTestGeoPackage(file: File) {
        val url = "jdbc:sqlite:${file.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                // Create GeoPackage metadata tables
                stmt.execute(
                    """
                    CREATE TABLE gpkg_contents (
                        table_name TEXT NOT NULL PRIMARY KEY,
                        data_type TEXT NOT NULL,
                        identifier TEXT,
                        description TEXT,
                        last_change TEXT,
                        min_x DOUBLE,
                        min_y DOUBLE,
                        max_x DOUBLE,
                        max_y DOUBLE,
                        srs_id INTEGER
                    )
                    """.trimIndent(),
                )

                stmt.execute(
                    """
                    CREATE TABLE belagenhetsadress (
                        objektidentitet TEXT,
                        adressplatstyp TEXT,
                        adressomrade_faststalltnamn TEXT,
                        gardsadressomrade_faststalltnamn TEXT,
                        adressplatsnummer INTEGER,
                        bokstavstillagg TEXT,
                        lagestillagg TEXT,
                        lagestillaggsnummer INTEGER,
                        avvikerfranstandarden INTEGER DEFAULT 0,
                        avvikandeadressplatsbeteckning TEXT,
                        postnummer TEXT,
                        postort TEXT,
                        kommunkod TEXT,
                        kommunnamn TEXT,
                        lanskod TEXT,
                        popularnamn TEXT,
                        kommundel_faststalltnamn TEXT,
                        statusforbelagenhetsadress TEXT,
                        geom BLOB
                    )
                    """.trimIndent(),
                )

                stmt.execute(
                    "INSERT INTO gpkg_contents (table_name, data_type) VALUES ('belagenhetsadress', 'features')",
                )
            }

            // Insert test data
            val insertSql =
                """
                INSERT INTO belagenhetsadress (
                    objektidentitet, adressplatstyp, adressomrade_faststalltnamn,
                    gardsadressomrade_faststalltnamn, adressplatsnummer, bokstavstillagg,
                    lagestillagg, lagestillaggsnummer, avvikerfranstandarden,
                    avvikandeadressplatsbeteckning, postnummer, postort,
                    kommunkod, kommunnamn, lanskod, popularnamn,
                    kommundel_faststalltnamn, statusforbelagenhetsadress, geom
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()

            conn.prepareStatement(insertSql).use { ps ->
                // Gatuadressplats (street address) - simple number
                insertAddress(
                    ps,
                    "uuid-gatu-1",
                    "Gatuadressplats",
                    "Storgatan",
                    null,
                    42,
                    null,
                    null,
                    null,
                    false,
                    null,
                    "11120",
                    "STOCKHOLM",
                    "0180",
                    "Stockholm",
                    "01",
                    null,
                    "Södermalm",
                    "Gallande",
                    674032.0,
                    6580994.0,
                )

                // Gatuadressplats with letter
                insertAddress(
                    ps,
                    "uuid-gatu-2",
                    "Gatuadressplats",
                    "Storgatan",
                    null,
                    5,
                    "B",
                    null,
                    null,
                    false,
                    null,
                    "11120",
                    "STOCKHOLM",
                    "0180",
                    "Stockholm",
                    "01",
                    null,
                    null,
                    "Gallande",
                    674050.0,
                    6581010.0,
                )

                // Gatuadressplats with entrance code
                insertAddress(
                    ps,
                    "uuid-gatu-3",
                    "Gatuadressplats",
                    "Kungsgatan",
                    null,
                    10,
                    null,
                    "LGH",
                    12,
                    false,
                    null,
                    "11120",
                    "STOCKHOLM",
                    "0180",
                    "Stockholm",
                    "01",
                    null,
                    null,
                    "Gallande",
                    674070.0,
                    6581020.0,
                )

                // Non-standard address
                insertAddress(
                    ps,
                    "uuid-avvik-1",
                    "Gatuadressplats",
                    "Storgatan",
                    null,
                    99,
                    null,
                    null,
                    null,
                    true,
                    "S:t Eriksgatan 99X",
                    "11120",
                    "STOCKHOLM",
                    "0180",
                    "Stockholm",
                    "01",
                    null,
                    null,
                    "Gallande",
                    674090.0,
                    6581030.0,
                )

                // Byadressplats (village address)
                insertAddress(
                    ps,
                    "uuid-by-1",
                    "Byadressplats",
                    "Norrby",
                    null,
                    3,
                    null,
                    null,
                    null,
                    false,
                    null,
                    "74040",
                    "ENKÖPING",
                    "0381",
                    "Enköping",
                    "03",
                    null,
                    null,
                    "Gallande",
                    640000.0,
                    6610000.0,
                )

                // Gardsadressplats (farm address)
                insertAddress(
                    ps,
                    "uuid-gard-1",
                    "Gardsadressplats",
                    null,
                    "Storegården",
                    1,
                    null,
                    null,
                    null,
                    false,
                    null,
                    "74040",
                    "ENKÖPING",
                    "0381",
                    "Enköping",
                    "03",
                    null,
                    null,
                    "Gallande",
                    640010.0,
                    6610010.0,
                )

                // Non-Gallande (should be filtered out)
                insertAddress(
                    ps,
                    "uuid-avregistrerad",
                    "Gatuadressplats",
                    "Gamlavägen",
                    null,
                    1,
                    null,
                    null,
                    null,
                    false,
                    null,
                    "11120",
                    "STOCKHOLM",
                    "0180",
                    "Stockholm",
                    "01",
                    null,
                    null,
                    "Avregistrerad",
                    674100.0,
                    6581040.0,
                )
            }
        }
    }

    @Suppress("LongParameterList")
    private fun insertAddress(
        ps: java.sql.PreparedStatement,
        objektidentitet: String,
        adressplatstyp: String,
        adressomradeFaststalltnamn: String?,
        gardsadressomradeFaststalltnamn: String?,
        adressplatsnummer: Int?,
        bokstavstillagg: String?,
        lagestillagg: String?,
        lagestillaggsnummer: Int?,
        avvikerfranstandarden: Boolean,
        avvikandeadressplatsbeteckning: String?,
        postnummer: String,
        postort: String,
        kommunkod: String,
        kommunnamn: String,
        lanskod: String,
        popularnamn: String?,
        kommundelFaststalltnamn: String?,
        status: String,
        easting: Double,
        northing: Double,
    ) {
        ps.setString(1, objektidentitet)
        ps.setString(2, adressplatstyp)
        ps.setString(3, adressomradeFaststalltnamn)
        ps.setString(4, gardsadressomradeFaststalltnamn)
        if (adressplatsnummer != null) ps.setInt(5, adressplatsnummer) else ps.setNull(5, java.sql.Types.INTEGER)
        ps.setString(6, bokstavstillagg)
        ps.setString(7, lagestillagg)
        if (lagestillaggsnummer != null) ps.setInt(8, lagestillaggsnummer) else ps.setNull(8, java.sql.Types.INTEGER)
        ps.setInt(9, if (avvikerfranstandarden) 1 else 0)
        ps.setString(10, avvikandeadressplatsbeteckning)
        ps.setString(11, postnummer)
        ps.setString(12, postort)
        ps.setString(13, kommunkod)
        ps.setString(14, kommunnamn)
        ps.setString(15, lanskod)
        ps.setString(16, popularnamn)
        ps.setString(17, kommundelFaststalltnamn)
        ps.setString(18, status)
        ps.setBytes(19, createGeoPackagePoint(easting, northing))
        ps.executeUpdate()
    }

    companion object {
        private fun createGeoPackagePoint(x: Double, y: Double): ByteArray {
            val factory = GeometryFactory()
            val point = factory.createPoint(CoordinateXY(x, y))
            val wkbWriter = WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN)
            val wkb = wkbWriter.write(point)

            // GeoPackage binary geometry header
            val buf = ByteBuffer.allocate(8 + wkb.size)
            buf.order(ByteOrder.LITTLE_ENDIAN)
            buf.put(0x47.toByte()) // 'G'
            buf.put(0x50.toByte()) // 'P'
            buf.put(0x00.toByte()) // version
            buf.put(0x01.toByte()) // flags: little-endian, no envelope
            buf.putInt(3006) // SWEREF 99 TM SRS ID
            buf.put(wkb)
            return buf.array()
        }
    }
}
