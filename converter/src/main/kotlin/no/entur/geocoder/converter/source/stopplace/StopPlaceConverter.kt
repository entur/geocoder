package no.entur.geocoder.converter.source.stopplace

import no.entur.geocoder.common.*
import no.entur.geocoder.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.common.Category.GOSP
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.SOURCE_NSR
import no.entur.geocoder.common.Category.asCategory
import no.entur.geocoder.common.LegacyLayer.address
import no.entur.geocoder.common.LegacyLayer.venue
import no.entur.geocoder.common.LegacySource.*
import no.entur.geocoder.common.Text.OSM_TAG_SEPARATOR
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.ConverterConfig
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.common.Text.joinOsmValuesToString
import no.entur.geocoder.converter.source.ImportanceCalculator
import no.entur.geocoder.converter.source.NorwegianToEnglishTranslator
import no.entur.geocoder.converter.source.stopplace.StopPlaceConverter.StopPlaceRole.*
import no.entur.geocoder.converter.target.NominatimId
import no.entur.geocoder.converter.target.NominatimPlace
import no.entur.geocoder.converter.target.NominatimPlace.*
import java.io.File
import java.nio.file.Paths

class StopPlaceConverter(private val config: ConverterConfig) : Converter {
    private val stopPlacePopularityCalculator = StopPlacePopularityCalculator(config.stopPlace)
    private val groupOfStopPlacesPopularityCalculator = GroupOfStopPlacesPopularityCalculator(config.groupOfStopPlaces)
    private val importanceCalculator = ImportanceCalculator(config.importance)

    override fun convert(input: File, output: File, isAppending: Boolean) {
        val parser = NetexParser()
        val result: NetexParser.ParseResult = parser.parseXml(input)
        val entries: Sequence<NominatimPlace> = convertNetexParseResult(result)

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(entries, outputPath, isAppending)
    }

    fun convertStopPlaceToNominatim(
        stopPlace: StopPlace,
        topoPlaces: Map<String, TopographicPlace>,
        stopPlaceTypes: Map<String, List<String>>,
        fareZones: Map<String, FareZone>,
        popularity: Long,
        childStopNames: List<String> = emptyList(),
        childStops: List<StopPlace> = emptyList(),
    ): List<NominatimPlace> {
        val entries = mutableListOf<NominatimPlace>()
        val coord =
            Coordinate(
                stopPlace.centroid.location.latitude,
                stopPlace.centroid.location.longitude,
            )

        val localityGid = stopPlace.topographicPlaceRef?.ref
        val locality = topoPlaces[localityGid]?.descriptor?.name?.text
        val countyGid = topoPlaces[stopPlace.topographicPlaceRef?.ref]?.parentTopographicPlaceRef?.ref
        val county = topoPlaces[countyGid]?.descriptor?.name?.text
        val country = determineCountry(topoPlaces, stopPlace, coord)
        val childStopTypes = stopPlaceTypes.getOrDefault(stopPlace.id, emptyList())

        val importance = importanceCalculator.calculateImportance(popularity).toBigDecimalWithScale()

        val tariffZoneIds = tariffZoneIdCategories(stopPlace)
        val tariffZoneAuthorities = tariffZoneAuthorityCategories(stopPlace)
        val fareZoneAuthorities = fareZoneAuthorityCategories(stopPlace, fareZones)

        val stopPlaceRole = resolveStopPlaceRole(childStopTypes, stopPlace)
        val multimodalityCategory = resolveModalityCategory(stopPlaceRole)
        val inferredStopPlaceTypes = inferStopPlaceTypes(childStopTypes, stopPlace)
        val sourceCategory = resolveSourceCategory(stopPlaceRole)

        val visibleCategories: List<String> =
            listOf(Category.OSM_STOP_PLACE, venue.category())
                .plus(legacyTransportModeCategories(stopPlace))
                .plus(sourceCategory)

        val indexedCategories: List<String> =
            visibleCategories
                .plus(inferredStopPlaceTypes.map { LEGACY_CATEGORY_PREFIX + it })
                .plus(SOURCE_NSR + "." + stopPlaceRole.name)
                .plus(tariffZoneIds)
                .plus(tariffZoneAuthorities)
                .plus(fareZoneAuthorities)
                .plus(COUNTRY_PREFIX + country.name)
                .plus(countyGid?.let { Category.countyIdsCategory(it) })
                .plus(localityGid?.let { Category.localityIdsCategory(it) })
                .plus(multimodalityCategory)
                .plus(stopPlace.id.asCategory())
                .filterNotNull()

        val visibleAltStopNames: Set<String> = altStopNames(stopPlace, nameType = "label")
        val indexedAltStopNames: Set<String> = altStopNames(stopPlace) + childStopNames + stopPlace.id

        val tariffZoneList =
            stopPlace.tariffZones
                ?.tariffZoneRef
                ?.mapNotNull { it.ref }
                ?.joinOsmValuesToString()

        val extra =
            Extra(
                id = stopPlace.id,
                source = Source.NSR,
                accuracy = "point",
                country_a = country.threeLetterCode,
                county_gid = countyGid,
                locality = locality,
                locality_gid = localityGid,
                tariff_zones = tariffZoneList,
                alt_name = visibleAltStopNames.joinOsmValuesToString(),
                description = descriptionWithTranslation(stopPlace.description),
                tags = visibleCategories.joinOsmValuesToString(),
                transport_mode = collectTransportModes(stopPlace, childStops),
                stop_place_type = inferredStopPlaceTypes.joinToString(OSM_TAG_SEPARATOR).ifBlank { null },
            )

        val nominatimId = NominatimId.stopplace.create(stopPlace.id)
        val stopPlaceContent =
            PlaceContent(
                place_id = nominatimId,
                object_type = "N",
                object_id = nominatimId,
                categories = indexedCategories,
                rank_address = config.stopPlace.rankAddress,
                importance = importance,
                parent_place_id = 0,
                name =
                    Name(
                        name = stopPlace.name.text,
                        alt_name = indexedAltStopNames.joinOsmValuesToString(),
                    ),
                address =
                    Address(
                        city = locality,
                        county = county,
                    ),
                postcode = null,
                country_code = country.name,
                centroid = coord.centroid(),
                bbox = coord.bbox(),
                extra = extra,
            )
        entries.add(NominatimPlace("Place", listOf(stopPlaceContent)))

        return entries
    }

    private fun resolveStopPlaceRole(childStopTypes: List<String>, stopPlace: StopPlace): StopPlaceRole =
        when {
            childStopTypes.isNotEmpty() -> parent
            stopPlace.parentSiteRef?.ref != null -> child
            else -> standalone
        }

    private fun resolveModalityCategory(stopPlaceRole: StopPlaceRole): String? =
        when (stopPlaceRole) {
            parent -> "multimodal.parent"
            child -> "multimodal.child"
            standalone -> null
        }

    private fun resolveSourceCategory(stopPlaceRole: StopPlaceRole): String =
        when (stopPlaceRole) {
            parent -> openstreetmap.category()
            child -> geonames.category()
            standalone -> whosonfirst.category()
        }

    private fun altStopNames(stopPlace: StopPlace, nameType: String? = null): Set<String> =
        stopPlace.alternativeNames
            ?.alternativeName
            ?.filter { nameType == null || it.nameType == nameType }
            ?.mapNotNull { it.name?.text }
            ?.filter { stopPlace.name.text != it }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    private fun buildChildStopNamesMap(stopPlaces: List<StopPlace>): Map<String, List<String>> {
        val childStopNamesMap = mutableMapOf<String, MutableList<String>>()
        for (stopPlace in stopPlaces) {
            val parentRef = stopPlace.parentSiteRef?.ref ?: continue
            val name = stopPlace.name.text ?: continue

            childStopNamesMap.getOrPut(parentRef) { mutableListOf() }.add(name)
        }
        return childStopNamesMap
    }

    private fun buildChildStopsMap(stopPlaces: List<StopPlace>): Map<String, List<StopPlace>> {
        val childStopsMap = mutableMapOf<String, MutableList<StopPlace>>()
        for (stopPlace in stopPlaces) {
            val parentRef = stopPlace.parentSiteRef?.ref ?: continue
            childStopsMap.getOrPut(parentRef) { mutableListOf() }.add(stopPlace)
        }
        return childStopsMap
    }

    private fun inferStopPlaceTypes(childStopTypes: List<String>, stopPlace: StopPlace): List<String> =
        childStopTypes
            .plus(stopPlace.stopPlaceType)
            .filterNotNull()

    val includeTransportModeAsStopPlaceType = listOf("funicular")

    private fun legacyTransportModeCategories(stopPlace: StopPlace): List<String> =
        includeTransportModeAsStopPlaceType
            .filter { it == stopPlace.transportMode }
            .map { LEGACY_CATEGORY_PREFIX + it }

    private fun descriptionWithTranslation(desc: StopPlace.LocalizedText?): String? {
        val norwegianText = desc?.text ?: return null
        val englishText = NorwegianToEnglishTranslator.translate(norwegianText)
        return "nor:$norwegianText;eng:$englishText"
    }

    private fun formatTransportMode(stopPlace: StopPlace): String? {
        val mode = stopPlace.transportMode ?: return null
        val submode = extractTransportSubMode(stopPlace)
        return if (submode != null) "$mode:$submode" else mode
    }

    private fun collectTransportModes(stopPlace: StopPlace, childStops: List<StopPlace>): String? {
        val ownMode = formatTransportMode(stopPlace)
        val childModes = childStops.mapNotNull { formatTransportMode(it) }
        val allModes = (listOfNotNull(ownMode) + childModes).distinct()
        return allModes.ifEmpty { null }?.joinToString(OSM_TAG_SEPARATOR)
    }

    private fun extractTransportSubMode(stopPlace: StopPlace): String? =
        stopPlace.busSubmode
            ?: stopPlace.tramSubmode
            ?: stopPlace.railSubmode
            ?: stopPlace.metroSubmode
            ?: stopPlace.airSubmode
            ?: stopPlace.waterSubmode
            ?: stopPlace.telecabinSubmode

    private fun tariffZoneAuthorityCategories(stopPlace: StopPlace): Set<String> = (
        stopPlace.tariffZones
            ?.tariffZoneRef
            ?.asSequence()
            ?.mapNotNull { it.ref }
            ?.filter { it.contains(":TariffZone:") }
            ?.map { it.split(":").first() }
            ?.map { Category.TARIFF_ZONE_AUTH_PREFIX + it }
            ?.toSet()
            ?: emptySet()
    )

    private fun fareZoneAuthorityCategories(stopPlace: StopPlace, fareZones: Map<String, FareZone>): Set<String> = (
        stopPlace.tariffZones
            ?.tariffZoneRef
            ?.mapNotNull { tariffZoneRef ->
                tariffZoneRef.ref?.let { ref ->
                    fareZones[ref]?.authorityRef?.ref?.let { authorityRef ->
                        Category.fareZoneAuthorityCategory(authorityRef)
                    }
                }
            }?.toSet()
            ?: emptySet()
    )

    private fun tariffZoneIdCategories(stopPlace: StopPlace): Set<String> = (
        stopPlace.tariffZones
            ?.tariffZoneRef
            ?.mapNotNull {
                it.ref?.let { ref -> Category.tariffZoneIdCategory(ref) }
            }?.toSet()
            ?: emptySet()
    )

    private fun determineCountry(
        topoPlaces: Map<String, TopographicPlace>,
        stopPlace: StopPlace,
        coord: Coordinate,
    ): Country = (
        Country.parse(topoPlaces[stopPlace.topographicPlaceRef?.ref]?.countryRef?.ref)
            ?: Geo.getCountry(coord) ?: Country.no
    )

    fun convertGroupOfStopPlacesToNominatim(
        groupOfStopPlaces: GroupOfStopPlaces,
        topoPlaces: Map<String, TopographicPlace>,
        stopPlacePopularities: Map<String, Long>,
        stopPlaces: List<StopPlace>,
    ): NominatimPlace {
        val coord =
            Coordinate(
                groupOfStopPlaces.centroid.location.latitude,
                groupOfStopPlaces.centroid.location.longitude,
            )

        val groupName = groupOfStopPlaces.name.text

        var locality: String? = groupName
        var localityGid: String? = null
        var county: String? = null
        var countyGid: String? = null

        groupOfStopPlaces.members?.stopPlaceRef?.any { stopPlaceRef ->
            val stopPlace = stopPlaces.find { it.id == stopPlaceRef.ref }
            val topoPlaceRef = stopPlace?.topographicPlaceRef?.ref
            val topoPlace = topoPlaces[topoPlaceRef]
            if (topoPlace?.topographicPlaceType == "municipality") {
                localityGid = topoPlaceRef
                locality = topoPlace.descriptor?.name?.text
                countyGid = topoPlace.parentTopographicPlaceRef?.ref
                county = topoPlaces[countyGid]?.descriptor?.name?.text
                return@any true
            }
            return@any false
        }

        // Calculate importance based on member stop place popularities
        val memberPopularities =
            groupOfStopPlaces.members
                ?.stopPlaceRef
                ?.mapNotNull { it.ref }
                ?.mapNotNull { stopPlacePopularities[it] }
                ?: emptyList()

        val popularity = groupOfStopPlacesPopularityCalculator.calculatePopularity(memberPopularities)
        val importance = importanceCalculator.calculateImportance(popularity).toBigDecimalWithScale()

        val visibleCategories =
            listOf(Category.OSM_GOSP, address.category(), whosonfirst.category())
                .plus(LEGACY_CATEGORY_PREFIX + GOSP)

        val country = Geo.getCountry(coord) ?: Country.no
        val id = groupOfStopPlaces.id
        val indexedCategories =
            visibleCategories
                .plus(SOURCE_NSR)
                .plus(COUNTRY_PREFIX + country.name)
                .plus(countyGid?.let { Category.countyIdsCategory(it) })
                .plus(localityGid?.let { Category.localityIdsCategory(it) })
                .plus(id.asCategory())
                .filterNotNull()

        val nominatimId = NominatimId.gosp.create(id)
        val placeContent =
            PlaceContent(
                place_id = nominatimId,
                object_type = "N",
                object_id = nominatimId,
                categories = indexedCategories,
                rank_address = config.groupOfStopPlaces.rankAddress,
                importance = importance,
                parent_place_id = 0,
                name = Name(name = groupName, alt_name = id),
                address =
                    Address(
                        city = locality,
                        county = county,
                    ),
                postcode = null,
                country_code = country.name,
                centroid = coord.centroid(),
                bbox = coord.bbox(),
                extra =
                    Extra(
                        id = id,
                        source = Source.NSR,
                        accuracy = "point",
                        country_a = country.threeLetterCode,
                        county_gid = countyGid,
                        locality = locality,
                        locality_gid = localityGid,
                        tags = visibleCategories.joinOsmValuesToString(),
                    ),
            )

        return NominatimPlace("Place", listOf(placeContent))
    }

    fun convertNetexParseResult(result: NetexParser.ParseResult): Sequence<NominatimPlace> {
        val stopPlacesList = result.stopPlaces.toList()

        val stopPlacePopularities =
            stopPlacesList.associate { stopPlace ->
                val childStopTypes = result.stopPlaceTypes.getOrDefault(stopPlace.id, emptyList())
                val popularity = stopPlacePopularityCalculator.calculatePopularity(stopPlace, childStopTypes)
                stopPlace.id to popularity
            }

        // Build maps for parent stop places
        val childStopNamesMap = buildChildStopNamesMap(stopPlacesList)
        val childStopsMap = buildChildStopsMap(stopPlacesList)

        val stopPlaceEntries =
            stopPlacesList.asSequence().flatMap { stopPlace ->
                val popularity = stopPlacePopularities[stopPlace.id] ?: 0L
                val childStopNames = childStopNamesMap[stopPlace.id] ?: emptyList()
                val childStops = childStopsMap[stopPlace.id] ?: emptyList()
                convertStopPlaceToNominatim(
                    stopPlace,
                    result.topoPlaces,
                    result.stopPlaceTypes,
                    result.fareZones,
                    popularity,
                    childStopNames,
                    childStops,
                ).asSequence()
            }

        val groupOfStopPlacesEntries =
            result.groupOfStopPlaces.map {
                convertGroupOfStopPlacesToNominatim(
                    it,
                    result.topoPlaces,
                    stopPlacePopularities,
                    stopPlacesList,
                )
            }

        return stopPlaceEntries + groupOfStopPlacesEntries
    }

    enum class StopPlaceRole { child, parent, standalone }
}
