package no.entur.geocoder.converter.source.stedsnavn

import no.entur.geocoder.converter.ConverterConfig.StedsnavnConfig

/**
 * Calculates popularity scores for Norwegian place names (stedsnavn).
 *
 * Currently implements a flat popularity value matching kakka's placeBoost configuration.
 * All target place types (by, bydel, tettsted, tettsteddel, tettbebyggelse) receive
 * the same popularity score.
 *
 * This matches kakka's behavior where only PLACE entities receive the popularity boost,
 * while administrative units (Fylke, Kommune, Grunnkrets) receive no boost.
 *
 * Formula: popularity = defaultValue (currently flat, no differentiation)
 *
 * Future enhancement: Could be made configurable to support differentiated popularity
 * by place type (e.g., by > tettsted > tettbebyggelse) via configuration.
 *
 * Reference:
 * - kakka placeBoost configuration: 40
 * - kakka geocoderPlaceTypeWhitelist: tettsteddel,bydel,by,tettsted,tettbebyggelse
 */
class StedsnavnPopularityCalculator(private val config: StedsnavnConfig) {
    /**
     * Calculate popularity for a place name.
     *
     * Currently returns a flat value (from config) for all place types, matching kakka's behavior.
     * The navneobjekttype parameter is accepted for future differentiation support.
     *
     * @param navneobjekttype The place type from GML (e.g., "by", "tettsted") - currently unused
     * @return Popularity score (from config, default 40.0 for all types)
     */
    fun calculatePopularity(navneobjekttype: String? = null): Double = config.defaultValue
}
