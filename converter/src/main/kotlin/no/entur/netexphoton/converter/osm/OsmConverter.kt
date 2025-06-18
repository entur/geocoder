package no.entur.netexphoton.converter.osm

import crosby.binary.osmosis.OsmosisReader
import no.entur.netexphoton.common.domain.Extra
import no.entur.netexphoton.converter.Converter
import no.entur.netexphoton.converter.JsonWriter
import no.entur.netexphoton.converter.NominatimPlace
import no.entur.netexphoton.converter.NominatimPlace.*
import no.entur.netexphoton.converter.Util.titleize
import no.entur.netexphoton.converter.Util.toBigDecimalWithScale
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6.*
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs

class OsmConverter : Converter {
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

    private class OsmIterator(
        inputFile: File,
    ) : AbstractIterator<Entity>() {
        private val queue = LinkedBlockingQueue<Entity>()
        private val poisonPill: Entity = Node(CommonEntityData(-1L, 0, Date(0), null, 0L), 0.0, 0.0)

        init {
            val reader = OsmosisReader(inputFile)
            reader.setSink(
                object : Sink {
                    override fun initialize(metaData: MutableMap<String, Any>?) {}

                    override fun process(entityContainer: EntityContainer?) {
                        entityContainer?.entity?.let { queue.put(it) }
                    }

                    override fun complete() {
                        queue.put(poisonPill)
                    }

                    override fun close() {}
                },
            )
            Thread { reader.run() }.start()
        }

        override fun computeNext() {
            val entity = queue.take()
            if (entity === poisonPill) {
                done()
            } else {
                setNext(entity)
            }
        }
    }

    fun convertOsmEntityToNominatim(entity: Entity): NominatimPlace? =
        when (entity) {
            is Node -> convertNodeToNominatim(entity)
            is Way -> convertWayToNominatim(entity)
            is Relation -> convertRelationToNominatim(entity)
            else -> null
        }

    private fun convertNodeToNominatim(node: Node): NominatimPlace? {
        val tags = node.tags.associate { it.key to it.value }

        if (tags.isEmpty() || (tags.size == 1 && tags.containsKey("created_by"))) {
            return null
        }

        val name = tags["name"] ?: return null // Skip unnamed nodes
        val lat = node.latitude.toBigDecimalWithScale()
        val lon = node.longitude.toBigDecimalWithScale()

        val placeType = determineNodeType(tags)
        val importance = calculateImportance(tags, placeType)

        val city = tags["addr:city"]
        val street = tags["addr:street"]
        val housenumber = tags["addr:housenumber"]
        val postcode = tags["addr:postcode"] ?: "unknown"
        val country = tags["addr:country"] ?: "no" // Default to Norway

        val labelParts = mutableListOf(name)
        city?.let { labelParts.add(it) }
        val label = labelParts.joinToString(", ")

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
                housenumber = housenumber,
                address =
                    Address(
                        street = street,
                        city = city?.titleize(),
                        county = tags["addr:county"]?.titleize(),
                    ),
                postcode = postcode,
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

        // TODO: Dummy values
        val lat = 0.0.toBigDecimalWithScale()
        val lon = 0.0.toBigDecimalWithScale()

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

        val postcode = tags["addr:postcode"] ?: "unknown"

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
                address =
                    Address(
                        street = tags["addr:street"],
                        city = tags["addr:city"]?.titleize(),
                        county = tags["addr:county"]?.titleize(),
                    ),
                postcode = postcode,
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

        // TODO: Dummy values
        val lat = 0.0.toBigDecimalWithScale()
        val lon = 0.0.toBigDecimalWithScale()

        val extratags =
            Extra(
                id = relation.id.toString(),
                layer = placeType,
                source = "osm",
                source_id = relation.id.toString(),
                accuracy = "polygon",
                country_a = "NOR", // Default to Norway
                label = name,
            )

        val postcode = tags["addr:postcode"] ?: "unknown"

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
                postcode = postcode,
                country_code = (tags["addr:country"] ?: "no").lowercase(),
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extratags = extratags,
            )

        return NominatimPlace("Place", listOf(content))
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

        listOf("amenity", "shop", "tourism", "leisure", "highway", "building", "natural", "place").forEach { key ->
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
            "building" -> importance += 0.0
            "road" -> importance += 0.01
            "boundary" -> importance += 0.02
            "place" -> {
                importance +=
                    when (tags["place"]) {
                        "city" -> 0.05
                        "town" -> 0.04
                        "village" -> 0.03
                        "hamlet" -> 0.02
                        "suburb" -> 0.03
                        "neighbourhood" -> 0.02
                        else -> 0.01
                    }
            }
        }

        val nameKeys = tags.keys.count { it.startsWith("name:") }
        importance += nameKeys * 0.001

        return importance.coerceAtMost(0.2)
    }
}
