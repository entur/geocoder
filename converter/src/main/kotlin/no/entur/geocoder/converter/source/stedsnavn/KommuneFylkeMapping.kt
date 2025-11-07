package no.entur.geocoder.converter.source.stedsnavn

import java.io.File
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader

/**
 * Utility to build a mapping from kommune to fylke information by parsing Stedsnavn GML files.
 */
object KommuneFylkeMapping {
    data class KommuneInfo(
        val kommunenummer: String,
        val kommunenavn: String,
        val fylkesnummer: String,
        val fylkesnavn: String,
    )

    private var mapping: Map<String, KommuneInfo> = mapOf()

    fun build(stedsnavnGmlFile: File?): Map<String, KommuneInfo> {
        if (mapping.isEmpty() && stedsnavnGmlFile != null && stedsnavnGmlFile.exists()) {
            println("Building kommune-fylke mapping from ${stedsnavnGmlFile.absolutePath}...")
            mapping =
                buildMapping(stedsnavnGmlFile).also {
                    println("Loaded ${it.size} kommune-fylke mappings")
                }
        } else if (mapping.isNotEmpty()) {
            println("Using existing kommune-fylke mapping with ${mapping.size} entries")
        } else {
            println("No Stedsnavn GML file provided or file does not exist; kommune-fylke mapping will be empty")
        }
        return mapping
    }

    /**
     * Parse a Stedsnavn GML file and extract kommune to fylke mappings.
     * Returns a map where the key is kommunenummer and the value is KommuneInfo.
     */
    private fun buildMapping(stedsnavnGmlFile: File): Map<String, KommuneInfo> {
        val mapping = mutableMapOf<String, KommuneInfo>()

        val factory = XMLInputFactory.newInstance()
        val reader = factory.createXMLStreamReader(stedsnavnGmlFile.inputStream())

        try {
            while (reader.hasNext()) {
                reader.next()
                if (reader.eventType == XMLStreamConstants.START_ELEMENT &&
                    reader.localName == "Kommune"
                ) {
                    val kommuneInfo = parseKommune(reader)
                    if (kommuneInfo != null) {
                        mapping[kommuneInfo.kommunenummer] = kommuneInfo
                    }
                }
            }
        } finally {
            reader.close()
        }

        return mapping
    }

    private fun parseKommune(reader: XMLStreamReader): KommuneInfo? {
        var kommunenummer: String? = null
        var kommunenavn: String? = null
        var fylkesnummer: String? = null
        var fylkesnavn: String? = null

        while (reader.hasNext()) {
            reader.next()

            if (reader.eventType == XMLStreamConstants.END_ELEMENT &&
                reader.localName == "Kommune"
            ) {
                break
            }

            if (reader.eventType == XMLStreamConstants.START_ELEMENT) {
                when (reader.localName) {
                    "kommunenummer" -> kommunenummer = readElementText(reader)
                    "kommunenavn" -> kommunenavn = readElementText(reader)
                    "fylkesnummer" -> fylkesnummer = readElementText(reader)
                    "fylkesnavn" -> fylkesnavn = readElementText(reader)
                }
            }
        }

        return if (kommunenummer != null &&
            kommunenavn != null &&
            fylkesnummer != null &&
            fylkesnavn != null
        ) {
            KommuneInfo(
                kommunenummer,
                normalizeName(kommunenavn), fylkesnummer,
                normalizeName(fylkesnavn),
            )
        } else {
            null
        }
    }

    private fun normalizeName(name: String): String =
        name.split(" - ").first()

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
}
