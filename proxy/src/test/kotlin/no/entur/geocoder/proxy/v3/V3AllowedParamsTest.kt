package no.entur.geocoder.proxy.v3

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals

class V3AllowedParamsTest {
    private fun expectedParams(klass: KClass<*>): Set<String> =
        klass.primaryConstructor
            ?.parameters
            ?.mapNotNull { it.name }
            ?.toSet()
            .orEmpty()

    @Test
    fun `autocomplete ALLOWED_PARAMS matches data class fields`() {
        assertEquals(expectedParams(V3AutocompleteRequest::class), V3AutocompleteRequest.ALLOWED_PARAMS)
    }

    @Test
    fun `reverse ALLOWED_PARAMS matches data class fields`() {
        assertEquals(expectedParams(V3ReverseRequest::class), V3ReverseRequest.ALLOWED_PARAMS)
    }

    @Test
    fun `place ALLOWED_PARAMS matches data class fields`() {
        assertEquals(expectedParams(V3PlaceRequest::class), V3PlaceRequest.ALLOWED_PARAMS)
    }
}
