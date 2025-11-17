package no.entur.geocoder.converter.source.osm

import no.entur.geocoder.common.Coordinate
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test

class CoordinateStoreTest {
    @Test
    fun `put and get single coordinate`() {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(59.9, 10.5))

        val result = store.get(1L)
        assertNotNull(result)
        result?.let {
            assertEquals(10.5, it.lon, 0.00001)
            assertEquals(59.9, it.lat, 0.00001)
        }
    }

    @Test
    fun `get returns null for non-existent ID`() {
        val store = CoordinateStore(100)
        val result = store.get(999L)
        assertNull(result)
    }

    @ParameterizedTest
    @CsvSource(
        "10.7522, 59.9139",
        "-122.4194, 37.7749",
        "139.6917, 35.6895",
        "151.2093, -33.8688",
        "0.0, 0.0",
        "-180.0, -90.0",
        "180.0, 90.0",
    )
    fun `stores and retrieves coordinates with precision`(lon: Double, lat: Double) {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(lat, lon))

        val result = store.get(1L)
        assertNotNull(result)
        result?.let {
            assertEquals(lon, it.lon, 0.0001)
            assertEquals(lat, it.lat, 0.0001)
        }
    }

    @Test
    fun `stores multiple coordinates`() {
        val store = CoordinateStore(100)
        val coordinates =
            mapOf(
                1L to Coordinate(59.9139, 10.7522),
                2L to Coordinate(60.3913, 5.3221),
                3L to Coordinate(63.4305, 10.3951),
                4L to Coordinate(69.6492, 18.9560),
            )

        coordinates.forEach { (id, coord) ->
            store.put(id, coord)
        }

        coordinates.forEach { (id, expected) ->
            val result = store.get(id)
            assertNotNull(result, "Should retrieve coordinate for ID $id")
            result?.let {
                assertEquals(expected.lon, it.lon, 0.00001, "Longitude for ID $id")
                assertEquals(expected.lat, it.lat, 0.00001, "Latitude for ID $id")
            }
        }
    }

    @Test
    fun `updating coordinate replaces old value`() {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(60.0, 10.0))
        store.put(1L, Coordinate(61.0, 11.0))

        val result = store.get(1L)
        assertNotNull(result)
        result?.let {
            assertEquals(11.0, it.lon, 0.00001)
            assertEquals(61.0, it.lat, 0.00001)
        }
    }

    @Test
    fun `handles collision with linear probing`() {
        val store = CoordinateStore(10)

        val ids = (1L..20L).toList()
        ids.forEach { id ->
            store.put(id, Coordinate(id.toDouble() + 50.0, id.toDouble()))
        }

        ids.forEach { id ->
            val result = store.get(id)
            assertNotNull(result, "Should retrieve coordinate for ID $id")
            result?.let {
                assertEquals(id.toDouble(), it.lon, 0.00001)
                assertEquals(id.toDouble() + 50.0, it.lat, 0.00001)
            }
        }
    }

    @Test
    fun `handles many coordinates with resizing`() {
        val store = CoordinateStore(10)
        val count = 1000

        for (id in 1L..count) {
            store.put(id, Coordinate(50.0 + id.toDouble() / 100.0, id.toDouble() / 100.0))
        }

        for (id in 1L..count) {
            val result = store.get(id)
            assertNotNull(result, "Should retrieve coordinate for ID $id")
            result?.let {
                assertEquals(id.toDouble() / 100.0, it.lon, 0.0001)
                assertEquals(50.0 + id.toDouble() / 100.0, it.lat, 0.0001)
            }
        }
    }

    @Test
    fun `handles negative IDs`() {
        val store = CoordinateStore(100)
        store.put(-1L, Coordinate(60.0, 10.0))
        store.put(-999L, Coordinate(61.0, 11.0))
        store.put(-1234567L, Coordinate(62.0, 12.0))

        val result1 = store.get(-1L)
        assertNotNull(result1)
        result1?.let {
            assertEquals(10.0, it.lon, 0.00001)
            assertEquals(60.0, it.lat, 0.00001)
        }

        val result2 = store.get(-999L)
        assertNotNull(result2)
        result2?.let {
            assertEquals(11.0, it.lon, 0.00001)
            assertEquals(61.0, it.lat, 0.00001)
        }

        val result3 = store.get(-1234567L)
        assertNotNull(result3)
        result3?.let {
            assertEquals(12.0, it.lon, 0.00001)
            assertEquals(62.0, it.lat, 0.00001)
        }
    }

    @Test
    fun `handles large ID values`() {
        val store = CoordinateStore(100)
        val largeId = 9999999999L
        store.put(largeId, Coordinate(59.9, 10.5))

        val result = store.get(largeId)
        assertNotNull(result)
        result?.let {
            assertEquals(10.5, it.lon, 0.00001)
            assertEquals(59.9, it.lat, 0.00001)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "10.7522, 59.9139",
        "10.75220001, 59.91390001",
        "10.752199999, 59.913899999",
    )
    fun `precision is approximately 1 meter`(lon: Double, lat: Double) {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(lat, lon))

        val result = store.get(1L)
        assertNotNull(result)
        result?.let {
            assertEquals(lon, it.lon, 0.00001)
            assertEquals(lat, it.lat, 0.00001)
        }
    }

    @Test
    fun `handles coordinates at origin`() {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate.ZERO)

        val result = store.get(1L)
        assertNotNull(result)
        result?.let {
            assertEquals(0.0, it.lon, 0.00001)
            assertEquals(0.0, it.lat, 0.00001)
        }
    }

    @Test
    fun `mixed positive and negative IDs work correctly`() {
        val store = CoordinateStore(100)
        val testData =
            mapOf(
                -100L to Coordinate(60.0, 10.0),
                100L to Coordinate(61.0, 11.0),
                -200L to Coordinate(62.0, 12.0),
                200L to Coordinate(63.0, 13.0),
            )

        testData.forEach { (id, coord) ->
            store.put(id, coord)
        }

        testData.forEach { (id, expected) ->
            val result = store.get(id)
            assertNotNull(result)
            result?.let {
                assertEquals(expected.lon, it.lon, 0.00001)
                assertEquals(expected.lat, it.lat, 0.00001)
            }
        }
    }

    @Test
    fun `extreme longitude values within valid range`() {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(0.0, -179.999))
        store.put(2L, Coordinate(0.0, 179.999))

        val result1 = store.get(1L)
        assertNotNull(result1)
        result1?.let {
            assertEquals(-179.999, it.lon, 0.00001)
        }

        val result2 = store.get(2L)
        assertNotNull(result2)
        result2?.let {
            assertEquals(179.999, it.lon, 0.00001)
        }
    }

    @Test
    fun `extreme latitude values within valid range`() {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(-89.999, 0.0))
        store.put(2L, Coordinate(89.999, 0.0))

        val result1 = store.get(1L)
        assertNotNull(result1)
        result1?.let {
            assertEquals(-89.999, it.lat, 0.00001)
        }

        val result2 = store.get(2L)
        assertNotNull(result2)
        result2?.let {
            assertEquals(89.999, it.lat, 0.00001)
        }
    }

    @Test
    fun `retrieval of non-existent ID does not affect stored values`() {
        val store = CoordinateStore(100)
        store.put(1L, Coordinate(60.0, 10.0))

        assertNull(store.get(999L))

        val result = store.get(1L)
        assertNotNull(result)
        result?.let {
            assertEquals(10.0, it.lon, 0.00001)
            assertEquals(60.0, it.lat, 0.00001)
        }
    }
}
