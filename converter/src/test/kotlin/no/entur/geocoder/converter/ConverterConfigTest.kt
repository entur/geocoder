package no.entur.geocoder.converter

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConverterConfigTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `throws when config file is null`() {
        assertFailsWith<IllegalArgumentException> {
            ConverterConfig.load(null)
        }
    }

    @Test
    fun `throws when config file does not exist`() {
        val nonExistentFile = tempDir.resolve("nonexistent.json").toFile()
        assertFailsWith<IllegalArgumentException> {
            ConverterConfig.load(nonExistentFile)
        }
    }

    @Test
    fun `loads config from valid JSON file`() {
        val configFile = tempDir.resolve("test-config.json").toFile()
        configFile.writeText(FULL_CONFIG_JSON)

        val config = ConverterConfig.load(configFile)

        assertEquals(2.0, config.osm.defaultValue)
        assertEquals(1, config.osm.filters.size)
        assertEquals("amenity", config.osm.filters[0].key)
        assertEquals("hospital", config.osm.filters[0].value)
        assertEquals(9, config.osm.filters[0].priority)

        assertEquals(50.0, config.stedsnavn.defaultValue)
        assertEquals(30.0, config.matrikkel.addressPopularity)
        assertEquals(25.0, config.matrikkel.streetPopularity)
        assertEquals(60, config.stopPlace.defaultValue)
        assertEquals(3.0, config.stopPlace.stopTypeFactors["busStation"])
        assertEquals(4.0, config.stopPlace.interchangeFactors["recommendedInterchange"])
        assertEquals(15.0, config.groupOfStopPlaces.gosBoostFactor)
        assertEquals(2.0, config.importance.minPopularity)
        assertEquals(500000000.0, config.importance.maxPopularity)
        assertEquals(0.2, config.importance.floor)
    }

    @Test
    fun `throws on missing required section`() {
        val configFile = tempDir.resolve("partial-config.json").toFile()
        configFile.writeText(
            """
            {
              "osm": {
                "defaultValue": 3.0,
                "rankAddress": {"boundary": 10, "place": 20, "road": 26, "building": 28, "poi": 30},
                "filters": []
              }
            }
            """.trimIndent(),
        )

        assertFailsWith<Exception> {
            ConverterConfig.load(configFile)
        }
    }

    @Test
    fun `throws on malformed JSON`() {
        val configFile = tempDir.resolve("malformed.json").toFile()
        configFile.writeText("{ invalid json }")

        assertFailsWith<Exception> {
            ConverterConfig.load(configFile)
        }
    }

    @Test
    fun `throws on unknown property in JSON (ensures all JSON values are used)`() {
        val configFile = tempDir.resolve("unknown-prop.json").toFile()
        configFile.writeText(
            FULL_CONFIG_JSON.replace(
                "\"osm\":",
                "\"unknownSection\": {}, \"osm\":",
            ),
        )

        assertFailsWith<Exception> {
            ConverterConfig.load(configFile)
        }
    }

    @Test
    fun `loads rankAddress values from JSON`() {
        val configFile = tempDir.resolve("rankaddress-config.json").toFile()
        configFile.writeText(FULL_CONFIG_JSON)

        val config = ConverterConfig.load(configFile)

        // OSM rank address
        assertEquals(5, config.osm.rankAddress.boundary)
        assertEquals(15, config.osm.rankAddress.place)
        assertEquals(22, config.osm.rankAddress.road)
        assertEquals(25, config.osm.rankAddress.building)
        assertEquals(28, config.osm.rankAddress.poi)

        // Other converter rank address
        assertEquals(12, config.stedsnavn.rankAddress)
        assertEquals(24, config.matrikkel.rankAddress)
        assertEquals(0.6, config.poi.importance)
        assertEquals(29, config.poi.rankAddress)
        assertEquals(28, config.stopPlace.rankAddress)
        assertEquals(27, config.groupOfStopPlaces.rankAddress)
    }

    @Test
    fun `loads actual converter json successfully`() {
        val config = TestConfig.config

        assertNotNull(config)
        assertTrue(config.osm.filters.isNotEmpty(), "Should have filters")
        assertTrue(
            config.osm.filters.any { it.key == "amenity" && it.value == "hospital" },
            "Should include hospital filter",
        )
    }

    companion object {
        private val FULL_CONFIG_JSON =
            """
            {
              "osm": {
                "defaultValue": 2.0,
                "rankAddress": {
                  "boundary": 5,
                  "place": 15,
                  "road": 22,
                  "building": 25,
                  "poi": 28
                },
                "filters": [
                  {"key": "amenity", "value": "hospital", "priority": 9}
                ]
              },
              "stedsnavn": {
                "defaultValue": 50.0,
                "rankAddress": 12
              },
              "matrikkel": {
                "addressPopularity": 30.0,
                "streetPopularity": 25.0,
                "rankAddress": 24
              },
              "poi": {
                "importance": 0.6,
                "rankAddress": 29
              },
              "stopPlace": {
                "defaultValue": 60,
                "rankAddress": 28,
                "stopTypeFactors": {
                  "busStation": 3.0
                },
                "interchangeFactors": {
                  "recommendedInterchange": 4.0
                }
              },
              "groupOfStopPlaces": {
                "gosBoostFactor": 15.0,
                "rankAddress": 27
              },
              "importance": {
                "minPopularity": 2.0,
                "maxPopularity": 500000000.0,
                "floor": 0.2
              }
            }
            """.trimIndent()
    }
}
