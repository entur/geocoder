package no.entur.netex_to_json

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText

@JacksonXmlRootElement(localName = "StopPlace")
class StopPlace(
    @JacksonXmlProperty(isAttribute = true) val id: String,
    @JacksonXmlProperty(isAttribute = true) val version: String?,
    @JacksonXmlProperty(isAttribute = true) val modification: String?,
    @JacksonXmlProperty(isAttribute = true) val created: String?,
    @JacksonXmlProperty(isAttribute = true) val changed: String?,

    val validBetween: ValidBetween? = null,
    val keyList: KeyList? = null,
    val name: LocalizedText,
    val description: LocalizedText? = null,
    val centroid: Centroid? = null,
    val accessibilityAssessment: AccessibilityAssessment? = null,
    val alternativeNames: AlternativeNames? = null,
    val topographicPlaceRef: PlaceRef? = null,
    val parentSiteRef: PlaceRef? = null,
    val transportMode: String? = null,
    val stopPlaceType: String? = null,
    val weighting: String? = null,
    val tariffZones: TariffZones? = null,
    val quays: QuayList? = null
) {
    class ValidBetween {
        var fromDate: String? = null
        var toDate: String? = null
    }

    class KeyList {
        @JacksonXmlElementWrapper(useWrapping = false)
        var keyValue: List<KeyValue>? = null
    }

    data class KeyValue(
        val key: String?,
        val value: String?
    )

    class LocalizedText {
        @JacksonXmlProperty(isAttribute = true)
        var lang: String? = null

        @JacksonXmlText
        var text: String? = null
    }

    class Centroid {
        var location: Location? = null
    }

    class Location {
        var longitude: Double? = null
        var latitude: Double? = null
    }

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

    class AccessibilityLimitation {
        @JacksonXmlProperty(isAttribute = true)
        var modification: String? = null

        var audibleSignalsAvailable: String? = null
        var liftFreeAccess: String? = null
        var stepFreeAccess: String? = null
        var wheelchairAccess: String? = null
        var escalatorFreeAccess: String? = null
    }

    class AlternativeNames {
        @JacksonXmlProperty(localName = "AlternativeName")
        @JacksonXmlElementWrapper(useWrapping = false)
        var alternativeName: List<AlternativeName>? = null
    }

    class AlternativeName {
        @JacksonXmlProperty(isAttribute = true)
        var id: String? = null

        @JacksonXmlProperty(isAttribute = true)
        var version: String? = null

        @JacksonXmlProperty(isAttribute = true)
        var modification: String? = null

        var nameType: String? = null
        var name: LocalizedText? = null
    }

    data class PlaceRef(
        @JacksonXmlProperty(isAttribute = true) val ref: String?,
        @JacksonXmlProperty(isAttribute = true) val version: String?,
        @JacksonXmlProperty(isAttribute = true) val created: String?
    )

    class TariffZones {
        @JacksonXmlProperty(localName = "TariffZoneRef")
        @JacksonXmlElementWrapper(useWrapping = false)
        var tariffZoneRef: List<TariffZoneRef>? = null
    }

    class TariffZoneRef {
        @JacksonXmlProperty(isAttribute = true)
        var ref: String? = null
    }

    class QuayList {
        @JacksonXmlProperty(localName = "Quay")
        @JacksonXmlElementWrapper(useWrapping = false)
        var quay: List<Quay>? = null
    }

    data class Quay(
        @JacksonXmlProperty(isAttribute = true) val id: String? = null,
        @JacksonXmlProperty(isAttribute = true) val version: String? = null,
        @JacksonXmlProperty(isAttribute = true) val modification: String? = null,
        @JacksonXmlProperty(isAttribute = true) val changed: String? = null,
        val privateCode: String? = null,
        val publicCode: String? = null,
        val otherTransportModes: String? = null,
        val keyList: KeyList? = null,
        val centroid: Centroid? = null,
        val compassBearing: Double? = null
    )
}