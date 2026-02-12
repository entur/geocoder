package no.entur.geocoder.converter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Configuration for all popularity/boost calculations across different data sources.
 * Must be loaded from converter.json - all values are required.
 */
data class ConverterConfig(
    val osm: OsmConfig,
    val stedsnavn: StedsnavnConfig,
    val matrikkel: MatrikkelConfig,
    val lantmateriet: LantmaterietConfig,
    val poi: PoiConfig,
    val stopPlace: StopPlaceConfig,
    val groupOfStopPlaces: GroupOfStopPlacesConfig,
    val importance: ImportanceConfig,
) {
    data class OsmConfig(
        val defaultValue: Double,
        val rankAddress: RankAddress,
        val filters: List<POIFilter>,
    ) {
        data class POIFilter(
            val key: String,
            val value: String,
            val priority: Int,
        )

        data class RankAddress(
            val boundary: Int,
            val place: Int,
            val road: Int,
            val building: Int,
            val poi: Int,
        )
    }

    data class StedsnavnConfig(
        val defaultValue: Double,
        val rankAddress: Int,
    )

    data class MatrikkelConfig(
        val addressPopularity: Double,
        val streetPopularity: Double,
        val rankAddress: Int,
    )

    data class LantmaterietConfig(
        val addressPopularity: Double,
        val streetPopularity: Double,
        val rankAddress: Int,
    )

    data class PoiConfig(
        val importance: Double,
        val rankAddress: Int,
    )

    data class StopPlaceConfig(
        val defaultValue: Int,
        val rankAddress: Int,
        val stopTypeFactors: Map<String, Double>,
        val interchangeFactors: Map<String, Double>,
    )

    data class GroupOfStopPlacesConfig(
        val gosBoostFactor: Double,
        val rankAddress: Int,
    )

    data class ImportanceConfig(
        val minPopularity: Double,
        val maxPopularity: Double,
        val floor: Double,
    )

    companion object {
        private val mapper =
            jacksonObjectMapper().apply {
                // Fail if JSON has unknown properties (ensures all JSON values are used)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            }

        /**
         * Load configuration from a file. All values must be present in the JSON file.
         * @throws IllegalArgumentException if configFile is null or doesn't exist
         * @throws Exception if JSON is invalid or missing required properties
         */
        fun load(configFile: File?): ConverterConfig {
            requireNotNull(configFile) { "Config file is required" }
            require(configFile.exists()) { "Config file does not exist: ${configFile.absolutePath}" }

            return mapper.readValue<ConverterConfig>(configFile)
        }
    }
}
