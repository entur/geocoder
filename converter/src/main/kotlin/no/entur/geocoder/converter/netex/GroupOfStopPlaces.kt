package no.entur.geocoder.converter.netex

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import java.math.BigDecimal

@JacksonXmlRootElement(localName = "GroupOfStopPlaces")
class GroupOfStopPlaces(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val version: String?,
    @JacksonXmlProperty(isAttribute = true) val modification: String?,
    @JacksonXmlProperty(isAttribute = true) val created: String?,
    @JacksonXmlProperty(isAttribute = true) val changed: String?,
    val keyList: KeyList? = null,
    val name: LocalizedText,
    val description: LocalizedText? = null,
    val purposeOfGroupingRef: PurposeOfGroupingRef? = null,
    val members: Members? = null,
    val centroid: Centroid,
) {
    class KeyList {
        @JacksonXmlElementWrapper(useWrapping = false)
        var keyValue: List<KeyValue>? = null
    }

    data class KeyValue(
        val key: String?,
        val value: String?,
    )

    class LocalizedText {
        @JacksonXmlProperty(isAttribute = true)
        var lang: String? = null

        @JacksonXmlText
        var text: String? = null
    }

    data class PurposeOfGroupingRef(
        @JacksonXmlProperty(isAttribute = true) val ref: String?,
    )

    class Members {
        @JacksonXmlProperty(localName = "StopPlaceRef")
        @JacksonXmlElementWrapper(useWrapping = false)
        var stopPlaceRef: List<StopPlaceRef>? = null
    }

    data class StopPlaceRef(
        @JacksonXmlProperty(isAttribute = true) val ref: String?,
    )

    data class Centroid(
        val location: Location,
    )

    data class Location(
        val longitude: BigDecimal,
        val latitude: BigDecimal,
    )
}

