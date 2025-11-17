package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File

/** Collects node coordinates and calculates way centroids */
class NodeCoordinateCollector(
    private val nodesCoords: CoordinateStore,
    private val wayCentroids: CoordinateStore,
) {
    fun collectAllRequiredNodeIds(
        inputFile: File,
        adminRelations: List<AdminBoundaryCollector.AdminRelationData>,
        entityConverter: OsmEntityConverter,
    ): Set<Long> {
        val adminWayIds = adminRelations.flatMap { it.wayIds }.toSet()
        val nodeIds = hashSetOf<Long>()

        // First pass: collect member way IDs from POI relations
        val poiRelationMemberWayIds = hashSetOf<Long>()
        parsePbf(inputFile, null).forEach { entity ->
            if (entity is Relation && entityConverter.isPotentialPoi(entity)) {
                entity.members
                    .filter { it.memberType == EntityType.Way }
                    .forEach { poiRelationMemberWayIds.add(it.memberId) }
            }
        }

        // Second pass: collect node IDs from ways and relations
        parsePbf(inputFile, null).forEach { entity ->
            when (entity) {
                is Way -> {
                    if (entity.id in adminWayIds ||
                        entity.id in poiRelationMemberWayIds ||
                        entityConverter.isPotentialPoi(entity)
                    ) {
                        entity.wayNodes.forEach { nodeIds.add(it.nodeId) }
                    }
                }

                is Relation -> {
                    if (entity.tags.any { it.key == "boundary" && it.value == "administrative" }) {
                        entity.members
                            .filter { it.memberType == EntityType.Node }
                            .forEach { nodeIds.add(it.memberId) }
                    }
                }
            }
        }

        return nodeIds
    }

    fun collectNodeCoordinates(inputFile: File, neededNodeIds: Set<Long>) {
        parsePbf(inputFile, OsmIterator.NODE_FILTER).forEach { entity ->
            if (entity is Node && entity.id in neededNodeIds) {
                nodesCoords.put(entity.id, Coordinate(entity.latitude, entity.longitude))
            }
        }
    }

    fun calculateAndStoreWayCentroid(way: Way) {
        val wayNodeCoords = way.wayNodes.mapNotNull { nodesCoords.get(it.nodeId) }
        if (wayNodeCoords.isNotEmpty()) {
            GeometryCalculator.calculateCentroid(wayNodeCoords)?.let { coord ->
                wayCentroids.put(way.id, coord)
            }
        }
    }

    private fun parsePbf(inputFile: File, filter: ((Entity) -> Boolean)?): Sequence<Entity> =
        OsmIterator(inputFile, filter).asSequence()
}
