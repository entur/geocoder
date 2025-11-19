package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import no.entur.geocoder.common.Country
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType
import org.openstreetmap.osmosis.core.domain.v0_6.Relation
import org.openstreetmap.osmosis.core.domain.v0_6.Way
import java.io.File

/** Collects Norwegian admin boundaries and builds spatial index */
class AdminBoundaryCollector(
    private val nodesCoords: CoordinateStore,
    private val wayCentroids: CoordinateStore,
) {
    fun collectAdminRelations(inputFile: File): List<AdminRelationData> {
        val adminRelations = mutableListOf<AdminRelationData>()

        parsePbf(inputFile, OsmIterator.ADMIN_BOUNDARY_FILTER).forEach { entity ->
            if (entity is Relation) {
                val tags = entity.tags.associate { it.key to it.value }
                val adminLevelStr = tags["admin_level"]
                if (tags["boundary"] == "administrative" &&
                    adminLevelStr in
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
            }
        }

        return adminRelations
    }

    fun buildAdminBoundaryIndex(
        inputFile: File,
        adminRelations: List<AdminRelationData>,
        adminBoundaryIndex: AdministrativeBoundaryIndex,
    ) {
        val wayIdToRelations = mutableMapOf<Long, MutableList<AdminRelationData>>()
        adminRelations.forEach { relation ->
            relation.wayIds.forEach { wayId ->
                wayIdToRelations.getOrPut(wayId) { mutableListOf() }.add(relation)
            }
        }

        val wayIdToNodeCoords = mutableMapOf<Long, List<Coordinate>>()
        parsePbf(inputFile, OsmIterator.WAY_FILTER).forEach { entity ->
            if (entity is Way && entity.id in wayIdToRelations) {
                val wayNodeCoords = entity.wayNodes.mapNotNull { nodesCoords.get(it.nodeId) }
                if (wayNodeCoords.isNotEmpty()) {
                    wayIdToNodeCoords[entity.id] = wayNodeCoords
                    GeometryCalculator.calculateCentroid(wayNodeCoords)?.let { coord ->
                        wayCentroids.put(entity.id, coord)
                    }
                }
            }
        }

        adminRelations.forEach { relation ->
            val allNodeCoords =
                relation.wayIds.flatMap { wayId ->
                    wayIdToNodeCoords[wayId] ?: emptyList()
                }

            if (allNodeCoords.isNotEmpty()) {
                val centroid =
                    Coordinate(
                        allNodeCoords.map { it.lat }.average(),
                        allNodeCoords.map { it.lon }.average(),
                    )

                val bbox =
                    BoundingBox.fromCoordinates(allNodeCoords)

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

    private fun extractCountryCode(tags: Map<String, String>): Country? {
        val iso3166 =
            tags["ISO3166-2"] ?: tags["ISO3166-2-lvl4"] ?: tags["ISO3166-2:lvl4"] ?: tags["is_in:country_code"] ?: tags["country_code"]

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

    data class AdminRelationData(
        val id: Long,
        val name: String,
        val adminLevel: Int,
        val ref: String,
        val wayIds: List<Long>,
        val country: Country,
    )
}
