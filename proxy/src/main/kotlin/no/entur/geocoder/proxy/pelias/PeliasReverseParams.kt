package no.entur.geocoder.proxy.pelias

import io.ktor.server.request.*
import no.entur.geocoder.proxy.Text.safeVar

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
                radius = params["boundary.circle.radius"].safeVar(),
                size = params["size"]?.toIntOrNull() ?: 10,
                lang = params["lang"].safeVar() ?: "no",
            )
        }
    }
}
