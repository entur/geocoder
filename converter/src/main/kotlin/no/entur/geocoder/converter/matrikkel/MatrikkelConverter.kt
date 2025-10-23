package no.entur.geocoder.converter.matrikkel

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Source
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.PlaceId
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.NominatimPlace
import no.entur.geocoder.converter.NominatimPlace.*
import no.entur.geocoder.converter.Util.titleize
import no.entur.geocoder.converter.importance.ImportanceCalculator
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Paths

class MatrikkelConverter : Converter {
    override fun convert(
        input: File,
        output: File,
        isAppending: Boolean,
    ) {
        val outputPath = Paths.get(output.absolutePath)

        val addressNominatim = parseCsv(input).map { convertAddressToNominatim(it) }
        JsonWriter().export(addressNominatim, outputPath, isAppending)

        val streetData = mutableMapOf<Pair<String, String?>, StreetAggregator>()
        parseCsv(input).forEach { adresse ->
            if (adresse.adressenavn != null) {
                val key = adresse.adressenavn to adresse.kommunenummer
                val aggregator = streetData.getOrPut(key) {
                    StreetAggregator(adresse)
                }
                aggregator.add(adresse.nord, adresse.ost)
            }
        }

        val streetNominatim = streetData.values.asSequence().map { agg ->
            convertStreetToNominatim(agg.representative, agg.getAvgNord(), agg.getAvgOst())
        }
        JsonWriter().export(streetNominatim, outputPath, true)
    }

    private class StreetAggregator(val representative: MatrikkelAdresse) {
        private var sumNord = 0.0
        private var sumOst = 0.0
        private var count = 0

        fun add(nord: Double, ost: Double) {
            sumNord += nord
            sumOst += ost
            count++
        }

        fun getAvgNord() = sumNord / count
        fun getAvgOst() = sumOst / count
    }

    private fun convertAddressToNominatim(adresse: MatrikkelAdresse): NominatimPlace =
        convertToNominatim(
            adresse = adresse,
            northing = adresse.nord,
            easting = adresse.ost,
            placeId = PlaceId.address.create(adresse.lokalid),
            id = adresse.lokalid,
            categories = listOf(Category.OSM_ADDRESS, Category.SOURCE_ADRESSE),
            popularity = MatrikkelPopularityCalculator.calculateAddressPopularity(),
            displayName = null,
            housenumber = adresse.nummer + (adresse.bokstav ?: ""),
            postcode = adresse.postnummer,
        )

    private fun convertStreetToNominatim(
        adresse: MatrikkelAdresse,
        northing: Double,
        easting: Double,
    ): NominatimPlace {
        val streetName = adresse.adressenavn ?: ""
        return convertToNominatim(
            adresse = adresse,
            northing = northing,
            easting = easting,
            placeId = PlaceId.street.create(adresse.lokalid),
            id = "KVE:TopographicPlace:${adresse.kommunenummer}-$streetName",
            categories = listOf(Category.OSM_STREET, Category.SOURCE_ADRESSE),
            popularity = MatrikkelPopularityCalculator.calculateStreetPopularity(),
            displayName = streetName,
            housenumber = null,
            postcode = null,
        )
    }

    private fun convertToNominatim(
        adresse: MatrikkelAdresse,
        northing: Double,
        easting: Double,
        placeId: Long,
        id: String,
        categories: List<String>,
        popularity: Double,
        displayName: String?,
        housenumber: String?,
        postcode: String?,
    ): NominatimPlace {
        val (lat, lon) = Geo.convertUTM33ToLatLon(easting, northing)

        val extra =
            Extra(
                id = id,
                source = Source.KARTVERKET_ADRESSE,
                accuracy = "point",
                country_a = "NOR",
                county_gid = adresse.kommunenummer?.let { "KVE:TopographicPlace:${it.take(2)}" },
                locality = adresse.kommunenavn?.titleize(),
                locality_gid = adresse.kommunenummer?.let { "KVE:TopographicPlace:$it" },
                borough = adresse.grunnkretsnavn?.titleize(),
                borough_gid = adresse.grunnkretsnummer?.let { "borough:$it" },
                tags = categories.joinToString(","),
            )

        val properties =
            PlaceContent(
                place_id = placeId,
                object_type = "N",
                object_id = placeId,
                categories = categories,
                rank_address = 26,
                importance = ImportanceCalculator.calculateImportance(popularity),
                parent_place_id = 0,
                name = displayName?.let { Name(it) },
                housenumber = housenumber,
                address =
                    Address(
                        street = adresse.adressenavn,
                        city = adresse.poststed.titleize() ?: adresse.kommunenavn?.titleize(),
                        county = "TODO",
                    ),
                postcode = postcode,
                country_code = "no",
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extra = extra,
            )

        return NominatimPlace("Place", listOf(properties))
    }


    fun parseCsv(inputFile: File): Sequence<MatrikkelAdresse> =
        sequence {
            BufferedReader(FileReader(inputFile)).use { reader ->
                reader.readLine() // Skip header line

                var line: String? = reader.readLine()
                while (line != null) {
                    val tokens = line.split(';')
                    if (tokens.size >= 46 && tokens[3] == "vegadresse") {
                        val adresse =
                            MatrikkelAdresse(
                                lokalid = tokens[0].ifEmpty { "-1" },
                                kommunenummer = tokens[1].ifEmpty { null },
                                kommunenavn = tokens[2].ifEmpty { null },
                                adressetype = tokens[3].ifEmpty { null },
                                adressetilleggsnavn = tokens[4].ifEmpty { null },
                                adressetilleggsnavnKilde = tokens[5].ifEmpty { null },
                                adressekode = tokens[6].ifEmpty { null },
                                adressenavn = tokens[7].ifEmpty { null },
                                nummer = tokens[8].ifEmpty { null },
                                bokstav = tokens[9].ifEmpty { null },
                                gardsnummer = tokens[10].ifEmpty { null },
                                bruksnummer = tokens[11].ifEmpty { null },
                                festenummer = tokens[12].ifEmpty { null },
                                undernummer = tokens[13].ifEmpty { null },
                                adresseTekst = tokens[14],
                                adresseTekstUtenAdressetilleggsnavn = tokens[15].ifEmpty { null },
                                epsgKode = tokens[16].ifEmpty { null },
                                nord = tokens[17].toDouble(),
                                ost = tokens[18].toDouble(),
                                postnummer = tokens[19].ifEmpty { null },
                                poststed = tokens[20],
                                grunnkretsnummer = tokens[21].ifEmpty { null },
                                grunnkretsnavn = tokens[22].ifEmpty { null },
                                soknenummer = tokens[23].ifEmpty { null },
                                soknenavn = tokens[24].ifEmpty { null },
                                organisasjonsnummer = tokens[25].ifEmpty { null },
                                tettstednummer = tokens[26].ifEmpty { null },
                                tettstednavn = tokens[27].ifEmpty { null },
                                valgkretsnummer = tokens[28].ifEmpty { null },
                                valgkretsnavn = tokens[29].ifEmpty { null },
                                oppdateringsdato = tokens[30].ifEmpty { null },
                                datauttaksdato = tokens[31].ifEmpty { null },
                                adresseId = tokens[32],
                                uuidAdresse = tokens[33].ifEmpty { null },
                                atkomstId = tokens[34].ifEmpty { null },
                                uuidAtkomst = tokens[35].ifEmpty { null },
                                atkomstNord = tokens[36].toDoubleOrNull(),
                                atkomstOst = tokens[37].toDoubleOrNull(),
                                sommeratkomstId = tokens[38].ifEmpty { null },
                                uuidSommeratkomst = tokens[39].ifEmpty { null },
                                sommeratkomstNord = tokens[40].toDoubleOrNull(),
                                sommeratkomstOst = tokens[41].toDoubleOrNull(),
                                vinteratkomstId = tokens[42].ifEmpty { null },
                                uuidVinteratkomst = tokens[43].ifEmpty { null },
                                vinteratkomstNord = tokens[44].toDoubleOrNull(),
                                vinteratkomstOst = tokens[45].toDoubleOrNull(),
                            )
                        yield(adresse)
                    }
                    line = reader.readLine()
                }
            }
        }
}
