package no.entur.geocoder.converter.netex

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import no.entur.geocoder.converter.FileUtil.streamToFile
import no.entur.geocoder.converter.netex.Xml
import no.entur.geocoder.converter.netex.Xml.createReader
import no.entur.geocoder.converter.netex.Xml.elementSequence
import no.entur.geocoder.converter.netex.Xml.moveToStartElement
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

class NetexParser {
    val xmlInputFactory: XMLInputFactory = Xml.xmlInputFactory()
    val xmlMapper: XmlMapper = Xml.xmlMapper()

    fun parseXml(netexStream: InputStream): ParseResult = parseXml(streamToFile(netexStream))

    fun parseXml(netexXml: File): ParseResult {
        val categories = extractCategories(netexXml)
        val topoPlaces = extractTopoPlaces(netexXml)
        val stopPlaces = stopPlacesSequence(netexXml)

        return ParseResult(
            stopPlaces = stopPlaces,
            topoPlaces = topoPlaces,
            categories = categories,
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

    private fun extractTopoPlaces(netexXml: File): MutableMap<String, TopographicPlace> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        moveToStartElement(netexReader, "topographicPlaces")
        val topoPlaces = mutableMapOf<String, TopographicPlace>()
        for (topoPlace in elementSequence<TopographicPlace>(
            netexReader,
            xmlMapper,
            "TopographicPlace",
            "topographicPlaces",
        )) {
            topoPlaces[topoPlace.id] = topoPlace
        }
        return topoPlaces
    }

    private fun extractCategories(netexXml: File): MutableMap<String, List<String>> {
        val netexReader: XMLStreamReader = createReader(netexXml, xmlInputFactory)

        moveToStartElement(netexReader, "stopPlaces")
        val categories = mutableMapOf<String, List<String>>()
        for (stopPlace in elementSequence<StopPlace>(netexReader, xmlMapper, "StopPlace", "stopPlaces")) {
            if (stopPlace.parentSiteRef?.ref != null && stopPlace.stopPlaceType != null) {
                val key = stopPlace.parentSiteRef.ref
                val newValue = stopPlace.stopPlaceType
                categories.compute(key) { _, existingValue ->
                    existingValue?.plus(newValue) ?: listOf(newValue)
                }
            }
        }
        netexReader.close()
        return categories
    }

    data class ParseResult(
        val stopPlaces: Sequence<StopPlace>,
        val topoPlaces: Map<String, TopographicPlace>,
        val categories: Map<String, List<String>>,
    )
}
