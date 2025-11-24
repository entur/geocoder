package no.entur.geocoder.converter

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Configuration for all popularity/boost calculations across different data sources.
 * Can be loaded from converter.json or use default values.
 */
data class ConverterConfig(
    val osm: OsmConfig = OsmConfig(),
    val stedsnavn: StedsnavnConfig = StedsnavnConfig(),
    val matrikkel: MatrikkelConfig = MatrikkelConfig(),
    val stopPlace: StopPlaceConfig = StopPlaceConfig(),
    val groupOfStopPlaces: GroupOfStopPlacesConfig = GroupOfStopPlacesConfig(),
    val importance: ImportanceConfig = ImportanceConfig(),
) {
    data class OsmConfig(
        val defaultValue: Double = 1.0,
        val filters: List<POIFilter> = defaultFilters(),
    ) {
        data class POIFilter(
            val key: String,
            val value: String,
            val priority: Int,
        )

        companion object {
            private fun defaultFilters() =
                listOf(
                    POIFilter("amenity", "hospital", 9),
                    POIFilter("amenity", "school", 9),
                    POIFilter("amenity", "university", 9),
                    POIFilter("shop", "mall", 9),
                    POIFilter("amenity", "college", 8),
                    POIFilter("amenity", "place_of_worship", 8),
                    POIFilter("amenity", "exhibition_centre", 8),
                    POIFilter("leisure", "park", 8),
                    POIFilter("leisure", "sports_centre", 8),
                    POIFilter("leisure", "stadium", 8),
                    POIFilter("tourism", "museum", 8),
                    POIFilter("landuse", "winter_sports", 8),
                    POIFilter("amenity", "library", 7),
                    POIFilter("amenity", "kindergarten", 7),
                    POIFilter("amenity", "nursing_home", 7),
                    POIFilter("amenity", "food_court", 7),
                    POIFilter("leisure", "ice_rink", 7),
                    POIFilter("tourism", "hotel", 6),
                    POIFilter("amenity", "cafe", 6),
                    POIFilter("amenity", "restaurant", 6),
                    POIFilter("amenity", "social_facility", 6),
                    POIFilter("natural", "beach", 6),
                    POIFilter("name", "Barcode", 6),
                    POIFilter("tourism", "motel", 5),
                    POIFilter("tourism", "hostel", 5),
                    POIFilter("tourism", "alpine_hut", 5),
                    POIFilter("tourism", "camp_site", 5),
                    POIFilter("tourism", "gallery", 5),
                    POIFilter("tourism", "theme_park", 5),
                    POIFilter("amenity", "community_centre", 5),
                    POIFilter("amenity", "dentist", 5),
                    POIFilter("amenity", "concert_hall", 5),
                    POIFilter("amenity", "events_venue", 5),
                    POIFilter("amenity", "conference_centre", 5),
                    POIFilter("landuse", "cemetery", 5),
                    POIFilter("office", "government", 5),
                    POIFilter("name", "Munch brygge", 5),
                    POIFilter("sport", "karting", 5),
                    POIFilter("sport", "motor", 5),
                    POIFilter("amenity", "embassy", 4),
                    POIFilter("amenity", "clinic", 4),
                    POIFilter("amenity", "golf_course", 4),
                    POIFilter("tourism", "guest_house", 4),
                    POIFilter("amenity", "prison", 3),
                    POIFilter("shop", "furniture", 3),
                    POIFilter("amenity", "doctors", 2),
                    POIFilter("amenity", "police", 2),
                    POIFilter("amenity", "cinema", 1),
                    POIFilter("amenity", "theatre", 1),
                    POIFilter("amenity", "bank", 1),
                    POIFilter("amenity", "courthouse", 1),
                    POIFilter("amenity", "townhall", 1),
                    POIFilter("tourism", "attraction", 1),
                    POIFilter("name", "Entur AS", 1),
                    POIFilter("custom_poi", "festival", 1),
                )
        }
    }

    data class StedsnavnConfig(
        val defaultValue: Double = 40.0,
    )

    data class MatrikkelConfig(
        val addressPopularity: Double = 20.0,
        val streetPopularity: Double = 20.0,
    )

    data class StopPlaceConfig(
        val defaultValue: Int = 50,
        val stopTypeFactors: Map<String, Double> =
            mapOf(
                "busStation" to 2.0,
                "metroStation" to 2.0,
                "railStation" to 2.0,
            ),
        val interchangeFactors: Map<String, Double> =
            mapOf(
                "recommendedInterchange" to 3.0,
                "preferredInterchange" to 10.0,
            ),
    )

    data class GroupOfStopPlacesConfig(
        val gosBoostFactor: Double = 10.0,
    )

    data class ImportanceConfig(
        val minPopularity: Double = 1.0,
        val maxPopularity: Double = 1_000_000_000.0,
        val floor: Double = 0.1,
    )

    companion object {
        private val mapper =
            jacksonObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            }

        /**
         * Load configuration from a file, or return default config if file doesn't exist.
         */
        fun load(configFile: File?): ConverterConfig {
            if (configFile == null || !configFile.exists()) {
                return ConverterConfig()
            }

            return try {
                mapper.readValue<ConverterConfig>(configFile)
            } catch (e: Exception) {
                println("Warning: Failed to parse config file ${configFile.absolutePath}: ${e.message}")
                println("Using default configuration")
                ConverterConfig()
            }
        }
    }
}
