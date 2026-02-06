package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import no.entur.geocoder.common.Country
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.ConverterConfig
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.source.ImportanceCalculator
import no.entur.geocoder.converter.target.NominatimPlace
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType
import org.openstreetmap.osmosis.core.domain.v0_6.Node
import org.openstreetmap.osmosis.core.domain.v0_6.Relation
import org.openstreetmap.osmosis.core.domain.v0_6.Way
import java.io.File
import java.nio.file.Paths

/**
 * Converts OSM PBF files to Nominatim JSON in 4 passes:
 *
 * 1. Relations pass: Collect admin boundaries and POI relation member way IDs
 * 2. Ways pass: Collect all needed node IDs (streets, admin ways, POI ways, relation member ways)
 * 3. Nodes pass: Fetch node coordinates for all needed nodes
 * 4. Ways+Relations pass: Build indexes, calculate centroids, and convert POIs
 */
class OsmConverter(config: ConverterConfig) : Converter {
    private val nodesCoords = CoordinateStore(500000)
    private val wayCentroids = CoordinateStore(50000)
    private val adminBoundaryIndex = AdministrativeBoundaryIndex()
    private val streetIndex = StreetIndex()
    private val popularityCalculator = OSMPopularityCalculator(config.osm)
    private val importanceCalculator = ImportanceCalculator(config.importance)
    private val entityConverter =
        OsmEntityConverter(
            nodesCoords,
            wayCentroids,
            adminBoundaryIndex,
            streetIndex,
            popularityCalculator,
            importanceCalculator,
            config.osm,
        )

    override fun convert(input: File, output: File, isAppending: Boolean) {
        require(input.exists()) { "Input file does not exist: ${input.absolutePath}" }

        // =====================================================================
        // Pass 1: Relations only - collect admin boundaries and POI relation member way IDs
        // =====================================================================
        println("Pass 1/4: Scanning relations for admin boundaries and POI relations...")
        val adminRelations = mutableListOf<AdminRelationData>()
        val poiRelationMemberWayIds = hashSetOf<Long>()
        val poiRelationNodeIds = hashSetOf<Long>()

        parsePbf(input, OsmIterator.RELATION_FILTER).forEach { entity ->
            if (entity is Relation) {
                // Check for admin boundary
                val tags = entity.tags.associate { it.key to it.value }
                if (tags["boundary"] == "administrative") {
                    val adminLevelStr = tags["admin_level"]
                    if (adminLevelStr in
                        listOf(
                            AdministrativeBoundaryIndex.ADMIN_LEVEL_COUNTY.toString(),
                            AdministrativeBoundaryIndex.ADMIN_LEVEL_MUNICIPALITY.toString(),
                        )
                    ) {
                        val adminLevel = adminLevelStr?.toIntOrNull()
                        val name = tags["name"]
                        val ref = tags["ref"]
                        val country = extractCountryCode(tags)

                        if (adminLevel != null && name != null && ref != null && country != null) {
                            val wayIds =
                                entity.members
                                    .filter { it.memberType == EntityType.Way }
                                    .map { it.memberId }
                            adminRelations.add(AdminRelationData(entity.id, name, adminLevel, ref, wayIds, country))
                        }
                    }

                    // Also collect node members from admin relations
                    entity.members
                        .filter { it.memberType == EntityType.Node }
                        .forEach { poiRelationNodeIds.add(it.memberId) }
                }

                // Check for POI relation (has name and matching tags)
                if (entityConverter.isPotentialPoi(entity)) {
                    entity.members.forEach { member ->
                        when (member.memberType) {
                            EntityType.Way -> {
                                poiRelationMemberWayIds.add(member.memberId)
                            }

                            EntityType.Node -> {
                                poiRelationNodeIds.add(member.memberId)
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
        println("  Found ${adminRelations.size} admin boundary relations")
        println("  Found ${poiRelationMemberWayIds.size} POI relation member ways")

        // =====================================================================
        // Pass 2: Ways only - collect all required node IDs and way metadata
        // =====================================================================
        println("Pass 2/4: Scanning ways for streets, admin boundaries, and POIs...")
        val adminWayIds = adminRelations.flatMap { it.wayIds }.toSet()
        val streetWays = mutableListOf<StreetWayData>()
        val neededNodeIds = hashSetOf<Long>()
        neededNodeIds.addAll(poiRelationNodeIds)

        // Track which ways we'll need in pass 4
        val poiWayIds = hashSetOf<Long>()
        val adminWayNodeIds = mutableMapOf<Long, List<Long>>()

        parsePbf(input, OsmIterator.WAY_FILTER).forEach { entity ->
            if (entity is Way) {
                val nodeIds = entity.wayNodes.map { it.nodeId }

                // Street way?
                val tags = entity.tags.associate { it.key to it.value }
                val highway = tags["highway"]
                if (tags.containsKey("name") && highway != null && highway in StreetIndex.HIGHWAY_TYPES) {
                    streetWays.add(StreetWayData(entity.id, tags["name"]!!, nodeIds))
                    neededNodeIds.addAll(nodeIds)
                }

                // Admin boundary way?
                if (entity.id in adminWayIds) {
                    adminWayNodeIds[entity.id] = nodeIds
                    neededNodeIds.addAll(nodeIds)
                }

                // POI relation member way?
                if (entity.id in poiRelationMemberWayIds) {
                    neededNodeIds.addAll(nodeIds)
                }

                // Direct POI way?
                if (entityConverter.isPotentialPoi(entity)) {
                    poiWayIds.add(entity.id)
                    neededNodeIds.addAll(nodeIds)
                }
            }
        }
        println("  Found ${streetWays.size} street ways")
        println("  Found ${poiWayIds.size} POI ways")
        println("  Total unique node coordinates needed: ${neededNodeIds.size}")

        // =====================================================================
        // Pass 3: Nodes only - collect coordinates for all needed nodes
        // =====================================================================
        println("Pass 3/4: Collecting node coordinates...")
        parsePbf(input, OsmIterator.NODE_FILTER).forEach { entity ->
            if (entity is Node && entity.id in neededNodeIds) {
                nodesCoords.put(entity.id, Coordinate(entity.latitude, entity.longitude))
            }
        }

        // =====================================================================
        // Build admin boundary index from collected data (no file read needed)
        // =====================================================================
        println("  Building administrative boundary index...")
        buildAdminBoundaryIndex(adminRelations, adminWayNodeIds)
        println("  ${adminBoundaryIndex.getStatistics()}")

        // =====================================================================
        // Build street index from collected data (no file read needed)
        // =====================================================================
        println("  Building street index...")
        buildStreetIndex(streetWays)
        println("  ${streetIndex.getStatistics()}")

        // =====================================================================
        // Pass 4: Ways + Relations - calculate centroids and convert POIs
        // =====================================================================
        println("Pass 4/4: Processing POI entities and writing output...")
        val nominatimEntries = processEntities(input, poiWayIds, poiRelationMemberWayIds)
        JsonWriter().export(nominatimEntries, Paths.get(output.absolutePath), isAppending)
    }

    private fun buildAdminBoundaryIndex(
        adminRelations: List<AdminRelationData>,
        adminWayNodeIds: Map<Long, List<Long>>,
    ) {
        for (relation in adminRelations) {
            val allNodeCoords =
                relation.wayIds.flatMap { wayId ->
                    adminWayNodeIds[wayId]?.mapNotNull { nodeId ->
                        nodesCoords.get(nodeId)
                    } ?: emptyList()
                }

            if (allNodeCoords.isNotEmpty()) {
                val centroid =
                    Coordinate(
                        allNodeCoords.map { it.lat }.average(),
                        allNodeCoords.map { it.lon }.average(),
                    )
                val bbox = BoundingBox.fromCoordinates(allNodeCoords)
                val boundaryNodes = allNodeCoords.map { Coordinate(it.lat, it.lon) }

                val boundary =
                    AdministrativeBoundary(
                        id = relation.id,
                        name = relation.name,
                        adminLevel = relation.adminLevel,
                        refCode = relation.ref,
                        country = relation.country,
                        centroid = centroid,
                        bbox = bbox,
                        boundaryNodes = boundaryNodes,
                    )
                adminBoundaryIndex.addBoundary(boundary)
            }
        }
    }

    private fun buildStreetIndex(streetWays: List<StreetWayData>) {
        var streetsAdded = 0
        var streetsSkipped = 0

        for (streetWay in streetWays) {
            val coordinates =
                streetWay.nodeIds.mapNotNull { nodeId ->
                    nodesCoords.get(nodeId)
                }

            if (coordinates.size >= 2) {
                streetIndex.addStreet(streetWay.name, coordinates)
                streetsAdded++
            } else {
                streetsSkipped++
            }
        }

        if (streetsSkipped > 0) {
            println("  Warning: Skipped $streetsSkipped streets due to missing node coordinates")
        }
    }

    private fun processEntities(
        inputFile: File,
        poiWayIds: Set<Long>,
        poiRelationMemberWayIds: Set<Long>,
    ): Sequence<NominatimPlace> =
        sequence {
            // Single pass: convert POI nodes, calculate way centroids and convert POI ways/relations
            // PBF files are ordered: Nodes → Ways → Relations, so we can handle all in one pass
            val poiFilter = OsmIterator.poiFilter(popularityCalculator)
            val allNeededWayIds = poiWayIds + poiRelationMemberWayIds

            var count = 0
            parsePbf(inputFile) { entity ->
                when (entity) {
                    is Node -> poiFilter(entity)
                    is Way -> entity.id in allNeededWayIds
                    is Relation -> poiFilter(entity)
                    else -> false
                }
            }.forEach { entity ->
                when (entity) {
                    is Node -> {
                        // Convert POI nodes directly (they have their own coordinates)
                        entityConverter.convert(entity)?.let {
                            yield(it)
                            count++
                        }
                    }

                    is Way -> {
                        // Calculate and store centroid for this way
                        val wayNodeCoords = entity.wayNodes.mapNotNull { nodesCoords.get(it.nodeId) }
                        if (wayNodeCoords.isNotEmpty()) {
                            GeometryCalculator.calculateCentroid(wayNodeCoords)?.let { coord ->
                                wayCentroids.put(entity.id, coord)
                            }
                        }
                        // Convert if it's a POI way (not just a relation member way)
                        if (entity.id in poiWayIds) {
                            entityConverter.convert(entity)?.let {
                                yield(it)
                                count++
                            }
                        }
                    }

                    is Relation -> {
                        entityConverter.convert(entity)?.let {
                            yield(it)
                            count++
                        }
                    }
                }
            }

            println("Finished processing $count entities")
        }

    private fun extractCountryCode(tags: Map<String, String>): Country? {
        val iso3166 =
            tags["ISO3166-2"] ?: tags["ISO3166-2-lvl4"] ?: tags["ISO3166-2:lvl4"]
                ?: tags["is_in:country_code"] ?: tags["country_code"]

        val country = Country.parse(iso3166?.take(2))
        if (country != null) {
            return country
        }
        if (tags["ref"]?.all { it.isDigit() } == true) {
            return Country.no
        }
        return null
    }

    private fun parsePbf(inputFile: File, filter: ((Entity) -> Boolean)?): Sequence<Entity> =
        OsmIterator(inputFile, filter).asSequence()

    /** Data class for admin relation metadata collected in pass 1 */
    private data class AdminRelationData(
        val id: Long,
        val name: String,
        val adminLevel: Int,
        val ref: String,
        val wayIds: List<Long>,
        val country: Country,
    )

    /** Data class for street way metadata collected in pass 2 */
    private data class StreetWayData(
        val wayId: Long,
        val name: String,
        val nodeIds: List<Long>,
    )
}
