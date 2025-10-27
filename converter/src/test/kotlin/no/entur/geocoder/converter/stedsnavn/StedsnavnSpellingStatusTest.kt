package no.entur.geocoder.converter.stedsnavn

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for StedsnavnSpellingStatus enum validation.
 *
 * Validates spelling status codes according to Kartverket's place name registry quality control rules,
 * matching kakka's implementation (KartverketFeatureSpellingStatusCode.java).
 */
class StedsnavnSpellingStatusTest {

    @Test
    fun `vedtatt status is accepted`() {
        assertTrue(StedsnavnSpellingStatus.isAccepted("vedtatt"), "vedtatt (approved) should be accepted")
    }

    @Test
    fun `godkjent status is accepted`() {
        assertTrue(StedsnavnSpellingStatus.isAccepted("godkjent"), "godkjent (approved) should be accepted")
    }

    @Test
    fun `privat status is accepted`() {
        assertTrue(StedsnavnSpellingStatus.isAccepted("privat"), "privat (private) should be accepted")
    }

    @Test
    fun `samlevedtak status is accepted`() {
        assertTrue(StedsnavnSpellingStatus.isAccepted("samlevedtak"), "samlevedtak (collective decision) should be accepted")
    }

    @Test
    fun `uvurdert status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("uvurdert"), "uvurdert (unevaluated) should be rejected")
    }

    @Test
    fun `avslått status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("avslått"), "avslått (rejected) should be rejected")
    }

    @Test
    fun `foreslått status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("foreslått"), "foreslått (proposed) should be rejected")
    }

    @Test
    fun `klage status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("klage"), "klage (complaint) should be rejected")
    }

    @Test
    fun `internasjonal status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("internasjonal"), "internasjonal (international) should be rejected")
    }

    @Test
    fun `historisk status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("historisk"), "historisk (historical) should be rejected")
    }

    @Test
    fun `null status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted(null), "null status should be rejected")
    }

    @Test
    fun `empty status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted(""), "empty status should be rejected")
    }

    @Test
    fun `unknown status is rejected`() {
        assertFalse(StedsnavnSpellingStatus.isAccepted("unknown"), "unknown status should be rejected")
    }

    @Test
    fun `status is case insensitive`() {
        assertTrue(StedsnavnSpellingStatus.isAccepted("vedtatt"), "vedtatt should be accepted (case insensitive)")
        assertTrue(StedsnavnSpellingStatus.isAccepted("Vedtatt"), "Vedtatt should be accepted (case insensitive)")
        assertFalse(StedsnavnSpellingStatus.isAccepted("UVURDERT"), "UVURDERT should be rejected (case insensitive)")
    }

    @Test
    fun `all accepted statuses are documented`() {
        val accepted = listOf("vedtatt", "godkjent", "privat", "samlevedtak")
        accepted.forEach { status ->
            assertTrue(
                StedsnavnSpellingStatus.isAccepted(status),
                "$status should be in accepted list"
            )
        }
    }

    @Test
    fun `all rejected statuses are documented`() {
        val rejected = listOf("uvurdert", "avslått", "foreslått", "klage", "internasjonal", "historisk")
        rejected.forEach { status ->
            assertFalse(
                StedsnavnSpellingStatus.isAccepted(status),
                "$status should be in rejected list"
            )
        }
    }
}
