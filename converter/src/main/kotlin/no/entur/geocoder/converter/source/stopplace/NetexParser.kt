package no.entur.geocoder.converter.source.stopplace

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import no.entur.geocoder.converter.FileUtil.streamToFile
import no.entur.geocoder.converter.source.stopplace.Xml.createReader
import no.entur.geocoder.converter.source.stopplace.Xml.elementSequence
import no.entur.geocoder.converter.source.stopplace.Xml.moveToStartElement
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

class NetexParser {
    val xmlInputFactory: XMLInputFactory = Xml.xmlInputFactory()
    val xmlMapper: XmlMapper = Xml.xmlMapper()

    fun parseXml(netexStream: InputStream): ParseResult = parseXml(streamToFile(netexStream))

    fun parseXml(netexXml: File): ParseResult {
        val stopPlaceTypes = extractStopPlaceTypes(netexXml)
        val topoPlaces = extractTopoPlaces(netexXml)
        val fareZones = extractFareZones(netexXml)
        val stopPlaces = stopPlacesSequence(netexXml)
        val groupOfStopPlaces = groupOfStopPlacesSequence(netexXml)

        return ParseResult(
            stopPlaces = stopPlaces,
            groupOfStopPlaces = groupOfStopPlaces,
            topoPlaces = topoPlaces,
            stopPlaceTypes = stopPlaceTypes,
            fareZones = fareZones,
        )
    }

    private fun stopPlacesSequence(netexXml: File): Sequence<StopPlace> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        val seq =
            sequence {
                moveToStartElement(netexReader, "stopPlaces")
                for (stopPlace in elementSequence<StopPlace>(
                    netexReader,
                    xmlMapper,
                    "StopPlace",
                    "stopPlaces",
                )) {
                    yield(stopPlace)
                }
                netexReader.close()
            }
        return seq
    }

    private fun groupOfStopPlacesSequence(netexXml: File): Sequence<GroupOfStopPlaces> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        val seq =
            sequence {
                try {
                    moveToStartElement(netexReader, "groupsOfStopPlaces")
                    for (groupOfStopPlaces in elementSequence<GroupOfStopPlaces>(
                        netexReader,
                        xmlMapper,
                        "GroupOfStopPlaces",
                        "groupsOfStopPlaces",
                    )) {
                        yield(groupOfStopPlaces)
                    }
                } catch (_: IllegalStateException) {
                    // Element not found, return empty sequence
                } finally {
                    netexReader.close()
                }
            }
        return seq
    }

    internal fun extractTopoPlaces(netexXml: File): MutableMap<String, TopographicPlace> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        moveToStartElement(netexReader, "topographicPlaces")
        val topoPlaces = mutableMapOf<String, TopographicPlace>()
        for (topoPlace in elementSequence<TopographicPlace>(
            netexReader,
            xmlMapper,
            "TopographicPlace",
            "topographicPlaces",
        )) {
            topoPlace.id?.let { topoPlaces[it] = topoPlace }
        }
        return topoPlaces
    }

    private fun extractStopPlaceTypes(netexXml: File): MutableMap<String, List<String>> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        moveToStartElement(netexReader, "stopPlaces")
        val types = mutableMapOf<String, List<String>>()
        for (stopPlace in elementSequence<StopPlace>(netexReader, xmlMapper, "StopPlace", "stopPlaces")) {
            if (stopPlace.parentSiteRef?.ref != null && stopPlace.stopPlaceType != null) {
                val key = stopPlace.parentSiteRef.ref
                val newValue = stopPlace.stopPlaceType
                types.compute(key) { _, existingValue ->
                    existingValue?.plus(newValue) ?: listOf(newValue)
                }
            }
        }
        netexReader.close()
        return types
    }

    internal fun extractFareZones(netexXml: File): Map<String, FareZone> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        val fareZones = mutableMapOf<String, FareZone>()
        try {
            moveToStartElement(netexReader, "fareZones")
            for (fareZone in elementSequence<FareZone>(
                netexReader,
                xmlMapper,
                "FareZone",
                "fareZones",
            )) {
                fareZone.id?.let { fareZones[it] = fareZone }
            }
        } catch (_: IllegalStateException) {
            // Element not found, return empty map
        } finally {
            netexReader.close()
        }
        return fareZones
    }

    data class ParseResult(
        val stopPlaces: Sequence<StopPlace>,
        val groupOfStopPlaces: Sequence<GroupOfStopPlaces>,
        val topoPlaces: Map<String, TopographicPlace>,
        val stopPlaceTypes: Map<String, List<String>>,
        val fareZones: Map<String, FareZone>,
    )
}
