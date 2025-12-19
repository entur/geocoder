package no.entur.geocoder.converter

import no.entur.geocoder.converter.Text.createAltNameList
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class TextTest {
    @Test
    fun `altName filters null and blank values`() {
        assertEquals("Hello;World;Kotlin", createAltNameList("Hello", null, "Foo", "World", "", "Kotlin", skip = "Foo"))
        assertNull(createAltNameList())
        assertNull(createAltNameList(null, "", "   "))
    }

    @Test
    fun `altName list extension works identically`() {
        assertEquals("Oslo;Bergen;Trondheim", listOf("Oslo", null, "Bergen", "", "Trondheim").createAltNameList())
        assertNull(emptyList<String>().createAltNameList())
        assertEquals("A;B", listOf("A", "B").createAltNameList())
    }
}
