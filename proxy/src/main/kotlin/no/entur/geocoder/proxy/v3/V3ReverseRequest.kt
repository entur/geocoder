package no.entur.geocoder.proxy.v3

import io.ktor.http.*

data class V3ReverseRequest(
    val lat: Double,
    val lon: Double,
    val radius: Double? = null,
    val limit: Int = 10,
    val lang: String = "no",
    val layers: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val multimodal: String = "parent",
) {
    init {
        require(lat in -90.0..90.0) { "Parameter 'lat' must be between -90 and 90" }
        require(lon in -180.0..180.0) { "Parameter 'lon' must be between -180 and 180" }
    }

    companion object {
        internal val ALLOWED_PARAMS =
            setOf(
                "lat", "lon", "radius", "limit", "lang",
                "layers", "sources", "multimodal",
            )

        fun from(req: Parameters): V3ReverseRequest {
            val unknown = req.names().filterNot { it in ALLOWED_PARAMS }
            require(unknown.isEmpty()) { "Unknown parameter(s): ${unknown.joinToString()}" }

            return V3ReverseRequest(
                lat = req["lat"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Parameter 'lat' is required"),
                lon =
                    req["lon"]?.toDoubleOrNull()
                        ?: throw IllegalArgumentException("Parameter 'lon' is required"),
                radius = req["radius"]?.toDoubleOrNull(),
                limit = req["limit"]?.toIntOrNull() ?: 10,
                lang = req["lang"] ?: "no",
                layers = req["layers"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                sources = req["sources"]?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                multimodal = req["multimodal"] ?: "parent",
            )
        }
    }
}
