package no.entur.netexphoton.converter

import java.io.File

interface Converter {
    fun convert(
        input: File,
        output: File,
        isAppending: Boolean = false,
    )
}
