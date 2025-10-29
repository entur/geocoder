package no.entur.geocoder.converter.osm

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Source
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.PlaceId
import no.entur.geocoder.common.Util.titleize
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.converter.importance.ImportanceCalculator
import no.entur.geocoder.converter.photon.NominatimPlace
import no.entur.geocoder.converter.photon.NominatimPlace.*
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths

/**
 * Converts OSM PBF files to Nominatim-compatible JSON format.
 *
 * This converter processes POIs (Points of Interest) from OpenStreetMap data,
 * enriching them with administrative boundary information and calculating
 * importance scores based on configured filters.
 *
 * ## Conversion Process (5 passes):
 *
 * **Pass 1**: Collect admin boundary relations
 * - Finds all Norwegian county and municipality boundaries
 * - Extracts relation metadata (name, admin_level, ref code)
 *
 * **Pass 2**: Collect all required node IDs
 * - Collects node IDs from admin boundary ways
 * - Collects node IDs from POI ways (e.g., cinema buildings, parks)
 * - This ensures way centroids can be calculated for all POIs
 *
 * **Pass 3**: Collect node coordinates
 * - Fetches lat/lon coordinates for all collected node IDs
 * - Stores in memory-efficient CoordinateStore
 *
 * **Pass 4**: Build administrative boundary index
 * - Calculates centroids and bounding boxes for admin boundaries
 * - Creates spatial index for fast POI-to-boundary lookups
 *
 * **Pass 5**: Process POIs and write output
 * - Converts nodes, ways, and relations to Nominatim format
 * - Enriches with county/municipality information
 * - Calculates importance scores
 * - Writes as newline-delimited JSON
 *
 * ## Supported POI Types:
 * - Nodes: Point POIs (shops, restaurants, single buildings)
 * - Ways: Polygon POIs (larger buildings, parks, areas)
 * - Relations: Complex POIs (multipolygons, building complexes)
 *
 * @see OSMPopularityCalculator for POI filtering configuration
 * @see AdministrativeBoundaryIndex for spatial boundary lookups
 */
class OsmConverter : Converter {
    private val nodesCoords = CoordinateStore(500000)
    private val wayCentroids = CoordinateStore(50000)
    private val adminBoundaryIndex = AdministrativeBoundaryIndex()

    override fun convert(input: File, output: File, isAppending: Boolean) {
        require(input.exists()) { "Input file does not exist: ${input.absolutePath}" }

        // Pass 1: Collect admin boundary relations
        println("Pass 1: Collecting admin boundaries...")
        val adminRelations = collectAdminRelations(input)
        println("  Found ${adminRelations.size} admin boundary relations")

        // Pass 2: Collect all needed node IDs (admin boundaries + POI ways)
        println("Pass 2: Collecting required node IDs...")
        val allNeededNodeIds = collectAllRequiredNodeIds(input, adminRelations)
        println("  Total unique node coordinates needed: ${allNeededNodeIds.size}")

        // Pass 3: Collect all needed node coordinates
        println("Pass 3: Collecting node coordinates...")
        collectNodeCoordinates(input, allNeededNodeIds)

        // Pass 4: Build admin boundary index
        println("Pass 4: Building administrative boundary index...")
        buildAdminBoundaryIndex(input, adminRelations)
        println(adminBoundaryIndex.getStatistics())

        // Pass 5: Process POI entities, convert to Nominatim format, and write to JSON
        println("Pass 5: Processing POI entities and writing output...")
        val nominatimEntries = processEntities(input)
        JsonWriter().export(nominatimEntries, Paths.get(output.absolutePath), isAppending)
    }

    /**
     * Collects admin boundary relations for Norwegian counties and municipalities.
     */
    private fun collectAdminRelations(inputFile: File): List<AdminRelationData> {
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

                    // Only process Norwegian boundaries
                    if (adminLevel != null && name != null && ref != null && countryCode == "NO") {
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

    /**
     * Collects all node IDs needed for both admin boundaries and POI ways in a single pass.
     * This is more efficient than multiple passes over the PBF file.
     */
    private fun collectAllRequiredNodeIds(inputFile: File, adminRelations: List<AdminRelationData>): Set<Long> {
        val adminWayIds = adminRelations.flatMap { it.wayIds }.toSet()
        val nodeIds = hashSetOf<Long>()

        parsePbf(inputFile, null).forEach { entity ->
            when (entity) {
                is Way -> {
                    if (entity.id in adminWayIds) {
                        // Collect nodes from admin boundary ways
                        entity.wayNodes.forEach { nodeIds.add(it.nodeId) }
                    } else if (isPotentialPoi(entity)) {
                        // Collect nodes from POI ways
                        entity.wayNodes.forEach { nodeIds.add(it.nodeId) }
                    }
                }

                is Relation -> {
                    if (entity.tags.any { it.key == "boundary" && it.value == "administrative" }) {
                        // Collect direct node members from admin boundaries
                        entity.members
                            .filter { it.memberType == EntityType.Node }
                            .forEach { nodeIds.add(it.memberId) }
                    }
                }
            }
        }

        return nodeIds
    }

    /**
     * Quick check if an entity might be a POI (has name + wanted tag).
     * Used during node collection to avoid storing unnecessary coordinates.
     */
    private fun isPotentialPoi(entity: Entity): Boolean {
        val tags = entity.tags.associate { it.key to it.value }
        return tags.containsKey("name") &&
            tags.any { (key, value) -> OSMPopularityCalculator.hasFilter(key, value) }
    }

    /**
     * Extracts the country code from OSM administrative boundary tags.
     *
     * Uses a priority chain:
     * 1. ISO3166-2 tags (most reliable)
     * 2. Other country code tags
     * 3. Numeric ref as fallback for backward compatibility
     */
    private fun extractCountryCode(tags: Map<String, String>): String? {
        val ref = tags["ref"]
        val iso3166 = tags["ISO3166-2"] ?: tags["ISO3166-2-lvl4"] ?: tags["ISO3166-2:lvl4"]

        return when {
            iso3166?.startsWith("NO-") == true -> "NO"
            tags["is_in:country_code"] == "NO" -> "NO"
            tags["country_code"] == "NO" -> "NO"
            ref?.all { it.isDigit() } == true -> "NO" // Fallback for backward compatibility
            else -> null
        }
    }

    private fun collectNodeCoordinates(inputFile: File, neededNodeIds: Set<Long>) {
        parsePbf(inputFile, OsmIterator.NODE_FILTER).forEach { entity ->
            if (entity is Node && entity.id in neededNodeIds) {
                nodesCoords.put(entity.id, entity.longitude, entity.latitude)
            }
        }
    }

    private fun buildAdminBoundaryIndex(inputFile: File, adminRelations: List<AdminRelationData>) {
        val wayIdToRelations = mutableMapOf<Long, MutableList<AdminRelationData>>()
        adminRelations.forEach { relation ->
            relation.wayIds.forEach { wayId ->
                wayIdToRelations.getOrPut(wayId) { mutableListOf() }.add(relation)
            }
        }

        // Collect all node coordinates for each way (for better centroid calculation)
        val wayIdToNodeCoords = mutableMapOf<Long, List<Pair<Double, Double>>>()
        parsePbf(inputFile, OsmIterator.WAY_FILTER).forEach { entity ->
            if (entity is Way && entity.id in wayIdToRelations) {
                val wayNodeCoords = entity.wayNodes.mapNotNull { nodesCoords.get(it.nodeId) }
                if (wayNodeCoords.isNotEmpty()) {
                    wayIdToNodeCoords[entity.id] = wayNodeCoords
                    // Still calculate way centroids for backward compatibility
                    calculateCentroid(wayNodeCoords)?.let { (lon, lat) ->
                        wayCentroids.put(entity.id, lon.toDouble(), lat.toDouble())
                    }
                }
            }
        }

        // Build admin boundaries from the collected data
        adminRelations.forEach { relation ->
            // Collect ALL node coordinates from all ways in the boundary (not just way centroids)
            val allNodeCoords =
                relation.wayIds.flatMap { wayId ->
                    wayIdToNodeCoords[wayId] ?: emptyList()
                }

            if (allNodeCoords.isNotEmpty()) {
                // Calculate centroid from all actual nodes for better accuracy
                val centroidLon = allNodeCoords.map { it.first }.average()
                val centroidLat = allNodeCoords.map { it.second }.average()

                val bbox =
                    BoundingBox.fromCoordinates(
                        allNodeCoords.map { (lon, lat) -> lat to lon },
                    )

                // Convert node coords to (lat, lon) pairs for ray-casting
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

    private data class AdminRelationData(
        val id: Long,
        val name: String,
        val adminLevel: Int,
        val ref: String,
        val wayIds: List<Long>,
        val countryCode: String,
    )

    private fun processEntities(inputFile: File): Sequence<NominatimPlace> =
        sequence {
            var count = 0
            parsePbf(inputFile, OsmIterator.POI_FILTER).forEach { entity ->
                if (entity is Way) {
                    calculateAndStoreWayCentroid(entity)
                }

                convertOsmEntityToNominatim(entity)?.let {
                    yield(it)
                    count++
                }
            }
            println("Finished processing $count entities")
        }

    private fun calculateAndStoreWayCentroid(way: Way) {
        val wayNodeCoords = way.wayNodes.mapNotNull { nodesCoords.get(it.nodeId) }
        if (wayNodeCoords.isNotEmpty()) {
            calculateCentroid(wayNodeCoords)?.let { (lon, lat) ->
                wayCentroids.put(way.id, lon.toDouble(), lat.toDouble())
            }
        }
    }

    private fun parsePbf(inputFile: File, filter: ((Entity) -> Boolean)?): Sequence<Entity> =
        OsmIterator(inputFile, filter).asSequence()

    internal fun convertOsmEntityToNominatim(entity: Entity): NominatimPlace? {
        val tags = filterTags(entity.tags)
        val name = entity.tags.firstOrNull { it.key == "name" }?.value

        if (name.isNullOrEmpty()) return null

        return when (entity) {
            is Node -> convertNodeToNominatim(entity, tags, name)
            is Way -> convertWayToNominatim(entity, tags, name)
            is Relation -> convertRelationToNominatim(entity, tags, name)
            else -> null
        }
    }

    private fun filterTags(tags: Collection<Tag>): Map<String, String> =
        tags
            .associate { it.key to it.value }
            .filter { (key, value) -> OSMPopularityCalculator.hasFilter(key, value) }

    private fun createPlaceContent(
        entity: Entity,
        tags: Map<String, String>,
        name: String,
        objectType: String,
        accuracy: String,
        centroid: Pair<BigDecimal, BigDecimal>,
        address: Address = Address(),
    ): NominatimPlace {
        val (lon, lat) = centroid

        // Look up administrative boundaries for this location
        val (county, municipality) = adminBoundaryIndex.findCountyAndMunicipality(lat.toDouble(), lon.toDouble())

        val country = determineCountry(county, municipality, tags)
        val updatedAddress = address.copy(county = county?.name?.titleize() ?: address.county)

        val altName =
            listOfNotNull(tags["alt_name"], tags["old_name"], tags["no:name"], tags["loc_name"], tags["short_name"])
                .joinToString(";")
                .ifBlank { null }

        val extra =
            Extra(
                id = "OSM:TopographicPlace:" + entity.id,
                source = Source.OSM,
                accuracy = accuracy,
                country_a = if (country.equals("no", ignoreCase = true)) "NOR" else country,
                county_gid = county?.refCode?.let { "KVE:TopographicPlace:$it" },
                locality = municipality?.name?.titleize(),
                locality_gid = municipality?.refCode?.let { "KVE:TopographicPlace:$it" },
                tags = tags.map { "${it.key}.${it.value}" }.joinToString(","),
                alt_name = altName,
            )

        val categories = buildCategories(tags, country, county, municipality)

        val placeId = PlaceId.osm.create(entity.id)
        val content =
            PlaceContent(
                place_id = placeId,
                object_type = objectType,
                object_id = placeId,
                categories = categories,
                rank_address = determineRankAddress(tags),
                importance = calculateImportance(tags),
                parent_place_id = 0,
                name = Name(name = name, alt_name = altName),
                housenumber = null,
                address = updatedAddress,
                postcode = null,
                country_code = country.lowercase(),
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extra = extra,
            )

        return NominatimPlace("Place", listOf(content))
    }

    /**
     * Determines the country code for a POI using a priority chain:
     * 1. County's country code (from ISO3166-2)
     * 2. Municipality's country code (from ISO3166-2)
     * 3. POI's addr:country tag
     * 4. Default to "no"
     */
    private fun determineCountry(
        county: AdministrativeBoundary?,
        municipality: AdministrativeBoundary?,
        tags: Map<String, String>,
    ): String {
        val addrCountry = tags["addr:country"]
        return when {
            county?.countryCode != null -> county.countryCode.lowercase()
            municipality?.countryCode != null -> municipality.countryCode.lowercase()
            addrCountry != null -> addrCountry
            else -> "no"
        }
    }

    /**
     * Builds the list of category tags for the POI.
     */
    private fun buildCategories(
        tags: Map<String, String>,
        country: String,
        county: AdministrativeBoundary?,
        municipality: AdministrativeBoundary?,
    ): List<String> =
        buildList {
            add(Category.OSM_POI)
            addAll(tags.map { "${it.key}.${it.value}" })
            add("source.osm")
            add("layer.address")
            add("country.$country")
            county?.refCode?.let { add("county_gid.KVE:TopographicPlace:$it") }
            municipality?.refCode?.let { add("locality_gid.KVE:TopographicPlace:$it") }
        }

    private fun convertNodeToNominatim(node: Node, tags: Map<String, String>, name: String): NominatimPlace =
        createPlaceContent(
            entity = node,
            tags = tags,
            name = name,
            objectType = "N",
            accuracy = "point",
            centroid = node.longitude.toBigDecimalWithScale() to node.latitude.toBigDecimalWithScale(),
        )

    private fun convertWayToNominatim(way: Way, tags: Map<String, String>, name: String): NominatimPlace? {
        val (lon, lat) = wayCentroids.get(way.id) ?: return null

        return createPlaceContent(
            entity = way,
            tags = tags,
            name = name,
            objectType = "W",
            accuracy = "polygon",
            centroid = lon.toBigDecimalWithScale() to lat.toBigDecimalWithScale(),
        )
    }

    private fun convertRelationToNominatim(relation: Relation, tags: Map<String, String>, name: String): NominatimPlace? {
        val memberCoords =
            relation.members.mapNotNull {
                when (it.memberType) {
                    EntityType.Node -> nodesCoords.get(it.memberId)
                    EntityType.Way -> wayCentroids.get(it.memberId)
                    else -> null
                }
            }
        if (memberCoords.isEmpty()) return null

        val centroid = calculateCentroid(memberCoords) ?: return null
        val address =
            if (tags["type"] == "boundary" && tags["boundary"] == "administrative") {
                Address(county = tags["name"]?.titleize())
            } else {
                Address()
            }

        return createPlaceContent(
            entity = relation,
            tags = tags,
            name = name,
            objectType = "R",
            accuracy = "polygon",
            centroid = centroid,
            address = address,
        )
    }

    private fun calculateCentroid(coords: List<Pair<Double, Double>>): Pair<BigDecimal, BigDecimal>? {
        if (coords.isEmpty()) return null
        val lon = coords.map { it.first }.average().toBigDecimalWithScale()
        val lat = coords.map { it.second }.average().toBigDecimalWithScale()
        return lon to lat
    }

    private fun determineRankAddress(tags: Map<String, String>): Int =
        when {
            tags.containsKey("boundary") -> 10
            tags.containsKey("place") -> 20
            tags.containsKey("road") -> 26
            tags.containsKey("building") -> 28
            tags.containsKey("amenity") -> 30
            tags.containsKey("shop") -> 30
            tags.containsKey("tourism") -> 30
            else -> 30
        }

    private fun calculateImportance(tags: Map<String, String>): Double {
        val popularity = OSMPopularityCalculator.calculatePopularity(tags)
        return ImportanceCalculator.calculateImportance(popularity)
    }
}
