package no.entur.netexphoton.converter.osm

import no.entur.netexphoton.common.domain.Extra
import no.entur.netexphoton.converter.Converter
import no.entur.netexphoton.converter.JsonWriter
import no.entur.netexphoton.converter.NominatimPlace
import no.entur.netexphoton.converter.NominatimPlace.*
import no.entur.netexphoton.converter.Util.titleize
import no.entur.netexphoton.converter.Util.toBigDecimalWithScale
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths
import kotlin.math.abs

class OsmConverter : Converter {
    private val nodesCoords = mutableMapOf<Long, Pair<Double, Double>>()
    private val wayCentroids = mutableMapOf<Long, Pair<Double, Double>>()
    private val poiKeys = setOf(
        "amenity", "shop", "tourism", "leisure", "historic", "office", "craft",
        "public_transport", "railway", "station", "aeroway", "natural", "waterway"
    )

    override fun convert(
        input: File,
        output: File,
        isAppending: Boolean,
    ) {
        val entities = parsePbf(input)
        val nominatimEntries = entities.mapNotNull { convertOsmEntityToNominatim(it) }

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(nominatimEntries, outputPath, isAppending)
    }

    private fun parsePbf(inputFile: File): Sequence<Entity> = OsmIterator(inputFile).asSequence()

    fun convertOsmEntityToNominatim(entity: Entity): NominatimPlace? {
        val tags = entity.tags.associate { it.key to it.value }

        val isPoi = tags.keys.any { it in poiKeys }
        val isPlace = tags.containsKey("place")
        val isBoundary = tags["type"] == "boundary"

        if (!isPoi && !isPlace && !isBoundary) {
            return null
        }

        return when (entity) {
            is Node -> convertNodeToNominatim(entity)
            is Way -> convertWayToNominatim(entity)
            is Relation -> convertRelationToNominatim(entity)
            else -> null
        }
    }

    private fun convertNodeToNominatim(node: Node): NominatimPlace? {
        val tags = node.tags.associate { it.key to it.value }

        if (tags.isEmpty() || (tags.size == 1 && tags.containsKey("created_by"))) {
            return null
        }

        val name = tags["name"] ?: return null // Skip unnamed nodes
        val lat = node.latitude.toBigDecimalWithScale()
        val lon = node.longitude.toBigDecimalWithScale()
        nodesCoords[node.id] = Pair(node.longitude, node.latitude)

        val placeType = determineNodeType(tags)
        val importance = calculateImportance(tags, placeType)

        val country = tags["addr:country"] ?: "no" // Default to Norway

        val label = name

        val extratags =
            Extra(
                id = node.id.toString(),
                layer = placeType,
                source = "osm",
                source_id = node.id.toString(),
                accuracy = "point",
                country_a = if (country.equals("no", ignoreCase = true)) "NOR" else country,
                label = label,
            )

        val content =
            PlaceContent(
                place_id = abs(node.id),
                object_type = "N",
                object_id = abs(node.id),
                categories = determineCategories(tags),
                rank_address = determineRankAddress(placeType),
                importance = importance,
                parent_place_id = 0,
                name = Name(name),
                housenumber = null,
                address = Address(),
                postcode = null,
                country_code = country.lowercase(),
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extratags = extratags,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun convertWayToNominatim(way: Way): NominatimPlace? {
        val tags = way.tags.associate { it.key to it.value }

        if (tags.isEmpty()) {
            return null
        }

        val name = tags["name"] ?: return null // Skip unnamed ways

        val placeType = determineWayType(tags)
        val importance = calculateImportance(tags, placeType)

        val wayNodeCoords = way.wayNodes.mapNotNull { nodesCoords[it.nodeId] }
        val (lon, lat) = calculateCentroid(wayNodeCoords) ?: return null

        wayCentroids[way.id] = Pair(lon.toDouble(), lat.toDouble())

        val label = name // TODO: maybe something else

        val extratags =
            Extra(
                id = way.id.toString(),
                layer = placeType,
                source = "osm",
                source_id = way.id.toString(),
                accuracy = "polygon",
                country_a = "NOR", // Default to Norway
                label = label,
            )

        val content =
            PlaceContent(
                place_id = abs(way.id),
                object_type = "W",
                object_id = abs(way.id),
                categories = determineCategories(tags),
                rank_address = determineRankAddress(placeType),
                importance = importance,
                parent_place_id = 0,
                name = Name(name),
                address = Address(),
                postcode = null,
                country_code = (tags["addr:country"] ?: "no").lowercase(),
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extratags = extratags,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun convertRelationToNominatim(relation: Relation): NominatimPlace? {
        val tags = relation.tags.associate { it.key to it.value }

        if (tags.isEmpty()) {
            return null
        }

        val name = tags["name"] ?: return null // Skip unnamed relations

        val placeType = determineRelationType(tags)
        val importance = calculateImportance(tags, placeType)

        val memberCoords = relation.members.mapNotNull {
            when (it.memberType) {
                EntityType.Node -> nodesCoords[it.memberId]
                EntityType.Way -> wayCentroids[it.memberId]
                else -> null
            }
        }
        val (lon, lat) = calculateCentroid(memberCoords) ?: return null

        val extratags =
            Extra(
                id = relation.id.toString(),
                layer = placeType,
                source = "osm",
                source_id = relation.id.toString(),
                accuracy = "polygon",
                country_a = "NOR",
                label = name,
            )

        val content =
            PlaceContent(
                place_id = abs(relation.id),
                object_type = "R",
                object_id = abs(relation.id),
                categories = determineCategories(tags),
                rank_address = determineRankAddress(placeType),
                importance = importance,
                parent_place_id = 0,
                name = Name(name),
                address =
                    Address(
                        county = if (tags["type"] == "boundary" && tags["boundary"] == "administrative") tags["name"]?.titleize() else null,
                    ),
                postcode = null,
                country_code = (tags["addr:country"] ?: "no").lowercase(),
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extratags = extratags,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun calculateCentroid(coords: List<Pair<Double, Double>>): Pair<BigDecimal, BigDecimal>? {
        if (coords.isEmpty()) {
            return null
        }
        val lon = coords.map { it.first }.average().toBigDecimalWithScale()
        val lat = coords.map { it.second }.average().toBigDecimalWithScale()
        return Pair(lon, lat)
    }

    private fun determineNodeType(tags: Map<String, String>): String =
        when {
            tags.containsKey("amenity") -> "amenity"
            tags.containsKey("shop") -> "shop"
            tags.containsKey("tourism") -> "tourism"
            tags.containsKey("leisure") -> "leisure"
            tags.containsKey("highway") -> "highway"
            tags.containsKey("place") -> "place"
            else -> "node"
        }

    private fun determineWayType(tags: Map<String, String>): String =
        when {
            tags.containsKey("building") -> "building"
            tags.containsKey("highway") -> "road"
            tags.containsKey("natural") -> "natural"
            tags.containsKey("waterway") -> "waterway"
            tags.containsKey("landuse") -> "landuse"
            else -> "way"
        }

    private fun determineRelationType(tags: Map<String, String>): String =
        when {
            tags["type"] == "boundary" && tags["boundary"] == "administrative" -> "boundary"
            tags["type"] == "multipolygon" -> "multipolygon"
            tags["type"] == "route" -> "route"
            else -> "relation"
        }

    private fun determineCategories(tags: Map<String, String>): List<String> {
        val categories = mutableListOf<String>()

        (poiKeys + "place" + "building" + "highway").forEach { key ->
            tags[key]?.let { categories.add("$key:$it") }
        }

        return categories
    }

    private fun determineRankAddress(placeType: String): Int =
        when (placeType) {
            "amenity" -> 30
            "shop" -> 30
            "tourism" -> 30
            "building" -> 28
            "road" -> 26
            "boundary" -> 10
            "place" -> 20
            else -> 30
        }

    private fun calculateImportance(
        tags: Map<String, String>,
        placeType: String,
    ): Double {
        var importance = 0.03

        when (placeType) {
            "amenity" -> importance += 0.01
            "shop" -> importance += 0.005
            "tourism" -> importance += 0.015
            "boundary" -> importance += 0.02
        }

        val nameKeys = tags.keys.count { it.startsWith("name:") }
        importance += nameKeys * 0.001

        return importance.coerceAtMost(0.2)
    }
}
