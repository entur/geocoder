package no.entur.geocoder.proxy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.entur.geocoder.proxy.Routing.configureRouting
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class ProxyTest {
    private val samplePhotonResponse =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Point",
                "coordinates": [10.752200, 59.913900]
              },
              "properties": {
                "name": "Oslo",
                "county": "Oslo",
                "label": "Oslo,Norway",
                "extra": {
                  "id": "1",
                  "gid": "osm:1",
                  "layer": "city",
                  "source": "osm",
                  "source_id": "1",
                  "accuracy": "centre",
                  "country_a": "NOR",
                  "county_gid": "county:1",
                  "locality": "Oslo",
                  "locality_gid": "locality:1",
                  "label": "Oslo, Norway",
                  "transport_modes": "city,transport",
                  "tariff_zones": "zone1,zone2"
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
                    assertEquals("test_query", request.url.parameters["q"])
                    assertEquals("5", request.url.parameters["limit"])
                    assertEquals("en", request.url.parameters["lang"])

                    respond(
                        content = samplePhotonResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            application {
                configureRouting(
                    client = HttpClient(mockEngine),
                    transformer = FeatureTransformer(),
                    photonBaseUrl = "http://photon-test",
                )
            }

            val response =
                client.get("/v1/autocomplete") {
                    parameter("text", "test_query")
                    parameter("size", "5")
                    parameter("lang", "en")
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val collection: FeatureCollection = jacksonObjectMapper().readValue(response.bodyAsText())
            assertEquals(1, collection.features.size)

            val feature = collection.features[0]
            assertEquals(listOf(BigDecimal("10.752200"), BigDecimal("59.913900")), feature.geometry.coordinates)
            assertEquals("1", feature.properties.id)
            assertEquals("city", feature.properties.layer)
            assertEquals("Oslo", feature.properties.name)
            assertEquals("Oslo", feature.properties.county)
            assertEquals("Oslo, Norway", feature.properties.label)
            assertEquals(listOf("city", "transport"), feature.properties.category)
            assertEquals(listOf("zone1", "zone2"), feature.properties.tariff_zones)
            assertEquals(null, feature.properties.extra)
        }

    @Test
    fun `test reverse endpoint`() =
        testApplication {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("59.9139", request.url.parameters["lat"])
                    assertEquals("10.7522", request.url.parameters["lon"])
                    assertEquals("100", request.url.parameters["radius"])
                    assertEquals("3", request.url.parameters["limit"])
                    assertEquals("no", request.url.parameters["lang"])

                    respond(
                        content = samplePhotonResponse,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            application {
                configureRouting(
                    client = HttpClient(mockEngine),
                    transformer = FeatureTransformer(),
                    photonBaseUrl = "http://photon-test",
                )
            }

            val response =
                client.get("/v1/reverse") {
                    parameter("point.lat", "59.9139")
                    parameter("point.lon", "10.7522")
                    parameter("boundary.circle.radius", "100")
                    parameter("size", "3")
                    parameter("lang", "no")
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val collection: FeatureCollection = jacksonObjectMapper().readValue(response.bodyAsText())
            assertEquals(1, collection.features.size)
            assertEquals(
                listOf(BigDecimal("10.752200"), BigDecimal("59.913900")),
                collection.features[0].geometry.coordinates,
            )
        }
}
