package no.entur.geocoder.proxy.photon

import kotlin.math.max
import kotlin.math.pow

object LocationBiasCalculator {
    private const val PELIAS_MAX_WEIGHT: Double = 50.0
    private const val CURVE_EXPONENT: Double = 0.185

    /**
     * Maps Pelias `focus.weight` to Photon's `location_bias_scale` parameter.
     *
     * In Photon (see `OpenSearchSearchHandler`), `location_bias_scale` (locally named `scale`)
     * primarily *attenuates the importance contribution* to a hit's score: importance enters as
     * `IMPORTANCE_FACTOR * scale * importance` (with `IMPORTANCE_FACTOR = 30`), while the
     * location-bias contribution enters as `(1 - scale) * locationContribution` where
     * `locationContribution` peaks near 1.0 at the focus point and decays with distance. The two
     * terms have very different magnitudes: importance can reach `30 * scale * 0.5 ~ 3` for a
     * decent hit, while location bias caps near `1 - scale`. So lower Photon `scale` does not
     * mean "mostly location bias" - it mostly dampens importance, with location bias as a
     * smaller additive term that mainly tie-breaks among similar-importance candidates. Other
     * components (normalised OpenSearch text relevance, `QueryReranker`) also contribute up to
     * ~1.0 each to the final score.
     *
     * Pelias `focus.weight` is inverted: higher weight = stronger focus emphasis = lower Photon
     * scale. The curve `1 - (weight / 50)^0.185` interpolates through `(0 -> 1.0)`, `(15 -> 0.2)`,
     * `(50 -> 0.0)`; the 0.185 exponent is reverse-engineered to hit the `15 -> 0.2` anchor
     * (Pelias API default `weight=15`). Monotonically decreasing and smooth.
     */
    fun calculateLocationBias(peliasWeight: Double): Double {
        val weight = max(0.0, peliasWeight)
        val normalized = weight / PELIAS_MAX_WEIGHT
        val curved = normalized.pow(CURVE_EXPONENT)
        return max(0.0, 1.0 - curved)
    }
}
