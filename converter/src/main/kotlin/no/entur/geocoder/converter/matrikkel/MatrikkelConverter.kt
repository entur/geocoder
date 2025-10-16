package no.entur.geocoder.converter.matrikkel

import no.entur.geocoder.common.Extra
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.NominatimPlace
import no.entur.geocoder.converter.NominatimPlace.Address
import no.entur.geocoder.converter.NominatimPlace.Name
import no.entur.geocoder.converter.NominatimPlace.PlaceContent
import no.entur.geocoder.converter.Util.titleize
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import kotlin.math.abs

class MatrikkelConverter : Converter {
    override fun convert(
        input: File,
        output: File,
        isAppending: Boolean,
    ) {
        val outputPath = Paths.get(output.absolutePath)
        val adressEntries = parseCsv(input).toList()

        // Convert addresses
        val addressNominatim = adressEntries.asSequence().map { convertAddressToNominatim(it) }
        JsonWriter().export(addressNominatim, outputPath, isAppending)

        // Group by street and create street entries
        val streetNominatim = adressEntries
            .filter { it.adressenavn != null }
            .groupBy { it.adressenavn!! to it.kommunenummer }
            .values
            .asSequence()
            .map { addressesForStreet ->
                val avgNord = addressesForStreet.map { it.nord }.average()
                val avgOst = addressesForStreet.map { it.ost }.average()
                val representative = addressesForStreet.first()
                convertStreetToNominatim(representative, avgNord, avgOst)
            }

        JsonWriter().export(streetNominatim, outputPath, true)

    }

    private fun convertAddressToNominatim(adresse: MatrikkelAdresse): NominatimPlace =
        convertToNominatim(
            adresse = adresse,
            nord = adresse.nord,
            ost = adresse.ost,
            id = adresse.lokalid,
            source = "kartverket",
            categories = listOf("osm.public_transport.address"),
            importance = 0.09,
            displayName = adresse.adresseTekst,
            housenumber = adresse.nummer,
            postcode = adresse.postnummer,
            label = "${adresse.adresseTekst}, ${adresse.poststed.titleize()}",
        )

    private fun convertStreetToNominatim(
        adresse: MatrikkelAdresse,
        nord: Double,
        ost: Double,
    ): NominatimPlace {
        val streetName = adresse.adressenavn ?: ""
        return convertToNominatim(
            adresse = adresse,
            nord = nord,
            ost = ost,
            id = "KVE:TopographicPlace:${adresse.kommunenummer}-$streetName",
            source = "kartverket",
            categories = listOf("osm.public_transport.street"),
            importance = 0.1,
            displayName = streetName,
            housenumber = null,
            postcode = null,
            label = "$streetName, ${adresse.kommunenavn?.titleize() ?: adresse.poststed.titleize()}",
        )
    }

    private fun convertToNominatim(
        adresse: MatrikkelAdresse,
        nord: Double,
        ost: Double,
        id: String?,
        source: String,
        categories: List<String>,
        importance: Double,
        displayName: String,
        housenumber: String?,
        postcode: String?,
        label: String,
    ): NominatimPlace {
        val (lat, lon) = Geo.convertUTM33ToLatLon(ost, nord)

        val extra =
            Extra(
                id = id,
                source = source,
                accuracy = "point",
                country_a = "NOR",
                county_gid = adresse.kommunenummer?.let { "KVE:TopographicPlace:${it.take(2)}" },
                locality = adresse.kommunenavn?.titleize(),
                locality_gid = adresse.kommunenummer?.let { "KVE:TopographicPlace:$it" },
                borough = adresse.grunnkretsnavn?.titleize(),
                borough_gid = adresse.grunnkretsnummer?.let { "borough:$it" },
                label = label,
                tags = categories.joinToString(","),
            )

        val properties =
            PlaceContent(
                place_id = abs((id ?: adresse.adresseId).hashCode().toLong()),
                object_type = "N",
                object_id = abs((id ?: adresse.adresseId).hashCode().toLong()),
                categories = categories,
                rank_address = 26,
                importance = importance,
                parent_place_id = 0,
                name = Name(displayName),
                housenumber = housenumber,
                address =
                    Address(
                        street = adresse.adressenavn,
                        city = adresse.kommunenavn?.titleize() ?: adresse.poststed.titleize(),
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
                                lokalid = tokens[0].ifEmpty { null },
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
