package no.entur.geocoder.converter.osm

import no.entur.geocoder.common.Extra
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.NominatimPlace
import no.entur.geocoder.converter.NominatimPlace.*
import no.entur.geocoder.converter.Util.titleize
import no.entur.geocoder.converter.Util.toBigDecimalWithScale
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths
import kotlin.math.abs

class OsmConverter : Converter {
    private val nodesCoords = CoordinateStore(500000)
    private val wayCentroids = CoordinateStore(50000)


    override fun convert(input: File, output: File, isAppending: Boolean) {
        require(input.exists()) { "Input file does not exist: ${input.absolutePath}" }

        // Pass 1: Identify all node IDs required for POIs to minimize memory usage in the next pass.
        val neededNodeIds = collectNeededNodeIds(input)

        // Pass 2: Store coordinates only for the nodes identified in Pass 1.
        collectNodeCoordinates(input, neededNodeIds)

        // Pass 3: Process all entities, calculate centroids, convert to Nominatim format, and write to JSON.
        val nominatimEntries = processEntities(input)
        JsonWriter().export(nominatimEntries, Paths.get(output.absolutePath), isAppending)
    }


    private fun collectNeededNodeIds(inputFile: File): Set<Long> {
        val neededNodeIds = hashSetOf<Long>()
        parsePbf(inputFile).forEach { entity ->
            when (entity) {
                is Node -> neededNodeIds.add(entity.id)
                is Way -> entity.wayNodes.forEach { neededNodeIds.add(it.nodeId) }
                is Relation ->
                    entity.members
                        .filter { it.memberType == EntityType.Node }
                        .forEach { neededNodeIds.add(it.memberId) }
            }
        }
        return neededNodeIds
    }

    private fun collectNodeCoordinates(inputFile: File, neededNodeIds: Set<Long>) {
        parsePbf(inputFile).forEach { entity ->
            if (entity is Node && entity.id in neededNodeIds) {
                nodesCoords.put(entity.id, entity.longitude, entity.latitude)
            }
        }
    }

    private fun processEntities(inputFile: File): Sequence<NominatimPlace> =
        sequence {
            var count = 0
            parsePbf(inputFile).forEach { entity ->
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

    private fun parsePbf(inputFile: File): Sequence<Entity> = OsmIterator(inputFile).asSequence()

    internal fun convertOsmEntityToNominatim(entity: Entity): NominatimPlace? {
        val tags = entity.tags.associate { it.key to it.value }

        return when (entity) {
            is Node -> convertNodeToNominatim(entity, tags)
            is Way -> convertWayToNominatim(entity, tags)
            is Relation -> convertRelationToNominatim(entity, tags)
            else -> null
        }
    }

    private fun createPlaceContent(
        entity: Entity,
        tags: Map<String, String>,
        name: String,
        objectType: String,
        accuracy: String,
        centroid: Pair<BigDecimal, BigDecimal>,
        address: Address = Address(),
    ): NominatimPlace {
        val importance = calculateImportance(tags)
        val country = (tags["addr:country"] ?: "no")
        val (lon, lat) = centroid

        val extra =
            Extra(
                id = entity.id.toString(),
                source = "openstreetmap",
                accuracy = accuracy,
                country_a = if (country.equals("no", ignoreCase = true)) "NOR" else country,
                label = name,
                tags = tags.map { "${it.key}.${it.value}" }.joinToString(","),
            )

        val categories: List<String> = listOf("osm.public_transport.poi")
            .plus(tags.map { "${it.key}.${it.value}" })
            .plus("source.osm")
            .plus("layer.address")
            .plus("country.$country")

        val content =
            PlaceContent(
                place_id = abs(entity.id),
                object_type = objectType,
                object_id = abs(entity.id),
                categories = categories,
                rank_address = determineRankAddress(tags),
                importance = importance,
                parent_place_id = 0,
                name = Name(name),
                housenumber = null,
                address = address,
                postcode = null,
                country_code = country.lowercase(),
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extra = extra,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun convertNodeToNominatim(node: Node, tags: Map<String, String>): NominatimPlace? {
        val name = tags["name"] ?: return null
        return createPlaceContent(
            entity = node,
            tags = tags,
            name = name,
            objectType = "N",
            accuracy = "point",
            centroid = node.longitude.toBigDecimalWithScale() to node.latitude.toBigDecimalWithScale(),
        )
    }

    private fun convertWayToNominatim(way: Way, tags: Map<String, String>): NominatimPlace? {
        val name = tags["name"] ?: return null
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

    private fun convertRelationToNominatim(relation: Relation, tags: Map<String, String>): NominatimPlace? {
        val name = tags["name"] ?: return null
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
        var importance = 0.03

        when {
            tags.containsKey("amenity") -> importance += 0.01
            tags.containsKey("shop") -> importance += 0.005
            tags.containsKey("tourism") -> importance += 0.015
            tags.containsKey("boundary") -> importance += 0.02
        }

        val nameKeys = tags.keys.count { it.startsWith("name") }
        importance += nameKeys * 0.001

        return importance.coerceAtMost(0.2)
    }
}
