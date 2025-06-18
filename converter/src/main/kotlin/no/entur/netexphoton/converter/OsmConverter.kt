package no.entur.netexphoton.converter

import crosby.binary.osmosis.OsmosisReader
import no.entur.netexphoton.common.domain.Extra
import no.entur.netexphoton.converter.NominatimPlace.*
import no.entur.netexphoton.converter.Util.titleize
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer
import org.openstreetmap.osmosis.core.domain.v0_6.*
import org.openstreetmap.osmosis.core.task.v0_6.Sink
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs

class OsmConverter {

    fun convert(input: File, output: File, isAppending: Boolean = false) {
        val entities = parsePbf(input)
        val nominatimEntries = entities.mapNotNull { convertOsmEntityToNominatim(it) }

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(nominatimEntries, outputPath, isAppending)
    }

    private fun parsePbf(inputFile: File): Sequence<Entity> = OsmIterator(inputFile).asSequence()

    private class OsmIterator(inputFile: File) : AbstractIterator<Entity>() {
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

        // Skip nodes without meaningful tags (usually just coordinate points)
        if (tags.isEmpty() || (tags.size == 1 && tags.containsKey("created_by"))) {
            return null
        }

        // Extract basic properties
        val name = tags["name"] ?: return null // Skip unnamed nodes
        val lat = node.latitude
        val lon = node.longitude

        val placeType = determineNodeType(tags)
        val importance = calculateImportance(tags, placeType)

        // Get address components
        val city = tags["addr:city"]
        val street = tags["addr:street"]
        val housenumber = tags["addr:housenumber"]
        val postcode = tags["addr:postcode"] ?: "unknown"
        val country = tags["addr:country"] ?: "no" // Default to Norway

        // Create a label by concatenating name and city if available
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
                centroid = listOf(BigDecimal(lon.toString()), BigDecimal(lat.toString())),
                bbox =
                    listOf(
                        BigDecimal(lon.toString()),
                        BigDecimal(lat.toString()),
                        BigDecimal(lon.toString()),
                        BigDecimal(lat.toString()),
                    ),
                extratags = extratags,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun convertWayToNominatim(way: Way): NominatimPlace? {
        val tags = way.tags.associate { it.key to it.value }

        // Skip ways without meaningful tags
        if (tags.isEmpty()) {
            return null
        }

        val name = tags["name"] ?: return null // Skip unnamed ways

        val placeType = determineWayType(tags)
        val importance = calculateImportance(tags, placeType)

        // Since we don't have the actual coordinates for the way in this simplified version,
        // we'll use dummy values. In a real implementation, you'd calculate these from node data.
        val lat = 0.0
        val lon = 0.0

        // Create a label using just the name for now
        val label = name

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
                centroid = listOf(BigDecimal(lon.toString()), BigDecimal(lat.toString())),
                bbox =
                    listOf(
                        BigDecimal(lon.toString()),
                        BigDecimal(lat.toString()),
                        BigDecimal(lon.toString()),
                        BigDecimal(lat.toString()),
                    ),
                extratags = extratags,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun convertRelationToNominatim(relation: Relation): NominatimPlace? {
        val tags = relation.tags.associate { it.key to it.value }

        // Skip relations without meaningful tags
        if (tags.isEmpty()) {
            return null
        }

        val name = tags["name"] ?: return null // Skip unnamed relations

        val placeType = determineRelationType(tags)
        val importance = calculateImportance(tags, placeType)

        // Dummy coordinates - in a real implementation, you'd calculate these from member data
        val lat = 0.0
        val lon = 0.0

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
                centroid = listOf(BigDecimal(lon.toString()), BigDecimal(lat.toString())),
                bbox =
                    listOf(
                        BigDecimal(lon.toString()),
                        BigDecimal(lat.toString()),
                        BigDecimal(lon.toString()),
                        BigDecimal(lat.toString()),
                    ),
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

        // Add relevant tags as categories
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
        var importance = 0.3 // base importance

        // Adjust importance based on place type
        when (placeType) {
            "amenity" -> importance += 0.1
            "shop" -> importance += 0.05
            "tourism" -> importance += 0.15
            "building" -> importance += 0.0
            "road" -> importance += 0.1
            "boundary" -> importance += 0.2
            "place" -> {
                // Places have different importance based on their type
                when (tags["place"]) {
                    "city" -> importance += 0.5
                    "town" -> importance += 0.4
                    "village" -> importance += 0.3
                    "hamlet" -> importance += 0.2
                    "suburb" -> importance += 0.3
                    "neighbourhood" -> importance += 0.2
                    else -> importance += 0.1
                }
            }
        }

        // Names in multiple languages indicate importance
        val nameKeys = tags.keys.count { it.startsWith("name:") }
        importance += nameKeys * 0.01

        return importance.coerceAtMost(1.0)
    }

    /**
     * Mock method for testing purposes only
     * Returns a sequence of test entities
     */
    fun parseMockPbfForTesting(): Sequence<Entity> =
        sequence {
            // Create a test node
            val tags =
                listOf(
                    Tag("name", "Test Node"),
                    Tag("amenity", "restaurant"),
                    Tag("cuisine", "italian"),
                )

            val entityData =
                CommonEntityData(
                    1L, // id
                    1, // version
                    Date(), // timestamp
                    null, // user
                    0L, // changesetId
                    tags,
                )

            val node = Node(entityData, 59.9133, 10.7389) // Oslo coordinates
            yield(node)
        }
}
