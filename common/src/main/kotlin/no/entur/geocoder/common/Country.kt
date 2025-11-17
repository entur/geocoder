package no.entur.geocoder.common

enum class Country(
    val threeLetterCode: String,
) {
    al("ALB"), // Albania
    ad("AND"), // Andorra
    at("AUT"), // Austria
    by("BLR"), // Belarus
    be("BEL"), // Belgium
    ba("BIH"), // Bosnia and Herzegovina
    bg("BGR"), // Bulgaria
    hr("HRV"), // Croatia
    cy("CYP"), // Cyprus
    cz("CZE"), // Czech Republic
    dk("DNK"), // Denmark
    ee("EST"), // Estonia
    fo("FRO"), // Faroe Islands
    fi("FIN"), // Finland
    fr("FRA"), // France
    de("DEU"), // Germany
    gi("GIB"), // Gibraltar
    gr("GRC"), // Greece
    gg("GGY"), // Guernsey
    hu("HUN"), // Hungary
    `is`("ISL"), // Iceland
    ie("IRL"), // Ireland
    im("IMN"), // Isle of Man
    it("ITA"), // Italy
    je("JEY"), // Jersey
    lv("LVA"), // Latvia
    li("LIE"), // Liechtenstein
    lt("LTU"), // Lithuania
    lu("LUX"), // Luxembourg
    mk("MKD"), // North Macedonia
    mt("MLT"), // Malta
    md("MDA"), // Moldova
    mc("MCO"), // Monaco
    me("MNE"), // Montenegro
    nl("NLD"), // Netherlands
    no("NOR"), // Norway
    pl("POL"), // Poland
    pt("PRT"), // Portugal
    ro("ROU"), // Romania
    ru("RUS"), // Russia
    sm("SMR"), // San Marino
    rs("SRB"), // Serbia
    sk("SVK"), // Slovakia
    si("SVN"), // Slovenia
    es("ESP"), // Spain
    sj("SJM"), // Svalbard and Jan Mayen
    se("SWE"), // Sweden
    ch("CHE"), // Switzerland
    ua("UKR"), // Ukraine
    gb("GBR"), // United Kingdom
    va("VAT"), // Vatican City
    ;

    companion object {
        fun parse(twoLetterCode: String?): Country? =
            entries.firstOrNull { it.name == twoLetterCode?.lowercase() }

        fun getThreeLetterCode(twoLetterCode: String?): String? =
            entries.firstOrNull { it.name == twoLetterCode?.lowercase() }?.threeLetterCode
    }
}
