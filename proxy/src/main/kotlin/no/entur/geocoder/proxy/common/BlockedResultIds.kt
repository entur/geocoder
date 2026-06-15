package no.entur.geocoder.proxy.common

/**
 * IDs that must never appear in proxy results, regardless of endpoint
 * (v2/v3, autocomplete/reverse/place). Matched against [Extra.id] and
 * filtered out centrally in `PhotonApi.convertResponse`.
 *
 * This is a band-aid: prefer fixing the data upstream (poiman /
 * nominatim-converter / NSR). Block here only as a stopgap, and remove the
 * entry once the source is corrected.
 *
 * Match is against the canonical index id as Photon returns it in
 * `extra.id` (e.g. `NSR:StopPlace:64116`), BEFORE the v2/v3 transformers
 * normalise it - so use that form, not a legacy v2 wire id.
 *
 * Each entry must say what is wrong and link a tracking issue so the list
 * does not silently mask upstream bugs forever.
 */
object BlockedResultIds {
    val ids: Set<String> =
        setOf(
            // Berlin Hbf is currently out of service: https://entur.slack.com/archives/C560UP56W/p1781510022951439
            "NSR:StopPlace:64116",
        )

    fun isBlocked(id: String?): Boolean = id != null && id in ids
}
