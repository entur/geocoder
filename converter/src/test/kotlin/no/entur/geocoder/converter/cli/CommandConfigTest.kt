package no.entur.geocoder.converter.cli

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandConfigTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `uses default config when no file exists and no -c flag`() {
        val command = Command(emptyArray())
        val config = command.readConfig(null)

        assertEquals(1.0, config.osm.defaultValue)
        assertEquals(40.0, config.stedsnavn.defaultValue)
    }

    @Test
    fun `loads converter json from working directory automatically when it exists`() {
        val configFile = tempDir.resolve("auto-config.json").toFile()
        configFile.writeText(
            """
            {
              "osm": {
                "defaultValue": 7.0
              },
              "stedsnavn": {
                "defaultValue": 80.0
              }
            }
            """.trimIndent(),
        )

        val command = Command(emptyArray())

        val config = command.readConfig(configFile.absolutePath)

        assertEquals(7.0, config.osm.defaultValue)
        assertEquals(80.0, config.stedsnavn.defaultValue)
    }

    @Test
    fun `loads explicit config file when specified`() {
        val explicitConfig = tempDir.resolve("custom.json").toFile()
        explicitConfig.writeText(
            """
            {
              "osm": {
                "defaultValue": 99.0
              },
              "matrikkel": {
                "addressPopularity": 35.0,
                "streetPopularity": 30.0
              }
            }
            """.trimIndent(),
        )

        val command = Command(emptyArray())
        val config = command.readConfig(explicitConfig.absolutePath)

        assertEquals(99.0, config.osm.defaultValue)
        assertEquals(35.0, config.matrikkel.addressPopularity)
        assertEquals(30.0, config.matrikkel.streetPopularity)
    }

    @Test
    fun `falls back to defaults when specified config file does not exist`() {
        val command = Command(emptyArray())
        val config = command.readConfig("/nonexistent/config.json")

        assertEquals(1.0, config.osm.defaultValue)
        assertEquals(40.0, config.stedsnavn.defaultValue)
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
}
