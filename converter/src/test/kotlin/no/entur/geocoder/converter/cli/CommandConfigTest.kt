package no.entur.geocoder.converter.cli

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CommandConfigTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads converter json from working directory when no -c flag`() {
        // This test assumes converter.json exists in the working directory (converter module)
        val command = Command(emptyArray())
        val config = command.readConfig(null)

        // Just verify it loaded successfully - values come from converter.json
        assert(config.osm.filters.isNotEmpty()) { "Should have loaded OSM filters from converter.json" }
    }

    @Test
    fun `loads explicit config file when specified`() {
        val explicitConfig = tempDir.resolve("custom.json").toFile()
        explicitConfig.writeText(FULL_CONFIG_JSON)

        val command = Command(emptyArray())
        val config = command.readConfig(explicitConfig.absolutePath)

        assertEquals(99.0, config.osm.defaultValue)
        assertEquals(35.0, config.matrikkel.addressPopularity)
        assertEquals(30.0, config.matrikkel.streetPopularity)
    }

    @Test
    fun `throws when specified config file does not exist`() {
        val command = Command(emptyArray())
        assertFailsWith<IllegalArgumentException> {
            command.readConfig("/nonexistent/config.json")
        }
    }

    @Test
    fun `verifies converter json exists in project for integration use`() {
        val userDir = File(System.getProperty("user.dir"))

        val converterJson =
            if (userDir.name == "converter") {
                File(userDir, "converter.json")
            } else {
                File(userDir, "converter/converter.json")
            }

        assert(converterJson.exists()) {
            "converter.json should exist at ${converterJson.absolutePath} for automatic loading"
        }
    }

    companion object {
        private val FULL_CONFIG_JSON =
            """
            {
              "osm": {
                "defaultValue": 99.0,
                "rankAddress": {
                  "boundary": 10,
                  "place": 20,
                  "road": 26,
                  "building": 28,
                  "poi": 30
                },
                "filters": []
              },
              "stedsnavn": {
                "defaultValue": 40.0,
                "rankAddress": 16
              },
              "matrikkel": {
                "addressPopularity": 35.0,
                "streetPopularity": 30.0,
                "rankAddress": 26
              },
              "poi": {
                "importance": 0.5,
                "rankAddress": 30
              },
              "stopPlace": {
                "defaultValue": 50,
                "rankAddress": 30,
                "stopTypeFactors": {},
                "interchangeFactors": {}
              },
              "groupOfStopPlaces": {
                "gosBoostFactor": 10.0,
                "rankAddress": 30
              },
              "importance": {
                "minPopularity": 1.0,
                "maxPopularity": 1000000000.0,
                "floor": 0.1
              }
            }
            """.trimIndent()
    }
}
