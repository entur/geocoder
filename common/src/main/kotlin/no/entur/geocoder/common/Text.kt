package no.entur.geocoder.common

object Text {
    fun Collection<String>?.joinOsmValuesToString(): String? {
        val names = this?.filter { it.isNotBlank() }?.joinToString(OSM_TAG_SEPARATOR)
        if (names.isNullOrBlank()) return null
        return names
    }

    // the OSM standard separator for multiple values within a single tag
    const val OSM_TAG_SEPARATOR = ";"
}