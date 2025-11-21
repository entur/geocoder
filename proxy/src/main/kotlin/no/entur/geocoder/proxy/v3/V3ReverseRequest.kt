package no.entur.geocoder.proxy.v3

import io.ktor.http.*

data class V3ReverseRequest(
    val lat: Double,
    val lon: Double,
    val radius: Double? = null,
    val limit: Int = 10,
    val language: String = "no",
) {
    init {
        require(lat in -90.0..90.0) { "Parameter 'latitude' must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Parameter 'longitude' must be between -180 and 180" }
    }

    companion object {
        fun from(req: Parameters) =
            V3ReverseRequest(
                lat = req["latitude"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'latitude' is required"),
                lon =
                    req["longitude"]?.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Parameter 'longitude' is required"),
                radius = req["radius"]?.toDoubleOrNull() ?: 10.0,
                limit = req["limit"]?.toIntOrNull() ?: 10,
                language = req["language"] ?: req["lang"] ?: "no",
            )
    }
}
