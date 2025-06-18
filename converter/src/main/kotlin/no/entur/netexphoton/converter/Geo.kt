package no.entur.netexphoton.converter

import no.entur.netexphoton.converter.Util.toBigDecimalWithScale
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.Coordinate
import java.math.BigDecimal

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
}
