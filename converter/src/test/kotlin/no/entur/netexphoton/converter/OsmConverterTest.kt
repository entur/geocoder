package no.entur.netexphoton.converter

import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Tag
import java.io.File
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OsmConverterTest {
    @Test
    fun testOsmPbfParsing() {
        // This test requires a small OSM PBF file for testing
        // You can create a mock file or use a real small PBF file
        val osmConverter = OsmConverter()

        // Create a mock PBF parsing method that returns test data
        val testEntities = osmConverter.parseMockPbfForTesting()

        // Verify we have some entities
        assertTrue(testEntities.count() > 0, "Should return at least one entity")
    }

    @Test
    fun testConvertOsmEntityToNominatim() {
        val osmConverter = OsmConverter()

        // Create a mock node with test data
        val mockNode = createMockNode()

        // Convert it to Nominatim format
        val nominatimPlace = osmConverter.convertOsmEntityToNominatim(mockNode)

        // Verify the conversion worked
        assertNotNull(nominatimPlace, "Should successfully convert the node")
        assertEquals(
            "Test Node",
            nominatimPlace.content[0].name?.name,
            "Should have the correct name",
        )
        assertEquals(
            "amenity",
            nominatimPlace.content[0].extratags.layer,
            "Should have the correct layer",
        )
    }

    @Test
    fun testFullConversionProcess() {
        // This test would ideally use a small test PBF file
        // For now, we'll just verify the converter can be instantiated
        val osmConverter = OsmConverter()

        // Make sure we can create a converter instance without exceptions
        assertNotNull(osmConverter, "Should create converter instance")

        // If you have a test PBF file, you could test the full conversion process:
         val testInputFile = File("src/test/resources/andorra-latest.osm.pbf")
         val testOutputFile = File("build/tmp/test_output.json")
         osmConverter.convert(testInputFile, testOutputFile)
         assertTrue(testOutputFile.exists(), "Output file should be created")
         assertTrue(testOutputFile.length() > 0, "Output file should have content")
    }

    /**
     * Helper method to create a mock OSM node for testing
     */
    private fun createMockNode(): Node {
        val tags =
            listOf(
                Tag("name", "Test Node"),
                Tag("amenity", "restaurant"),
                Tag("cuisine", "italian"),
            )

        val entityData =
            CommonEntityData(
                1L, // id
                1, // version
                Date(), // timestamp
                null, // user
                0L, // changesetId
                tags,
            )

        return Node(entityData, 59.9133, 10.7389) // Oslo coordinates
    }
}
