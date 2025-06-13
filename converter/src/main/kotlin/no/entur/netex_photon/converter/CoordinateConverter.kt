package no.entur.netex_photon.converter

import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import java.math.BigDecimal
import java.math.RoundingMode

object CoordinateConverter {
    fun convertUTM33ToLatLon(easting: Double, northing: Double): Pair<BigDecimal, BigDecimal> {
        val sourceCRS: CoordinateReferenceSystem = CRS.decode("EPSG:25833", true)
        val targetCRS: CoordinateReferenceSystem = CRS.decode("EPSG:4326", true)
        val transform: MathTransform = CRS.findMathTransform(sourceCRS, targetCRS, true)

        val srcCoord = Coordinate(easting, northing)
        val dstCoord = JTS.transform(srcCoord, null, transform)

        val lat = BigDecimal(dstCoord.y).setScale(6, RoundingMode.HALF_UP)
        val lon = BigDecimal(dstCoord.x).setScale(6, RoundingMode.HALF_UP)

        return Pair(lat, lon)
    }
}
