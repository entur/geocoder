package no.entur.geocoder.converter.stedsnavn

import no.entur.geocoder.common.Category.LEGACY_LAYER_ADDRESS
import no.entur.geocoder.common.Category.LEGACY_SOURCE_WHOSONFIRST
import no.entur.geocoder.common.Category.OSM_POI
import no.entur.geocoder.common.Category.SOURCE_STEDSNAVN
import no.entur.geocoder.common.Extra
import no.entur.geocoder.common.Geo
import no.entur.geocoder.common.Source
import no.entur.geocoder.common.Util.titleize
import no.entur.geocoder.converter.Converter
import no.entur.geocoder.converter.JsonWriter
import no.entur.geocoder.converter.PlaceId
import no.entur.geocoder.converter.Text.altName
import no.entur.geocoder.converter.importance.ImportanceCalculator
import no.entur.geocoder.converter.photon.NominatimPlace
import no.entur.geocoder.converter.photon.NominatimPlace.*
import java.io.File
import java.nio.file.Paths
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

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
        var skrivemåtestatus: String? = null
        val coordinates = mutableListOf<Pair<Double, Double>>()
        val annenSkrivemåte = mutableListOf<String>()
        var insideAnnenSkrivemåte = false

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
                    "komplettskrivemåte" -> {
                        val text = readElementText(reader)
                        if (insideAnnenSkrivemåte) {
                            annenSkrivemåte.add(text)
                        } else if (stedsnavn == null) {
                            stedsnavn = text
                        }
                    }

                    "navneobjekttype" -> navneobjekttype = readElementText(reader)
                    "skrivemåtestatus" -> {
                        val text = readElementText(reader)
                        if (!insideAnnenSkrivemåte && skrivemåtestatus == null) {
                            skrivemåtestatus = text
                        }
                    }

                    "matrikkelId" -> matrikkelId = readElementText(reader)
                    "adressekode" -> adressekode = readElementText(reader)
                    "kommunenummer" -> kommunenummer = readElementText(reader)
                    "kommunenavn" -> kommunenavn = readElementText(reader)
                    "fylkesnummer" -> fylkesnummer = readElementText(reader)
                    "fylkesnavn" -> fylkesnavn = readElementText(reader)
                    "annenSkrivemåte" -> insideAnnenSkrivemåte = true
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
            } else if (reader.eventType == XMLStreamConstants.END_ELEMENT) {
                if (reader.localName == "annenSkrivemåte") {
                    insideAnnenSkrivemåte = false
                }
            }
        }

        // Filter 1: Place type must be in target types (matching kakka's geocoderPlaceTypeWhitelist)
        val isTargetType = StedsnavnPlaceType.isTarget(navneobjekttype)

        // Filter 2: Spelling status must be accepted (matching kakka's spelling status validation)
        val hasAcceptedStatus = StedsnavnSpellingStatus.isAccepted(skrivemåtestatus)

        // Filter 3: All required fields must be present
        val hasRequiredFields =
            lokalId != null &&
                navnerom != null &&
                stedsnavn != null &&
                kommunenummer != null &&
                kommunenavn != null &&
                fylkesnummer != null &&
                fylkesnavn != null

        return if (isTargetType && hasAcceptedStatus && hasRequiredFields) {
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
                skrivemåtestatus = skrivemåtestatus,
                coordinates = coordinates,
                annenSkrivemåte = annenSkrivemåte,
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

        val tags = listOf(OSM_POI, LEGACY_SOURCE_WHOSONFIRST, LEGACY_LAYER_ADDRESS, "place.${entry.navneobjekttype}")

        val categories = tags.plus(SOURCE_STEDSNAVN)

        val id = entry.lokalId
        val extra =
            Extra(
                id = id,
                source = Source.KARTVERKET_STEDSNAVN,
                accuracy = "point",
                country_a = "NOR",
                county_gid = "KVE:TopographicPlace:${entry.fylkesnummer}",
                locality = entry.kommunenavn,
                locality_gid = "KVE:TopographicPlace:${entry.kommunenummer}",
                tags = tags.joinToString(","),
            )

        val placeId = PlaceId.stedsnavn.create(entry.lokalId)
        val properties =
            PlaceContent(
                place_id = placeId,
                object_type = "N",
                object_id = placeId,
                categories = categories,
                rank_address = 16,
                importance =
                    ImportanceCalculator.calculateImportance(
                        StedsnavnPopularityCalculator.calculatePopularity(entry.navneobjekttype),
                    ),
                parent_place_id = 0,
                name =
                    Name(
                        name = entry.stedsnavn,
                        alt_name = entry.annenSkrivemåte.plus(id).altName()
                    ),
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
