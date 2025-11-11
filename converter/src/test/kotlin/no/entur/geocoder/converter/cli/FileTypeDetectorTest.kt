package no.entur.geocoder.converter.cli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertNotNull

class FileTypeDetectorTest {
    private val detector = FileTypeDetector()

    @Test
    fun `detectFileType identifies XML from test resources`() {
        val xmlFile = getTestResource("/oslo.xml")
        val type = detector.detectFileType(xmlFile)
        assertEquals(FileTypeDetector.FileType.XML, type)
    }

    @Test
    fun `detectFileType identifies GML from test resources`() {
        val gmlFile = getTestResource("/bydel.gml")
        val type = detector.detectFileType(gmlFile)
        assertEquals(FileTypeDetector.FileType.GML, type)
    }

    @Test
    fun `detectFileType identifies CSV from test resources`() {
        val csvFile = getTestResource("/Basisdata_3420_Elverum_25833_MatrikkelenAdresse.csv")
        val type = detector.detectFileType(csvFile)
        assertEquals(FileTypeDetector.FileType.CSV, type)
    }

    @Test
    fun `detectFileType identifies PBF from test resources`() {
        val pbfFile = getTestResource("/oslo-center.osm.pbf")
        val type = detector.detectFileType(pbfFile)
        assertEquals(FileTypeDetector.FileType.PBF, type)
    }

    @Test
    fun `detectFileType returns UNKNOWN for non-existent file`() {
        val file = File("/non/existent/file.txt")
        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.UNKNOWN, type)
    }

    @Test
    fun `detectFileType returns UNKNOWN for directory`() {
        val dir = File(System.getProperty("java.io.tmpdir"))
        val type = detector.detectFileType(dir)
        assertEquals(FileTypeDetector.FileType.UNKNOWN, type)
    }

    @Test
    fun `detectFileType identifies XML with proper header`() {
        val xmlContent =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <element>test</element>
            </root>
            """.trimIndent()
        val file = createTempFile(xmlContent, ".xml")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.XML, type)
        file.delete()
    }

    @Test
    fun `detectFileType identifies GML from XML with gml namespace`() {
        val gmlContent =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <gml:FeatureCollection xmlns:gml="http://www.opengis.net/gml">
                <gml:featureMember>test</gml:featureMember>
            </gml:FeatureCollection>
            """.trimIndent()
        val file = createTempFile(gmlContent, ".gml")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.GML, type)
        file.delete()
    }

    @Test
    fun `detectFileType identifies CSV with commas`() {
        val csvContent =
            """
            id,name,address,city
            1,Place A,Street 1,Oslo
            2,Place B,Street 2,Bergen
            3,Place C,Street 3,Trondheim
            """.trimIndent()
        val file = createTempFile(csvContent, ".csv")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.CSV, type)
        file.delete()
    }

    @Test
    fun `detectFileType identifies CSV with semicolons`() {
        val csvContent =
            """
            id;name;address;city
            1;Place A;Street 1;Oslo
            2;Place B;Street 2;Bergen
            3;Place C;Street 3;Trondheim
            """.trimIndent()
        val file = createTempFile(csvContent, ".csv")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.CSV, type)
        file.delete()
    }

    @Test
    fun `detectFileType handles empty file`() {
        val file = createTempFile("", ".txt")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.UNKNOWN, type)
        file.delete()
    }

    @Test
    fun `detectFileType handles file with only whitespace`() {
        val file = createTempFile("   \n\n  \t  ", ".txt")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.UNKNOWN, type)
        file.delete()
    }

    @Test
    fun `validateFileType throws for non-existent file`() {
        val file = File("/non/existent/file.xml")

        assertThrows<IllegalArgumentException> {
            detector.validateFileType(file, FileTypeDetector.FileType.XML, "--test-file")
        }
    }

    @Test
    fun `validateFileType throws for wrong file type`() {
        val csvFile = getTestResource("/Basisdata_3420_Elverum_25833_MatrikkelenAdresse.csv")

        val exception =
            assertThrows<IllegalArgumentException> {
                detector.validateFileType(csvFile, FileTypeDetector.FileType.XML, "--csv-file")
            }

        assertTrue(exception.message?.contains("Expected: XML") == true)
        assertTrue(exception.message?.contains("detected: CSV") == true)
    }

    @Test
    fun `validateFileType succeeds for correct XML file`() {
        val xmlFile = getTestResource("/oslo.xml")

        assertDoesNotThrow {
            detector.validateFileType(xmlFile, FileTypeDetector.FileType.XML, "--xml-file")
        }
    }

    @Test
    fun `validateFileType succeeds for correct GML file`() {
        val gmlFile = getTestResource("/bydel.gml")

        assertDoesNotThrow {
            detector.validateFileType(gmlFile, FileTypeDetector.FileType.GML, "--gml-file")
        }
    }

    @Test
    fun `validateFileType succeeds for correct CSV file`() {
        val csvFile = getTestResource("/Basisdata_3420_Elverum_25833_MatrikkelenAdresse.csv")

        assertDoesNotThrow {
            detector.validateFileType(csvFile, FileTypeDetector.FileType.CSV, "--csv-file")
        }
    }

    @Test
    fun `validateFileType succeeds for correct PBF file`() {
        val pbfFile = getTestResource("/oslo-center.osm.pbf")

        assertDoesNotThrow {
            detector.validateFileType(pbfFile, FileTypeDetector.FileType.PBF, "--pbf-file")
        }
    }

    @Test
    fun `detectFileType handles XML with leading whitespace`() {
        val xmlContent =
            """
            
            <?xml version="1.0"?>
            <root><element>test</element></root>
            """.trimIndent()
        val file = createTempFile(xmlContent, ".xml")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.XML, type)
        file.delete()
    }

    @Test
    fun `detectFileType handles malformed CSV with inconsistent columns`() {
        val csvContent =
            """
            col1,col2,col3
            a,b
            c,d,e,f
            g
            """.trimIndent()
        val file = createTempFile(csvContent, ".csv")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.CSV, type)
        file.delete()
    }

    @Test
    fun `detectFileType rejects file with no separators`() {
        val content =
            """
            This is just plain text
            without any separators
            or structure
            """.trimIndent()
        val file = createTempFile(content, ".txt")

        val type = detector.detectFileType(file)
        assertEquals(FileTypeDetector.FileType.UNKNOWN, type)
        file.delete()
    }

    private fun getTestResource(path: String): File {
        val resource = this::class.java.getResource(path)
        assertNotNull(resource, "Test resource $path not found")
        return File(resource.toURI())
    }

    private fun createTempFile(content: String, suffix: String): File {
        val file = File.createTempFile("test", suffix)
        file.writeText(content)
        return file
    }
}
