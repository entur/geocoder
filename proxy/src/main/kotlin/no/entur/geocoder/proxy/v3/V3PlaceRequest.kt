package no.entur.geocoder.proxy.v3

import io.ktor.http.*
import no.entur.geocoder.proxy.common.SearchDefaults

data class V3PlaceRequest(
    val ids: List<String>,
    val lang: String = SearchDefaults.LANG,
    val debug: Boolean = false,
) {
    init {
        require(ids.isNotEmpty()) { "Parameter 'ids' is required" }
        require(ids.size <= MAX_IDS) { "Parameter 'ids' accepts at most $MAX_IDS ids" }
    }

    companion object {
        const val MAX_IDS = 100

        internal val ALLOWED_PARAMS = setOf("ids", "lang", "debug")

        fun from(req: Parameters): V3PlaceRequest {
            val unknown = req.names().filterNot { it in ALLOWED_PARAMS }
            require(unknown.isEmpty()) { "Unknown parameter(s): ${unknown.joinToString()}" }

            return V3PlaceRequest(
                ids = req["ids"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                lang = req["lang"] ?: SearchDefaults.LANG,
                debug = req["debug"].toBoolean(),
            )
        }
    }
}
