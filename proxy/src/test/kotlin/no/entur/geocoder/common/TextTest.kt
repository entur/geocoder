package no.entur.geocoder.common

import no.entur.geocoder.common.Text.joinOsmValuesToString
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class TextTest {
    @Test
    fun `altName filters null and blank values`() {
        Assertions.assertEquals("Hello;World;Kotlin", setOf("Hello", "World", "", "Kotlin").joinOsmValuesToString())
    }
}
