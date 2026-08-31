# AGENTS.md

Guidelines for AI coding assistants working on the Geocoder project.

## Entur standards

Read and follow https://github.com/entur/ai/blob/main/AGENTS.md, plus the docs it links for
the task at hand (e.g. java.md, helm.md, docker.md).

## Layout

- `proxy/` - the only Gradle module. Ktor HTTP server exposing v2 (Pelias-compatible) and v3
  APIs. Packages under `no.entur.geocoder.proxy`: `common`, `health`, `pelias`, `photon`, `v3`.
- `photon/` - not a Gradle module: Photon config, import scripts, Dockerfile, dev scripts.
- `helm/` - charts for `geocoder-proxy` and `geocoder-photon`.

The proxy transforms requests and responses. It does not rank: scoring lives in the Photon
fork (separate repo), and the proxy only filters, prunes and reshapes what Photon returns.

## Build

```bash
./gradlew build          # all modules with tests
./gradlew test
./gradlew ktlintCheck
./gradlew ktlintFormat
```

Running Photon and the proxy locally, including building an index: see README.md.

## Cross-repo invariants

These fail silently. Nothing here produces a compile error or a red test when it drifts, only
wrong query results at runtime.

| In this repo | Must agree with |
| --- | --- |
| `common/Category.kt` string constants | `nominatim-converter/src/common/category.rs` |
| `proxy/src/main/resources/transliteration.csv` | the converter's copy, byte-identical. Changing it invalidates the index and needs a reindex |
| `V3Result.StopPlaceRole` enum names | the converter's `StopPlaceRole::as_str` (pinned by a test) |
| the `:FareZone:` branch in `PhotonFilterBuilder` | the converter's `tariff_zone_refs` shape check |
| `VERSION` in `photon/import/create-nominatim-data.sh` | the config keys in `photon/import/config/converter-*.json`. The converter rejects unknown keys, so a new key needs its release published and the pin bumped in the same commit; rollbacks move both together |
| v3 request and response code | `proxy/src/main/resources/openapi3.yml` (parameter names, defaults, schemas) |

## Conventions

- Kotlin, max line 140. `.editorconfig` holds the limit and disables several ktlint standard
  rules; check there before "fixing" style.
- HTTP handlers are suspend functions.
- Pipeline: user request -> internal request -> Photon -> internal response -> user response.
  Request and response models stay separate per API version (`pelias`, `v3`, `photon`).
- v3 JSON keys and query parameters are camelCase. Never snake_case.
- Coordinates are WGS84 (EPSG:4326) and serialize through BigDecimal to pin scale
  (`Util.toBigDecimalWithScale`), so precision does not drift between versions.
- Tests use `kotlin.test` assertions; endpoint tests use Ktor `testApplication`.

## Categories

Categories are the filter mechanism: the converter writes them into the index and the proxy
turns query parameters into `include`/`exclude` category filters. Prefixes are declared in
`common/Category.kt` (`layer.`, `country.`, `tariff_zone_id.`, `stop_place_type.`,
`legacy.category.`, `county_gid.` and so on), plus `source.<name>` built from the `sources`
parameter.

Values are case-sensitive and mostly camelCase after the prefix: `layer.stopPlace`, not
`layer.stopplace`. A wrong case silently matches nothing.

Photon accepts only `[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+`, so `String.asCategory()` maps colons
to dots and transliterates the rest. It must produce byte-identical output to the converter's
`as_category`.

## Error handling

- v2: `ErrorHandler.kt`, Pelias-style error bodies.
- v3: `v3respond` in `App.kt`, RFC 9457 `application/problem+json` with `status`, `title`,
  `detail`.

## Photon data flow

The image tag and the data tag are generated once and shared, so `geocoder-photon:<tag>` always
pairs with `photon-data/<tag>/photon_data.tar.gz`. The container fetches `$PHOTON_DATA_URL` on
startup, verifies the `.sha256` sidecar, extracts atomically, and writes a `photon_data/.ready`
sentinel. `helm/geocoder-photon/templates/photon-data-validation.yaml` fails the helm render
when the URL is missing, so a manual `helm upgrade` without injection never reaches the cluster.
Bucket layout and rollback: README.md.

## Avoid

- Breaking Pelias compatibility in v2 endpoints. Known exception, from moving fare zones to
  their own source: v2 `tariff_zones` reorders on 934 stops and gains one entry on
  `NSR:StopPlace:63766`, where NSR held a stale zone version. No stop loses a zone.
- Removing legacy category prefixes without a migration plan.
- Changing boost, popularity or importance without measuring the ranking effect. The viable
  bands are narrow, and the geocoder-acceptance-tests suite is the check that catches it.
- Writing to a local `photon` index you still want to compare against (`_update` and friends).
  It perturbs tie-broken rankings irreversibly. Rebuild, or work on a copy.

## Key files

| Purpose | Location |
| --- | --- |
| Server entry and routing | `proxy/src/main/kotlin/no/entur/geocoder/proxy/App.kt` |
| Category constants and `asCategory` | `proxy/src/main/kotlin/no/entur/geocoder/proxy/common/Category.kt` |
| Query parameter to category filters | `proxy/src/main/kotlin/no/entur/geocoder/proxy/photon/PhotonFilterBuilder.kt` |
| Photon client | `proxy/src/main/kotlin/no/entur/geocoder/proxy/photon/PhotonApi.kt` |
| Pelias API implementation | `proxy/src/main/kotlin/no/entur/geocoder/proxy/pelias/PeliasApi.kt` |
| OpenAPI specs | `proxy/src/main/resources/openapi2.yml` (v2), `openapi3.yml` (v3) |
| Photon import scripts and config | `photon/import/`, `photon/import/config/` |
| Dependency versions | `gradle/libs.versions.toml` |
