package no.entur.geocoder.proxy.common

/**
 * Fallback values when a Pelias client sends `focus.point` without `focus.scale`/`focus.weight`.
 * Tuned together: exact mappings to Photon `zoom` and `location_bias_scale` are pinned in
 * `PhotonAutocompleteRequestTest.applies FocusDefaults ...`.
 */
object FocusDefaults {
    /** Default `focus.scale` in kilometres. */
    const val SCALE_KM: Int = 2500

    /**
     * Default `focus.weight` (mapped by `LocationBiasCalculator` to the Photon `scale` param).
     * `1.2` is calibrated to land near `scale = 0.5`, the same balance as v3's `weight = 0.5`
     * default. Deliberately diverges from the upstream Pelias default `15` (which would give
     * `scale ~ 0.2`) so importance and location bias contribute roughly equally to ranking;
     * keeps far-focus major cities winning against near-focus same-prefix streets.
     */
    const val WEIGHT: Double = 1.2
}
