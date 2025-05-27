package no.entur.netex_to_json

import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.databind.DeserializationFeature
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLInputFactory.IS_COALESCING
import javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE
import javax.xml.stream.XMLStreamConstants.END_ELEMENT
import javax.xml.stream.XMLStreamConstants.START_ELEMENT
import javax.xml.stream.XMLStreamReader


class NetexParser {
    val topoPlaces = mutableMapOf<String, TopographicPlace>()

    val xmlInputFactory: XMLInputFactory = XMLInputFactory.newInstance().apply {
        setProperty(IS_NAMESPACE_AWARE, false)
        setProperty(IS_COALESCING, true)
    }
    val xmlMapper: XmlMapper = XmlMapper.builder()
        .enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        .addModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    fun parseXmlFile(xmlFile: File): Sequence<StopPlace> =
        parseXml(xmlFile.inputStream())

    fun parseXml(inputStream: InputStream): Sequence<StopPlace> {
        val streamReader: XMLStreamReader = xmlInputFactory.createXMLStreamReader(inputStream)

        moveToStartElement(streamReader, "topographicPlaces")
        for (topoPlace in elementSequence(streamReader, "TopographicPlace", "topographicPlaces", TopographicPlace::class.java)) {
            topoPlaces.put(topoPlace.id, topoPlace)
        }
        return sequence {
            try {
                moveToStartElement(streamReader, "stopPlaces")
                for (stopPlace in elementSequence(streamReader, "StopPlace", "stopPlaces", StopPlace::class.java)) {
                    yield(stopPlace)
                }
            } finally {
                streamReader.close()
            }
        }
    }

    private fun isStartElement(reader: XMLStreamReader, name: String) =
        reader.eventType == START_ELEMENT && reader.localName.equals(name, ignoreCase = true)

    private fun isEndElement(reader: XMLStreamReader, name: String) =
        reader.eventType == END_ELEMENT && reader.localName.equals(name, ignoreCase = true)

    private fun moveToStartElement(reader: XMLStreamReader, elementName: String) {
        while (reader.hasNext()) {
            reader.next()
            if (isStartElement(reader, elementName)) {
                return
            }
        }
        throw IllegalStateException("Element <$elementName> not found in XML stream.")
    }

    private inline fun <reified C> elementSequence(
        reader: XMLStreamReader,
        startElement: String,
        endElement: String,
        valueType: Class<C>
    ): Sequence<C> = sequence {
        while (reader.hasNext()) {
            nextRelevantEvent(reader)
            when {
                isStartElement(reader, startElement) ->
                    yield(xmlMapper.readValue(reader, valueType))

                isEndElement(reader, endElement) -> break
            }
        }
    }

    private fun nextRelevantEvent(reader: XMLStreamReader) {
        var event: Int
        do {
            event = reader.next()
        } while (reader.hasNext() && event != START_ELEMENT && event != END_ELEMENT)
    }
}
