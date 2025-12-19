package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.ConverterConfig
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.source.ImportanceCalculator
import no.entur.geocoder.converter.target.NominatimPlace
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File
import java.nio.file.Paths

/**
 * Converts OSM PBF files to Nominatim JSON in multiple passes:
 * 1. Collect admin boundaries
 * 2. Collect street ways and their node IDs
 * 3. Collect node IDs (including from POI relation member ways and streets)
 * 4. Fetch node coordinates
 * 5. Build administrative boundary index
 * 6. Build street index
 * 7. Process POI entities:
 *    a. Calculate Way POI centroids and collect relation member way IDs
 *    b. Calculate centroids for relation member ways
 *    c. Convert all POI entities to Nominatim format
 */
class OsmConverter(config: ConverterConfig) : Converter {
    private val nodesCoords = CoordinateStore(500000)
    private val wayCentroids = CoordinateStore(50000)
    private val adminBoundaryIndex = AdministrativeBoundaryIndex()
    private val streetIndex = StreetIndex()
    private val nodeCollector = NodeCoordinateCollector(nodesCoords, wayCentroids)
    private val boundaryCollector = AdminBoundaryCollector(nodesCoords, wayCentroids)
    private val streetCollector = StreetCollector(nodesCoords)
    private val popularityCalculator = OSMPopularityCalculator(config.osm)
    private val importanceCalculator = ImportanceCalculator(config.importance)
    private val entityConverter =
        OsmEntityConverter(nodesCoords, wayCentroids, adminBoundaryIndex, streetIndex, popularityCalculator, importanceCalculator)

    override fun convert(input: File, output: File, isAppending: Boolean) {
        require(input.exists()) { "Input file does not exist: ${input.absolutePath}" }

        println("Pass 1: Collecting admin boundaries...")
        val adminRelations = boundaryCollector.collectAdminRelations(input)
        println("  Found ${adminRelations.size} admin boundary relations")

        println("Pass 2: Collecting street ways...")
        val (streetWays, streetNodeIds) = streetCollector.collectStreetWays(input)
        println("  Found ${streetWays.size} street ways")

        println("Pass 3: Collecting required node IDs...")
        val allNeededNodeIds =
            nodeCollector
                .collectAllRequiredNodeIds(input, adminRelations, entityConverter)
                .plus(streetNodeIds)
        println("  Total unique node coordinates needed: ${allNeededNodeIds.size}")

        println("Pass 4: Collecting node coordinates...")
        nodeCollector.collectNodeCoordinates(input, allNeededNodeIds)

        println("Pass 5: Building administrative boundary index...")
        boundaryCollector.buildAdminBoundaryIndex(input, adminRelations, adminBoundaryIndex)
        println("  ${adminBoundaryIndex.getStatistics()}")

        println("Pass 6: Building street index...")
        streetCollector.buildStreetIndex(streetWays, streetIndex)
        println("  ${streetIndex.getStatistics()}")

        println("Pass 7: Processing POI entities and writing output...")
        val nominatimEntries = processEntities(input)
        JsonWriter().export(nominatimEntries, Paths.get(output.absolutePath), isAppending)
    }

    private fun processEntities(inputFile: File): Sequence<NominatimPlace> =
        sequence {
            // First pass: collect member way IDs from POI relations and calculate Way POI centroids
            val relationMemberWayIds = hashSetOf<Long>()
            val poiFilter = OsmIterator.poiFilter(popularityCalculator)
            parsePbf(inputFile, poiFilter).forEach { entity ->
                when (entity) {
                    is Way -> {
                        nodeCollector.calculateAndStoreWayCentroid(entity)
                    }

                    is Relation -> {
                        entity.members
                            .filter { it.memberType == EntityType.Way }
                            .forEach { relationMemberWayIds.add(it.memberId) }
                    }
                }
            }

            // Second pass: calculate centroids for relation member ways
            if (relationMemberWayIds.isNotEmpty()) {
                parsePbf(inputFile, OsmIterator.WAY_FILTER).forEach { entity ->
                    if (entity is Way && entity.id in relationMemberWayIds) {
                        nodeCollector.calculateAndStoreWayCentroid(entity)
                    }
                }
            }

            // Third pass: convert all POI entities to Nominatim format
            var count = 0
            parsePbf(inputFile, poiFilter).forEach { entity ->

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
