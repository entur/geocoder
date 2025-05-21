package no.entur.netex_to_json

import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppTest {
    @Test
    fun parseThatFile() {
        val app = App()
        val file: InputStream = this::class.java.getResourceAsStream("/StopPlace.xml")
        assertNotNull(app.parseXml(file), "app should have a greeting")
    }
}
