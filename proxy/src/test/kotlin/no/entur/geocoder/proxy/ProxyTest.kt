package no.entur.geocoder.proxy

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.server.testing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import no.entur.geocoder.common.Category.LEGACY_CATEGORY_PREFIX
import no.entur.geocoder.common.Category.LEGACY_LAYER_ADDRESS
import no.entur.geocoder.common.Category.LEGACY_SOURCE_OPENSTREETMAP
import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.proxy.Proxy.Companion.configureApp
import no.entur.geocoder.proxy.pelias.PeliasResult
import no.entur.geocoder.proxy.photon.PhotonAutocompleteRequest.Companion.RESULT_PRUNING_HEADROOM
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
                    assertEquals("${(5 + RESULT_PRUNING_HEADROOM)}", request.url.parameters["limit"])
                    assertEquals("en", request.url.parameters["lang"])
                    assertEquals("59.0", request.url.parameters["lat"])
                    assertEquals("10.0", request.url.parameters["lon"])

                    respond(
                        content = samplePhotonResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, Json.toString()),
                    )
                }

            application {
                configureApp(
                    client = HttpClient(mockEngine),
                    photonBaseUrl = "http://photon-test",
                    micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
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

            val collection: PeliasResult = jacksonMapper.readValue(response.bodyAsText())
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
            var capturedRequest: HttpRequestData? = null
            val mockEngine =
                MockEngine { request ->
                    capturedRequest = request
                    respond(
                        content = samplePhotonResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, Json.toString()),
                    )
                }

            application {
                configureApp(
                    client = HttpClient(mockEngine),
                    photonBaseUrl = "http://photon-test",
                    micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
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

            val collection: PeliasResult = jacksonMapper.readValue(response.bodyAsText())
            assertEquals(1, collection.features.size)
            assertEquals(
                listOf(BigDecimal("10.752200"), BigDecimal("59.913900")),
                collection.features[0].geometry.coordinates,
            )

            // Assert on captured request parameters
            val params = requireNotNull(capturedRequest).url.parameters
            assertEquals("59.9139", params["lat"])
            assertEquals("10.7522", params["lon"])
            assertEquals("100.0", params["radius"])
            assertEquals("3", params["limit"])
            assertEquals("no", params["lang"])
        }
}
