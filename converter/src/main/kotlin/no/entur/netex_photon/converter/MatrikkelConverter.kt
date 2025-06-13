package no.entur.netex_photon.converter

import no.entur.netex_photon.converter.ConverterUtils.titleize
import no.entur.netex_photon.converter.ConverterUtils.mapOfNotNull
import no.entur.netex_photon.converter.NominatimPlace.PlaceContent
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Paths
import kotlin.math.abs

class MatrikkelConverter {

    fun convertCsv(input: File, output: File, isAppending: Boolean = false) {
        val adressEntries = parseCsv(input)
        val nominatimEntries = adressEntries.map { convertMatrikkelAdresseToNominatim(it) }

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(nominatimEntries, outputPath, isAppending)
    }

    fun parseCsv(inputFile: File): Sequence<MatrikkelAdresse> =
        sequence {
            BufferedReader(FileReader(inputFile)).use { reader ->
                reader.readLine() // Skip header line

                var line: String? = reader.readLine()
                while (line != null) {
                    val tokens = line.split(';')
                    if (tokens.size >= 46) {
                        val adresse = MatrikkelAdresse(
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
                            vinteratkomstOst = tokens[45].toDoubleOrNull()
                        )
                        yield(adresse)
                    }
                    line = reader.readLine()
                }
            }
        }

    fun convertMatrikkelAdresseToNominatim(adresse: MatrikkelAdresse): NominatimPlace {
        val (lat, lon) = CoordinateConverter.convertUTM33ToLatLon(adresse.ost, adresse.nord)

        val extratags = mapOfNotNull(
            "id" to adresse.lokalid,
            "gid" to "openaddresses:address:${adresse.lokalid}",
            "layer" to "address",
            "source" to "kartverket",
            "source_id" to adresse.lokalid,
            "name" to adresse.adresseTekst,
            "housenumber" to adresse.nummer,
            "street" to adresse.adressenavn,
            "postalcode" to adresse.postnummer,
            "accuracy" to "point",
            "country_a" to "NOR",
            "county" to "TODO",
            "county_gid" to adresse.kommunenummer?.let { "KVE:TopographicPlace:${it.take(2)}" }, // TODO: just guessing
            "locality" to adresse.kommunenavn?.titleize(),
            "locality_gid" to adresse.kommunenummer?.let { "KVE:TopographicPlace:$it" },
            "borough" to adresse.grunnkretsnavn?.titleize(),
            "borough_gid" to adresse.grunnkretsnummer?.let { "borough:$it" },
            "label" to (adresse.adresseTekst + ", " + adresse.poststed.titleize()),
            "category" to adresse.adressetype
        )

        val properties = PlaceContent(
            place_id = abs(adresse.adresseId.hashCode().toLong()),
            object_type = "N",
            object_id = abs(adresse.adresseId.hashCode().toLong()),
            categories = emptyList(),
            rank_address = 26,
            importance = 0.1,
            parent_place_id = 0,
            name = mapOf("name" to adresse.adresseTekst),
            address = mapOfNotNull(
                "housenumber" to adresse.nummer,
                "street" to adresse.adressenavn,
                "postcode" to adresse.postnummer,
                "city" to adresse.poststed.titleize(),
                "county" to "TODO" // Placeholder for county, needs proper mapping
            ),
            postcode = adresse.postnummer,
            country_code = "no",
            centroid = listOf(lon.toDouble(), lat.toDouble()),
            bbox = listOf(lon.toDouble(), lat.toDouble(), lon.toDouble(), lat.toDouble()),
            extratags = extratags
        )
        return NominatimPlace("Place", listOf(properties))
    }
}
