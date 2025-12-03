package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import no.entur.geocoder.common.Country
import no.entur.geocoder.converter.ConverterConfig
import no.entur.geocoder.converter.source.ImportanceCalculator
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.util.*
import kotlin.test.*

class OsmConverterTest {
    private fun createConverter(): OsmEntityConverter {
        val nodesCoords = CoordinateStore(100)
        val wayCentroids = CoordinateStore(100)
        val adminBoundaryIndex = AdministrativeBoundaryIndex()
        val config = ConverterConfig()
        val popularityCalculator = OSMPopularityCalculator(config.osm)
        val importanceCalculator = ImportanceCalculator(config.importance)
        return OsmEntityConverter(nodesCoords, wayCentroids, adminBoundaryIndex, popularityCalculator, importanceCalculator)
    }

    @Test
    fun testConvertOsmNodeToNominatim() {
        val converter = createConverter()

        val mockNode =
            createMockNode(
                id = 1L,
                lat = 59.9133,
                lon = 10.7389,
                tags =
                    listOf(
                        Tag("name", "Test Node"),
                        Tag("amenity", "restaurant"),
                    ),
            )

        val nominatimPlace = converter.convert(mockNode)

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
        val converter = createConverter()

        val mockWay =
            createMockWay(
                id = 1L,
                name = "Test Cinema",
                tags =
                    listOf(
                        Tag("name", "Test Cinema"),
                        Tag("amenity", "cinema"),
                    ),
                nodeIds = listOf(100L, 101L, 102L, 103L),
            )

        // Without pre-loaded node coordinates, way conversion should return null
        val nominatimPlace = converter.convert(mockWay)

        assertNull(nominatimPlace, "Should return null when way node coordinates are not available")
    }

    @Test
    fun testConvertOsmWayWithMissingName() {
        val converter = createConverter()

        val mockWay =
            createMockWay(
                id = 1L,
                name = null,
                tags =
                    listOf(
                        Tag("amenity", "cinema"),
                    ),
                nodeIds = listOf(100L, 101L, 102L, 103L),
            )

        val nominatimPlace = converter.convert(mockWay)

        assertNull(nominatimPlace, "Should return null when way doesn't have a name")
    }

    @Test
    fun testConvertOsmWayWithoutMatchingTags() {
        val converter = createConverter()

        val mockWay =
            createMockWay(
                id = 1L,
                name = "Random Way",
                tags =
                    listOf(
                        Tag("name", "Random Way"),
                        Tag("random_tag", "random_value"),
                    ),
                nodeIds = listOf(100L, 101L, 102L, 103L),
            )

        val nominatimPlace = converter.convert(mockWay)

        assertNull(nominatimPlace, "Should return null when way doesn't have any matching POI tags")
    }

    @Test
    fun testIsPotentialPoiCheck() {
        val converter = createConverter()

        // Create ways to test
        val cinemaWay =
            createMockWay(
                id = 1L,
                name = "ODEON Oslo",
                tags =
                    listOf(
                        Tag("name", "ODEON Oslo"),
                        Tag("amenity", "cinema"),
                    ),
                nodeIds = listOf(100L, 101L, 102L),
            )

        val noNameWay =
            createMockWay(
                id = 2L,
                name = null,
                tags =
                    listOf(
                        Tag("amenity", "cinema"),
                    ),
                nodeIds = listOf(200L, 201L, 202L),
            )

        val noMatchingTagWay =
            createMockWay(
                id = 3L,
                name = "Random Building",
                tags =
                    listOf(
                        Tag("name", "Random Building"),
                        Tag("building", "yes"),
                    ),
                nodeIds = listOf(300L, 301L, 302L),
            )

        assertTrue(
            converter.isPotentialPoi(cinemaWay),
            "Cinema with name should be potential POI",
        )
        assertFalse(
            converter.isPotentialPoi(noNameWay),
            "Way without name should not be potential POI",
        )

        assertFalse(
            converter.isPotentialPoi(noMatchingTagWay),
            "Way without matching POI tags should not be potential POI",
        )
    }

    @Test
    fun `should convert node with valid hospital tag`() {
        val converter = createConverter()

        val hospital =
            createMockNode(
                id = 123L,
                lat = 59.9139,
                lon = 10.7522,
                tags =
                    listOf(
                        Tag("name", "Oslo University Hospital"),
                        Tag("amenity", "hospital"),
                    ),
            )

        val result = converter.convert(hospital)

        assertNotNull(result, "Hospital should be converted")
        assertEquals("Oslo University Hospital", result.content[0].name?.name)
        assertTrue(result.content[0].importance.toDouble() > 0.0, "Hospital should have importance > 0")
    }

    @Test
    fun `should convert nodes with matching POI tags`() {
        val converter = createConverter()

        val restaurant =
            createMockNode(
                id = 456L,
                lat = 59.9139,
                lon = 10.7522,
                tags =
                    listOf(
                        Tag("name", "Nice Restaurant"),
                        Tag("amenity", "restaurant"),
                    ),
            )

        val result = converter.convert(restaurant)

        assertNotNull(result, "Restaurant should be converted as it matches filter list")
        assertEquals("Nice Restaurant", result.content[0].name?.name)
    }

    @Test
    fun `converted nodes should have point accuracy`() {
        val converter = createConverter()

        val museum =
            createMockNode(
                id = 789L,
                lat = 59.9139,
                lon = 10.7522,
                tags =
                    listOf(
                        Tag("name", "National Museum"),
                        Tag("tourism", "museum"),
                    ),
            )

        val result = converter.convert(museum)

        assertNotNull(result)
        assertEquals("point", result.content[0].extra.accuracy)
    }

    @Test
    fun `converted nodes should have correct object_type`() {
        val converter = createConverter()

        val school =
            createMockNode(
                id = 999L,
                lat = 59.9139,
                lon = 10.7522,
                tags =
                    listOf(
                        Tag("name", "Central School"),
                        Tag("amenity", "school"),
                    ),
            )

        val result = converter.convert(school)

        assertNotNull(result)
        assertEquals("N", result.content[0].object_type)
    }

    @Test
    fun `popularity should vary based on POI type`() {
        val converter = createConverter()

        val hospital =
            createMockNode(
                id = 1L,
                lat = 59.9,
                lon = 10.7,
                tags = listOf(Tag("name", "Hospital"), Tag("amenity", "hospital")),
            )

        val cinema =
            createMockNode(
                id = 2L,
                lat = 59.9,
                lon = 10.7,
                tags = listOf(Tag("name", "Cinema"), Tag("amenity", "cinema")),
            )

        val hospitalResult = converter.convert(hospital)
        val cinemaResult = converter.convert(cinema)

        assertNotNull(hospitalResult)
        assertNotNull(cinemaResult)

        val hospitalImportance = hospitalResult.content[0].importance
        val cinemaImportance = cinemaResult.content[0].importance

        assertTrue(
            hospitalImportance > cinemaImportance,
            "Hospital should have higher importance than cinema",
        )
    }

    @Test
    fun `nodes without names should not be converted`() {
        val converter = createConverter()

        val nameless =
            createMockNode(
                id = 111L,
                lat = 59.9,
                lon = 10.7,
                tags = listOf(Tag("amenity", "hospital")),
            )

        val result = converter.convert(nameless)

        assertNull(result, "Node without name should not be converted")
    }

    @Test
    fun `coordinates should be within valid range`() {
        val converter = createConverter()

        val place =
            createMockNode(
                id = 222L,
                lat = 59.9139,
                lon = 10.7522,
                tags =
                    listOf(
                        Tag("name", "Test Place"),
                        Tag("tourism", "hotel"),
                    ),
            )

        val result = converter.convert(place)

        assertNotNull(result)
        val centroid = result.content[0].centroid
        assertEquals(2, centroid.size)
        assertTrue(centroid[0].toDouble() in -180.0..180.0, "Longitude should be valid")
        assertTrue(centroid[1].toDouble() in -90.0..90.0, "Latitude should be valid")
    }

    @Test
    fun `converted entries should have country code`() {
        val converter = createConverter()

        val place =
            createMockNode(
                id = 333L,
                lat = 59.9,
                lon = 10.7,
                tags =
                    listOf(
                        Tag("name", "Test Place"),
                        Tag("amenity", "restaurant"),
                    ),
            )

        val result = converter.convert(place)

        assertNotNull(result)
        assertNotNull(result.content[0].country_code)
        assertEquals(2, result.content[0].country_code?.length)
    }

    private fun createMockNode(
        id: Long,
        lat: Double,
        lon: Double,
        tags: List<Tag>,
    ): Node {
        val commonEntityData = CommonEntityData(id, 1, Date(), OsmUser(1, "test"), 1, tags)
        return Node(commonEntityData, lat, lon)
    }

    private fun createMockWay(
        id: Long,
        name: String?,
        tags: List<Tag>,
        nodeIds: List<Long>,
    ): Way {
        val wayNodes = nodeIds.map { WayNode(it) }
        val allTags = name?.let { tags + Tag("name", it) } ?: tags
        val commonEntityData = CommonEntityData(id, 1, Date(), OsmUser(1, "test"), 1, allTags)
        return Way(commonEntityData, wayNodes)
    }

    @Test
    fun `OSM entities should have county_gid and locality_gid in categories when admin boundaries are set`() {
        val nodesCoords = CoordinateStore(100)
        val wayCentroids = CoordinateStore(100)
        val adminBoundaryIndex = AdministrativeBoundaryIndex()

        // Add admin boundaries
        val countyBoundary =
            AdministrativeBoundary(
                id = 1L,
                name = "Oslo",
                adminLevel = AdministrativeBoundaryIndex.ADMIN_LEVEL_COUNTY,
                refCode = "03",
                country = Country.no,
                centroid = Coordinate(59.9, 10.7),
                bbox = BoundingBox(59.8, 60.0, 10.6, 10.8),
                boundaryNodes =
                    listOf(
                        Coordinate(59.8, 10.6),
                        Coordinate(60.0, 10.6),
                        Coordinate(60.0, 10.8),
                        Coordinate(59.8, 10.8),
                    ),
            )
        adminBoundaryIndex.addBoundary(countyBoundary)

        val municipalityBoundary =
            AdministrativeBoundary(
                id = 2L,
                name = "Oslo kommune",
                adminLevel = AdministrativeBoundaryIndex.ADMIN_LEVEL_MUNICIPALITY,
                refCode = "301",
                country = Country.no,
                centroid = Coordinate(59.9, 10.7),
                bbox = BoundingBox(59.8, 60.0, 10.6, 10.8),
                boundaryNodes =
                    listOf(
                        Coordinate(59.8, 10.6),
                        Coordinate(60.0, 10.6),
                        Coordinate(60.0, 10.8),
                        Coordinate(59.8, 10.8),
                    ),
            )
        adminBoundaryIndex.addBoundary(municipalityBoundary)

        val config = ConverterConfig()
        val popularityCalculator = OSMPopularityCalculator(config.osm)
        val importanceCalculator = ImportanceCalculator(config.importance)
        val converter = OsmEntityConverter(nodesCoords, wayCentroids, adminBoundaryIndex, popularityCalculator, importanceCalculator)

        val mockNode =
            createMockNode(
                id = 1L,
                lat = 59.9,
                lon = 10.7,
                tags =
                    listOf(
                        Tag("name", "Test Place"),
                        Tag("amenity", "restaurant"),
                    ),
            )

        val result = converter.convert(mockNode)

        assertNotNull(result)
        val categories = result.content[0].categories
        assertTrue(categories.contains("county_gid.KVE.TopographicPlace.03"), "Should contain county_gid with correct format")
        assertTrue(categories.contains("locality_gid.KVE.TopographicPlace.301"), "Should contain locality_gid with correct format")
    }
}
