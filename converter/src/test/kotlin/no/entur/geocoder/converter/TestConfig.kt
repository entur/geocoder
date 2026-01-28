package no.entur.geocoder.converter

import java.io.File

/**
 * Test utility to load the converter config from the project's converter.json file.
 * This ensures tests use the same configuration as production.
 */
object TestConfig {
    val config: ConverterConfig by lazy {
        val configFile = File("converter.json")
        require(configFile.exists()) {
            "converter.json not found. Tests must be run from the converter module directory."
        }
        ConverterConfig.load(configFile)
    }
}
