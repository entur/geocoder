package no.entur.geocoder.proxy

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.testing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_ADDRESS
import no.entur.geocoder.common.Category.LEGACY_SOURCE_OPENSTREETMAP
import no.entur.geocoder.proxy.Routing.configureRouting
import no.entur.geocoder.proxy.pelias.PeliasResult
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest.Companion.CITY_AND_GOSP_LIST_HEADROOM
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class ProxyTest {
    private val samplePhotonResponse =
        """
        {
          "type": "PhotonResult",
          "features": [
            {
              "type": "PhotonFeature",
              "geometry": {
                "type": "Point",
                "coordinates": [10.752200, 59.913900]
              },
              "properties": {
                "name": "Oslo",
                "county": "Oslo",
                "extra": {
                  "id": "1",
                  "source": "openstreetmap",
                  "accuracy": "centre",
                  "country_a": "NOR",
                  "county_gid": "county:1",
                  "locality": "Oslo",
                  "locality_gid": "locality:1",
                  "transport_modes": "city,transport",
                  "tariff_zones": "zone1,zone2",
                  "tags": "$LEGACY_SOURCE_OPENSTREETMAP,$LEGACY_LAYER_ADDRESS,${LEGACY_CATEGORY_PREFIX}poi,${LEGACY_CATEGORY_PREFIX}transport,${LEGACY_CATEGORY_PREFIX}city"
                }
              }
            }
          ]
        }
        """.trimIndent()

    @Test
    fun `test autocomplete endpoint`() =
        testApplication {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("Test_query", request.url.parameters["q"])
                    assertEquals("${5 + CITY_AND_GOSP_LIST_HEADROOM}", request.url.parameters["limit"])
                    assertEquals("en", request.url.parameters["lang"])
                    assertEquals("59", request.url.parameters["lat"])
                    assertEquals("10", request.url.parameters["lon"])

                    respond(
                        content = samplePhotonResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, Json.toString()),
                    )
                }

            application {
                install(ContentNegotiation) {
                    jackson {
                        setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                    }
                }
                configureRouting(
                    client = HttpClient(mockEngine),
                    photonBaseUrl = "http://photon-test",
                    appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                )
            }

            val response =
                client.get("/v2/autocomplete") {
                    parameter("text", "test_query")
                    parameter("size", "5")
                    parameter("lang", "en")
                    parameter("focus.point.lat", "59")
                    parameter("focus.point.lon", "10")
                    header(HttpHeaders.Accept, Json.toString())
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val collection: PeliasResult = jacksonObjectMapper().readValue(response.bodyAsText())
            assertEquals(1, collection.features.size)

            val feature = collection.features[0]
            assertEquals(listOf(BigDecimal("10.752200"), BigDecimal("59.913900")), feature.geometry.coordinates)
            assertEquals("1", feature.properties.id)
            assertEquals("address", feature.properties.layer)
            assertEquals("Oslo", feature.properties.name)
            assertEquals("Oslo", feature.properties.county)
            assertEquals("Oslo", feature.properties.label)
            assertEquals(setOf("city", "transport", "poi").toSet(), feature.properties.category?.toSet())
            assertEquals(listOf("zone1", "zone2"), feature.properties.tariff_zones)
        }

    @Test
    fun `test reverse endpoint`() =
        testApplication {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("59.9139", request.url.parameters["lat"])
                    assertEquals("10.7522", request.url.parameters["lon"])
                    assertEquals("100.0", request.url.parameters["radius"])
                    assertEquals("3", request.url.parameters["limit"])
                    assertEquals("no", request.url.parameters["lang"])

                    respond(
                        content = samplePhotonResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, Json.toString()),
                    )
                }

            application {
                install(ContentNegotiation) {
                    jackson {
                        setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                    }
                }
                configureRouting(
                    client = HttpClient(mockEngine),
                    photonBaseUrl = "http://photon-test",
                    appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                )
            }

            val response =
                client.get("/v2/reverse") {
                    parameter("point.lat", "59.9139")
                    parameter("point.lon", "10.7522")
                    parameter("boundary.circle.radius", "100")
                    parameter("size", "3")
                    parameter("lang", "no")
                    header(HttpHeaders.Accept, Json.toString())
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val collection: PeliasResult = jacksonObjectMapper().readValue(response.bodyAsText())
            assertEquals(1, collection.features.size)
            assertEquals(
                listOf(BigDecimal("10.752200"), BigDecimal("59.913900")),
                collection.features[0].geometry.coordinates,
            )
        }
}
