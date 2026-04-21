package no.entur.geocoder.common

/**
 * Fallback values when a Pelias client sends `focus.point` without `focus.scale`/`focus.weight`.
 * Tuned together: exact mappings to Photon `zoom` and `location_bias_scale` are pinned in
 * `PhotonAutocompleteRequestTest.applies FocusDefaults ...`.
 */
object FocusDefaults {
    /** Default `focus.scale` in kilometres. */
    const val SCALE_KM: Int = 10

    /** Default `focus.weight` (mapped by `LocationBiasCalculator` to the Photon `scale` param). */
    const val WEIGHT: Double = 28.0
}
