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

### 2d. Pre-existing bbox sentinel bug

Both `V3ResultTransformer.calculateBbox` and `PeliasResultTransformer.calculateBoundingBox` initialise `maxLon` and `maxLat` with `BigDecimal(Double.MIN_VALUE)`. `Double.MIN_VALUE` is the smallest *positive* double (~4.9e-324), not negative infinity. For features with negative longitude or latitude, `maxOf(sentinel, -5)` returns the sentinel, not `-5`.

Operational impact in Norway: zero (all of Norway is positive lon/lat). Fix is trivial - swap to `-Double.MAX_VALUE` or use a nullable accumulator. Drop it in opportunistically.

### 2e. `properties.name` as a JSON object breaks GeoJSON renderers

GeoJSON tools (Leaflet, Mapbox GL `text-field`, ogr2ogr) assume `properties.name` is a string. v3 makes it an object `{ default, label?, display }`, which renders as `[object Object]` in stock tooling.

Cheap fix while we can still break the contract: rename to `properties.names` (plural). The OpenAPI schema is already named `Names`, so this aligns the field name with the type name.

### 2f. `multimodal=parent` default is opinionated

Hides multimodal children (quays, platforms) by default. Fine for journey-planner clients (they want to route to the stop place, not a specific platform), surprising for a general-purpose geocoder where "reverse-geocode my current position" should probably return the platform.

The behaviour is now documented in both v3.md and the OpenAPI parameter description. Remaining decision: keep `parent` or flip the default to `all`.

### 2g. Parameter naming inconsistency

Request params mix short (`q`, `lat`, `lon`, `lang`) with long camelCase (`countyIds`, `localityIds`, `tariffZones`, `fareZoneAuthorities`). The metadata response echoes back yet a third form (`latitude`, `longitude`, `language`).

The mixed convention is defensible (short for the most common params, longer for compound ones), but the metadata echo should mirror the request keys: `lat`/`lon`/`lang`, not the long form. Cheap to align before GA.

### 2h. v3 weight semantics misleading at extremes

Photon's `location_bias_scale` doesn't behave as the v3 kdoc claims. Photon multiplies importance by `30 * scale`, then adds the location-bias term separately - so v3 `weight=1` -> `scale=0` zeroes out importance entirely (popular places lose their advantage), and `weight=0` -> `scale=1` gives full importance but Photon may still apply some location bias internally. The linear mapping is plausible UX but its actual effect on ranking is non-obvious.

Options:

- Apply the same tuned curve v2 uses (`LocationBiasCalculator`/`Geo.peliasScaleToPhotonZoom`) to v3's `weight` parameter. Pro: consistent v2 behaviour, well-tuned for Norway. Con: surprises the docs.
- Keep the linear mapping but document the actual effect: "weight is a linear blend between text/importance ranking (0) and pure location preference (1). At weight=1, popularity is ignored."

### 2i. Missing response fields (partially shipped)

Shipped: `distance` per feature on reverse (km, 3-decimal precision) and `description` (per-language map). Still open:

- `score` per feature (Photon's relevance score).
- All `alt_name` entries, not just the first.
- Per-feature `bbox` for streets and group-of-stop-places (currently only top-level).
- Language-of-match indicator per name.

### 2j. Photon features not surfaced

Photon's `/api` accepts `bbox`, `osm_tag`, `dedupe`. Photon's `/reverse` accepts `query_string_filter` and a `distance_sort` toggle. None plumb through to v3. The original plan called out `bbox` and `categories` filters as quick wins. Re-confirmed by the panel.

### 2k. `Place` endpoint batch cap

`V3PlaceRequest` accepts an unbounded `ids` list. The OpenAPI says nothing. The reviewer-recommended cap is 100. Easy to enforce in code and document. Original plan §3g.

### 2l. `osm.public_transport.*` removed from emitted categories

The `osm.public_transport.{address,street,stop_place,poi,custom_poi,group_of_stop_places}` primary-entity tags are no longer emitted into the Nominatim NDJSON, and Photon docs no longer carry them. The proxy was migrated to use `layer.*` for include/exclude filtering. Anything that talks to Photon directly (analytics, dashboards, third-party tooling not under our control) and filters via `?include=osm.public_transport.X` is now silently broken. Migration path: replace with the corresponding `layer.X` filter (e.g. `osm.public_transport.address` -> `layer.address`, `osm.public_transport.group_of_stop_places` -> `layer.groupOfStopPlaces`). Drop this note once production has been reindexed and the direct-Photon consumers audited.

## 3. Settled

Decisions made; recorded here so they don't get re-litigated.

- **Reverse `radius` stays km** (industry norm is metres; we keep km for v2 symmetry, decimals accepted).
- **`KVE:PlaceName:N` / `KVE:Borough:N`** are geocoder-local namespaces, not NeTEx-canonical entity types; opaque to NeTEx-aware consumers by design. SSR subtype is not exposed (`layer=place` for all five); add a `topographicPlaceType` field if a client ever needs it.
- **Stedsnavn usage CSV** keeps bare-numeric keys; ranking falls back to default importance on key-shape mismatch (accepted).
- **Bare-numeric stedsnavn place_ids in old Photon indexes**: tooling reading raw Photon output must handle `KVE-PlaceName-N` (accepted, no proxy impact).
