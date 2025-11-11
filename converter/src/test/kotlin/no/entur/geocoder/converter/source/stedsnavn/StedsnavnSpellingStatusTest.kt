package no.entur.geocoder.converter.source.stedsnavn

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StedsnavnSpellingStatusTest {
    @ParameterizedTest
    @ValueSource(strings = ["vedtatt", "VEDTATT", "godkjent", "privat", "samlevedtak"])
    fun `accepts valid statuses case insensitively`(status: String) {
        assertTrue(StedsnavnSpellingStatus.isAccepted(status))
        assertNotNull(StedsnavnSpellingStatus.fromString(status))
    }

    @ParameterizedTest
    @ValueSource(strings = ["uvurdert", "avslått", "foreslått", "klage", "historisk"])
    fun `rejects invalid statuses`(status: String) {
        assertFalse(StedsnavnSpellingStatus.isAccepted(status))
        assertNull(StedsnavnSpellingStatus.fromString(status))
    }

    @Test
    fun `rejects null and empty`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted(null))
        assertFalse(StedsnavnSpellingStatus.isAccepted(""))
        assertNull(StedsnavnSpellingStatus.fromString(null))
    }

    @Test
    fun `has exactly 4 accepted statuses`() {
        assertEquals(4, StedsnavnSpellingStatus.entries.size)
    }
}
