package no.entur.geocoder.converter.source.lantmateriet

data class BelagenhetsAdress(
    val objektidentitet: String,
    val adressplatstyp: String,
    val adressomradeFaststalltnamn: String?,
    val gardsadressomradeFaststalltnamn: String?,
    val adressplatsnummer: Int?,
    val bokstavstillagg: String?,
    val lagestillagg: String?,
    val lagestillaggsnummer: Int?,
    val avvikerfranstandarden: Boolean,
    val avvikandeadressplatsbeteckning: String?,
    val postnummer: String,
    val postort: String,
    val kommunkod: String,
    val kommunnamn: String,
    val lanskod: String,
    val popularnamn: String?,
    val kommundelFaststalltnamn: String?,
    val easting: Double,
    val northing: Double,
) {
    val isStreetAddress: Boolean
        get() = adressplatstyp == "Gatuadressplats" || adressplatstyp == "Metertalsadressplats"

    fun housenumber(): String? {
        if (avvikerfranstandarden && !avvikandeadressplatsbeteckning.isNullOrBlank()) {
            return avvikandeadressplatsbeteckning
        }
        val num = adressplatsnummer ?: return null
        val sb = StringBuilder()
        sb.append(num)
        if (!bokstavstillagg.isNullOrBlank()) {
            sb.append(bokstavstillagg)
        }
        if (!lagestillagg.isNullOrBlank()) {
            sb.append(" ")
            sb.append(lagestillagg)
            if (lagestillaggsnummer != null) {
                sb.append(lagestillaggsnummer)
            }
        }
        return sb.toString()
    }

    fun streetName(): String? =
        when (adressplatstyp) {
            "Gatuadressplats", "Metertalsadressplats" -> adressomradeFaststalltnamn
            "Byadressplats" -> adressomradeFaststalltnamn
            "Gardsadressplats" -> gardsadressomradeFaststalltnamn ?: adressomradeFaststalltnamn
            else -> adressomradeFaststalltnamn
        }

    fun placeName(): String? =
        when (adressplatstyp) {
            "Byadressplats" -> adressomradeFaststalltnamn
            "Gardsadressplats" -> gardsadressomradeFaststalltnamn ?: adressomradeFaststalltnamn
            else -> null
        }
}
