# v3 API follow-ups

Captured during the panel review of the v3 work. Two sections: (1) **deployment notes** that must accompany any v3 rollout, and (2) **open design questions** flagged by the reviewers (Photon / Transmodel / Pelias / general geocoder UX / clean code) that are out of scope for the initial v3 launch but worth a deliberate decision later.

## 1. Deployment notes

### 1a. Stedsnavn usage CSV must be re-keyed before re-indexing

Stedsnavn IDs in the index changed from bare numeric `434810` to `KVE:PlaceName:434810` (see `nominatim-converter/src/source/stedsnavn/convert.rs`). The importance/popularity lookup (`importance_calc.calculate_importance_for(&id, ...)`) now passes the canonical form. If the production usage CSV still has bare-numeric keys for stedsnavn rows, every stedsnavn entry will silently fall back to the default importance (`config.stedsnavn.default_value`), since `usage.rs` returns `1.0` on miss with no warning.

Two viable mitigations, pick one before the next reindex:

- **Re-key the CSV**: rewrite stedsnavn rows so the key column is `KVE:PlaceName:<lokal_id>` instead of `<lokal_id>`. This aligns stedsnavn with matrikkel/belagenhet/osm which already use canonical IDs.
- **Tolerate either form**: extend `UsageBoost::lookup` to try both `id` and `id.split(':').last()` so old CSVs keep working. Simpler operationally, but masks future ID changes the same way.

Until one of these is done, stedsnavn ranking will degrade silently after a reindex.

### 1b. Old Photon indexes still have bare-numeric stedsnavn place_ids

A v2 client looking up `?ids=434810` already fails validation (`PeliasPlaceRequest` requires 3-part colon IDs) so this doesn't break the wire. But any tooling that talks to Photon directly and parsed numeric `place_id`/`osm_id` values for stedsnavn must be updated to handle the new `KVE-PlaceName-N` shape. Audit dashboards, log scrapers, and analytics jobs that read raw Photon output.

## 2. Open design questions

### 2a. Reverse `radius` units

Industry norm for reverse-geocoder radius is metres (Mapbox, Google, HERE). v3 currently uses kilometres on both autocomplete focus and reverse, with decimals accepted. The UX reviewer flagged this as a footgun: copy-pasting a `radius=500` from a Mapbox example into v3 means "500 km radius" which is meaningless.

If we want to flip reverse to metres later, that's still a breaking change but the V3 codebase is set up for it cleanly: divide by 1000 inside `PhotonReverseRequest.from(V3ReverseRequest)`. Autocomplete focus is a soft bias so the unit matters less - keep it km for symmetry.

Decision needed before public GA.

### 2b. `KVE:PlaceName:` is not a Transmodel-canonical entity

NeTEx would call SSR settlements (`by`, `bydel`, `tettsted`, `tettsteddel`, `tettbebyggelse`) `TopographicPlace` entries with appropriate type codes. We chose `KVE:PlaceName:` because `KVE:TopographicPlace:` is already overloaded three ways (fylker, kommuner, streets). The pragmatic call is right but worth flagging:

- `KVE:PlaceName:N` will be opaque to downstream NeTEx-aware consumers (journey planner, NeTEx exports). They'll treat it as an unrecognised entity.
- The real semantic debt is the street-as-`TopographicPlace` overload in `nominatim-converter/src/source/matrikkel/convert.rs:162`. Moving streets to a different namespace (e.g. `KVE:Road:KOMNR-NAME`) would free `TopographicPlace` for SSR settlements and align with NeTEx. Separate ticket.

For now, document `KVE:PlaceName:` as a geocoder-local extension in v3.md (already partially done in the ID format section).

### 2c. v3.md response shape diff is too thin

Pelias reviewer's strongest point. v2 emits a long list of flat properties (`popular_name`, `street`, `housenumber`, `postalcode`, `country_a`, `county`, `county_gid`, `locality`, `locality_gid`, `borough`, `borough_gid`, `label`, `category`, `mode`, `tariff_zones`, `distance`, `accuracy`, `source_id`, `gid`, `description`). The current doc only narrates a few high-level structural changes. A property-by-property v2 -> v3 mapping table (or "dropped") would save migrating clients real time.

Notable v2 fields with no v3 mapping documented:

- `gid` (the `source:layer:id` triple - core Pelias shape). Gone in v3? Renamed?
- `source_id`, `accuracy`, `popular_name`, `distance`, `tariff_zones`, `mode`, `description` - need explicit mapping or "removed".
- `*_gid` family (`county_gid`, `locality_gid`, `borough_gid`) - v3 drops the WOF prefix; say so explicitly.
- v2 reverse carries `distance`; v3 currently doesn't.

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
