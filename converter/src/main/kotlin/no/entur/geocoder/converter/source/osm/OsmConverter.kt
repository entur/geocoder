package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.target.NominatimPlace
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File
import java.nio.file.Paths

/**
 * Converts OSM PBF files to Nominatim JSON in 5 passes:
 * 1. Collect admin boundaries, 2. Collect node IDs, 3. Fetch coordinates,
 * 4. Build spatial index, 5. Convert and export POIs
 */
class OsmConverter : Converter {
    private val nodesCoords = CoordinateStore(500000)
    private val wayCentroids = CoordinateStore(50000)
    private val adminBoundaryIndex = AdministrativeBoundaryIndex()
    private val nodeCollector = NodeCoordinateCollector(nodesCoords, wayCentroids)
    private val boundaryCollector = AdminBoundaryCollector(nodesCoords, wayCentroids)
    private val entityConverter = OsmEntityConverter(nodesCoords, wayCentroids, adminBoundaryIndex)

    override fun convert(input: File, output: File, isAppending: Boolean) {
        require(input.exists()) { "Input file does not exist: ${input.absolutePath}" }

        println("Pass 1: Collecting admin boundaries...")
        val adminRelations = boundaryCollector.collectAdminRelations(input)
        println("  Found ${adminRelations.size} admin boundary relations")

        println("Pass 2: Collecting required node IDs...")
        val allNeededNodeIds = nodeCollector.collectAllRequiredNodeIds(input, adminRelations, entityConverter)
        println("  Total unique node coordinates needed: ${allNeededNodeIds.size}")

        println("Pass 3: Collecting node coordinates...")
        nodeCollector.collectNodeCoordinates(input, allNeededNodeIds)

        println("Pass 4: Building administrative boundary index...")
        boundaryCollector.buildAdminBoundaryIndex(input, adminRelations, adminBoundaryIndex)
        println(adminBoundaryIndex.getStatistics())

        println("Pass 5: Processing POI entities and writing output...")
        val nominatimEntries = processEntities(input)
        JsonWriter().export(nominatimEntries, Paths.get(output.absolutePath), isAppending)
    }

    private fun processEntities(inputFile: File): Sequence<NominatimPlace> =
        sequence {
            var count = 0
            parsePbf(inputFile, OsmIterator.POI_FILTER).forEach { entity ->
                if (entity is Way) {
                    nodeCollector.calculateAndStoreWayCentroid(entity)
                }

                entityConverter.convert(entity)?.let {
                    yield(it)
                    count++
                }
            }
            println("Finished processing $count entities")
        }


    private fun parsePbf(inputFile: File, filter: ((Entity) -> Boolean)?): Sequence<Entity> =
        OsmIterator(inputFile, filter).asSequence()
}
