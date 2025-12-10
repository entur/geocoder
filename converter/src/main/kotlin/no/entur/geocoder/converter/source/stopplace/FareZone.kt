package no.entur.geocoder.converter.source.stopplace

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "FareZone")
class FareZone(
    @JacksonXmlProperty(isAttribute = true) val id: String?,
    @JacksonXmlProperty(isAttribute = true) val version: String?,
    val authorityRef: AuthorityRef? = null,
) {
    class AuthorityRef {
        @JacksonXmlProperty(isAttribute = true)
        var ref: String? = null
    }
}
