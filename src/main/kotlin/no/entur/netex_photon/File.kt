package no.entur.netex_photon

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object File {
    fun streamToFile(inputStream: InputStream): java.io.File {
        val tempFile = File.createTempFile("stream", ".tmp")
        Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return tempFile
    }
}