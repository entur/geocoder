package no.entur.geocoder.converter

import no.entur.geocoder.converter.Text.altName
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class TextTest {
    @Test
    fun `altName filters null and blank values`() {
        assertEquals("Hello;World;Kotlin", altName("Hello", null, "World", "", "Kotlin"))
        assertNull(altName())
        assertNull(altName(null, "", "   "))
    }

    @Test
    fun `altName supports custom separators`() {
        assertEquals("A;B;C", altName("A", "B", "C"))
        assertEquals("A, B, C", altName("A", "B", "C", separator = ", "))
    }

    @Test
    fun `altName list extension works identically`() {
        assertEquals("Oslo;Bergen;Trondheim", listOf("Oslo", null, "Bergen", "", "Trondheim").altName())
        assertNull(emptyList<String>().altName())
        assertEquals("A | B", listOf("A", "B").altName(" | "))
    }
}
