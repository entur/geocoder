package no.entur.geocoder.proxy.common

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder

object Jackson {
    val jacksonMapper =
        jacksonMapperBuilder()
            // Jackson 3 sorts non-constructor properties alphabetically by default; keep declaration order
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .changeDefaultPropertyInclusion { JsonInclude.Value.construct(NON_NULL, NON_NULL) }
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()
}
