package no.entur.geocoder.proxy

import io.ktor.http.HttpStatusCode

data class ProxyResponse(val message: Any, val status: HttpStatusCode = HttpStatusCode.OK)
