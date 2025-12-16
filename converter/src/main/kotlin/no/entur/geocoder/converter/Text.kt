package no.entur.geocoder.converter

object Text {
    fun altName(vararg text: String?): String? =
        listOf(*text).altName()

    fun List<String?>.altName(): String? =
        this
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(";")
            .ifBlank { null }
}
