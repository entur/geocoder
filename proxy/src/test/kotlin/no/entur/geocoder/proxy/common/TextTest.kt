package no.entur.geocoder.proxy.common

import no.entur.geocoder.proxy.common.Text.joinOsmValuesToString
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class TextTest {
    @Test
    fun `altName filters null and blank values`() {
        Assertions.assertEquals("Hello;World;Kotlin", setOf("Hello", "World", "", "Kotlin").joinOsmValuesToString())
    }
}
