package  no.entur.geocoder.proxy

object Text {

    fun String?.safeVar(): String? = this?.replace("[^\\p{L}\\p{Nd}_.,'\\-\\s:]+".toRegex(), " ")

    fun List<String>?.safeVars(): List<String>? = this?.mapNotNull { it.safeVar() }
}
