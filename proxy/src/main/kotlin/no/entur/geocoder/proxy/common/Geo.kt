package no.entur.geocoder.proxy.common

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

    private const val SATURATION_SCALE_KM: Double = 300.0

    /**
     * Converts Pelias `focus.scale` (km) to Photon `zoom` [0-18].
     *
     * Empirical tuning, not a literal Pelias-Photon equivalence. The literal half-score match
     * (Pelias half-score at `dist = scale`, Photon half-score at `dist = biasRadius + decayRadius`
     * since `negDecay = ln(0.5) / decayRadius`) leaves too little location discrimination for
     * short queries in country-sized indexes after Photon's `location_bias_score` widening of
     * `biasRadius`. The current heuristic targets `biasRadius = scale / 2`, paired with a tanh
     * saturation around [SATURATION_SCALE_KM] so the API default `scale=2500` lands at a useful
     * `zoom=9` (biasRadius ~120 km, decayRadius ~720 km) instead of `zoom=6` (biasRadius ~1280 km,
     * which would swallow Norway). Pinned by `GeoTest` and `PhotonAutocompleteRequestTest`.
     *
     * `biasRadius = 2.2^(18 - zoom) * 0.1`, `decayRadius = max(8, biasRadius * (zoom - 3))`,
     * see Photon's `SearchRequestBase`.
     */
    fun peliasScaleToPhotonZoom(scale: Int): Int {
        val saturated = SATURATION_SCALE_KM * tanh(scale / SATURATION_SCALE_KM)
        val zoom = (18.0 - log(5.0 * saturated, 2.2)).roundToInt()
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

    // See common/README.md for details on how to country mapper
    fun getCountry(coord: Coordinate): Country? =
        boundaries
            ?.getIds(coord.lon, coord.lat)
            ?.firstOrNull { it.length == 2 }
            ?.let { Country.parse(it) }
}
