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
     * `1.6` lands near `scale = 0.47`, giving location bias a slight edge over importance
     * (53% loc / 47% imp). v2's default focus radius is wider than v3's (Photon `biasRadius`
     * ~115 km vs ~55 km), so the no-decay zone covers more terrain; location bias needs the
     * heavier weight here than v3 (which uses `weight = 0.5` -> `scale = 0.5`) to keep
     * near-focus winners on top when far competitors are still inside the no-decay offset.
     * Deliberately diverges from the upstream Pelias default `15` (which would give
     * `scale ~ 0.2`).
     */
    const val WEIGHT: Double = 1.6
}
