package no.entur.geocoder.converter.source.stedsnavn

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StedsnavnPlaceTypeTest {
    @ParameterizedTest
    @ValueSource(strings = ["by", "BY", "bydel", "BYDEL", "tettsted", "tettsteddel", "tettbebyggelse"])
    fun `isTarget recognizes all target types case insensitively`(type: String) {
        assertTrue(StedsnavnPlaceType.isTarget(type))
        assertNotNull(StedsnavnPlaceType.fromString(type))
    }

    @ParameterizedTest
    @ValueSource(strings = ["grend", "fylke", "kommune"])
    fun `isTarget rejects non-target types`(type: String) {
        assertFalse(StedsnavnPlaceType.isTarget(type))
        assertNull(StedsnavnPlaceType.fromString(type))
    }

    @Test
    fun `rejects null and empty`() {
        assertFalse(StedsnavnPlaceType.isTarget(null))
        assertFalse(StedsnavnPlaceType.isTarget(""))
        assertNull(StedsnavnPlaceType.fromString(null))
        assertNull(StedsnavnPlaceType.fromString(""))
    }

    @Test
    fun `has exactly 5 target types`() {
        assertEquals(5, StedsnavnPlaceType.entries.size)
        assertEquals(5, StedsnavnPlaceType.targetTypes().size)
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
