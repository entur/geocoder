package no.entur.geocoder.converter.source.stopplace

import no.entur.geocoder.converter.ConverterConfig.GroupOfStopPlacesConfig

/**
 * Calculates popularity (boost) values for GroupOfStopPlaces based on legacy kakka boost configuration.
 *
 * Configuration reference (pelias.gos.boost.factor):
 * gosBoostFactor = 10 (from production config)
 * See: /Users/testower/Developer/github/entur/kakka/helm/kakka/env/values-kub-ent-*.yaml
 *
 * Formula: popularity = gosBoostFactor × (product of all member stop place popularities)
 *
 * Key insight: Unlike stop places which SUM type factors, groups MULTIPLY member popularities.
 * This creates exponential growth with more members, making large groups very prominent.
 *
 * Source reference:
 * DeliveryPublicationStreamToElasticsearchCommands.java:151
 * ```
 * double popularity = gosBoostFactor * groupOfStopPlaces.getMembers().getStopPlaceRef().stream()
 *     .map(sp -> popularityPerStopPlaceId.get(sp.getRef()))
 *     .filter(Objects::nonNull)
 *     .reduce(1L, Math::multiplyExact);
 * ```
 */
class GroupOfStopPlacesPopularityCalculator(private val config: GroupOfStopPlacesConfig) {
    /**
     * Calculate raw popularity value (boost) for a GroupOfStopPlaces based on member stop popularities.
     *
     * @param memberPopularities List of popularity values for all member stop places
     * @return Raw popularity value (not normalized). Returns gosBoostFactor for empty groups.
     */
    fun calculatePopularity(memberPopularities: List<Long>): Double {
        if (memberPopularities.isEmpty()) {
            // Empty groups get just the boost factor
            return config.gosBoostFactor
        }

        // Calculate product of all member popularities
        // Use Double to handle large products (can exceed Long.MAX_VALUE)
        val product =
            memberPopularities.fold(1.0) { acc, popularity ->
                acc * popularity
            }

        // Apply gosBoostFactor
        return config.gosBoostFactor * product
    }
}
