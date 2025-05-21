package no.entur.netex_to_json

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "StopPlace")
data class StopPlace(
    @JacksonXmlProperty(isAttribute = true) val id: String?,
    @JacksonXmlProperty(isAttribute = true) val version: String?,
    @JacksonXmlProperty(isAttribute = true) val modification: String?,
    @JacksonXmlProperty(isAttribute = true) val created: String?,
    @JacksonXmlProperty(isAttribute = true) val changed: String?,

    val ValidBetween: ValidBetween? = null,
    val keyList: KeyList? = null,
    val Name: LocalizedText? = null,
    val Description: LocalizedText? = null,
    val Centroid: Centroid? = null,
    val AccessibilityAssessment: AccessibilityAssessment? = null,
    val alternativeNames: AlternativeNames? = null,
    val TopographicPlaceRef: TopographicPlaceRef? = null,
    val TransportMode: String? = null,
    val StopPlaceType: String? = null,
    val Weighting: String? = null,
    val tariffZones: TariffZones? = null,
    val quays: QuayList? = null
)

class ValidBetween {
    @JacksonXmlProperty(localName = "FromDate")
    var fromDate: String? = null

    @JacksonXmlProperty(localName = "ToDate")
    var toDate: String? = null
}

class KeyList {
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "KeyValue")
    var keyValue: List<KeyValue>? = null
}

data class KeyValue(val Key: String?, val Value: String?)

class LocalizedText {
    @JacksonXmlProperty(isAttribute = true)
    var lang: String? = null

    @JacksonXmlText
    var text: String? = null
}

class Centroid {
    @JacksonXmlProperty(localName = "Location")
    var location: Location? = null
}

class Location {
    @JacksonXmlProperty(localName = "Longitude")
    var longitude: Double? = null

    @JacksonXmlProperty(localName = "Latitude")
    var latitude: Double? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AccessibilityAssessment {
    @JacksonXmlProperty(isAttribute = true)
    var version: String? = null

    @JacksonXmlProperty(isAttribute = true)
    var modification: String? = null

    var mobilityImpairedAccess: String? = null
    var limitations: Limitations? = null
}

class Limitations {
    @JacksonXmlProperty(localName = "AccessibilityLimitation")
    @JacksonXmlElementWrapper(useWrapping = false)
    var accessibilityLimitations: List<AccessibilityLimitation>? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AccessibilityLimitation {
    @JacksonXmlProperty(isAttribute = true)
    var modification: String? = null

    var audibleSignalsAvailable: Boolean? = null
    var liftFreeAccess: Boolean? = null
    var stepFreeAccess: Boolean? = null
    var wheelchairAccess: Boolean? = null
    var escalatorFreeAccess: Boolean? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlternativeNames {
    @JacksonXmlProperty(localName = "AlternativeName")
    @JacksonXmlElementWrapper(useWrapping = false)
    var alternativeName: List<AlternativeName>? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AlternativeName {
    @JacksonXmlProperty(isAttribute = true)
    var id: String? = null

    @JacksonXmlProperty(isAttribute = true)
    var version: String? = null

    @JacksonXmlProperty(isAttribute = true)
    var modification: String? = null

    var NameType: String? = null
    var Name: LocalizedText? = null
}

data class TopographicPlaceRef(
    @JacksonXmlProperty(isAttribute = true) val ref: String?,
    @JacksonXmlProperty(isAttribute = true) val version: String?,
    @JacksonXmlProperty(isAttribute = true) val created: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
class TariffZones {
    @JacksonXmlProperty(localName = "TariffZoneRef")
    @JacksonXmlElementWrapper(useWrapping = false)
    var tariffZoneRef: List<TariffZoneRef>? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class TariffZoneRef {
    @JacksonXmlProperty(isAttribute = true)
    var ref: String? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
class QuayList {
    @JacksonXmlProperty(localName = "Quay")
    @JacksonXmlElementWrapper(useWrapping = false)
    var quay: List<Quay>? = null
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Quay(
    @JacksonXmlProperty(isAttribute = true) val id: String? = null,
    @JacksonXmlProperty(isAttribute = true) val version: String? = null,
    @JacksonXmlProperty(isAttribute = true) val modification: String? = null,
    @JacksonXmlProperty(isAttribute = true) val changed: String? = null,
    val PrivateCode: String? = null,
    val PublicCode: String? = null,
    val OtherTransportModes: String? = null,
    val keyList: KeyList? = null,
    val Centroid: Centroid? = null,
    val CompassBearing: Double? = null
)