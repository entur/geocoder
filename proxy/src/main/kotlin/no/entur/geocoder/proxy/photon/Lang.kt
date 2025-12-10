package no.entur.geocoder.proxy.photon

object Lang {
    val supportedLanguages = listOf("no", "en")

    fun handleLang(lang: String): String = if (supportedLanguages.contains(lang)) lang else "no"
}
