package no.entur.netex_to_json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.entur.netex_to_json.NominatimHeader.Features
import no.entur.netex_to_json.NominatimHeader.HeaderContent
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class JsonWriter {

    fun export(entries: Sequence<NominatimPlace>, outputPath: Path) {
        val objectMapper = jacksonObjectMapper()
        addDecimalFormatter(objectMapper)

        Files.createDirectories(outputPath.parent)

        val headerContent = HeaderContent(
            version = "0.1.0",
            generator = "netex-to-json",
            database_version = "0.3.6-1",
            data_timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            features = Features(true, false)
        )
        val header = NominatimHeader(
            type = "NominatimDumpFile",
            content = headerContent
        )
        Files.newBufferedWriter(outputPath).use { writer ->
            writer.write(objectMapper.writeValueAsString(header))
            writer.newLine()
            entries.forEach { entry ->
                writer.write(objectMapper.writeValueAsString(entry))
                writer.newLine()
            }
        }
    }

    private fun addDecimalFormatter(objectMapper: ObjectMapper) {
        objectMapper.configure(WRITE_BIGDECIMAL_AS_PLAIN, true)

        val module = SimpleModule()
        module.addSerializer(Double::class.java, object : JsonSerializer<Double>() {
            override fun serialize(value: Double?, gen: JsonGenerator, serializers: SerializerProvider) {
                if (value == null) {
                    gen.writeNull()
                } else {
                    gen.writeNumber(BigDecimal.valueOf(value))
                }
            }
        })
        objectMapper.registerModule(module)
    }

}