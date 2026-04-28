package no.entur.geocoder.proxy.common

object Category {
    const val OSM_ADDRESS = "osm.public_transport.address" // Addresses with house numbers
    const val OSM_GOSP = "osm.public_transport.group_of_stop_places"

    const val LAYER_STOP_PLACE = "layer.stopPlace"

    const val GOSP = "GroupOfStopPlaces"

    const val COUNTRY_PREFIX = "country."

    const val TARIFF_ZONE_ID_PREFIX = "tariff_zone_id."
    const val TARIFF_ZONE_AUTH_PREFIX = "tariff_zone_authority."
    const val FARE_ZONE_PREFIX = "fare_zone_authority."

    const val COUNTY_ID_PREFIX = "county_gid."
    const val LOCALITY_ID_PREFIX = "locality_gid."

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

    fun tariffZoneIdCategory(ref: String) = TARIFF_ZONE_ID_PREFIX + ref.asCategory()

    fun fareZoneAuthorityCategory(ref: String) = FARE_ZONE_PREFIX + ref.asCategory()

    fun countyIdsCategory(ref: String) = COUNTY_ID_PREFIX + ref.asCategory()

    fun localityIdsCategory(ref: String) = LOCALITY_ID_PREFIX + ref.asCategory()

    fun String.asCategory() = this.replace(":", ".")
}
