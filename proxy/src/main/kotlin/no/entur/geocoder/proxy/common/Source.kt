package no.entur.geocoder.proxy.common

object Source {
    const val OSM = "openstreetmap"
    const val KARTVERKET_ADRESSE = "kartverket-matrikkelenadresse"
    const val KARTVERKET_STEDSNAVN = "kartverket-stedsnavn"
    const val LANTMATERIET_ADRESSE = "lantmateriet-belagenhetsadress"
    const val NSR = "nsr"
    const val SSR = "ssr"

    val ADDRESSES = setOf(KARTVERKET_ADRESSE, LANTMATERIET_ADRESSE)
}
