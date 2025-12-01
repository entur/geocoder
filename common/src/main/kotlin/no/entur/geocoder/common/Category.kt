package no.entur.geocoder.common

object Category {
    const val OSM_ADDRESS = "osm.public_transport.address" // Addresses with house numbers
    const val OSM_STREET = "osm.public_transport.street" // Streets without house numbers
    const val OSM_STOP_PLACE = "osm.public_transport.stop_place"
    const val OSM_POI = "osm.public_transport.poi"
    const val OSM_CUSTOM_POI = "osm.public_transport.custom_poi"
    const val OSM_GOSP = "osm.public_transport.group_of_stop_places"
    const val SOURCE_ADRESSE = "source.kartverket.matrikkelenadresse"
    const val SOURCE_STEDSNAVN = "source.kartverket.stedsnavn"
    const val SOURCE_NSR = "source.nsr"

    const val COUNTRY_PREFIX = "country."

    const val TARIFF_ZONE_ID_PREFIX = "tariff_zone_id."
    const val TARIFF_ZONE_AUTH_PREFIX = "tariff_zone_authority."

    const val COUNTY_ID_PREFIX = "county_gid."
    const val LOCALITY_ID_PREFIX = "locality_gid."

    const val LEGACY_LAYER_PREFIX = "legacy.layer."
    const val LEGACY_LAYER_VENUE = LEGACY_LAYER_PREFIX + "venue"
    const val LEGACY_LAYER_ADDRESS = LEGACY_LAYER_PREFIX + "address"

    const val LEGACY_SOURCE_PREFIX = "legacy.source."
    const val LEGACY_SOURCE_WHOSONFIRST = LEGACY_SOURCE_PREFIX + "whosonfirst"
    const val LEGACY_SOURCE_OPENADDRESSES = LEGACY_SOURCE_PREFIX + "openaddresses"
    const val LEGACY_SOURCE_OPENSTREETMAP = LEGACY_SOURCE_PREFIX + "openstreetmap"

    /**
     * NSR, layer = venue:
     * - railStation
     * - onstreetBus
     * - busStation
     * - metroStation
     * - coachStation
     * - onstreetTram
     * - tramStation
     * - ferryStop
     * - ferryPort
     * - harbourPort
     * - vehicleRailInterchange
     * - airport
     * - liftStation
     * - other
     *
     * NSR, layer = address:
     * - GroupOfStopPlaces
     *
     * Kartverket, layer = address:
     * - street
     * - vegadresse
     *
     * OSM, layer = address:
     * - poi
     */
    const val LEGACY_CATEGORY_PREFIX = "legacy.category."

    fun tariffZoneIdCategory(ref: String) = TARIFF_ZONE_ID_PREFIX + ref.replace(":", ".")

    fun countyIdsCategory(ref: String) = COUNTY_ID_PREFIX + ref.replace(":", ".")

    fun localityIdsCategory(ref: String) = LOCALITY_ID_PREFIX + ref.replace(":", ".")
}
