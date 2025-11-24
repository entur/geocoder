package no.entur.geocoder.converter

import no.entur.geocoder.common.JsonMapper.jacksonMapper
import no.entur.geocoder.converter.target.NominatimHeader
import no.entur.geocoder.converter.target.NominatimHeader.Features
import no.entur.geocoder.converter.target.NominatimHeader.HeaderContent
import no.entur.geocoder.converter.target.NominatimPlace
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class JsonWriter {
    fun export(
        entries: Sequence<NominatimPlace>,
        outputPath: Path,
        isAppending: Boolean = true,
    ) {
        Files.createDirectories(outputPath.parent)

        if (!isAppending) {
            val headerContent =
                HeaderContent(
                    version = "0.1.0",
                    generator = "geocoder",
                    database_version = "0.3.6-1",
                    data_timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    features = Features(true, false),
                )
            val header =
                NominatimHeader(
                    type = "NominatimDumpFile",
                    content = headerContent,
                )
            Files.newBufferedWriter(outputPath).use { writer ->
                writer.write(jacksonMapper.writeValueAsString(header))
                writer.newLine()
            }
        }

        Files.newBufferedWriter(outputPath, APPEND, CREATE).use { writer ->
            entries.forEach { entry ->
                writer.write(jacksonMapper.writeValueAsString(entry))
                writer.newLine()
            }
        }
    }
}
