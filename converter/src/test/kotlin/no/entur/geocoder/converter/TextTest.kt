package no.entur.geocoder.converter

import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class TextTest {
    @Test
    fun testAltNameVarargs() {
        val result1 = Text.altName("Hello", null, "World", "", "Kotlin")
        assertEquals("Hello;World;Kotlin", result1)

        val result2 = Text.altName(null, "", "   ")
        assertNull(result2)

        val result3 = Text.altName("SingleValue")
        assertEquals("SingleValue", result3)
    }
}
