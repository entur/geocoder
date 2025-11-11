package no.entur.geocoder.proxy

object Text {
    fun String?.safeVar(): String? = this?.replace("[^\\p{L}\\p{Nd}_.,'\\-\\s:]+".toRegex(), " ")?.trim()

    fun List<String>?.safeVars(): List<String>? = this?.mapNotNull { it.safeVar() }
}
