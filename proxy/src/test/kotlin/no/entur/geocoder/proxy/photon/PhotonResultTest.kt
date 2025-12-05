package no.entur.geocoder.proxy.photon

import io.ktor.http.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PhotonResultTest {
    @Test
    fun `parse creates PhotonResult from valid JSON`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.757933, 59.911491]
                        },
                        "properties": {
                            "name": "Oslo Sentralstasjon",
                            "street": "Jernbanetorget",
                            "housenumber": "1",
                            "postcode": "0154",
                            "city": "Oslo",
                            "county": "Oslo",
                            "countrycode": "NO",
                            "osm_type": "W",
                            "osm_id": 123456789,
                            "osm_key": "railway",
                            "osm_value": "station",
                            "extra": {
                                "id": "W123456789",
                                "source": "osm",
                                "locality": "Oslo",
                                "locality_gid": "0301",
                                "county_gid": "03",
                                "country_a": "NOR",
                                "accuracy": "point",
                                "tags": "legacy.source.osm,legacy.layer.venue,legacy.category.transport"
                            }
                        }
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        assertEquals("FeatureCollection", result.type)
        assertEquals(1, result.features.size)

        val feature = result.features[0]
        assertEquals("Point", feature.geometry.type)
        assertEquals(2, feature.geometry.coordinates.size)
        assertEquals(10.757933, feature.geometry.coordinates[0])
        assertEquals(59.911491, feature.geometry.coordinates[1])

        val props = feature.properties
        assertEquals("Oslo Sentralstasjon", props.name)
        assertEquals("Jernbanetorget", props.street)
        assertEquals("1", props.housenumber)
        assertEquals("0154", props.postcode)
        assertEquals("Oslo", props.city)
        assertEquals("Oslo", props.county)
        assertEquals("NO", props.countrycode)
        assertEquals("W", props.osm_type)
        assertEquals(123456789L, props.osm_id)
        assertEquals("railway", props.osm_key)
        assertEquals("station", props.osm_value)

        assertNotNull(props.extra)
        assertEquals("W123456789", props.extra.id)
        assertEquals("osm", props.extra.source)
        assertEquals("Oslo", props.extra.locality)
        assertEquals("0301", props.extra.locality_gid)
        assertEquals("03", props.extra.county_gid)
        assertEquals("NOR", props.extra.country_a)
        assertEquals("point", props.extra.accuracy)
        assertEquals("legacy.source.osm,legacy.layer.venue,legacy.category.transport", props.extra.tags)
    }

    @Test
    fun `parse handles empty features list`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": []
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        assertEquals("FeatureCollection", result.type)
        assertEquals(0, result.features.size)
    }

    @Test
    fun `parse handles multiple features`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.0, 60.0]
                        },
                        "properties": {
                            "name": "Place 1"
                        }
                    },
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [11.0, 61.0]
                        },
                        "properties": {
                            "name": "Place 2"
                        }
                    },
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [12.0, 62.0]
                        },
                        "properties": {
                            "name": "Place 3"
                        }
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        assertEquals(3, result.features.size)
        assertEquals("Place 1", result.features[0].properties.name)
        assertEquals("Place 2", result.features[1].properties.name)
        assertEquals("Place 3", result.features[2].properties.name)
    }

    @Test
    fun `parse handles minimal feature`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.0, 60.0]
                        },
                        "properties": {}
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        assertEquals(1, result.features.size)
        val feature = result.features[0]
        assertEquals(10.0, feature.geometry.coordinates[0])
        assertEquals(60.0, feature.geometry.coordinates[1])
    }

    @Test
    fun `parse handles extra with all fields`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.0, 60.0]
                        },
                        "properties": {
                            "name": "Test",
                            "extra": {
                                "id": "123",
                                "source": "osm",
                                "locality": "Oslo",
                                "locality_gid": "0301",
                                "county_gid": "03",
                                "borough": "Grünerløkka",
                                "borough_gid": "030109",
                                "country_a": "NOR",
                                "accuracy": "point",
                                "tariff_zones": "RUT:TariffZone:01,RUT:TariffZone:02",
                                "transport_modes": "bus,tram",
                                "alt_name": "Alternative Name",
                                "tags": "legacy.source.osm,legacy.layer.venue"
                            }
                        }
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        val extra = result.features[0].properties.extra
        assertNotNull(extra)
        assertEquals("123", extra.id)
        assertEquals("osm", extra.source)
        assertEquals("Oslo", extra.locality)
        assertEquals("0301", extra.locality_gid)
        assertEquals("03", extra.county_gid)
        assertEquals("Grünerløkka", extra.borough)
        assertEquals("030109", extra.borough_gid)
        assertEquals("NOR", extra.country_a)
        assertEquals("point", extra.accuracy)
        assertEquals("RUT:TariffZone:01,RUT:TariffZone:02", extra.tariff_zones)
        assertEquals("bus,tram", extra.transport_modes)
        assertEquals("Alternative Name", extra.alt_name)
        assertEquals("legacy.source.osm,legacy.layer.venue", extra.tags)
    }

    @Test
    fun `parse handles missing features field`() {
        val json =
            """
            {
                "type": "FeatureCollection"
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        assertEquals("FeatureCollection", result.type)
        assertEquals(0, result.features.size)
    }

    @Test
    fun `parse ignores unknown properties`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "include": "some_value",
                "unknown_field": "should_be_ignored",
                "features": [
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.0, 60.0],
                            "unknown_geo_field": "ignored"
                        },
                        "properties": {
                            "name": "Test Place",
                            "unknown_prop_field": "ignored"
                        },
                        "unknown_feature_field": "ignored"
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        assertEquals("FeatureCollection", result.type)
        assertEquals(1, result.features.size)
        assertEquals("Test Place", result.features[0].properties.name)
    }

    @Test
    fun `parse handles addresses with full details`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.757933, 59.911491]
                        },
                        "properties": {
                            "name": "Oslo Sentralstasjon",
                            "street": "Jernbanetorget",
                            "housenumber": "1",
                            "postcode": "0154",
                            "city": "Oslo",
                            "county": "Oslo",
                            "countrycode": "NO",
                            "type": "house"
                        }
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        val props = result.features[0].properties
        assertEquals("Oslo Sentralstasjon", props.name)
        assertEquals("Jernbanetorget", props.street)
        assertEquals("1", props.housenumber)
        assertEquals("0154", props.postcode)
        assertEquals("Oslo", props.city)
        assertEquals("Oslo", props.county)
        assertEquals("NO", props.countrycode)
        assertEquals("house", props.type)
    }

    @Test
    fun `parse handles norwegian characters correctly`() {
        val json =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "geometry": {
                            "type": "Point",
                            "coordinates": [10.0, 60.0]
                        },
                        "properties": {
                            "name": "Ålesund",
                            "street": "Løkkeveien",
                            "city": "Tromsø"
                        }
                    }
                ]
            }
            """.trimIndent()

        val result = PhotonResult.parse(json, Url("http://foo"))

        val props = result.features[0].properties
        assertEquals("Ålesund", props.name)
        assertEquals("Løkkeveien", props.street)
        assertEquals("Tromsø", props.city)
    }
}
