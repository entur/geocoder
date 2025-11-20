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
    private val utm33n: CoordinateReferenceSystem = CRS.decode("EPSG:25833") // https://epsg.io/25833
    private val wgs84: CoordinateReferenceSystem = DefaultGeographicCRS.WGS84 // https://epsg.io/3857
    private val utm33nToWgs84: MathTransform = CRS.findMathTransform(utm33n, wgs84, true)

    fun convertUtm33ToLatLon(coord: UtmCoordinate): Coordinate {
        val srcCoord =
            org.locationtech.jts.geom
                .Coordinate(coord.easting, coord.northing)
        val dstCoord = JTS.transform(srcCoord, null, utm33nToWgs84)

        val lat = dstCoord.y
        val lon = dstCoord.x

        return Coordinate(lat, lon)
    }

    /**
     * Great-circle distance between two points on a sphere using the Haversine formula.
     *
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
     * Converts Pelias focus.scale to Photon zoom level.
     *
     * Pelias uses linear decay (score drops to 0 at 2×scale), while Photon uses exponential decay
     * (score never reaches 0). Formula: `(scale + 1) / 2.5` accounts for Pelias's 1km offset and
     * balances the different decay curves to provide similar user experience.
     *
     * @param peliasScale The Pelias focus.scale in km, or null for 100km
     * @return Photon zoom level [0-18]
     */
    fun peliasScaleToPhotonZoom(peliasScale: Int?): Int {
        val effectiveScale = peliasScale ?: 100
        val targetRadius = (effectiveScale + 1.0) / 2.5
        val zoom = (18 - log2(targetRadius * 4)).toInt()
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
