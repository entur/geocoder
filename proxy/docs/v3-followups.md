# v3 API follow-ups

Captured during the panel review of the v3 work. Two sections: (1) **deployment notes** that must accompany any v3 rollout, and (2) **open design questions** flagged by the reviewers (Photon / Transmodel / Pelias / general geocoder UX / clean code) that are out of scope for the initial v3 launch but worth a deliberate decision later.

## 1. Deployment notes

### 1c. Street id lookups only work after the transliteration reindex

Before the transliteration change, street id categories with spaces or Norwegian
characters (`KVE.TopographicPlace.0301-Karl Johans gate`) were silently dropped
by Photon's category charset filter at index time - street place lookups for
such names have never worked. They start working only once the index is rebuilt
with the transliterating converter. No interim fallback is possible (there is
no old category shape to fall back to). The acceptance cases
`street-with-spaces` / `street-with-diacritics` (marked `status: pass`, i.e. the
post-reindex contract) will fail when run against an environment that has not
yet been reindexed - expected until the reindex completes.

The proxy-side interim fallbacks for `OSM:PointOfInterest:N` and `borough:N` /
bare-numeric grunnkrets gids live in `InterimIds.kt`, which also documents the
observable trigger for deleting them after the reindex.

## 2. Open design questions

### 2f. `multimodal=parent` default is opinionated

Hides multimodal children (quays, platforms) by default. Fine for journey-planner clients (they want to route to the stop place, not a specific platform), surprising for a general-purpose geocoder where "reverse-geocode my current position" should probably return the platform.

The behaviour is now documented in both v3.md and the OpenAPI parameter description. Remaining decision: keep `parent` or flip the default to `all`.

### 2h. v3 weight tuning

The linear `weight` mapping is now documented honestly (weight=1 ignores popularity entirely - kdoc and v3.md). Still open if ranking quality disappoints: apply the v2-tuned curve (`LocationBiasCalculator`/`Geo.peliasScaleToPhotonZoom`) to v3's `weight` instead of the linear mapping. Pro: well-tuned for Norway. Con: surprises the docs.

### 2i. Missing response fields (mostly shipped)

Shipped: `distance` (reverse, km), `description` (per-language map), per-feature `bbox` for streets and groups of stop places (converter computes real extents; needs the next reindex to appear in responses). Still open:

- All `alt_name` entries, not just the first (small; data already indexed `;`-joined).
- `score` per feature: blocked - Photon only emits the ES score in debug mode and /photon is off-limits; would require an upstream contribution.
- Language-of-match indicator: blocked - Photon does not track which language field matched.

### 2j. Photon features not surfaced (partially shipped)

Shipped: `bbox` viewport filter on `/v3/autocomplete`, `distanceSort` toggle on `/v3/reverse`. Still open:

- `categories` as a request filter (the principled version of Photon's raw `osm_tag` - v3 returns categories but does not accept them as a filter). Needs a design pass on the value set.
- `query_string_filter` on reverse: powerful but exposes ES query-string syntax; needs a curated wrapper if ever exposed.
- `dedupe` toggle: trivial, low value (debug tool).

### 2l. `osm.public_transport.*` removed from emitted categories

The `osm.public_transport.{address,street,stop_place,poi,custom_poi,group_of_stop_places}` primary-entity tags are no longer emitted into the Nominatim NDJSON, and Photon docs no longer carry them. The proxy was migrated to use `layer.*` for include/exclude filtering. Anything that talks to Photon directly (analytics, dashboards, third-party tooling not under our control) and filters via `?include=osm.public_transport.X` is now silently broken. Migration path: replace with the corresponding `layer.X` filter (e.g. `osm.public_transport.address` -> `layer.address`, `osm.public_transport.group_of_stop_places` -> `layer.groupOfStopPlaces`). Drop this note once production has been reindexed and the direct-Photon consumers audited.

## 3. Settled

Decisions made; recorded here so they don't get re-litigated.

- **Reverse `radius` stays km** (industry norm is metres; we keep km for v2 symmetry, decimals accepted).
- **`KVE:PlaceName:N` / `KVE:Borough:N`** are geocoder-local namespaces, not NeTEx-canonical entity types; opaque to NeTEx-aware consumers by design. SSR subtype is not exposed (`layer=place` for all five); add a `topographicPlaceType` field if a client ever needs it.
- **Stedsnavn usage CSV** keeps bare-numeric keys; ranking falls back to default importance on key-shape mismatch (accepted).
- **Bare-numeric stedsnavn place_ids in old Photon indexes**: tooling reading raw Photon output must handle `KVE-PlaceName-N` (accepted, no proxy impact).
- **bbox sentinel bug** fixed: nullable accumulators replace the broken `Double.MIN_VALUE` sentinel in both transformers.
- **`properties.name` -> `properties.names`**: renamed so stock GeoJSON tooling doesn't choke on an object where it expects a `name` string.
- **Metadata echo mirrors request keys**: `lat`/`lon`/`lang` instead of `latitude`/`longitude`/`language`.
- **`/v3/place` caps `ids` at 100** (code + OpenAPI `maxItems`).
- **`weight` extremes documented**: linear blend, weight=1 ignores popularity (kdoc + v3.md).
