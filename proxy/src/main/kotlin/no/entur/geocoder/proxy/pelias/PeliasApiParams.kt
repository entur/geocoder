package no.entur.geocoder.proxy.pelias

import io.ktor.server.request.*

data class FocusParams(
    val lat: String,
    val lon: String,
    val scale: String?,
    val weight: String?,
    val function: String?
) {
    init {
        require(lat.isNotBlank()) { "Parameter 'point.lat' is required" }
        require(lon.isNotBlank()) { "Parameter 'point.lon' is required" }
        require(lat.toDoubleOrNull() != null) { "Parameter 'point.lat' must be a valid number" }
        require(lon.toDoubleOrNull() != null) { "Parameter 'point.lon' must be a valid number" }
        val latitude = lat.toDouble()
        val longitude = lon.toDouble()
        require(latitude in -90.0..90.0) { "Parameter 'point.lat' must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Parameter 'point.lon' must be between -180 and 180" }
        if (scale != null) {
            val scaleValue = scale.split("km")[0]
            require(
                scale.endsWith("km")
                        && scaleValue.toIntOrNull() != null
                        && scaleValue.toInt() > 0
            ) { "Parameter 'scale' must be a number > 0 followed by 'km'" }
        }
        if (weight != null) {
            val weightValue = weight.toDouble()
            require(weightValue > 0) { "Parameter 'weight' must be a number > 0" }
        }
        if (function != null) {
            require(function == "linear" || function == "exp") { "Parameter 'function' must be 'linear' or 'exp'"}
        }
    }
}

data class PeliasAutocompleteParams(
    val text: String = "",
    val size: Int = 10,
    val lang: String = "no",
    val boundaryCountry: String? = null,
    val boundaryCountyIds: List<String> = emptyList(),
    val boundaryLocalityIds: List<String> = emptyList(),
    val tariffZones: List<String> = emptyList(),
    val tariffZoneAuthorities: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val layers: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val focus: FocusParams? = null,
    val multiModal: String,
) {
    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasAutocompleteParams {
            val params = request.queryParameters
            return PeliasAutocompleteParams(
                text = params["text"] ?: "",
                size = params["size"]?.toIntOrNull() ?: 10,
                lang = params["lang"] ?: "no",
                boundaryCountry = params["boundary.country"],
                boundaryCountyIds = params["boundary.county.ids"]?.split(",") ?: emptyList(),
                boundaryLocalityIds = params["boundary.locality.ids"]?.split(",") ?: emptyList(),
                tariffZones = params["tariff_zone_ids"]?.split(",") ?: emptyList(),
                tariffZoneAuthorities = params["tariff_zone_authorities"]?.split(",") ?: emptyList(),
                sources = params["sources"]?.split(",") ?: emptyList(),
                layers = params["layers"]?.split(",") ?: emptyList(),
                categories = params["transport_mode"]?.split(",") ?: emptyList(),
                focus =
                    params["focus.point.lat"]?.let { lat ->
                        params["focus.point.lon"]?.let { lon ->
                            FocusParams(lat, lon, params["focus.scale"], params["focus.weight"], params["focus.function"])
                        }
                    },
                multiModal =
                    when (params["multiModal"]) {
                        "child" -> "child"
                        "parent" -> "parent"
                        "both" -> "both"
                        else -> "parent"
                    },
            )
        }
    }
}

data class PeliasReverseParams(
    val lat: String = "",
    val lon: String = "",
    val radius: String? = null,
    val size: Int = 10,
    val lang: String = "no",
) {
    init {
        require(lat.isNotBlank()) { "Parameter 'point.lat' is required" }
        require(lon.isNotBlank()) { "Parameter 'point.lon' is required" }
        require(lat.toDoubleOrNull() != null) { "Parameter 'point.lat' must be a valid number" }
        require(lon.toDoubleOrNull() != null) { "Parameter 'point.lon' must be a valid number" }
        val latitude = lat.toDouble()
        val longitude = lon.toDouble()
        require(latitude in -90.0..90.0) { "Parameter 'point.lat' must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Parameter 'point.lon' must be between -180 and 180" }
    }

    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasReverseParams {
            val params = request.queryParameters
            return PeliasReverseParams(
                lat = params["point.lat"] ?: "",
                lon = params["point.lon"] ?: "",
                radius = params["boundary.circle.radius"],
                size = params["size"]?.toIntOrNull() ?: 10,
                lang = params["lang"] ?: "no",
            )
        }
    }
}

data class PeliasPlaceParams(val ids: List<String> = emptyList()) {
    init {
        require(ids.isNotEmpty()) { "Parameter 'ids' is required" }
        require(ids.all { it.split(":").size == 3 }) { "id must be colon separated" }
    }

    companion object {
        fun fromRequest(request: ApplicationRequest): PeliasPlaceParams {
            val params = request.queryParameters
            return PeliasPlaceParams(
                ids = params["ids"]
                    ?.split(",")
                    ?.map { it.split(":").takeLast(3).joinToString(":") }
                    ?: emptyList(),
            )
        }
    }
}
