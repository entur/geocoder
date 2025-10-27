package no.entur.geocoder.converter.stedsnavn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for StedsnavnPlaceType enum matching kakka's place type filtering.
 *
 * Validates the 5 target place types from Kartverket's stedsnavn registry:
 * - by (city/town)
 * - bydel (city district)
 * - tettsted (urban settlement)
 * - tettsteddel (small town part)
 * - tettbebyggelse (built-up area)
 */
class StedsnavnPlaceTypeTest {

    @Test
    fun `by type is recognized`() {
        val type = StedsnavnPlaceType.fromString("by")
        assertNotNull(type, "by should be recognized")
        assertEquals(StedsnavnPlaceType.by, type)
    }

    @Test
    fun `bydel type is recognized`() {
        val type = StedsnavnPlaceType.fromString("bydel")
        assertNotNull(type, "bydel should be recognized")
        assertEquals(StedsnavnPlaceType.bydel, type)
    }

    @Test
    fun `tettsted type is recognized`() {
        val type = StedsnavnPlaceType.fromString("tettsted")
        assertNotNull(type, "tettsted should be recognized")
        assertEquals(StedsnavnPlaceType.tettsted, type)
    }

    @Test
    fun `tettsteddel type is recognized`() {
        val type = StedsnavnPlaceType.fromString("tettsteddel")
        assertNotNull(type, "tettsteddel should be recognized")
        assertEquals(StedsnavnPlaceType.tettsteddel, type)
    }

    @Test
    fun `tettbebyggelse type is recognized`() {
        val type = StedsnavnPlaceType.fromString("tettbebyggelse")
        assertNotNull(type, "tettbebyggelse should be recognized")
        assertEquals(StedsnavnPlaceType.tettbebyggelse, type)
    }

    @Test
    fun `unknown type returns null`() {
        val type = StedsnavnPlaceType.fromString("grend")
        assertNull(type, "grend should return null (not in target types)")
    }

    @Test
    fun `null type returns null`() {
        val type = StedsnavnPlaceType.fromString(null)
        assertNull(type, "null should return null")
    }

    @Test
    fun `empty type returns null`() {
        val type = StedsnavnPlaceType.fromString("")
        assertNull(type, "empty string should return null")
    }

    @Test
    fun `type matching is case insensitive`() {
        assertNotNull(StedsnavnPlaceType.fromString("BY"), "BY should be recognized (case insensitive)")
        assertNotNull(StedsnavnPlaceType.fromString("Bydel"), "Bydel should be recognized (case insensitive)")
        assertNotNull(StedsnavnPlaceType.fromString("TETTSTED"), "TETTSTED should be recognized")
    }

    @Test
    fun `isTarget returns true for target types`() {
        assertTrue(StedsnavnPlaceType.isTarget("by"), "by is a target type")
        assertTrue(StedsnavnPlaceType.isTarget("bydel"), "bydel is a target type")
        assertTrue(StedsnavnPlaceType.isTarget("tettsted"), "tettsted is a target type")
        assertTrue(StedsnavnPlaceType.isTarget("tettsteddel"), "tettsteddel is a target type")
        assertTrue(StedsnavnPlaceType.isTarget("tettbebyggelse"), "tettbebyggelse is a target type")
    }

    @Test
    fun `isTarget returns false for non-target types`() {
        assertFalse(StedsnavnPlaceType.isTarget("grend"), "grend is not a target type")
        assertFalse(StedsnavnPlaceType.isTarget("boligfelt"), "boligfelt is not a target type")
        assertFalse(StedsnavnPlaceType.isTarget("fylke"), "fylke is not a target type")
    }

    @Test
    fun `isTarget handles null and empty`() {
        assertFalse(StedsnavnPlaceType.isTarget(null), "null is not a target type")
        assertFalse(StedsnavnPlaceType.isTarget(""), "empty string is not a target type")
    }

    @Test
    fun `all five target types are defined`() {
        val targetTypes = listOf("by", "bydel", "tettsted", "tettsteddel", "tettbebyggelse")
        assertEquals(5, targetTypes.size, "Should have exactly 5 target types")

        targetTypes.forEach { typeName ->
            assertTrue(
                StedsnavnPlaceType.isTarget(typeName),
                "$typeName should be a target type"
            )
        }
    }

    @Test
    fun `each type has correct typeName`() {
        // Verify each type has the correct typeName for matching GML data
        assertEquals("by", StedsnavnPlaceType.by.name)
        assertEquals("bydel", StedsnavnPlaceType.bydel.name)
        assertEquals("tettsted", StedsnavnPlaceType.tettsted.name)
        assertEquals("tettsteddel", StedsnavnPlaceType.tettsteddel.name)
        assertEquals("tettbebyggelse", StedsnavnPlaceType.tettbebyggelse.name)
    }
}