package no.entur.netex_to_json

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText

@JacksonXmlRootElement(localName = "TopographicPlace")
data class TopographicPlace(
    @JacksonXmlProperty(isAttribute = true) val created: String? = null,
    @JacksonXmlProperty(isAttribute = true) val modification: String,
    @JacksonXmlProperty(isAttribute = true) val version: String,
    @JacksonXmlProperty(isAttribute = true) val id: String,

    val validBetween: ValidBetween? = null,

    @JacksonXmlProperty(localName = "Polygon", namespace = "ns2")
    val polygon: Polygon? = null,

    val isoCode: String? = null,

    val descriptor: Descriptor? = null,

    val topographicPlaceType: String? = null,

    val countryRef: CountryRef? = null,

    val parentTopographicPlaceRef: ParentTopographicPlaceRef? = null
) {
    class ValidBetween {
        var fromDate: String? = null
        var toDate: String? = null
    }

    data class Polygon(
        @JacksonXmlProperty(isAttribute = true, localName = "id", namespace = "ns2")
        val id: String,

        @JacksonXmlProperty(localName = "exterior", namespace = "ns2")
        val exterior: Exterior
    )

    data class Exterior(
        @JacksonXmlProperty(localName = "LinearRing", namespace = "ns2")
        val linearRing: LinearRing
    )

    data class LinearRing(
        @JacksonXmlProperty(localName = "posList", namespace = "ns2")
        val posList: String
    )

    class LocalizedText {
        @JacksonXmlProperty(isAttribute = true)
        var lang: String? = null

        @JacksonXmlText
        var text: String? = null
    }

    data class Descriptor(
        val name: LocalizedText
    )

    data class CountryRef(
        @JacksonXmlProperty(isAttribute = true)
        val ref: String
    )

    data class ParentTopographicPlaceRef(
        @JacksonXmlProperty(isAttribute = true)
        val ref: String,

        @JacksonXmlProperty(isAttribute = true)
        val version: String
    )
}