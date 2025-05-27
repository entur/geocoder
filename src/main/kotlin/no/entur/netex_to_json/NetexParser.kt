package no.entur.netex_to_json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    fun parseXml(netexStream: InputStream): ParseResult =
        parseXml(streamToFile(netexStream))

    fun parseXml(netexXml: File): ParseResult {
        val categories = extractCategories(netexXml)

        val netexReader: XMLStreamReader = createReader(netexXml)

        moveToStartElement(netexReader, "topographicPlaces")
        val topoPlaces = mutableMapOf<String, TopographicPlace>()
        for (topoPlace in elementSequence(
            netexReader,
            "TopographicPlace",
            "topographicPlaces",
            TopographicPlace::class.java
        )) {
            topoPlaces.put(topoPlace.id, topoPlace)
        }
        val seq = sequence {
            moveToStartElement(netexReader, "stopPlaces")
            for (stopPlace in elementSequence(netexReader, "StopPlace", "stopPlaces", StopPlace::class.java)) {
                yield(stopPlace)
            }
            netexReader.close()
        }

        return ParseResult(
            stopPlaces = seq,
            topoPlaces = topoPlaces,
            categories = categories
        )
    }

    private fun extractCategories(netexXml: File): MutableMap<String, List<String>> {
        val netexStream: XMLStreamReader = createReader(netexXml)

        moveToStartElement(netexStream, "stopPlaces")
        val categories = mutableMapOf<String, List<String>>()
        for (stopPlace in elementSequence(netexStream, "StopPlace", "stopPlaces", StopPlace::class.java)) {
            if (stopPlace.parentSiteRef?.ref != null && stopPlace.stopPlaceType != null) {
                val key = stopPlace.parentSiteRef.ref
                val newValue = stopPlace.stopPlaceType
                categories.compute(key) { _, existingValue ->
                    existingValue?.plus(newValue) ?: listOf(newValue)
                }
            }
        }
        netexStream.close()
        return categories
    }

    private fun createReader(netexXml: File): XMLStreamReader {
        val secondPass: InputStream = FileInputStream(netexXml)
        val streamReader: XMLStreamReader = xmlInputFactory.createXMLStreamReader(secondPass)
        return streamReader
    }

    private fun streamToFile(inputStream: InputStream): File {
        val tempFile = File.createTempFile("stream", ".tmp")
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile
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

    data class ParseResult(
        val stopPlaces: Sequence<StopPlace>,
        val topoPlaces: Map<String, TopographicPlace>,
        val categories: Map<String, List<String>>
    )
}
