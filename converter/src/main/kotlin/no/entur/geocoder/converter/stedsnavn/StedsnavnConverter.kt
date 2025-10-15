package no.entur.geocoder.converter.stedsnavn

import no.entur.geocoder.common.Extra
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.NominatimPlace
import no.entur.geocoder.converter.NominatimPlace.Address
import no.entur.geocoder.converter.NominatimPlace.Name
import no.entur.geocoder.converter.NominatimPlace.PlaceContent
import no.entur.geocoder.converter.Util.titleize
import no.entur.geocoder.converter.matrikkel.Geo
import java.io.File
import java.nio.file.Paths
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import kotlin.math.abs

class StedsnavnConverter : Converter {
    override fun convert(
        input: File,
        output: File,
        isAppending: Boolean,
    ) {
        val entries = parseGml(input)
        val nominatimEntries = entries.map { convertToNominatim(it) }

        val outputPath = Paths.get(output.absolutePath)
        JsonWriter().export(nominatimEntries, outputPath, isAppending)
    }

    fun parseGml(inputFile: File): Sequence<StedsnavnEntry> =
        sequence {
            val factory = XMLInputFactory.newInstance()
            val reader = factory.createXMLStreamReader(inputFile.inputStream())

            try {
                while (reader.hasNext()) {
                    reader.next()
                    if (reader.eventType == XMLStreamConstants.START_ELEMENT &&
                        reader.localName == "featureMember"
                    ) {
                        val entry = parseFeatureMember(reader)
                        if (entry != null) {
                            yield(entry)
                        }
                    }
                }
            } finally {
                reader.close()
            }
        }

    private fun readElementText(reader: XMLStreamReader): String {
        val sb = StringBuilder()
        while (reader.hasNext()) {
            reader.next()
            when (reader.eventType) {
                XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> {
                    sb.append(reader.text)
                }
                XMLStreamConstants.END_ELEMENT -> {
                    return sb.toString()
                }
            }
        }
        return sb.toString()
    }

    private fun parseFeatureMember(reader: XMLStreamReader): StedsnavnEntry? {
        var lokalId: String? = null
        var navnerom: String? = null
        var versjonId: String? = null
        var oppdateringsdato: String? = null
        var stedsnavn: String? = null
        var kommunenummer: String? = null
        var kommunenavn: String? = null
        var fylkesnummer: String? = null
        var fylkesnavn: String? = null
        var matrikkelId: String? = null
        var adressekode: String? = null
        var navneobjekttype: String? = null
        val coordinates = mutableListOf<Pair<Double, Double>>()
        var hasVegreferanse = false

        while (reader.hasNext()) {
            reader.next()

            if (reader.eventType == XMLStreamConstants.END_ELEMENT &&
                reader.localName == "featureMember"
            ) {
                break
            }

            if (reader.eventType == XMLStreamConstants.START_ELEMENT) {
                when (reader.localName) {
                    "lokalId" -> {
                        lokalId = readElementText(reader)
                    }
                    "navnerom" -> {
                        navnerom = readElementText(reader)
                    }
                    "versjonId" -> {
                        versjonId = readElementText(reader)
                    }
                    "oppdateringsdato" -> {
                        if (oppdateringsdato == null) {
                            oppdateringsdato = readElementText(reader)
                        }
                    }
                    "komplettskrivemåte" -> {
                        if (stedsnavn == null) {
                            stedsnavn = readElementText(reader)
                        }
                    }
                    "navneobjekttype" -> {
                        navneobjekttype = readElementText(reader)
                    }
                    "vegreferanse" -> {
                        hasVegreferanse = true
                    }
                    "matrikkelId" -> {
                        if (hasVegreferanse && matrikkelId == null) {
                            matrikkelId = readElementText(reader)
                        }
                    }
                    "adressekode" -> {
                        if (hasVegreferanse && adressekode == null) {
                            adressekode = readElementText(reader)
                        }
                    }
                    "kommunenummer" -> {
                        if (kommunenummer == null) {
                            kommunenummer = readElementText(reader)
                        }
                    }
                    "kommunenavn" -> {
                        if (kommunenavn == null) {
                            kommunenavn = readElementText(reader)
                        }
                    }
                    "fylkesnummer" -> {
                        if (fylkesnummer == null) {
                            fylkesnummer = readElementText(reader)
                        }
                    }
                    "fylkesnavn" -> {
                        if (fylkesnavn == null) {
                            fylkesnavn = readElementText(reader)
                        }
                    }
                    "posList" -> {
                        val text = readElementText(reader).trim()
                        val coords = text.split("\\s+".toRegex())
                        for (i in coords.indices step 2) {
                            if (i + 1 < coords.size) {
                                val east = coords[i].toDoubleOrNull()
                                val north = coords[i + 1].toDoubleOrNull()
                                if (east != null && north != null) {
                                    coordinates.add(Pair(east, north))
                                }
                            }
                        }
                    }
                }
            }
        }

        return if (hasVegreferanse &&
            lokalId != null &&
            navnerom != null &&
            stedsnavn != null &&
            kommunenummer != null &&
            kommunenavn != null &&
            fylkesnummer != null &&
            fylkesnavn != null &&
            matrikkelId != null &&
            adressekode != null
        ) {
            StedsnavnEntry(
                lokalId = lokalId,
                navnerom = navnerom,
                versjonId = versjonId,
                oppdateringsdato = oppdateringsdato,
                stedsnavn = stedsnavn,
                kommunenummer = kommunenummer,
                kommunenavn = kommunenavn,
                fylkesnummer = fylkesnummer,
                fylkesnavn = fylkesnavn,
                matrikkelId = matrikkelId,
                adressekode = adressekode,
                navneobjekttype = navneobjekttype,
                coordinates = coordinates,
            )
        } else {
            null
        }
    }

    fun convertToNominatim(entry: StedsnavnEntry): NominatimPlace {
        val (lat, lon) =
            if (entry.coordinates.isNotEmpty()) {
                val centerEast = entry.coordinates.map { it.first }.average()
                val centerNorth = entry.coordinates.map { it.second }.average()
                Geo.convertUTM33ToLatLon(centerEast, centerNorth)
            } else {
                Pair(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)
            }

        val id = "KVE:TopographicPlace:${entry.kommunenummer}-${entry.stedsnavn}"

        val extra =
            Extra(
                id = id,
                source = "whosonfirst",
                accuracy = "point",
                country_a = "NOR",
                county_gid = "whosonfirst:county:KVE:TopographicPlace:${entry.fylkesnummer}",
                locality = entry.kommunenavn,
                locality_gid = "whosonfirst:locality:KVE:TopographicPlace:${entry.kommunenummer}",
                borough = null,
                borough_gid = null,
                label = "${entry.stedsnavn}, ${entry.kommunenavn}",
            )

        val properties =
            PlaceContent(
                place_id = abs(id.hashCode().toLong()),
                object_type = "N",
                object_id = abs(id.hashCode().toLong()),
                categories = listOf("street"),
                rank_address = 26,
                importance = 0.1,
                parent_place_id = 0,
                name = Name(entry.stedsnavn),
                housenumber = null,
                address =
                    Address(
                        street = entry.stedsnavn,
                        city = entry.kommunenavn.titleize(),
                        county = entry.fylkesnavn,
                    ),
                postcode = null,
                country_code = "no",
                centroid = listOf(lon, lat),
                bbox = listOf(lon, lat, lon, lat),
                extra = extra,
            )

        return NominatimPlace("Place", listOf(properties))
    }
}

