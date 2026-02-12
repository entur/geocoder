package no.entur.geocoder.converter.source.lantmateriet

object SwedishCounty {
    private val counties =
        mapOf(
            "01" to "Stockholms län",
            "03" to "Uppsala län",
            "04" to "Södermanlands län",
            "05" to "Östergötlands län",
            "06" to "Jönköpings län",
            "07" to "Kronobergs län",
            "08" to "Kalmar län",
            "09" to "Gotlands län",
            "10" to "Blekinge län",
            "12" to "Skåne län",
            "13" to "Hallands län",
            "14" to "Västra Götalands län",
            "17" to "Värmlands län",
            "18" to "Örebro län",
            "19" to "Västmanlands län",
            "20" to "Dalarnas län",
            "21" to "Gävleborgs län",
            "22" to "Västernorrlands län",
            "23" to "Jämtlands län",
            "24" to "Västerbottens län",
            "25" to "Norrbottens län",
        )

    fun getCountyName(lanskod: String): String? = counties[lanskod]
}
