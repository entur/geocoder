package no.entur.geocoder.proxy.metrics

import io.ktor.http.HttpStatusCode
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.config.MeterFilter

internal const val REQUEST_METRIC = "http.server.requests"

// Drops Ktor's own labels; uri and exception values are Spring Boot's, see App.configureApp
internal val springBootTags =
    object : MeterFilter {
        override fun map(id: Meter.Id): Meter.Id {
            if (id.name != REQUEST_METRIC) return id
            val tags =
                id.tags.mapNotNull { tag ->
                    when (tag.key) {
                        // address is our own host and port, throwable is replaced by exception
                        "address", "throwable" -> null
                        // "n/a" is an unmatched route, as long as distinctNotRegisteredRoutes stays off
                        "route" -> Tag.of("uri", if (tag.value == "n/a") "NOT_FOUND" else tag.value)
                        else -> tag
                    }
                }
            return id.replaceTags(tags)
        }
    }

internal fun outcome(status: HttpStatusCode?) =
    when (status?.value) {
        null -> "UNKNOWN"
        in 100..199 -> "INFORMATIONAL"
        in 200..299 -> "SUCCESS"
        in 300..399 -> "REDIRECTION"
        in 400..499 -> "CLIENT_ERROR"
        else -> "SERVER_ERROR"
    }

// simpleName is null for anonymous and local classes, and "none" must only mean "no exception"
internal fun exceptionName(cause: Throwable?) = cause?.let { it::class.simpleName ?: "unknown" } ?: "none"
