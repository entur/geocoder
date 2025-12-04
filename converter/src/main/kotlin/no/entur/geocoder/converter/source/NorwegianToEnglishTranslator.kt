package no.entur.geocoder.converter.source

/**
 * Translates Norwegian stopplace descriptions to English using simple word substitution.
 *
 * This class maintains a dictionary of common Norwegian words and phrases found in
 * stopplace descriptions and translates them to their English equivalents.
 */
object NorwegianToEnglishTranslator {
    private val dictionary =
        mapOf(
            "allé" to "avenue",
            "alléen" to "avenue",
            "ankomsthall" to "arrival hall",
            "anløpes" to "served",
            "arrangement" to "event",
            "austgåande" to "eastbound",
            "av" to "by",
            "avgangshall" to "departure hall",
            "avgangshallen" to "departure hall",
            "avgangssted" to "departure point",
            "avkjøring" to "exit",
            "avkjøringsrampe" to "exit ramp",
            "avstigning" to "exit only",
            "avstigning" to "exit",
            "avvik" to "deviation",
            "avviksstopp" to "deviation stop",
            "bak" to "behind",
            "bane" to "rail",
            "bare" to "only",
            "beltebil" to "tracked vehicle",
            "bensinstasjon" to "gas station",
            "bensinstasjonen" to "gas station",
            "benyttes" to "used",
            "bestillingsrute" to "on-demand route",
            "bestillingsruter" to "on-demand routes",
            "betjenes" to "served",
            "betjent" to "served",
            "bru" to "bridge",
            "brua" to "bridge",
            "bruk" to "use",
            "brukes" to "used",
            "brygga" to "pier",
            "brygge" to "pier",
            "bryggen" to "pier",
            "buss" to "bus",
            "bussterminal" to "bus terminal",
            "busstopp" to "bus stop",
            "bygg" to "building",
            "bygning" to "building",
            "drosje" to "taxi",
            "egen" to "separate",
            "ekspressbuss" to "express bus",
            "eller" to "or",
            "etasje" to "floor",
            "ferjekai" to "ferry pier",
            "flybuss" to "airport bus",
            "flybussen" to "airport bus",
            "for" to "for",
            "foran" to "in front of",
            "fotballbane" to "football field",
            "fotballbanen" to "football field",
            "fotballstadion" to "football stadium",
            "fra" to "from",
            "gamle" to "old",
            "gammel" to "old",
            "gata" to "street",
            "gate" to "street",
            "gravplass" to "cemetery",
            "gravplassen" to "cemetery",
            "hall" to "hall",
            "hallen" to "hall",
            "holdeplass" to "stop",
            "hovedinngang" to "main entrance",
            "hovedinngangen" to "main entrance",
            "hurtigbåt" to "express boat",
            "høyre" to "right",
            "i" to "in",
            "idrettsbane" to "sports field",
            "idrettsbanen" to "sports field",
            "ikke" to "not",
            "ikkje" to "not",
            "indre" to "inner",
            "info" to "info",
            "informasjon" to "information",
            "inngang" to "entrance",
            "inngangen" to "entrance",
            "kai" to "quay",
            "kaia" to "quay",
            "kirke" to "church",
            "kirkegård" to "cemetery",
            "kirkegården" to "cemetery",
            "kirken" to "church",
            "kjøpesenter" to "shopping center",
            "kryss" to "intersection",
            "krysset" to "intersection",
            "kun" to "only",
            "kyrkja" to "church",
            "langs" to "along",
            "lenger" to "longer",
            "ligger" to "located",
            "lokal" to "local",
            "med" to "with",
            "mellom" to "between",
            "midl" to "temporary",
            "midlertidig" to "temporary",
            "midtre" to "middle",
            "mot" to "towards",
            "motsiden" to "opposite side",
            "nattbuss" to "night bus",
            "nedenfor" to "below",
            "nedre" to "lower",
            "nedsiden" to "lower side",
            "nord" to "north",
            "nordgående" to "northbound",
            "nordsiden" to "north side",
            "ny" to "new",
            "nye" to "new",
            "når" to "when",
            "nøyaktig" to "exact",
            "og" to "and",
            "omkjøring" to "detour",
            "oppdatert" to "updated",
            "over" to "across",
            "parkeringa" to "parking",
            "parkeringshus" to "parking garage",
            "parkeringshuset" to "parking garage",
            "parkeringsplass" to "parking lot",
            "parkeringsplassen" to "parking lot",
            "plan" to "floor",
            "plass" to "square",
            "plassen" to "square",
            "på" to "at",
            "påkjøringsrampe" to "entrance ramp",
            "påstigning" to "entry",
            "rampe" to "ramp",
            "rampen" to "ramp",
            "reserve" to "backup",
            "reserveplass" to "backup location",
            "retning" to "direction",
            "ring" to "ring road",
            "ringvei" to "ring road",
            "ringveien" to "ring road",
            "rundkjøring" to "roundabout",
            "rundkjøringen" to "roundabout",
            "rutebuss" to "scheduled bus",
            "se" to "see",
            "senter" to "center",
            "senteret" to "center",
            "sentrum" to "center",
            "servicerute" to "service route",
            "shuttlebuss" to "shuttle bus",
            "skole" to "school",
            "skolebuss" to "school bus",
            "skoleelever" to "pupils",
            "skolen" to "school",
            "skoleplass" to "school yard",
            "skoleplassen" to "school yard",
            "skuleplassen" to "school yard",
            "snuplass" to "turning area",
            "snuplassen" to "turning area",
            "som" to "as",
            "spor" to "track",
            "sporet" to "track",
            "stasjon" to "station",
            "stasjonen" to "station",
            "stopp" to "stop",
            "stoppested" to "stop",
            "stormarked" to "supermarket",
            "sydsiden" to "south side",
            "sør" to "south",
            "sørgående" to "southbound",
            "sørsiden" to "south side",
            "t-bane" to "metro",
            "terminal" to "terminal",
            "terminalen" to "terminal",
            "til" to "to",
            "tog" to "train",
            "trikk" to "tram",
            "tunnel" to "tunnel",
            "tunnelen" to "tunnel",
            "under" to "under",
            "utgang" to "exit",
            "utsiden" to "outside",
            "ved" to "at",
            "veg" to "road",
            "vegen" to "road",
            "vei" to "road",
            "veien" to "road",
            "vendt" to "facing",
            "venstre" to "left",
            "vest" to "west",
            "vestgående" to "westbound",
            "vestsiden" to "west side",
            "ytre" to "outer",
            "øst" to "east",
            "østgående" to "eastbound",
            "østsiden" to "east side",
            "øvre" to "upper",
        )

    /**
     * Translates a Norwegian text to English by substituting known words.
     *
     * The translation is performed word-by-word, preserving:
     * - Original word order
     * - Capitalization (upp ercase words remain uppercase)
     * - Punctuation
     * - Unknown words (left as-is)
     *
     * @param norwegianText The Norwegian text to translate
     * @return The translated English text
     */
    fun translate(norwegianText: String): String {
        if (norwegianText.isBlank()) {
            return norwegianText
        }

        // Split on whitespace and punctuation boundaries while preserving them
        // Don't split on hyphens to preserve compound words like "T-bane"
        val tokens = norwegianText.split(Regex("(?<=\\s)|(?=\\s)|(?<=[,.])|(?=[,.])"))

        val translated =
            tokens.map { token ->
                when {
                    token.isBlank() -> token
                    token.matches(Regex("[,.]")) -> token
                    else -> translateWord(token)
                }
            }

        return translated.joinToString("")
    }

    /**
     * Translates a single word, preserving capitalization.
     */
    private fun translateWord(word: String): String {
        val lowerWord = word.lowercase()
        val translation = dictionary[lowerWord]

        return if (translation != null) {
            when {
                word.all { it.isUpperCase() || !it.isLetter() } -> translation.uppercase()
                word.first().isUpperCase() -> translation.replaceFirstChar { it.uppercase() }
                else -> translation
            }
        } else {
            word
        }
    }
}
