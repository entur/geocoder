package no.entur.netexphoton.converter.osm

import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Tag
import java.io.File
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OsmConverterTest {
    @Test
    fun testConvertOsmEntityToNominatim() {
        val osmConverter = OsmConverter()

        val mockNode = createMockNode()

        val nominatimPlace = osmConverter.convertOsmEntityToNominatim(mockNode)

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
        val osmConverter = OsmConverter()

        assertNotNull(osmConverter, "Should create converter instance")

        val testInputFile = File("src/test/resources/andorra-latest.osm.pbf")
        val testOutputFile = File("build/tmp/test_output.ndjson")
        osmConverter.convert(testInputFile, testOutputFile)
        assertTrue(testOutputFile.exists(), "Output file should be created")
        assertTrue(testOutputFile.length() > 0, "Output file should have content")
    }

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
