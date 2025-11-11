package no.entur.geocoder.converter.source.osm

import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File

/** Collects Norwegian admin boundaries and builds spatial index */
class AdminBoundaryCollector(
    private val nodesCoords: CoordinateStore,
    private val wayCentroids: CoordinateStore,
) {
    companion object {
        private const val COUNTRY_CODE_NORWAY = "NO"
    }

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
                    val countryCode = extractCountryCode(tags)

                    if (adminLevel != null && name != null && ref != null && countryCode == COUNTRY_CODE_NORWAY) {
                        val wayIds =
                            entity.members
                                .filter { it.memberType == EntityType.Way }
                                .map { it.memberId }

                        adminRelations.add(AdminRelationData(entity.id, name, adminLevel, ref, wayIds, countryCode))
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

        val wayIdToNodeCoords = mutableMapOf<Long, List<Pair<Double, Double>>>()
        parsePbf(inputFile, OsmIterator.WAY_FILTER).forEach { entity ->
            if (entity is Way && entity.id in wayIdToRelations) {
                val wayNodeCoords = entity.wayNodes.mapNotNull { nodesCoords.get(it.nodeId) }
                if (wayNodeCoords.isNotEmpty()) {
                    wayIdToNodeCoords[entity.id] = wayNodeCoords
                    GeometryCalculator.calculateCentroid(wayNodeCoords)?.let { (lon, lat) ->
                        wayCentroids.put(entity.id, lon.toDouble(), lat.toDouble())
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
                val centroidLon = allNodeCoords.map { it.first }.average()
                val centroidLat = allNodeCoords.map { it.second }.average()

                val bbox =
                    BoundingBox.fromCoordinates(
                        allNodeCoords.map { (lon, lat) -> lat to lon },
                    )

                val boundaryNodes = allNodeCoords.map { (lon, lat) -> lat to lon }

                val boundary =
                    AdministrativeBoundary(
                        id = relation.id,
                        name = relation.name,
                        adminLevel = relation.adminLevel,
                        refCode = relation.ref,
                        countryCode = relation.countryCode,
                        centroid = centroidLat to centroidLon,
                        bbox = bbox,
                        boundaryNodes = boundaryNodes,
                    )

                adminBoundaryIndex.addBoundary(boundary)
            }
        }
    }

    private fun extractCountryCode(tags: Map<String, String>): String? {
        val ref = tags["ref"]
        val iso3166 = tags["ISO3166-2"] ?: tags["ISO3166-2-lvl4"] ?: tags["ISO3166-2:lvl4"]

        return when {
            iso3166?.startsWith("$COUNTRY_CODE_NORWAY-") == true -> COUNTRY_CODE_NORWAY
            tags["is_in:country_code"] == COUNTRY_CODE_NORWAY -> COUNTRY_CODE_NORWAY
            tags["country_code"] == COUNTRY_CODE_NORWAY -> COUNTRY_CODE_NORWAY
            ref?.all { it.isDigit() } == true -> COUNTRY_CODE_NORWAY
            else -> null
        }
    }

    private fun parsePbf(inputFile: File, filter: ((Entity) -> Boolean)?): Sequence<Entity> =
        OsmIterator(inputFile, filter).asSequence()

    data class AdminRelationData(
        val id: Long,
        val name: String,
        val adminLevel: Int,
        val ref: String,
        val wayIds: List<Long>,
        val countryCode: String,
    )
}
