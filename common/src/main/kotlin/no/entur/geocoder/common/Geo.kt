package no.entur.geocoder.common

import de.westnordost.countryboundaries.CountryBoundaries
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.geotools.referencing.crs.DefaultGeographicCRS
import kotlin.math.*

object Geo {
    val utm33n: CoordinateReferenceSystem = CRS.decode("EPSG:25833")
    val wgs84: CoordinateReferenceSystem = DefaultGeographicCRS.WGS84
    val transform: MathTransform = CRS.findMathTransform(utm33n, wgs84, true)

    fun convertUTM33ToLatLon(
        easting: Double,
        northing: Double,
    ): Coordinate {
        val srcCoord =
            org.locationtech.jts.geom
                .Coordinate(easting, northing)
        val dstCoord = JTS.transform(srcCoord, null, transform)

        val lat = dstCoord.y
        val lon = dstCoord.x

        return Coordinate(lat, lon)
    }

    /**
     * Great-circle distance between two points on a sphere using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in meters
     */
    fun haversineDistance(coord1: Coordinate, coord2: Coordinate): Double {
        val earthRadius = 6371008.8 // Mean Earth radius (WGS84 authalic/mean ≈ 6371.0088)

        val φ1 = Math.toRadians(coord1.lat)
        val φ2 = Math.toRadians(coord2.lat)
        val Δφ = Math.toRadians(coord2.lat - coord1.lat)
        val Δλ = Math.toRadians(coord2.lon - coord1.lon)

        val sinDLat = sin(Δφ / 2)
        val sinDLon = sin(Δλ / 2)

        var a = sinDLat * sinDLat + cos(φ1) * cos(φ2) * sinDLon * sinDLon

        a = a.coerceIn(0.0, 1.0) // Clamp for numerical stability

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Converts a radius to a zoom level
     *
     * @param radius The radius in kilometers
     * @return Zoom level as a string, clamped to the range [0, 18]
     */
    fun radiusToZoom(radius: Double): Int {
        val zoom = (18 - log2(radius * 4)).toInt()
        return zoom.coerceIn(0, 18)
    }

    private val boundaries: CountryBoundaries? by lazy {
        val source =
            Geo.javaClass
                .getResourceAsStream("/countryboundaries/boundaries60x30.ser")
                ?.asSource()
                ?.buffered()
        source?.let { CountryBoundaries.deserializeFrom(source) }
    }

    fun getCountry(coord: Coordinate): Country? =
        boundaries
            ?.getIds(coord.lon, coord.lat)
            ?.firstOrNull { it.length == 2 }
            ?.let { Country.parse(it) }
}
