package no.entur.netex_photon.proxy

import kotlinx.serialization.json.*

class FeatureTransformer {
    private val json = Json { ignoreUnknownKeys = true }

    fun transformFeature(feature: JsonObject): JsonObject {
        val type = feature["type"] ?: return feature
        val geometry = feature["geometry"] ?: return feature
        val properties = feature["properties"] as? JsonObject ?: return feature
        val extra = properties["extra"] as? JsonObject

        val outProps = mutableMapOf<String, JsonElement>()
        if (extra != null) {
            for ((k, v) in extra.entries) {
                when (k) {
                    "category" -> outProps["category"] = toArray(v)
                    "tariff_zones" -> outProps["tariff_zones"] = toArray(v)
                    "label" -> outProps["label"] = formatLabel(v)
                    else -> outProps[k] = v
                }
            }
        }

        properties["id"]?.let { outProps["id"] = it }
        properties["gid"]?.let { outProps["gid"] = it }
        properties["layer"]?.let { outProps["layer"] = it }
        properties["source"]?.let { outProps["source"] = it }
        properties["source_id"]?.let { outProps["source_id"] = it }
        properties["name"]?.let { outProps["name"] = it }
        properties["street"]?.let { outProps["street"] = it }
        properties["accuracy"]?.let { outProps["accuracy"] = it }
        properties["country_a"]?.let { outProps["country_a"] = it }
        properties["county"]?.let { outProps["county"] = it }
        properties["county_gid"]?.let { outProps["county_gid"] = it }
        properties["locality"]?.let { outProps["locality"] = it }
        properties["locality_gid"]?.let { outProps["locality_gid"] = it }
        properties["label"]?.let { outProps["label"] = it }
        properties["category"]?.let {
            if (outProps["category"] == null) {
                outProps["category"] = JsonArray(it.jsonPrimitive.content.split(',').map { JsonPrimitive(it.trim()) })
            }
        }
        properties["tariff_zones"]?.let {
            if (outProps["tariff_zones"] == null) {
                outProps["tariff_zones"] =
                    JsonArray(it.jsonPrimitive.content.split(',').map { JsonPrimitive(it.trim()) })
            }
        }

        val builder = mutableMapOf<String, JsonElement>()
        builder["type"] = type
        builder["geometry"] = geometry
        builder["properties"] = JsonObject(outProps)
        return JsonObject(builder)
    }

    fun transformCollection(collection: JsonObject): JsonObject {
        val features = (collection["features"] as? JsonArray)?.mapNotNull {
            if (it is JsonObject) transformFeature(it) else null
        } ?: emptyList()
        return JsonObject(collection.toMutableMap().apply {
            put("features", JsonArray(features))
        })
    }

    fun parseAndTransform(input: String): JsonElement {
        val element = json.parseToJsonElement(input)
        return when {
            element is JsonObject && element["type"]?.jsonPrimitive?.content == "FeatureCollection" && element["features"] is JsonArray ->
                transformCollection(element)

            element is JsonObject -> transformFeature(element)
            element is JsonArray -> JsonArray(element.mapNotNull { if (it is JsonObject) transformFeature(it) else null })
            else -> element
        }
    }

    fun encodeToString(element: JsonElement): String = json.encodeToString(element)

    private fun toArray(element: JsonElement): JsonArray =
        JsonArray(element.jsonPrimitive.content.split(',').map { JsonPrimitive(it.trim()) })

    private fun formatLabel(element: JsonElement): JsonPrimitive =
        JsonPrimitive(element.jsonPrimitive.content.replace(",", ", "))
}
