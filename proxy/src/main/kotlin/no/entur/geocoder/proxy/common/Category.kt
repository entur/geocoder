package no.entur.geocoder.proxy.common

// String constants must stay in sync with nominatim-converter/src/common/category.rs.
object Category {
    const val LAYER_PREFIX = "layer."
    const val LAYER_ADDRESS = "layer.address" // Addresses with house numbers
    const val LAYER_STOP_PLACE = "layer.stopPlace"
    const val LAYER_GOSP = "layer.groupOfStopPlaces"

    const val GOSP = "GroupOfStopPlaces"

    const val COUNTRY_PREFIX = "country."

    const val TARIFF_ZONE_ID_PREFIX = "tariff_zone_id."
    const val TARIFF_ZONE_AUTH_PREFIX = "tariff_zone_authority."
    const val FARE_ZONE_ID_PREFIX = "fare_zone_id."
    const val FARE_ZONE_AUTH_PREFIX = "fare_zone_authority."

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

    fun fareZoneIdCategory(ref: String) = FARE_ZONE_ID_PREFIX + ref.asCategory()

    fun fareZoneAuthorityCategory(ref: String) = FARE_ZONE_AUTH_PREFIX + ref.asCategory()

    fun countyIdsCategory(ref: String) = COUNTY_ID_PREFIX + ref.asCategory()

    fun localityIdsCategory(ref: String) = LOCALITY_ID_PREFIX + ref.asCategory()

    /**
     * Transliteration table loaded from `transliteration.csv` - the single source
     * for char -> replacement mappings, duplicated byte-identically in the
     * nominatim-converter repo (src/common/transliteration.csv). See the CSV
     * header for the sync and reindex constraints.
     */
    private val transliterations: Map<Int, String> by lazy {
        checkNotNull(Category::class.java.getResourceAsStream("/transliteration.csv")) {
            "transliteration.csv missing from classpath"
        }.bufferedReader().readLines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line ->
                val (from, to) = line.split(';', limit = 2)
                require(from.codePointCount(0, from.length) == 1) {
                    "transliteration.csv: left side must be one char: $line"
                }
                from.codePointAt(0) to to
            }
    }

    /**
     * Convert a colon-separated ID to a Photon-safe category string.
     *
     * Photon's `CATEGORY_PATTERN` allows only `[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+`:
     * colons become dots (namespace separators), characters in the allowed set pass
     * through, table characters are transliterated (å -> aa, ø -> oe, etc.), and
     * anything else becomes `_`. Iterates code points (not UTF-16 chars) so
     * astral-plane characters map to a single `_`, matching the Rust side.
     *
     * The nominatim-converter applies the same transform
     * (`src/common/category.rs::as_category`) from its copy of the same table when
     * writing index categories - the two must produce byte-identical output.
     */
    fun String.asCategory(): String {
        val out = StringBuilder(this.length)
        var i = 0
        while (i < this.length) {
            val cp = this.codePointAt(i)
            i += Character.charCount(cp)
            when {
                cp == ':'.code -> out.append('.')
                cp in 'a'.code..'z'.code || cp in 'A'.code..'Z'.code ||
                    cp in '0'.code..'9'.code || cp == '_'.code || cp == '-'.code ->
                    out.appendCodePoint(cp)
                else -> out.append(transliterations[cp] ?: "_")
            }
        }
        return out.toString()
    }
}
