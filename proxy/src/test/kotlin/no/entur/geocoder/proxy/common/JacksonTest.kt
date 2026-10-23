package no.entur.geocoder.proxy.common

import no.entur.geocoder.proxy.common.Jackson.jacksonMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals

class JacksonTest {
    data class Sample(val b: Int, val a: Int, val c: String? = null, val m: Map<String, String?> = emptyMap()) {
        val z = 0
        val y = 0
    }

    @Test
    fun `keeps declaration order and drops nulls`() {
        val json = jacksonMapper.writeValueAsString(Sample(b = 1, a = 2, m = mapOf("x" to null, "y" to "z")))
        assertEquals("""{"b":1,"a":2,"m":{"y":"z"},"z":0,"y":0}""", json)
    }

    @Test
    fun `ignores unknown properties`() {
        assertEquals(Sample(b = 1, a = 2), jacksonMapper.readValue<Sample>("""{"b":1,"a":2,"new":true}"""))
    }
}
