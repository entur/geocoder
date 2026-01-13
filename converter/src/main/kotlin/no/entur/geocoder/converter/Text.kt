package no.entur.geocoder.converter

object Text {
    fun Set<String>?.joinAltNamesToString(): String? {
        val names = this?.filter { it.isNotBlank() }?.joinToString(ALT_NAME_SEPARATOR)
        if (names.isNullOrBlank()) return null
        return names
    }

    const val ALT_NAME_SEPARATOR = ";" // alt_names (the OSM standard separator for multiple values within a single tag)
    const val REGULAR_SEPARATOR = "," // tags, tariff_zones
}
