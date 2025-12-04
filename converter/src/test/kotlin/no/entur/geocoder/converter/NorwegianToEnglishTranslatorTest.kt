package no.entur.geocoder.converter

import no.entur.geocoder.converter.NorwegianToEnglishTranslator.translate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class NorwegianToEnglishTranslatorTest {
    @Test
    fun `handle empty and blank strings`() {
        assertEquals("", translate(""))
        assertEquals("  ", translate("  "))
    }

    @ParameterizedTest
    @CsvSource(
        "i Pilestredet | in Pilestredet",
        "ved Schweigaards gate | at Schweigaards street",
        "på bensinstasjonen | at gas station",
        "mot Oslo | towards Oslo",
        "fra Bergen | from Bergen",
        "retning sentrum | direction center",
        "langs E6 | along E6",
        "nordgående i Kongsvegen | northbound in Kongsvegen",
        "sørgående i Vestsidevegen | southbound in Vestsidevegen",
        "nord | north",
        "sør | south",
        "buss for bane | bus for rail",
        "buss for T-bane | bus for Metro",
        "buss for trikk | bus for tram",
        "skolebuss | school bus",
        "nattbuss | night bus",
        "busstopp | bus stop",
        "ved spor 19 | at track 19",
        "ankomsthall, nedre plan | arrival hall, lower floor",
        "avgangshall øvre plan | departure hall upper floor",
        "parkeringsplass | parking lot",
        "midlertidig stopp | temporary stop",
        "ved inngangen | at entrance",
        "øvre plan | upper floor",
        "kun avstigning | only exit only",
        "midlertidig i Holtegata | temporary in Holtegata",
        "i Akersgata | in Akersgata",
        "ved rundkjøringen | at roundabout",
        "Nord | North",
        "NORD | NORTH",
        "i Pilestredet | in Pilestredet",
        "i Pilestredet | in Pilestredet",
        "ved Oslo S | at Oslo S",
        "Ådnavegen | Ådnavegen",
        "ikke i bruk | not in use",
        "Ikkje betjent av rutebuss | Not served by scheduled bus",
        "brukes kun av flybuss | used only by airport bus",
        "Bare avstigning for skoleelever | Only exit only for pupils",
        "Betjenes av bestillingsruter | Served by on-demand routes",
        "bussterminal ved terminal 3 | bus terminal at terminal 3",
        "Inngang A | Entrance A",
        "Inngangen til Vestlandshallen | Entrance to Vestlandshallen",
        "Hovedinngangen ved Åsane stormarked | Main entrance at Åsane supermarket",
        "nedenfor parkeringshuset | below parking garage",
        "foran Hotel Opera | in front of Hotel Opera",
        "foran Oslo S | in front of Oslo S",
        "øvre | upper",
        "Avkjøringsrampe E6 nordgående | Exit ramp E6 northbound",
        "Påkjøringsrampe E6 sørgående | Entrance ramp E6 southbound",
        "avkjøringsrampe mot Grorud | exit ramp towards Grorud",
        "påkjøringsrampe mot sentrum | entrance ramp towards center",
        delimiter = '|',
    )
    fun `translate ramp descriptions`(input: String, expected: String) {
        assertEquals(expected, translate(input))
    }
}
