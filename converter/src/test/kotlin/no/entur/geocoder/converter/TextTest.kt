package no.entur.geocoder.converter

import no.entur.geocoder.converter.Text.joinOsmValuesToString
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class TextTest {
    @Test
    fun `altName filters null and blank values`() {
        assertEquals("Hello;World;Kotlin", setOf("Hello", "World", "", "Kotlin").joinOsmValuesToString())
    }
}
