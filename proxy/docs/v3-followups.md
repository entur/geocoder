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

### 1a. Stedsnavn usage CSV must be re-keyed before re-indexing (IGNORE)

Stedsnavn IDs in the index are `KVE:PlaceName:<lokal_id>` (see `nominatim-converter/src/source/stedsnavn/convert.rs`). The importance/popularity lookup (`importance_calc.calculate_importance_for(&id, ...)`) passes the canonical form. If the production usage CSV still has bare-numeric keys for stedsnavn rows, every stedsnavn entry will silently fall back to the default importance (`config.stedsnavn.default_value`), since `usage.rs` returns `1.0` on miss with no warning.

Two viable mitigations, pick one before the next reindex:

- **Re-key the CSV**: rewrite stedsnavn rows so the key column is `KVE:PlaceName:<lokal_id>` instead of `<lokal_id>`. This aligns stedsnavn with matrikkel/osm which already use canonical IDs.
- **Tolerate either form**: extend `UsageBoost::lookup` to try both `id` and `id.split(':').last()` so old CSVs keep working. Simpler operationally, but masks future ID changes the same way.

Until one of these is done, stedsnavn ranking will degrade silently after a reindex.

### 1b. Old Photon indexes still have bare-numeric stedsnavn place_ids (IGNORE)

A v2 client looking up `?ids=434810` already fails validation (`PeliasPlaceRequest` requires 3-part colon IDs) so this doesn't break the wire. But any tooling that talks to Photon directly and parsed numeric `place_id`/`osm_id` values for stedsnavn must be updated to handle the `KVE-PlaceName-N` shape. Audit dashboards, log scrapers, and analytics jobs that read raw Photon output.

## 2. Open design questions

### 2a. Reverse `radius` units (IGNORE)

Industry norm for reverse-geocoder radius is metres (Mapbox, Google, HERE). v3 currently uses kilometres on both autocomplete focus and reverse, with decimals accepted. The UX reviewer flagged this as a footgun: copy-pasting a `radius=500` from a Mapbox example into v3 means "500 km radius" which is meaningless.

If we want to flip reverse to metres later, that's still a breaking change but the V3 codebase is set up for it cleanly: divide by 1000 inside `PhotonReverseRequest.from(V3ReverseRequest)`. Autocomplete focus is a soft bias so the unit matters less - keep it km for symmetry.

Decision needed before public GA.

### 2b. `KVE:*` namespaces are not strictly Transmodel-canonical (IGNORE)

Stedsnavn lives in `KVE:PlaceName:N` and grunnkrets in `KVE:Borough:N` - dedicated per-concept namespaces rather than NeTEx-canonical entity types. NeTEx would model both as `TopographicPlace` entries with a `topographicPlaceType` discriminator; `Borough` in NeTEx specifically means an urban subdivision while grunnkrets also covers rural areas. The per-namespace approach is a pragmatic call: every ID is self-discriminating without length heuristics, and the namespaces will be opaque to NeTEx-aware downstream consumers (journey planner, NeTEx exports).

SSR subtype (`by`/`bydel`/`tettsted`/`tettsteddel`/`tettbebyggelse`) is not exposed in the v3 response - all five collapse to `layer=place`. If a client needs to differentiate, add a `topographicPlaceType` field to `Place`.

### 2d. Pre-existing bbox sentinel bug

Both `V3ResultTransformer.calculateBbox` and `PeliasResultTransformer.calculateBoundingBox` initialise `maxLon` and `maxLat` with `BigDecimal(Double.MIN_VALUE)`. `Double.MIN_VALUE` is the smallest *positive* double (~4.9e-324), not negative infinity. For features with negative longitude or latitude, `maxOf(sentinel, -5)` returns the sentinel, not `-5`.

Operational impact in Norway: zero (all of Norway is positive lon/lat). Fix is trivial - swap to `-Double.MAX_VALUE` or use a nullable accumulator. Drop it in opportunistically.

### 2e. `properties.name` as a JSON object breaks GeoJSON renderers

GeoJSON tools (Leaflet, Mapbox GL `text-field`, ogr2ogr) assume `properties.name` is a string. v3 makes it an object `{ default, label?, display }`, which renders as `[object Object]` in stock tooling.

Cheap fix while we can still break the contract: rename to `properties.names` (plural). The OpenAPI schema is already named `Names`, so this aligns the field name with the type name.

### 2f. `multimodal=parent` default is opinionated

Hides quays and platforms by default. Fine for journey-planner clients (they want to route to the stop place, not a specific platform), surprising for a general-purpose geocoder where "reverse-geocode my current position" should probably return the platform.

Two options: flip the default to `all`, or keep `parent` but document loudly in the OpenAPI description that child stops are hidden by default. Status quo (silent `parent`) is the worst of both.

### 2g. Parameter naming inconsistency

Request params mix short (`q`, `lat`, `lon`, `lang`) with long camelCase (`countyIds`, `localityIds`, `tariffZones`, `fareZoneAuthorities`). The metadata response echoes back yet a third form (`latitude`, `longitude`, `language`).

The mixed convention is defensible (short for the most common params, longer for compound ones), but the metadata echo should mirror the request keys: `lat`/`lon`/`lang`, not the long form. Cheap to align before GA.

### 2h. v3 weight semantics misleading at extremes

Photon's `location_bias_scale` doesn't behave as the v3 kdoc claims. Photon multiplies importance by `30 * scale`, then adds the location-bias term separately - so v3 `weight=1` -> `scale=0` zeroes out importance entirely (popular places lose their advantage), and `weight=0` -> `scale=1` gives full importance but Photon may still apply some location bias internally. The linear mapping is plausible UX but its actual effect on ranking is non-obvious.

Options:

- Apply the same tuned curve v2 uses (`LocationBiasCalculator`/`Geo.peliasScaleToPhotonZoom`) to v3's `weight` parameter. Pro: consistent v2 behaviour, well-tuned for Norway. Con: surprises the docs.
- Keep the linear mapping but document the actual effect: "weight is a linear blend between text/importance ranking (0) and pure location preference (1). At weight=1, popularity is ignored."

### 2i. Missing response fields (already in the plan)

These were flagged again by the reviewers; they were already noted as quick wins in the original plan §3 but listed here for completeness:

- `distance` (metres) per feature on reverse.
- `score` per feature (Photon's relevance score).
- All `alt_name` entries, not just the first.
- POI `description` (per-language map).
- Per-feature `bbox` for streets and group-of-stop-places (currently only top-level).
- Language-of-match indicator per name.

### 2j. Photon features not surfaced

Photon's `/api` accepts `bbox`, `osm_tag`, `dedupe`. Photon's `/reverse` accepts `query_string_filter` and a `distance_sort` toggle. None plumb through to v3. The original plan called out `bbox` and `categories` filters as quick wins. Re-confirmed by the panel.

### 2k. `Place` endpoint batch cap

`V3PlaceRequest` accepts an unbounded `ids` list. The OpenAPI says nothing. The reviewer-recommended cap is 100. Easy to enforce in code and document. Original plan §3g.

### 2l. `osm.public_transport.*` removed from emitted categories

The `osm.public_transport.{address,street,stop_place,poi,custom_poi,group_of_stop_places}` primary-entity tags are no longer emitted into the Nominatim NDJSON, and Photon docs no longer carry them. The proxy was migrated to use `layer.*` for include/exclude filtering. Anything that talks to Photon directly (analytics, dashboards, third-party tooling not under our control) and filters via `?include=osm.public_transport.X` is now silently broken. Migration path: replace with the corresponding `layer.X` filter (e.g. `osm.public_transport.address` -> `layer.address`, `osm.public_transport.group_of_stop_places` -> `layer.groupOfStopPlaces`).
