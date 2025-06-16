package no.entur.netex_photon.converter

import no.entur.netex_photon.converter.ConverterUtils.toBigDecimalWithScale
import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import java.math.BigDecimal

object CoordinateConverter {
    fun convertUTM33ToLatLon(easting: Double, northing: Double): Pair<BigDecimal, BigDecimal> {
        val sourceCRS: CoordinateReferenceSystem = CRS.decode("EPSG:25833", true)
        val targetCRS: CoordinateReferenceSystem = CRS.decode("EPSG:4326", true)
        val transform: MathTransform = CRS.findMathTransform(sourceCRS, targetCRS, true)

        val srcCoord = Coordinate(easting, northing)
        val dstCoord = JTS.transform(srcCoord, null, transform)

        val lat = dstCoord.y.toBigDecimalWithScale()
        val lon = dstCoord.x.toBigDecimalWithScale()

        return Pair(lat, lon)
    }
}
