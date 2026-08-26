package no.entur.geocoder.proxy.photon

import no.entur.geocoder.proxy.common.Category
import no.entur.geocoder.proxy.common.Category.containsTag
import no.entur.geocoder.proxy.common.Source
import no.entur.geocoder.proxy.photon.PhotonResult.PhotonFeature

object PhotonResultFilter {
    /**
     * Drop the stedsnavn place when a GOSP for the same municipality carries the same name.
     * Municipality is compared by id: the locality names differ for bilingual kommunenavn
     * ("Harstad - Hárstták" vs "Harstad").
     */
    fun dropPlacesCoveredByGosp(features: List<PhotonFeature>): List<PhotonFeature> {
        val gospKeys =
            features
                .filter { it.properties.extra.tags.containsTag(Category.LAYER_GOSP) }
                .mapNotNullTo(mutableSetOf()) { it.nameAndLocality() }
        if (gospKeys.isEmpty()) return features
        return features.filterNot { feature ->
            feature.properties.extra.source == Source.KARTVERKET_STEDSNAVN &&
                feature.nameAndLocality()?.let { it in gospKeys } == true
        }
    }

    private fun PhotonFeature.nameAndLocality(): Pair<String, String>? {
        val name = properties.name ?: return null
        val locality = properties.extra.locality_gid ?: return null
        return name to locality
    }
}
