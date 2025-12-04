package no.entur.geocoder.converter

/**
 * Translates Norwegian stopplace descriptions to English using simple word substitution.
 *
 * This class maintains a dictionary of common Norwegian words and phrases found in
 * stopplace descriptions and translates them to their English equivalents.
 */
object NorwegianToEnglishTranslator {
    private val dictionary =
        mapOf(
            // Prepositions and directional words
            "i" to "in",
            "på" to "at",
            "ved" to "at",
            "fra" to "from",
            "til" to "to",
            "mot" to "towards",
            "langs" to "along",
            "over" to "across",
            "under" to "under",
            "nedenfor" to "below",
            "foran" to "in front of",
            "bak" to "behind",
            "mellom" to "between",
            // Directions
            "nord" to "north",
            "sør" to "south",
            "øst" to "east",
            "vest" to "west",
            "sørgående" to "southbound",
            "nordgående" to "northbound",
            "vestgående" to "westbound",
            "austgåande" to "eastbound",
            "østgående" to "eastbound",
            "retning" to "direction",
            // Street/road types
            "gate" to "street",
            "gata" to "street",
            "vei" to "road",
            "veg" to "road",
            "veien" to "road",
            "vegen" to "road",
            "allé" to "avenue",
            "alléen" to "avenue",
            "plass" to "square",
            "plassen" to "square",
            // Transportation terms
            "buss" to "bus",
            "stopp" to "stop",
            "stoppested" to "stop",
            "holdeplass" to "stop",
            "terminal" to "terminal",
            "terminalen" to "terminal",
            "stasjon" to "station",
            "stasjonen" to "station",
            "ferjekai" to "ferry pier",
            "kai" to "quay",
            "kaia" to "quay",
            "brygge" to "pier",
            "brygga" to "pier",
            "bryggen" to "pier",
            "flybuss" to "airport bus",
            "flybussen" to "airport bus",
            "hurtigbåt" to "express boat",
            "rutebuss" to "scheduled bus",
            "ekspressbuss" to "express bus",
            "skolebuss" to "school bus",
            "skoleelever" to "pupils",
            "nattbuss" to "night bus",
            "shuttlebuss" to "shuttle bus",
            "trikk" to "tram",
            "tog" to "train",
            "t-bane" to "metro",
            "bane" to "rail",
            "beltebil" to "tracked vehicle",
            "drosje" to "taxi",
            "spor" to "track",
            "sporet" to "track",
            // Position words
            "øvre" to "upper",
            "nedre" to "lower",
            "midtre" to "middle",
            "ytre" to "outer",
            "indre" to "inner",
            "venstre" to "left",
            "høyre" to "right",
            // Facilities and locations
            "inngang" to "entrance",
            "inngangen" to "entrance",
            "utgang" to "exit",
            "ankomsthall" to "arrival hall",
            "avgangshall" to "departure hall",
            "avgangshallen" to "departure hall",
            "avgangssted" to "departure point",
            "parkeringsplass" to "parking lot",
            "parkeringsplassen" to "parking lot",
            "parkeringa" to "parking",
            "parkeringshus" to "parking garage",
            "parkeringshuset" to "parking garage",
            "snuplass" to "turning area",
            "snuplassen" to "turning area",
            "rundkjøring" to "roundabout",
            "rundkjøringen" to "roundabout",
            "krysset" to "intersection",
            "kryss" to "intersection",
            "kirke" to "church",
            "kirken" to "church",
            "kirkegård" to "cemetery",
            "kirkegården" to "cemetery",
            "gravplass" to "cemetery",
            "gravplassen" to "cemetery",
            "kyrkja" to "church",
            "skole" to "school",
            "skolen" to "school",
            "skuleplassen" to "school yard",
            "skoleplass" to "school yard",
            "skoleplassen" to "school yard",
            "idrettsbane" to "sports field",
            "idrettsbanen" to "sports field",
            "fotballbane" to "football field",
            "fotballbanen" to "football field",
            "fotballstadion" to "football stadium",
            "hall" to "hall",
            "hallen" to "hall",
            "senter" to "center",
            "senteret" to "center",
            "kjøpesenter" to "shopping center",
            "stormarked" to "supermarket",
            "sentrum" to "center",
            "bussterminal" to "bus terminal",
            "busstopp" to "bus stop",
            "bensinstasjon" to "gas station",
            "bensinstasjonen" to "gas station",
            // Bridge and water
            "bru" to "bridge",
            "brua" to "bridge",
            "tunnel" to "tunnel",
            "tunnelen" to "tunnel",
            // Descriptive/status words
            "kun" to "only",
            "bare" to "only",
            "ikke" to "not",
            "ikkje" to "not",
            "midlertidig" to "temporary",
            "midl" to "temporary",
            "gammel" to "old",
            "gamle" to "old",
            "ny" to "new",
            "nye" to "new",
            "hovedinngang" to "main entrance",
            "hovedinngangen" to "main entrance",
            "avstigning" to "exit",
            "avstigning" to "exit only",
            "påstigning" to "entry",
            "anløpes" to "served",
            "betjenes" to "served",
            "betjent" to "served",
            "bruk" to "use",
            "brukes" to "used",
            "benyttes" to "used",
            "ligger" to "located",
            "vendt" to "facing",
            "arrangement" to "event",
            // Ramps and access
            "rampe" to "ramp",
            "rampen" to "ramp",
            "avkjøringsrampe" to "exit ramp",
            "påkjøringsrampe" to "entrance ramp",
            "avkjøring" to "exit",
            // Administrative
            "reserve" to "backup",
            "reserveplass" to "backup location",
            "avvik" to "deviation",
            "avviksstopp" to "deviation stop",
            "omkjøring" to "detour",
            "bestillingsrute" to "on-demand route",
            "bestillingsruter" to "on-demand routes",
            "servicerute" to "service route",
            // Modifiers
            "for" to "for",
            "av" to "by",
            "med" to "with",
            "eller" to "or",
            "og" to "and",
            "når" to "when",
            "som" to "as",
            "lenger" to "longer",
            "nøyaktig" to "exact",
            "egen" to "separate",
            "lokal" to "local",
            "oppdatert" to "updated",
            "info" to "info",
            "informasjon" to "information",
            "se" to "see",
            // Sides
            "nordsiden" to "north side",
            "sydsiden" to "south side",
            "sørsiden" to "south side",
            "østsiden" to "east side",
            "vestsiden" to "west side",
            "nedsiden" to "lower side",
            "utsiden" to "outside",
            "motsiden" to "opposite side",
            // Ring roads
            "ring" to "ring road",
            "ringvei" to "ring road",
            "ringveien" to "ring road",
            // Plan/level
            "plan" to "floor",
            "etasje" to "floor",
            // Other common terms
            "bygning" to "building",
            "bygg" to "building",
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
