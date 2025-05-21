package no.entur.netex_to_json

import com.fasterxml.jackson.dataformat.xml.XmlFactory
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader


fun main(args: Array<String>) {
    val xmlFile = File(args[0])
    App().parseXmlFile(xmlFile)
}

class App {

    fun parseXmlFile(xmlFile: File) {
        parseXml(xmlFile.inputStream())
    }

    fun parseXml(inputStream: InputStream) {
        val xmlInputFactory = XMLInputFactory.newInstance()
        val streamReader: XMLStreamReader = xmlInputFactory.createXMLStreamReader(inputStream)
        val xmlMapper = XmlMapper(XmlFactory()).apply {
            registerKotlinModule()
        }

        while (streamReader.hasNext()) {
            val event = streamReader.next()
            if (event == XMLStreamConstants.START_ELEMENT && streamReader.localName == "stopPlaces") {
                break
            }
        }

        while (streamReader.hasNext()) {
            val event = streamReader.next()
            if (event == XMLStreamConstants.START_ELEMENT && streamReader.localName == "StopPlace") {
                val stopPlace: StopPlace = xmlMapper.readValue(streamReader, StopPlace::class.java)
                println("Parsed StopPlace: ${stopPlace.id}, Name: ${stopPlace.Name?.text}")
            } else if (event == XMLStreamConstants.END_ELEMENT && streamReader.localName == "stopPlaces") {
                break
            }
        }

        streamReader.close()
    }
}