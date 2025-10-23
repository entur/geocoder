package no.entur.geocoder.converter.osm

import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Tag
import org.openstreetmap.osmosis.core.domain.v0_6.Way
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode
import java.io.File
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OsmConverterTest {
    @Test
    fun testConvertOsmNodeToNominatim() {
        val osmConverter = OsmConverter()

        val mockNode = createMockNode(
            id = 1L,
            name = "Test Node",
            lat = 59.9133,
            lon = 10.7389,
            tags = listOf(
                Tag("name", "Test Node"),
                Tag("amenity", "restaurant"),
            )
        )

        val nominatimPlace = osmConverter.convertOsmEntityToNominatim(mockNode)

        assertNotNull(nominatimPlace, "Should successfully convert the node")
        assertEquals(
            "Test Node",
            nominatimPlace.content[0].name?.name,
            "Should have the correct name",
        )
        assertEquals("N", nominatimPlace.content[0].object_type, "Should have object_type 'N' for Node")
        assertEquals("point", nominatimPlace.content[0].extra.accuracy, "Should have 'point' accuracy for Node")
    }

    @Test
    fun testConvertOsmWayWithoutCoordinates() {
        val osmConverter = OsmConverter()

        val mockWay = createMockWay(
            id = 1L,
            name = "Test Cinema",
            tags = listOf(
                Tag("name", "Test Cinema"),
                Tag("amenity", "cinema"),
            ),
            nodeIds = listOf(100L, 101L, 102L, 103L)
        )

        // Without pre-loaded node coordinates, way conversion should return null
        val nominatimPlace = osmConverter.convertOsmEntityToNominatim(mockWay)

        assertNull(nominatimPlace, "Should return null when way node coordinates are not available")
    }

    @Test
    fun testConvertOsmWayWithMissingName() {
        val osmConverter = OsmConverter()

        val mockWay = createMockWay(
            id = 1L,
            name = null,
            tags = listOf(
                Tag("amenity", "cinema"),
            ),
            nodeIds = listOf(100L, 101L, 102L, 103L)
        )

        val nominatimPlace = osmConverter.convertOsmEntityToNominatim(mockWay)

        assertNull(nominatimPlace, "Should return null when way doesn't have a name")
    }

    @Test
    fun testConvertOsmWayWithoutMatchingTags() {
        val osmConverter = OsmConverter()

        val mockWay = createMockWay(
            id = 1L,
            name = "Random Way",
            tags = listOf(
                Tag("name", "Random Way"),
                Tag("random_tag", "random_value"),
            ),
            nodeIds = listOf(100L, 101L, 102L, 103L)
        )

        val nominatimPlace = osmConverter.convertOsmEntityToNominatim(mockWay)

        assertNull(nominatimPlace, "Should return null when way doesn't have any matching POI tags")
    }

    @Test
    fun testIsPotentialPoiCheck() {
        val osmConverter = OsmConverter()

        // Create ways to test
        val cinemaWay = createMockWay(
            id = 1L,
            name = "ODEON Oslo",
            tags = listOf(
                Tag("name", "ODEON Oslo"),
                Tag("amenity", "cinema"),
            ),
            nodeIds = listOf(100L, 101L, 102L)
        )

        val noNameWay = createMockWay(
            id = 2L,
            name = null,
            tags = listOf(
                Tag("amenity", "cinema"),
            ),
            nodeIds = listOf(200L, 201L, 202L)
        )

        val noMatchingTagWay = createMockWay(
            id = 3L,
            name = "Random Building",
            tags = listOf(
                Tag("name", "Random Building"),
                Tag("building", "yes"),
            ),
            nodeIds = listOf(300L, 301L, 302L)
        )

        // Test using reflection to access private method
        val isPotentialPoiMethod = OsmConverter::class.java.getDeclaredMethod("isPotentialPoi", org.openstreetmap.osmosis.core.domain.v0_6.Entity::class.java)
        isPotentialPoiMethod.isAccessible = true

        assertTrue(
            isPotentialPoiMethod.invoke(osmConverter, cinemaWay) as Boolean,
            "Cinema with name should be potential POI"
        )
        assertTrue(
            !(isPotentialPoiMethod.invoke(osmConverter, noNameWay) as Boolean),
            "Cinema without name should not be potential POI"
        )
        assertTrue(
            !(isPotentialPoiMethod.invoke(osmConverter, noMatchingTagWay) as Boolean),
            "Building without matching tags should not be potential POI"
        )
    }

    @Test
    fun testFullConversionProcess() {
        val osmConverter = OsmConverter()

        assertNotNull(osmConverter, "Should create converter instance")

        val testInputFile = File("src/test/resources/oslo-opera.osm.pbf")
        assertTrue(testInputFile.exists(), "Test input file should exist")

        val testOutputFile = File("build/tmp/test_output.ndjson")
        testOutputFile.parentFile.mkdirs()

        osmConverter.convert(testInputFile, testOutputFile)

        assertTrue(testOutputFile.exists(), "Output file should be created")
        assertTrue(testOutputFile.length() > 0, "Output file should have content")

        // Clean up
        testOutputFile.delete()
    }

    @Test
    fun testConversionIncludesPoiWays() {
        val osmConverter = OsmConverter()
        val testInputFile = File("src/test/resources/oslo-center.osm.pbf")

        if (!testInputFile.exists()) {
            println("Skipping test - oslo-center.osm.pbf not available")
            return
        }

        val testOutputFile = File("build/tmp/test_poi_ways_output.ndjson")
        testOutputFile.parentFile.mkdirs()

        osmConverter.convert(testInputFile, testOutputFile)

        assertTrue(testOutputFile.exists(), "Output file should be created")
        assertTrue(testOutputFile.length() > 0, "Output file should have content")

        // Read the output and verify it contains POI ways (object_type: "W")
        val content = testOutputFile.readText()
        val hasWayPois = content.contains("\"object_type\":\"W\"")

        println("Output contains POI ways: $hasWayPois")

        // Clean up
        testOutputFile.delete()
    }

    private fun createMockNode(
        id: Long,
        name: String?,
        lat: Double,
        lon: Double,
        tags: List<Tag>
    ): Node {
        val allTags = if (name != null && tags.none { it.key == "name" }) {
            tags + Tag("name", name)
        } else {
            tags
        }

        val entityData = CommonEntityData(
            id,
            1, // version
            Date(),
            null, // user
            0L, // changesetId
            allTags,
        )

        return Node(entityData, lat, lon)
    }

    private fun createMockWay(
        id: Long,
        name: String?,
        tags: List<Tag>,
        nodeIds: List<Long>
    ): Way {
        val allTags = if (name != null && tags.none { it.key == "name" }) {
            tags + Tag("name", name)
        } else {
            tags
        }

        val entityData = CommonEntityData(
            id,
            1, // version
            Date(),
            null, // user
            0L, // changesetId
            allTags,
        )

        val wayNodes = nodeIds.map { WayNode(it) }

        return Way(entityData, wayNodes)
    }
}
