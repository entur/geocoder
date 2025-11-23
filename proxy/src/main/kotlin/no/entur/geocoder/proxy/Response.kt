package no.entur.geocoder.proxy

import io.ktor.http.HttpStatusCode

data class Response(val message: Any, val status: HttpStatusCode = HttpStatusCode.OK)
