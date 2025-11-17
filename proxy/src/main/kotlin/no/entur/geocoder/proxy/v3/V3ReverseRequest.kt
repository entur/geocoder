package no.entur.geocoder.proxy.v3

import io.ktor.server.application.*

data class V3ReverseRequest(
    val lat: Double,
    val lon: Double,
    val radius: Double? = null,
    val limit: Int = 10,
    val language: String = "no",
) {
    init {
        require(lat.toDouble() in -90.0..90.0) { "Parameter 'latitude' must be between -90 and 90" }
        require(lon.toDouble() in -180.0..180.0) { "Parameter 'longitude' must be between -180 and 180" }
    }

    companion object {
        fun ApplicationCall.v3ReverseRequest(): V3ReverseRequest {
            val params = request.queryParameters
            return V3ReverseRequest(
                lat = params["latitude"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'latitude' is required"),
                lon =
                    params["longitude"]?.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Parameter 'longitude' is required"),
                radius = params["radius"]?.toDoubleOrNull() ?: 10.0,
                limit = params["limit"]?.toIntOrNull() ?: 10,
                language = params["language"] ?: params["lang"] ?: "no",
            )
        }
    }
}
