package no.entur.netex_photon.converter

import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.api.referencing.operation.MathTransform
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate

object CoordinateConverter {
    fun convertUTM33ToLatLon(easting: Double, northing: Double): Pair<Double, Double> {
        val sourceCRS: CoordinateReferenceSystem = CRS.decode("EPSG:25833", true)
        val targetCRS: CoordinateReferenceSystem = CRS.decode("EPSG:4326", true)
        val transform: MathTransform = CRS.findMathTransform(sourceCRS, targetCRS, true)

        val srcCoord = Coordinate(easting, northing)
        val dstCoord = JTS.transform(srcCoord, null, transform)

        return Pair(dstCoord.y, dstCoord.x)
    }
}
