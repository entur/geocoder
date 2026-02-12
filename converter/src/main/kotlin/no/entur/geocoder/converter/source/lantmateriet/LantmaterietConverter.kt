package no.entur.geocoder.converter.source.lantmateriet

import no.entur.geocoder.common.*
import no.entur.geocoder.common.Category.COUNTRY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.OSM_ADDRESS
import no.entur.geocoder.common.Category.OSM_STREET
import no.entur.geocoder.common.Category.SOURCE_LANTMATERIET
import no.entur.geocoder.common.Category.asCategory
import no.entur.geocoder.common.LegacyLayer.address
import no.entur.geocoder.common.LegacySource.openaddresses
import no.entur.geocoder.common.LegacySource.whosonfirst
import no.entur.geocoder.common.Text.joinOsmValuesToString
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.ConverterConfig
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.source.ImportanceCalculator
import no.entur.geocoder.converter.target.NominatimId
import no.entur.geocoder.converter.target.NominatimPlace
import no.entur.geocoder.converter.target.NominatimPlace.*
import java.io.File
import java.nio.file.Paths

class LantmaterietConverter(private val config: ConverterConfig) : Converter {
    private val popularityCalculator = LantmaterietPopularityCalculator(config.lantmateriet)
    private val importanceCalculator = ImportanceCalculator(config.importance)
    private val reader = GeoPackageReader()
    private val country = Country.se

    override fun convert(input: File, output: File, isAppending: Boolean) {
        val outputPath = Paths.get(output.absolutePath)
        val gpkgFiles = resolveGpkgFiles(input)

        // Pass 1: individual addresses
        val addresses =
            gpkgFiles.asSequence().flatMap { file ->
                reader.read(file).map { convertAddressToNominatim(it) }
            }
        JsonWriter().export(addresses, outputPath, isAppending)

        // Pass 2: aggregate streets/places by (name, kommunkod)
        val streetData = mutableMapOf<Pair<String, String>, StreetAggregator>()
        gpkgFiles.forEach { file ->
            reader.read(file).forEach { adress ->
                val name = adress.streetName() ?: return@forEach
                val key = name to adress.kommunkod
                val aggregator = streetData.getOrPut(key) { StreetAggregator(adress) }
                aggregator.add(adress.easting, adress.northing)
            }
        }

        val streets =
            streetData.values.asSequence().map { agg ->
                convertStreetToNominatim(agg.representative, agg.getAvgEasting(), agg.getAvgNorthing())
            }
        JsonWriter().export(streets, outputPath, true)
    }

    private fun resolveGpkgFiles(input: File): List<File> =
        if (input.isDirectory) {
            input.listFiles { f -> f.extension.equals("gpkg", ignoreCase = true) }?.toList()
                ?: emptyList()
        } else {
            listOf(input)
        }

    private class StreetAggregator(val representative: BelagenhetsAdress) {
        private var sumEasting = 0.0
        private var sumNorthing = 0.0
        private var count = 0

        fun add(easting: Double, northing: Double) {
            sumEasting += easting
            sumNorthing += northing
            count++
        }

        fun getAvgEasting() = sumEasting / count

        fun getAvgNorthing() = sumNorthing / count
    }

    private fun convertAddressToNominatim(adress: BelagenhetsAdress): NominatimPlace =
        convertToNominatim(
            adress = adress,
            easting = adress.easting,
            northing = adress.northing,
            placeId = NominatimId.sweAddress.create(adress.objektidentitet),
            id = adress.objektidentitet,
            tags = listOf(OSM_ADDRESS, openaddresses.category(), address.category(), LEGACY_CATEGORY_PREFIX + "vegadresse"),
            popularity = popularityCalculator.calculateAddressPopularity(),
            displayName = null,
            housenumber = adress.housenumber(),
            postcode = adress.postnummer,
        )

    private fun convertStreetToNominatim(
        adress: BelagenhetsAdress,
        avgEasting: Double,
        avgNorthing: Double,
    ): NominatimPlace {
        val name = adress.streetName() ?: ""
        return convertToNominatim(
            adress = adress,
            easting = avgEasting,
            northing = avgNorthing,
            placeId = NominatimId.sweStreet.create("${adress.kommunkod}-$name"),
            id = "LM:TopographicPlace:${adress.kommunkod}-$name",
            tags = listOf(OSM_STREET, whosonfirst.category(), address.category(), LEGACY_CATEGORY_PREFIX + "street"),
            popularity = popularityCalculator.calculateStreetPopularity(),
            displayName = name,
            housenumber = null,
            postcode = null,
        )
    }

    private fun convertToNominatim(
        adress: BelagenhetsAdress,
        easting: Double,
        northing: Double,
        placeId: Long,
        id: String,
        tags: List<String>,
        popularity: Double,
        displayName: String?,
        housenumber: String?,
        postcode: String?,
    ): NominatimPlace {
        val coord = Geo.convertSweref99TmToLatLon(easting, northing)
        val countyName = SwedishCounty.getCountyName(adress.lanskod)
        val localityGid = "LM:TopographicPlace:${adress.kommunkod}"

        val indexedAltNames: Set<String> = setOfNotNull(adress.popularnamn, id)

        val extra =
            Extra(
                id = id,
                source = Source.LANTMATERIET_ADRESSE,
                accuracy = "point",
                country_a = country.threeLetterCode,
                locality = adress.kommunnamn,
                locality_gid = localityGid,
                borough = adress.kommundelFaststalltnamn,
                tags = tags.joinOsmValuesToString(),
                alt_name = adress.popularnamn,
            )

        val indexedCategories =
            tags
                .plus(SOURCE_LANTMATERIET)
                .plus(COUNTRY_PREFIX + country.name)
                .plus(id.asCategory())
                .plus(listOfNotNull(localityGid.let { Category.countyIdsCategory(it) }))

        val street =
            if (adress.isStreetAddress) {
                adress.streetName()
            } else {
                null
            }

        val properties =
            PlaceContent(
                place_id = placeId,
                object_type = "N",
                object_id = placeId,
                categories = indexedCategories,
                rank_address = config.lantmateriet.rankAddress,
                importance = importanceCalculator.calculateImportance(popularity).toBigDecimalWithScale(),
                parent_place_id = 0,
                name =
                    displayName?.let {
                        Name(
                            name = displayName,
                            alt_name = indexedAltNames.joinOsmValuesToString(),
                        )
                    },
                housenumber = housenumber,
                address =
                    Address(
                        street = street,
                        city = adress.postort,
                        county = countyName,
                    ),
                postcode = postcode,
                country_code = country.name,
                centroid = coord.centroid(),
                bbox = coord.bbox(),
                extra = extra,
            )

        return NominatimPlace("Place", listOf(properties))
    }
}
