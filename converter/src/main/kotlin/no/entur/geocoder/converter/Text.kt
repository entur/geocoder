package no.entur.geocoder.converter

object Text {
    fun altName(vararg text: String?, separator: String = ";"): String? =
        listOf(*text).altName(separator)

    fun List<String?>.altName(separator: String = ";"): String? =
        this.filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(separator)
            .ifBlank { null }
}