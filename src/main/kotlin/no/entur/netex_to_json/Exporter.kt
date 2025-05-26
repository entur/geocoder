package no.entur.netex_to_json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path

class Exporter {

    fun export(entries: Sequence<NominatimEntry>, outputPath: Path) {
        val objectMapper = jacksonObjectMapper()
        addDecimalFormatter(objectMapper)

        Files.createDirectories(outputPath.parent)

        Files.newBufferedWriter(outputPath).use { writer ->
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