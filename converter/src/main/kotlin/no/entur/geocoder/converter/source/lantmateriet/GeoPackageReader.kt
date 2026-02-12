package no.entur.geocoder.converter.source.lantmateriet

import org.locationtech.jts.io.WKBReader
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class GeoPackageReader {
    fun read(gpkgFile: File): Sequence<BelagenhetsAdress> =
        sequence {
            val url = "jdbc:sqlite:${gpkgFile.absolutePath}"
            DriverManager.getConnection(url).use { conn ->
                val tableName = findAddressTable(conn) ?: return@sequence
                val sql =
                    """
                    SELECT objektidentitet, adressplatstyp,
                           adressomrade_faststalltnamn, gardsadressomrade_faststalltnamn,
                           adressplatsnummer, bokstavstillagg,
                           lagestillagg, lagestillaggsnummer,
                           avvikerfranstandarden, avvikandeadressplatsbeteckning,
                           postnummer, postort,
                           kommunkod, kommunnamn, lanskod,
                           popularnamn, kommundel_faststalltnamn,
                           geom
                    FROM $tableName
                    WHERE statusforbelagenhetsadress = 'Gallande'
                      AND postnummer > 0
                      AND postort IS NOT NULL AND postort != ''
                    """.trimIndent()

                conn.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        val wkbReader = WKBReader()
                        while (rs.next()) {
                            val geomBytes = rs.getBytes("geom") ?: continue
                            val coord = parseGeoPackageGeometry(geomBytes, wkbReader) ?: continue

                            yield(
                                BelagenhetsAdress(
                                    objektidentitet = rs.getString("objektidentitet") ?: continue,
                                    adressplatstyp = rs.getString("adressplatstyp") ?: continue,
                                    adressomradeFaststalltnamn = rs.getString("adressomrade_faststalltnamn"),
                                    gardsadressomradeFaststalltnamn = rs.getString("gardsadressomrade_faststalltnamn"),
                                    adressplatsnummer = rs.getObject("adressplatsnummer") as? Int,
                                    bokstavstillagg = rs.getString("bokstavstillagg"),
                                    lagestillagg = rs.getString("lagestillagg"),
                                    lagestillaggsnummer = rs.getObject("lagestillaggsnummer") as? Int,
                                    avvikerfranstandarden = rs.getInt("avvikerfranstandarden") == 1,
                                    avvikandeadressplatsbeteckning = rs.getString("avvikandeadressplatsbeteckning"),
                                    postnummer = rs.getString("postnummer"),
                                    postort = rs.getString("postort"),
                                    kommunkod = rs.getString("kommunkod"),
                                    kommunnamn = rs.getString("kommunnamn"),
                                    lanskod = rs.getString("lanskod"),
                                    popularnamn = rs.getString("popularnamn"),
                                    kommundelFaststalltnamn = rs.getString("kommundel_faststalltnamn"),
                                    easting = coord.x,
                                    northing = coord.y,
                                ),
                            )
                        }
                    }
                }
            }
        }

    private fun findAddressTable(conn: Connection): String? {
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT table_name FROM gpkg_contents WHERE data_type = 'features'").use { rs ->
                while (rs.next()) {
                    val name = rs.getString("table_name")
                    if (name.contains("belagenhetsadress", ignoreCase = true)) {
                        return name
                    }
                }
            }
        }
        // Fallback: try first features table
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT table_name FROM gpkg_contents WHERE data_type = 'features' LIMIT 1").use { rs ->
                if (rs.next()) return rs.getString("table_name")
            }
        }
        return null
    }

    companion object {
        /**
         * Parse GeoPackage binary geometry to extract coordinates.
         * GeoPackage geometry format: magic (2) + version (1) + flags (1) + srs_id (4) + [envelope] + WKB
         */
        fun parseGeoPackageGeometry(
            bytes: ByteArray,
            wkbReader: WKBReader,
        ): org.locationtech.jts.geom.Coordinate? {
            if (bytes.size < 8) return null

            // Check GeoPackage magic: "GP"
            if (bytes[0] != 0x47.toByte() || bytes[1] != 0x50.toByte()) return null

            val flags = bytes[3].toInt() and 0xFF
            val envelopeType = (flags shr 1) and 0x07

            // Envelope sizes: 0=none, 1=xy(32), 2=xyz(48), 3=xym(48), 4=xyzm(64)
            val envelopeSize =
                when (envelopeType) {
                    0 -> 0
                    1 -> 32
                    2 -> 48
                    3 -> 48
                    4 -> 64
                    else -> return null
                }

            val wkbOffset = 8 + envelopeSize
            if (wkbOffset >= bytes.size) return null

            val wkbBytes = bytes.copyOfRange(wkbOffset, bytes.size)
            val geometry = wkbReader.read(wkbBytes)
            return geometry.coordinate
        }
    }
}
