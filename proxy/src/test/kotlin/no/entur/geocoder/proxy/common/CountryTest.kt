package no.entur.geocoder.proxy.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CountryTest {
    @Test
    fun `parse returns country for lowercase 2-letter code`() {
        val country = Country.parse("no")
        assertNotNull(country)
        assertEquals("no", country.name)
        assertEquals("NOR", country.threeLetterCode)
    }

    @Test
    fun `parse is case-insensitive`() {
        assertEquals(Country.parse("no"), Country.parse("NO"))
        assertEquals(Country.parse("se"), Country.parse("SE"))
    }

    @Test
    fun `parse returns null for unknown code`() {
        assertNull(Country.parse("xx"))
        assertNull(Country.parse(null))
        assertNull(Country.parse(""))
    }

    @Test
    fun `fromThreeLetterCode returns country for uppercase 3-letter code`() {
        val country = Country.fromThreeLetterCode("NOR")
        assertNotNull(country)
        assertEquals("no", country.name)
        assertEquals("NOR", country.threeLetterCode)
    }

    @Test
    fun `fromThreeLetterCode is case-insensitive`() {
        assertEquals(Country.fromThreeLetterCode("NOR"), Country.fromThreeLetterCode("nor"))
    }

    @Test
    fun `fromThreeLetterCode returns null for unknown code`() {
        assertNull(Country.fromThreeLetterCode("XXX"))
        assertNull(Country.fromThreeLetterCode(null))
    }

    @Test
    fun `parse and fromThreeLetterCode are consistent`() {
        listOf("no" to "NOR", "se" to "SWE", "dk" to "DNK", "fi" to "FIN", "de" to "DEU", "gb" to "GBR").forEach { (iso2, iso3) ->
            val byIso2 = Country.parse(iso2)
            val byIso3 = Country.fromThreeLetterCode(iso3)
            assertNotNull(byIso2, "parse($iso2) should not be null")
            assertNotNull(byIso3, "fromThreeLetterCode($iso3) should not be null")
            assertEquals(byIso2, byIso3, "Both lookups for $iso2/$iso3 should return the same Country")
        }
    }

    @Test
    fun `Country_no constant is Norway`() {
        assertEquals("no", Country.no.name)
        assertEquals("NOR", Country.no.threeLetterCode)
    }
}
