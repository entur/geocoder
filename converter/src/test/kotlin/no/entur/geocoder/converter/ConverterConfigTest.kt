package no.entur.geocoder.converter

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConverterConfigTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `loads default config when file is null`() {
        val config = ConverterConfig.load(null)

        assertNotNull(config)
        assertEquals(1.0, config.osm.defaultValue)
        assertEquals(40.0, config.stedsnavn.defaultValue)
        assertEquals(20.0, config.matrikkel.addressPopularity)
        assertEquals(20.0, config.matrikkel.streetPopularity)
        assertEquals(50, config.stopPlace.defaultValue)
        assertEquals(10.0, config.groupOfStopPlaces.gosBoostFactor)
        assertEquals(1.0, config.importance.minPopularity)
        assertEquals(1_000_000_000.0, config.importance.maxPopularity)
        assertEquals(0.1, config.importance.floor)
    }

    @Test
    fun `loads default config when file does not exist`() {
        val nonExistentFile = tempDir.resolve("nonexistent.json").toFile()
        val config = ConverterConfig.load(nonExistentFile)

        assertNotNull(config)
        assertEquals(1.0, config.osm.defaultValue)
        assertEquals(40.0, config.stedsnavn.defaultValue)
    }

    @Test
    fun `loads config from valid JSON file`() {
        val configFile = tempDir.resolve("test-config.json").toFile()
        configFile.writeText(
            """
            {
              "osm": {
                "defaultValue": 2.0,
                "filters": [
                  {"key": "amenity", "value": "hospital", "priority": 9}
                ]
              },
              "stedsnavn": {
                "defaultValue": 50.0
              },
              "matrikkel": {
                "addressPopularity": 30.0,
                "streetPopularity": 25.0
              },
              "stopPlace": {
                "defaultValue": 60,
                "stopTypeFactors": {
                  "busStation": 3.0
                },
                "interchangeFactors": {
                  "recommendedInterchange": 4.0
                }
              },
              "groupOfStopPlaces": {
                "gosBoostFactor": 15.0
              },
              "importance": {
                "minPopularity": 2.0,
                "maxPopularity": 500000000.0,
                "floor": 0.2
              }
            }
            """.trimIndent(),
        )

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
    fun `uses defaults for missing config sections`() {
        val configFile = tempDir.resolve("partial-config.json").toFile()
        configFile.writeText(
            """
            {
              "osm": {
                "defaultValue": 3.0
              }
            }
            """.trimIndent(),
        )

        val config = ConverterConfig.load(configFile)

        // Custom osm value
        assertEquals(3.0, config.osm.defaultValue)

        // Default values for other sections
        assertEquals(40.0, config.stedsnavn.defaultValue)
        assertEquals(20.0, config.matrikkel.addressPopularity)
        assertEquals(50, config.stopPlace.defaultValue)
    }

    @Test
    fun `fallback to defaults on malformed JSON`() {
        val configFile = tempDir.resolve("malformed.json").toFile()
        configFile.writeText("{ invalid json }")

        val config = ConverterConfig.load(configFile)

        // Should fall back to defaults
        assertEquals(1.0, config.osm.defaultValue)
        assertEquals(40.0, config.stedsnavn.defaultValue)
    }

    @Test
    fun `OSM filters are loaded correctly`() {
        val config = ConverterConfig.load(null)

        assertTrue(config.osm.filters.isNotEmpty(), "Should have default filters")
        assertTrue(
            config.osm.filters.any { it.key == "amenity" && it.value == "hospital" },
            "Should include hospital filter",
        )
        assertTrue(
            config.osm.filters.any { it.key == "amenity" && it.value == "restaurant" },
            "Should include restaurant filter",
        )
    }

    @Test
    fun `config is used by popularity calculators`() {
        val configFile = tempDir.resolve("calculator-test.json").toFile()
        configFile.writeText(
            """
            {
              "osm": {
                "defaultValue": 5.0,
                "filters": [
                  {"key": "amenity", "value": "hospital", "priority": 8}
                ]
              },
              "stedsnavn": {
                "defaultValue": 100.0
              },
              "matrikkel": {
                "addressPopularity": 50.0,
                "streetPopularity": 45.0
              }
            }
            """.trimIndent(),
        )

        val config = ConverterConfig.load(configFile)

        // Test OSM popularity calculator
        val osmCalc =
            no.entur.geocoder.converter.source.osm
                .OSMPopularityCalculator(config.osm)
        val hospitalPop = osmCalc.calculatePopularity(mapOf("amenity" to "hospital"))
        assertEquals(40.0, hospitalPop) // 5.0 * 8 = 40.0

        // Test Stedsnavn popularity calculator
        val stedsnavnCalc =
            no.entur.geocoder.converter.source.stedsnavn
                .StedsnavnPopularityCalculator(config.stedsnavn)
        assertEquals(100.0, stedsnavnCalc.calculatePopularity())

        // Test Matrikkel popularity calculator
        val matrikkelCalc =
            no.entur.geocoder.converter.source.adresse
                .MatrikkelPopularityCalculator(config.matrikkel)
        assertEquals(50.0, matrikkelCalc.calculateAddressPopularity())
        assertEquals(45.0, matrikkelCalc.calculateStreetPopularity())
    }
}
