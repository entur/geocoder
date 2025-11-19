package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.*
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_ADDRESS
import no.entur.geocoder.common.Category.LEGACY_SOURCE_WHOSONFIRST
import no.entur.geocoder.common.Category.OSM_POI
import no.entur.geocoder.common.Util.titleize
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.converter.Text.altName
import no.entur.geocoder.converter.source.ImportanceCalculator
import no.entur.geocoder.converter.source.PlaceId
import no.entur.geocoder.converter.target.NominatimPlace
import no.entur.geocoder.converter.target.NominatimPlace.*
import org.openstreetmap.osmosis.core.domain.v0_6.*
import java.math.BigDecimal

/** Converts OSM entities to Nominatim format with admin boundary enrichment */
class OsmEntityConverter(
    private val nodesCoords: CoordinateStore,
    private val wayCentroids: CoordinateStore,
    private val adminBoundaryIndex: AdministrativeBoundaryIndex,
) {
    companion object {
        private const val OBJECT_TYPE_NODE = "N"
        private const val OBJECT_TYPE_WAY = "W"
        private const val OBJECT_TYPE_RELATION = "R"
        private const val ACCURACY_POINT = "point"
        private const val ACCURACY_POLYGON = "polygon"
        private const val RANK_BOUNDARY = 10
        private const val RANK_PLACE = 20
        private const val RANK_ROAD = 26
        private const val RANK_BUILDING = 28
        private const val RANK_POI = 30
    }

    fun convert(entity: Entity): NominatimPlace? {
        val tags = filterTags(entity.tags)
        val name = entity.tags.firstOrNull { it.key == "name" }?.value

        if (name.isNullOrEmpty()) return null

        return when (entity) {
            is Node -> convertNode(entity, tags, name)
            is Way -> convertWay(entity, tags, name)
            is Relation -> convertRelation(entity, tags, name)
            else -> null
        }
    }

    fun isPotentialPoi(entity: Entity): Boolean {
        val tags = entity.tags.associate { it.key to it.value }
        return tags.containsKey("name") &&
            tags.any { (key, value) -> OSMPopularityCalculator.hasFilter(key, value) }
    }

    private fun filterTags(tags: Collection<Tag>): Map<String, String> =
        tags
            .associate { it.key to it.value }
            .filter { (key, value) -> OSMPopularityCalculator.hasFilter(key, value) }

    private fun convertNode(node: Node, tags: Map<String, String>, name: String): NominatimPlace =
        createPlaceContent(
            entity = node,
            tags = tags,
            name = name,
            objectType = OBJECT_TYPE_NODE,
            accuracy = ACCURACY_POINT,
            centroid = Coordinate(node.latitude, node.longitude),
        )

    private fun convertWay(way: Way, tags: Map<String, String>, name: String): NominatimPlace? {
        val coord = wayCentroids.get(way.id) ?: return null

        return createPlaceContent(
            entity = way,
            tags = tags,
            name = name,
            objectType = OBJECT_TYPE_WAY,
            accuracy = ACCURACY_POLYGON,
            centroid = coord,
        )
    }

    private fun convertRelation(relation: Relation, tags: Map<String, String>, name: String): NominatimPlace? {
        val memberCoords =
            relation.members.mapNotNull {
                when (it.memberType) {
                    EntityType.Node -> nodesCoords.get(it.memberId)
                    EntityType.Way -> wayCentroids.get(it.memberId)
                    else -> null
                }
            }
        if (memberCoords.isEmpty()) return null

        val centroid = GeometryCalculator.calculateCentroid(memberCoords) ?: return null
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
            objectType = OBJECT_TYPE_RELATION,
            accuracy = ACCURACY_POLYGON,
            centroid = centroid,
            address = address,
        )
    }

    private fun createPlaceContent(
        entity: Entity,
        tags: Map<String, String>,
        name: String,
        objectType: String,
        accuracy: String,
        centroid: Coordinate,
        address: Address = Address(),
    ): NominatimPlace {
        val (county, municipality) = adminBoundaryIndex.findCountyAndMunicipality(centroid)

        val country = determineCountry(county, municipality, tags, centroid)
        val updatedAddress = address.copy(county = county?.name?.titleize() ?: address.county)
        val tagList: List<String> =
            listOf(LEGACY_SOURCE_WHOSONFIRST, LEGACY_LAYER_ADDRESS, OSM_POI, LEGACY_CATEGORY_PREFIX + "poi")
                .plus(tags.map { LEGACY_CATEGORY_PREFIX + it.value })

        val altName =
            altName(tags["alt_name"], tags["old_name"], tags["no:name"], tags["loc_name"], tags["short_name"])

        val id = "OSM:TopographicPlace:" + entity.id
        val extra =
            Extra(
                id = id,
                source = Source.OSM,
                accuracy = accuracy,
                country_a = country?.threeLetterCode,
                county_gid = county?.refCode?.let { "KVE:TopographicPlace:$it" },
                locality = municipality?.name?.titleize(),
                locality_gid = municipality?.refCode?.let { "KVE:TopographicPlace:$it" },
                tags = tagList.joinToString(","),
                alt_name = altName,
            )

        val categories = buildCategories(tagList, country, county, municipality)

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
                name = Name(name = name, alt_name = altName(altName, id)),
                housenumber = null,
                address = updatedAddress,
                postcode = null,
                country_code = country?.name,
                centroid = centroid.centroid(),
                bbox = centroid.bbox(),
                extra = extra,
            )

        return NominatimPlace("Place", listOf(content))
    }

    private fun determineCountry(
        county: AdministrativeBoundary?,
        municipality: AdministrativeBoundary?,
        tags: Map<String, String>,
        coord: Coordinate,
    ): Country? =
        county?.country ?: municipality?.country ?: Country.parse(
            tags["addr:country"],
        )
            ?: Geo.getCountry(coord)

    private fun buildCategories(
        tags: List<String>,
        country: Country?,
        county: AdministrativeBoundary?,
        municipality: AdministrativeBoundary?,
    ): List<String> =
        buildList {
            addAll(tags)
            country?.let { add("country.${it.name}") }
            county?.refCode?.let { add("county_gid.KVE:TopographicPlace:$it") }
            municipality?.refCode?.let { add("locality_gid.KVE:TopographicPlace:$it") }
        }

    private fun determineRankAddress(tags: Map<String, String>): Int =
        when {
            tags.containsKey("boundary") -> RANK_BOUNDARY
            tags.containsKey("place") -> RANK_PLACE
            tags.containsKey("road") -> RANK_ROAD
            tags.containsKey("building") -> RANK_BUILDING
            tags.containsKey("amenity") -> RANK_POI
            tags.containsKey("shop") -> RANK_POI
            tags.containsKey("tourism") -> RANK_POI
            else -> RANK_POI
        }

    private fun calculateImportance(tags: Map<String, String>): BigDecimal {
        val popularity = OSMPopularityCalculator.calculatePopularity(tags)
        return ImportanceCalculator.calculateImportance(popularity).toBigDecimalWithScale()
    }
}
