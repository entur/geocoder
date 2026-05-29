package no.entur.geocoder.proxy.common

/**
 * Rewrites for interim index shapes that may still exist during a rolling
 * reindex: docs written by a previous converter version carry
 * `OSM:PointOfInterest:N` (now `OSM:TopographicPlace:N`) and `borough:N` or
 * bare-numeric grunnkrets gids (now `KVE:Borough:N`).
 *
 * DELETE this object and its call sites together once production is fully
 * reindexed against the current converter. Observable trigger: a Photon query
 * with `include=OSM.PointOfInterest.<known poi id>` returns nothing, and
 * spot-checked address docs carry `KVE:Borough:`-prefixed `borough_gid`s.
 */
object InterimIds {
    private const val INTERIM_OSM_POI_PREFIX = "OSM:PointOfInterest:"
    private const val OSM_TOPO_PREFIX = "OSM:TopographicPlace:"
    private const val INTERIM_BOROUGH_PREFIX = "borough:"
    private const val KVE_BOROUGH_PREFIX = "KVE:Borough:"

    /** Map an interim `OSM:PointOfInterest:N` to the canonical `OSM:TopographicPlace:N`. */
    fun canonicaliseOsmId(id: String): String =
        if (id.startsWith(INTERIM_OSM_POI_PREFIX)) {
            OSM_TOPO_PREFIX + id.removePrefix(INTERIM_OSM_POI_PREFIX)
        } else {
            id
        }

    /**
     * Inverse of [canonicaliseOsmId]: the interim alias to also look up for a
     * canonical OSM id (query both shapes during the rollover), or null when not
     * applicable.
     */
    fun interimOsmAlias(id: String): String? =
        if (id.startsWith(OSM_TOPO_PREFIX)) {
            INTERIM_OSM_POI_PREFIX + id.removePrefix(OSM_TOPO_PREFIX)
        } else {
            null
        }

    /**
     * Map grunnkrets gid shapes to the canonical `KVE:Borough:N`: canonical input
     * passes through, interim `borough:N` and bare-numeric `N` are prefixed, and
     * anything else returns null (malformed - better dropped than emitted).
     */
    fun canonicaliseBoroughGid(raw: String): String? =
        when {
            raw.startsWith(KVE_BOROUGH_PREFIX) -> raw
            raw.startsWith(INTERIM_BOROUGH_PREFIX) -> KVE_BOROUGH_PREFIX + raw.removePrefix(INTERIM_BOROUGH_PREFIX)
            raw.isNotEmpty() && raw.all { it.isDigit() } -> KVE_BOROUGH_PREFIX + raw
            else -> null
        }

    /** The bare grunnkretsnummer from any accepted gid shape, or null (malformed). */
    fun boroughNumber(raw: String): String? =
        canonicaliseBoroughGid(raw)?.removePrefix(KVE_BOROUGH_PREFIX)
}
