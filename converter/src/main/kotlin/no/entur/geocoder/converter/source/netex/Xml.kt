package no.entur.geocoder.converter.source.netex

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

object Xml {
    fun xmlInputFactory(): XMLInputFactory =
        XMLInputFactory.newInstance().apply {
            setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false)
            setProperty(XMLInputFactory.IS_COALESCING, true)
        }

    fun xmlMapper(): XmlMapper =
        XmlMapper
            .builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .addModule(KotlinModule.Builder().build())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    fun createReader(
        file: File,
        inputFactory: XMLInputFactory,
    ): XMLStreamReader {
        val secondPass: InputStream = FileInputStream(file)
        return inputFactory.createXMLStreamReader(secondPass)
    }

    fun isStartElement(
        reader: XMLStreamReader,
        name: String,
    ) = reader.eventType == XMLStreamConstants.START_ELEMENT && reader.localName.equals(name, ignoreCase = true)

    fun isEndElement(
        reader: XMLStreamReader,
        name: String,
    ) = reader.eventType == XMLStreamConstants.END_ELEMENT && reader.localName.equals(name, ignoreCase = true)

    fun moveToStartElement(
        reader: XMLStreamReader,
        elementName: String,
    ) {
        while (reader.hasNext()) {
            reader.next()
            if (isStartElement(reader, elementName)) {
                return
            }
        }
        throw IllegalStateException("Element <$elementName> not found in XML stream.")
    }

    inline fun <reified C> elementSequence(
        reader: XMLStreamReader,
        mapper: XmlMapper,
        startElement: String,
        endElement: String,
    ): Sequence<C> =
        sequence {
            while (reader.hasNext()) {
                nextRelevantEvent(reader)
                when {
                    isStartElement(reader, startElement) ->
                        yield(mapper.readValue(reader, C::class.java))

                    isEndElement(reader, endElement) -> break
                }
            }
        }

    fun nextRelevantEvent(reader: XMLStreamReader) {
        var event: Int
        do {
            event = reader.next()
        } while (reader.hasNext() && event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT)
    }
}
