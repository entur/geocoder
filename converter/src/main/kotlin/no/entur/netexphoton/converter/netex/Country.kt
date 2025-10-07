package no.entur.netexphoton.converter.netex

enum class Country(
    val threeLetterCode: String,
) {
    AL("ALB"), // Albania
    AD("AND"), // Andorra
    AT("AUT"), // Austria
    BY("BLR"), // Belarus
    BE("BEL"), // Belgium
    BA("BIH"), // Bosnia and Herzegovina
    BG("BGR"), // Bulgaria
    HR("HRV"), // Croatia
    CY("CYP"), // Cyprus
    CZ("CZE"), // Czech Republic
    DK("DNK"), // Denmark
    EE("EST"), // Estonia
    FO("FRO"), // Faroe Islands
    FI("FIN"), // Finland
    FR("FRA"), // France
    DE("DEU"), // Germany
    GI("GIB"), // Gibraltar
    GR("GRC"), // Greece
    GG("GGY"), // Guernsey
    HU("HUN"), // Hungary
    IS("ISL"), // Iceland
    IE("IRL"), // Ireland
    IM("IMN"), // Isle of Man
    IT("ITA"), // Italy
    JE("JEY"), // Jersey
    LV("LVA"), // Latvia
    LI("LIE"), // Liechtenstein
    LT("LTU"), // Lithuania
    LU("LUX"), // Luxembourg
    MK("MKD"), // North Macedonia
    MT("MLT"), // Malta
    MD("MDA"), // Moldova
    MC("MCO"), // Monaco
    ME("MNE"), // Montenegro
    NL("NLD"), // Netherlands
    NO("NOR"), // Norway
    PL("POL"), // Poland
    PT("PRT"), // Portugal
    RO("ROU"), // Romania
    RU("RUS"), // Russia
    SM("SMR"), // San Marino
    RS("SRB"), // Serbia
    SK("SVK"), // Slovakia
    SI("SVN"), // Slovenia
    ES("ESP"), // Spain
    SJ("SJM"), // Svalbard and Jan Mayen
    SE("SWE"), // Sweden
    CH("CHE"), // Switzerland
    UA("UKR"), // Ukraine
    GB("GBR"), // United Kingdom
    VA("VAT"), // Vatican City
    ;

    companion object {
        fun getThreeLetterCode(twoLetterCode: String?): String? =
            entries.toTypedArray().firstOrNull { it.name == twoLetterCode?.uppercase() }?.threeLetterCode
    }
}
