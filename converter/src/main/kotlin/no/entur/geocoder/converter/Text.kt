package no.entur.geocoder.converter

object Text {
    fun createAltNameList(vararg text: String?, skip: String? = null): String? =
        listOf(*text).createAltNameList(skip)

    fun List<String?>.createAltNameList(skip: String? = null): String? =
        this.filterNot { it == skip }.joinToStringNoBlank()

    const val separator = ";"

    fun List<String?>.joinToStringNoBlank() =
        this
            .filterNotNull()
            .filter { it.isNotBlank() }
            .joinToString(separator)
            .ifBlank { null }

    fun joinToStringNoBlank(vararg text: String?) =
        listOf(*text).joinToStringNoBlank()
}
