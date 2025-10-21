package no.entur.geocoder.converter.stedsnavn

import no.entur.geocoder.common.Category
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Source
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.NominatimPlace
import no.entur.geocoder.converter.NominatimPlace.*
import no.entur.geocoder.converter.Util.titleize
import no.entur.geocoder.converter.matrikkel.Geo
import no.entur.geocoder.converter.importance.ImportanceCalculator
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

        while (reader.hasNext()) {
            reader.next()

            if (reader.eventType == XMLStreamConstants.END_ELEMENT &&
                reader.localName == "featureMember"
            ) {
                break
            }

            if (reader.eventType == XMLStreamConstants.START_ELEMENT) {
                when (reader.localName) {
                    "lokalId" -> lokalId = readElementText(reader)
                    "navnerom" -> navnerom = readElementText(reader)
                    "versjonId" -> versjonId = readElementText(reader)
                    "oppdateringsdato" -> oppdateringsdato = readElementText(reader)
                    "komplettskrivemåte" -> if (stedsnavn == null) stedsnavn = readElementText(reader)
                    "navneobjekttype" -> navneobjekttype = readElementText(reader)
                    "matrikkelId" -> matrikkelId = readElementText(reader)
                    "adressekode" -> adressekode = readElementText(reader)
                    "kommunenummer" -> kommunenummer = readElementText(reader)
                    "kommunenavn" -> kommunenavn = readElementText(reader)
                    "fylkesnummer" -> fylkesnummer = readElementText(reader)
                    "fylkesnavn" -> fylkesnavn = readElementText(reader)
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

                    "pos" -> {
                        val text = readElementText(reader).trim()
                        val coords = text.split("\\s+".toRegex())
                        if (coords.size >= 2) {
                            val east = coords[0].toDoubleOrNull()
                            val north = coords[1].toDoubleOrNull()
                            if (east != null && north != null) {
                                coordinates.add(Pair(east, north))
                            }
                        }
                    }
                }
            }
        }

        val targetTypes = setOf("tettsteddel", "bydel", "by", "tettsted", "tettbebyggelse")

        return if (navneobjekttype != null &&
            targetTypes.contains(navneobjekttype.lowercase()) &&
            lokalId != null &&
            navnerom != null &&
            stedsnavn != null &&
            kommunenummer != null &&
            kommunenavn != null &&
            fylkesnummer != null &&
            fylkesnavn != null
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

        val categories = listOf(Category.OSM_POI)
            .plus("place.${entry.navneobjekttype}")
            .plus(Category.SOURCE_KARTVERKET_STEDSNAVN)

        val extra =
            Extra(
                id = entry.lokalId,
                source = Source.KARTVERKET_STEDSNAVN,
                accuracy = "point",
                country_a = "NOR",
                county_gid = "KVE:TopographicPlace:${entry.fylkesnummer}",
                locality = entry.kommunenavn,
                locality_gid = "KVE:TopographicPlace:${entry.kommunenummer}",
                label = "${entry.stedsnavn}, ${entry.kommunenavn}",
                tags = categories.joinToString(",") { it },
            )

        val properties =
            PlaceContent(
                place_id = abs(entry.lokalId.hashCode().toLong()),
                object_type = "N",
                object_id = abs(entry.lokalId.hashCode().toLong()),
                categories = categories,
                rank_address = 16,
                importance = ImportanceCalculator.calculateImportance(
                    StedsnavnPopularityCalculator.calculatePopularity()
                ),
                parent_place_id = 0,
                name = Name(entry.stedsnavn),
                housenumber = null,
                address =
                    Address(
                        street = null,
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

