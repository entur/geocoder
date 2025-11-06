package no.entur.geocoder.converter

import java.io.File
import java.io.BufferedInputStream
import java.io.FileInputStream

/**
 * Detects and validates file types for converter inputs.
 */
class FileTypeDetector {

    enum class FileType {
        XML,
        CSV,
        PBF,
        GML,
        GZIP,
        UNKNOWN
    }

    /**
     * Detects the file type by examining magic bytes and content.
     */
    fun detectFileType(file: File): FileType {
        if (!file.exists() || !file.isFile) {
            return FileType.UNKNOWN
        }

        // Read first few bytes for magic number detection
        val bytes = ByteArray(512)
        val bytesRead = BufferedInputStream(FileInputStream(file)).use { input ->
            input.read(bytes)
        }

        if (bytesRead < 2) {
            return FileType.UNKNOWN
        }

        // Check for GZIP magic number (1f 8b)
        if (bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()) {
            return FileType.GZIP
        }

        // Check for PBF magic bytes
        // PBF files typically start with blob headers or have specific byte patterns
        if (isPbf(bytes)) {
            return FileType.PBF
        }

        // Convert to string for text-based detection
        val content = String(bytes, 0, bytesRead, Charsets.UTF_8)

        // Check for XML/GML (both are XML-based)
        if (content.trimStart().startsWith("<?xml")) {
            // Distinguish between GML and regular XML
            if (content.contains("gml:", ignoreCase = true)) {
                return FileType.GML
            }
            return FileType.XML
        }

        // Check for CSV - look for comma-separated values pattern
        if (isCsv(content)) {
            return FileType.CSV
        }

        return FileType.UNKNOWN
    }

    /**
     * Validates that the file matches the expected type.
     * @throws IllegalArgumentException if validation fails
     */
    fun validateFileType(file: File, expectedType: FileType, flagName: String) {
        val detectedType = detectFileType(file)

        if (detectedType == FileType.UNKNOWN) {
            throw IllegalArgumentException(
                "Error: Cannot determine file type for $flagName: ${file.absolutePath}"
            )
        }

        if (detectedType != expectedType) {
            throw IllegalArgumentException(
                "Error: Invalid file type for $flagName: ${file.absolutePath}\n" +
                "Expected: ${expectedType.name}, but detected: ${detectedType.name}"
            )
        }
    }

    private fun isPbf(bytes: ByteArray): Boolean {
        // PBF files contain specific OSM PBF header signatures
        // Look for "OSMHeader" or "OSMData" strings which are common in PBF files
        val content = String(bytes, 0, minOf(bytes.size, 256), Charsets.ISO_8859_1)
        return content.contains("OSMHeader") || content.contains("OSMData")
    }

    private fun isCsv(content: String): Boolean {
        val lines = content.lines().take(5)
        if (lines.isEmpty()) return false

        // Check if lines have consistent comma separators
        val commaCounts = lines.filter { it.isNotBlank() }.map { it.count { c -> c == ',' || c == ';' } }
        if (commaCounts.isEmpty()) return false

        // CSV should have consistent number of commas per line
        val avgCommas = commaCounts.average()
        return avgCommas > 0 && commaCounts.all { kotlin.math.abs(it - avgCommas) <= 2 }
    }
}

