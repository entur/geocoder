package no.entur.geocoder.proxy.common

import no.entur.geocoder.proxy.common.Category.asCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryTest {
    // Must stay in sync with the Rust converter's nominatim-converter/src/common/category.rs::as_category
    // tests. Identical inputs must produce identical outputs because the converter writes indexed
    // categories and the proxy queries against them.

    @Test
    fun `fallback and unicode handling`() {
        // Chars not in the table become a single `_` - including astral-plane
        // chars, which must match the Rust side's scalar iteration.
        assertEquals("S_o_Tome", "São Tomé".asCategory())
        assertEquals("Kara_johka", "Kárášjohka".asCategory())
        assertEquals("emoji___char", "emoji 🚀 char".asCategory())
    }

    @Test
    fun `replaces colons with dots`() {
        assertEquals("NSR.StopPlace.123", "NSR:StopPlace:123".asCategory())
    }

    @Test
    fun `no colons passes through unchanged`() {
        assertEquals("something", "something".asCategory())
    }

    @Test
    fun `transliterates Norwegian diacritics`() {
        assertEquals("KVE.TopographicPlace.3907-Aarfuglveien", "KVE:TopographicPlace:3907-Årfuglveien".asCategory())
        assertEquals("Bjoelsen", "Bjølsen".asCategory())
        assertEquals("Laerdal", "Lærdal".asCategory())
        assertEquals("Tromsoe", "Tromsø".asCategory())
        assertEquals("Aalesund", "Ålesund".asCategory())
    }

    @Test
    fun `street with spaces becomes underscores`() {
        assertEquals(
            "KVE.TopographicPlace.0301-Karl_Johans_gate",
            "KVE:TopographicPlace:0301-Karl Johans gate".asCategory(),
        )
    }

    @Test
    fun `output matches Photon CATEGORY_PATTERN for representative inputs`() {
        // PhotonDoc.CATEGORY_PATTERN: [a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+
        fun matchesPhotonPattern(s: String): Boolean {
            val segments = s.split('.')
            if (segments.size < 2) return false
            return segments.all { seg ->
                seg.isNotEmpty() && seg.all { c -> c.isLetterOrDigit() && c.code < 128 || c == '_' || c == '-' }
            }
        }
        val inputs = listOf(
            "KVE:TopographicPlace:0301-Karl Johans gate",
            "KVE:TopographicPlace:3407-Fahlstrøms plass",
            "KVE:PlaceName:434810",
            "KVE:Borough:34200205",
            "NSR:StopPlace:337",
            "OSM:TopographicPlace:545260792",
        )
        for (input in inputs) {
            val out = input.asCategory()
            assertTrue(matchesPhotonPattern(out), "asCategory($input) = $out does not match Photon CATEGORY_PATTERN")
        }
    }
}
