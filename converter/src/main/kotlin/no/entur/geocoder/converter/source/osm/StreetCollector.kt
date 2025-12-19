package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import org.openstreetmap.osmosis.core.domain.v0_6.Entity
import org.openstreetmap.osmosis.core.domain.v0_6.Way
import java.io.File

/**
 * Collects street ways from OSM data and builds a StreetIndex for nearest-street lookups.
 *
 * The collection happens in two passes:
 * 1. First pass: Collect all street way IDs and their required node IDs
 * 2. Second pass: Build the index using the collected node coordinates
 */
class StreetCollector(
    private val nodesCoords: CoordinateStore,
) {
    /**
     * Data class to hold street way information collected in the first pass.
     */
    data class StreetWayData(
        val wayId: Long,
        val name: String,
        val nodeIds: List<Long>,
    )

    /**
     * First pass: Collects all street ways and returns the set of required node IDs.
     *
     * @param inputFile The OSM PBF file to read
     * @return Pair of (list of street way data, set of required node IDs)
     */
    fun collectStreetWays(inputFile: File): Pair<List<StreetWayData>, Set<Long>> {
        val streetWays = mutableListOf<StreetWayData>()
        val requiredNodeIds = hashSetOf<Long>()

        parsePbf(inputFile, StreetIndex.STREET_FILTER).forEach { entity ->
            if (entity is Way) {
                val tags = entity.tags.associate { it.key to it.value }
                val name = tags["name"] ?: return@forEach

                val nodeIds = entity.wayNodes.map { it.nodeId }
                streetWays.add(StreetWayData(entity.id, name, nodeIds))
                requiredNodeIds.addAll(nodeIds)
            }
        }

        return streetWays to requiredNodeIds
    }

    /**
     * Second pass: Builds the StreetIndex using collected street ways and node coordinates.
     *
     * @param streetWays The street way data collected in the first pass
     * @param streetIndex The StreetIndex to populate
     */
    fun buildStreetIndex(streetWays: List<StreetWayData>, streetIndex: StreetIndex) {
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

    private fun parsePbf(inputFile: File, filter: ((Entity) -> Boolean)?): Sequence<Entity> =
        OsmIterator(inputFile, filter).asSequence()
}
