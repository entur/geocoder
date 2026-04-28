package no.entur.geocoder.proxy.common

object Text {
    fun Collection<String>?.joinOsmValuesToString(): String? {
        val names = this?.filter { it.isNotBlank() }?.joinToString(OSM_TAG_SEPARATOR)
        if (names.isNullOrBlank()) return null
        return names
    }

    fun String?.safeVar(): String? = this?.replace("[^\\p{L}\\p{Nd}_.,'\\-\\s:]+".toRegex(), " ")?.trim()

    fun List<String>?.safeVars(): List<String>? = this?.mapNotNull { it.safeVar() }

    // the OSM standard separator for multiple values within a single tag
    const val OSM_TAG_SEPARATOR = ";"
}
