package no.entur.netex_to_json

import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLInputFactory.IS_COALESCING
import javax.xml.stream.XMLInputFactory.IS_NAMESPACE_AWARE
import javax.xml.stream.XMLStreamConstants.END_ELEMENT
import javax.xml.stream.XMLStreamConstants.START_ELEMENT
import javax.xml.stream.XMLStreamReader


class NetexParser {

    val xmlInputFactory: XMLInputFactory = XMLInputFactory.newInstance().apply {
        setProperty(IS_NAMESPACE_AWARE, false)
        setProperty(IS_COALESCING, true)
    }
    val xmlMapper: XmlMapper = XmlMapper.builder()
        .enable(ACCEPT_CASE_INSENSITIVE_PROPERTIES)
        .addModule(KotlinModule.Builder().build())
        .build()

    fun parseXmlFile(xmlFile: File): Sequence<StopPlace> =
        parseXml(xmlFile.inputStream())

    fun parseXml(inputStream: InputStream): Sequence<StopPlace> {
        val streamReader: XMLStreamReader = xmlInputFactory.createXMLStreamReader(inputStream)
        return sequence {
            try {
                moveToStartElement(streamReader, "stopPlaces")
                for (stopPlace in stopPlaceSequence(streamReader)) {
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

    private fun stopPlaceSequence(reader: XMLStreamReader): Sequence<StopPlace> = sequence {
        while (reader.hasNext()) {
            nextRelevantEvent(reader)
            when {
                isStartElement(reader, "StopPlace") ->
                    yield(xmlMapper.readValue(reader, StopPlace::class.java))

                isEndElement(reader, "stopPlaces") -> break
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
