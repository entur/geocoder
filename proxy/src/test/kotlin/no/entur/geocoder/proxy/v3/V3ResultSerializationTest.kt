package no.entur.geocoder.proxy.v3

import no.entur.geocoder.proxy.common.JsonMapper
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the v3 wire format. Jackson derives JSON keys from Kotlin property names,
 * so a rename in the data classes silently changes the API contract - these
 * assertions catch that.
 */
class V3ResultSerializationTest {
    @Test
    fun `query echo uses request parameter names`() {
        val json =
            JsonMapper.jacksonMapper.writeValueAsString(
                V3Result.QueryInfo(lat = 59.911, lon = 10.752, limit = 10, lang = "no"),
            )
        assertTrue("\"lat\":" in json, json)
        assertTrue("\"lon\":" in json, json)
        assertTrue("\"lang\":" in json, json)
        assertFalse("\"latitude\"" in json, json)
        assertFalse("\"longitude\"" in json, json)
        assertFalse("\"language\"" in json, json)
    }

    @Test
    fun `place serializes names (plural) not name`() {
        val json =
            JsonMapper.jacksonMapper.writeValueAsString(
                V3Result.Place(
                    id = "NSR:StopPlace:337",
                    names = V3Result.Names(default = "Nationaltheatret", display = "Nationaltheatret, Oslo"),
                    layer = V3Result.Layer.stopPlace,
                    source = "nsr",
                ),
            )
        assertTrue("\"names\":" in json, json)
        assertFalse("\"name\":" in json, json)
    }
}
