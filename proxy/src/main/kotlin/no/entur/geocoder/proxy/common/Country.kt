package no.entur.geocoder.proxy.common

import java.util.Locale
import kotlin.collections.get

data class Country(
    val name: String, // 2-letter lowercase (e.g. "no")
    val threeLetterCode: String, // 3-letter uppercase (e.g. "NOR")
) {
    companion object {
        private val byIso2: Map<String, Country> by lazy {
            Locale
                .getISOCountries()
                .mapNotNull { iso2 ->
                    try {
                        val iso3 = Locale.of("", iso2).getISO3Country()
                        if (iso3.isNotBlank()) {
                            Country(iso2.lowercase(), iso3.uppercase())
                        } else {
                            null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }.associateBy { it.name }
        }

        private val byIso3: Map<String, Country> by lazy {
            byIso2.values.associateBy { it.threeLetterCode }
        }

        val no = Country("no", "NOR")

        fun parse(twoLetterCode: String?): Country? = byIso2[twoLetterCode?.lowercase()]

        fun fromThreeLetterCode(threeLetterCode: String?): Country? = byIso3[threeLetterCode?.uppercase()]
    }
}
