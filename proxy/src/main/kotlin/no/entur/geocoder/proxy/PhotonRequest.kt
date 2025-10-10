package no.entur.geocoder.proxy

data class PhotonAutocompleteRequest(
    val query: String,
    val limit: Int,
    val language: String,
    val includes: List<String> = emptyList()
) {
    companion object {
        fun from(params: V3AutocompleteParams): PhotonAutocompleteRequest {
            val includes = buildList {
                if (params.countries.isNotEmpty()) {
                    addAll(params.countries.map { "country.$it" })
                }
                if (params.countyIds.isNotEmpty()) {
                    addAll(params.countyIds.map { "county_gid.$it" })
                }
                if (params.localityIds.isNotEmpty()) {
                    addAll(params.localityIds.map { "locality_gid.$it" })
                }
                if (params.tariffZones.isNotEmpty()) {
                    addAll(params.tariffZones.map { "tariff_zone_id.$it" })
                }
                if (params.tariffZoneAuthorities.isNotEmpty()) {
                    addAll(params.tariffZoneAuthorities.map { "tariff_zone_authority.$it" })
                }
                if (params.sources.isNotEmpty()) {
                    addAll(params.sources.map { "source.$it" })
                }
                if (params.placeTypes.isNotEmpty()) {
                    addAll(params.placeTypes.map { "layer.$it" })
                }
            }

            return PhotonAutocompleteRequest(
                query = params.query,
                limit = params.limit,
                language = params.language,
                includes = includes
            )
        }

        fun from(params: PeliasAutocompleteParams): PhotonAutocompleteRequest {
            val includes = buildList {
                params.boundaryCountry?.let { add("country.$it") }
                if (params.boundaryCountyIds.isNotEmpty()) {
                    addAll(params.boundaryCountyIds.map { "county_gid.$it" })
                }
                if (params.boundaryLocalityIds.isNotEmpty()) {
                    addAll(params.boundaryLocalityIds.map { "locality_gid.$it" })
                }
                if (params.tariffZones.isNotEmpty()) {
                    addAll(params.tariffZones.map { "tariff_zone_id.$it" })
                }
                if (params.tariffZoneAuthorities.isNotEmpty()) {
                    addAll(params.tariffZoneAuthorities.map { "tariff_zone_authority.$it" })
                }
                if (params.sources.isNotEmpty()) {
                    addAll(params.sources.map { "source.$it" })
                }
                if (params.layers.isNotEmpty()) {
                    addAll(params.layers.map { "layer.$it" })
                }
            }

            return PhotonAutocompleteRequest(
                query = params.text,
                limit = params.size,
                language = params.lang,
                includes = includes
            )
        }
    }
}

data class PhotonReverseRequest(
    val latitude: String,
    val longitude: String,
    val language: String,
    val limit: Int,
    val radius: String? = null
) {
    companion object {
        fun from(params: V3ReverseParams): PhotonReverseRequest {
            return PhotonReverseRequest(
                latitude = params.latitude,
                longitude = params.longitude,
                language = params.language,
                limit = params.limit,
                radius = params.radius
            )
        }

        fun from(params: PeliasReverseParams): PhotonReverseRequest {
            return PhotonReverseRequest(
                latitude = params.lat,
                longitude = params.lon,
                language = params.lang,
                limit = params.size,
                radius = params.radius
            )
        }
    }
}

