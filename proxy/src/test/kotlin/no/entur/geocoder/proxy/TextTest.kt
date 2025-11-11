package no.entur.geocoder.proxy

import no.entur.geocoder.proxy.Text.safeVar
import no.entur.geocoder.proxy.Text.safeVars
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextTest {

    @Test
    fun `safeVar sanitizes real-world Norwegian place names and addresses`() {
        data class SanitizeTest(val input: String?, val expected: String?)

        val testCases = listOf(
            SanitizeTest("Ålesund", "Ålesund"),
            SanitizeTest("Tromsø", "Tromsø"),
            SanitizeTest("Bodø sentrum", "Bodø sentrum"),
            SanitizeTest("Karl Johans gate 22", "Karl Johans gate 22"),
            SanitizeTest("St. Olavs gate", "St. Olavs gate"),
            SanitizeTest("Grünerløkka", "Grünerløkka"),
            SanitizeTest("Sørenga, Oslo", "Sørenga, Oslo"),
            SanitizeTest("user<script>alert('xss')</script>input", "user script alert 'xss' script input"),
            SanitizeTest("Test@#\$%injection", "Test injection"),
            SanitizeTest("OSM:123456", "OSM:123456"),
            SanitizeTest("RUT:TariffZone:01", "RUT:TariffZone:01"),
            SanitizeTest(null, null),
            SanitizeTest("", "")
        )

        testCases.forEach { test ->
            assertEquals(test.expected, test.input.safeVar())
        }
    }

    @Test
    fun `safeVars sanitizes lists of Norwegian identifiers and names`() {
        data class ListTest(val input: List<String>?, val expected: List<String>?)

        val testCases = listOf(
            ListTest(
                listOf("03", "18", "50"),
                listOf("03", "18", "50")
            ),
            ListTest(
                listOf("Oslo<script>", "Bergen#test", "Trondheim"),
                listOf("Oslo script", "Bergen test", "Trondheim")
            ),
            ListTest(
                listOf("RUT:TariffZone:01", "ATB:TariffZone:A", "Kolumbus:TariffZone:1"),
                listOf("RUT:TariffZone:01", "ATB:TariffZone:A", "Kolumbus:TariffZone:1")
            ),
            ListTest(null, null),
            ListTest(emptyList(), emptyList())
        )

        testCases.forEach { test ->
            assertEquals(test.expected, test.input.safeVars())
        }
    }
}

