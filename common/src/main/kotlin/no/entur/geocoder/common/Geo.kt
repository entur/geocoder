package no.entur.geocoder.common

import de.westnordost.countryboundaries.CountryBoundaries
import kotlinx.io.asSource
import kotlinx.io.buffered
import no.entur.geocoder.common.Util.toBigDecimalWithScale
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.Coordinate
import java.math.BigDecimal
import kotlin.math.*

object Geo {
    val utm33n: CoordinateReferenceSystem = CRS.decode("EPSG:25833")
    val wgs84: CoordinateReferenceSystem = DefaultGeographicCRS.WGS84
    val transform: MathTransform = CRS.findMathTransform(utm33n, wgs84, true)

    fun convertUTM33ToLatLon(
        easting: Double,
        northing: Double,
    ): Pair<BigDecimal, BigDecimal> {
        val srcCoord = Coordinate(easting, northing)
        val dstCoord = JTS.transform(srcCoord, null, transform)

        val lat = dstCoord.y.toBigDecimalWithScale()
        val lon = dstCoord.x.toBigDecimalWithScale()

        return Pair(lat, lon)
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
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371008.8 // Mean Earth radius (WGS84 authalic/mean ≈ 6371.0088)

        val φ1 = Math.toRadians(lat1)
        val φ2 = Math.toRadians(lat2)
        val Δφ = Math.toRadians(lat2 - lat1)
        val Δλ = Math.toRadians(lon2 - lon1)

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

    fun getCountryCode(lat: Double, lon: Double): String? =
        boundaries
            ?.getIds(lon, lat)
            ?.firstOrNull { it.length == 2 }
}
