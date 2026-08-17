# Geocoder

Geocoding service consisting of a Photon backend search engine and a Proxy frontend.

## Deployment

All deployment runs from the `main` branch.

The daily import uses the `prod-approved` tag. Remember to update that whenever needed:
```
git tag -f prod-approved [sha]
git push origin prod-approved --force
```

### Proxy

**Automatic**
- Push to `main` → builds and deploys to dev. Further tst and prd deploys needs approval. All builds run acceptance tests after deployment.

**Manual**
- [proxy.yml](https://github.com/entur/geocoder/actions/workflows/proxy.yml) also supports manual dispatch (target: `dev only` | `dev → tst → prd` | `tst → prd`).
- [proxy-deploy.yml](https://github.com/entur/geocoder/actions/workflows/proxy-deploy.yml) — Deploy an existing image tag

### Photon

**Scheduled**
- Daily at 06:27 UTC: full data import + build + deploy to tst → prd (no approval gates). Checks out the `prod-approved` tag to avoid using untested commits, and updates the `latest-prod.txt` pointer.

**Manual:**
- [photon.yml](https://github.com/entur/geocoder/actions/workflows/photon.yml) — Import data, build Photon image, deploy (target: `dev only` | `dev → tst → prd` | `tst → prd`; optional `config`, default `converter-prod.json`)
- [photon-deploy.yml](https://github.com/entur/geocoder/actions/workflows/photon-deploy.yml) — Deploy an existing Photon image tag

### Sweden (dev only)

**Scheduled**
- [photon-sweden-scheduled.yml](https://github.com/entur/geocoder/actions/workflows/photon-sweden-scheduled.yml) — Mondays at 05:27 UTC: full data import + build + deploy to dev. Tracks `main` (Sweden never reaches prod, so there is no `prod-approved` tag) and updates `latest.txt`. Also keeps `photon-data-se/` inside the bucket's 90-day lifecycle window, so a running pod's `photon_data.tar.gz` can't be deleted out from under it.

**Manual:**
- [photon-sweden.yml](https://github.com/entur/geocoder/actions/workflows/photon-sweden.yml) — Import/build/deploy Photon for Sweden
- [proxy-sweden.yml](https://github.com/entur/geocoder/actions/workflows/proxy-sweden.yml) — Build/deploy Proxy for Sweden

### Scheduled & monitoring
- [cache-data-sources.yml](https://github.com/entur/geocoder/actions/workflows/cache-data-sources.yml) — Daily at 03:00 UTC: downloads the third-party source files (matrikkel, stedsnavn, custom POIs from poiman) plus PostHog popular-stops, verifies size, and uploads them to `gs://ent-geocoder-prd/data-sources/`. The nightly Photon import reads from this cache rather than hitting upstream directly.
- [monitor-photon-data.yml](https://github.com/entur/geocoder/actions/workflows/monitor-photon-data.yml) — Daily at 08:22 UTC: checks `photonImportDate` from the prod `/v2/info` endpoint and alerts Slack if the data is older than 50h.
- [api-docs.yml](https://github.com/entur/geocoder/actions/workflows/api-docs.yml) — Lints both OpenAPI specs (v2 `openapi.yml` + v3 `openapi3.yml`) on every push/PR touching `proxy/docs/**`, `openapi3.yml` or `.spectral.yml`; on push to `main` publishes the v3 spec to [developer.entur.no/apis/geocoder](https://developer.entur.no/apis/geocoder) and `proxy/docs/` to [the docs portal](https://developer.entur.no/docs/open-services/geocoder). The v2 spec is linted but no longer published.

Most workflows post a Slack notification on failure. The reusable [_generate-tag.yml](.github/workflows/_generate-tag.yml) and [_deploy-and-test.yml](.github/workflows/_deploy-and-test.yml) workflows back the build/deploy jobs; shared build steps live as composite actions under [.github/actions/](.github/actions/README.md).

### Photon data artifacts (GCS)

Built artifacts live in the public bucket [gs://ent-geocoder-prd/](https://console.cloud.google.com/storage/browser/ent-geocoder-prd?project=ent-geocoder-prd):

| Prefix                 | Contents                                     |
| ---------------------- | -------------------------------------------- |
| `nominatim-data/`      | `nominatim.ndjson.gz` per build (+ `.sha256`) |
| `nominatim-data-se/`   | Sweden variant                               |
| `photon-data/`         | `photon_data.tar.gz` per build (+ `.sha256`) |
| `photon-data-se/`      | Sweden variant                               |
| `data-sources/`        | Daily-refreshed source files (written by `cache-data-sources.yml`) |

Each build writes to `<prefix>/<tag>/<filename>`. The `<tag>` is generated once and shared between the docker image and the GCS upload, so `geocoder-photon:<tag>` always pairs with `gs://.../photon-data/<tag>/photon_data.tar.gz`. Pointer files at the prefix root track recent builds:

- `latest.txt` — most recent build from any branch
- `latest-prod.txt` — most recent build deployed to prod (written by `photon-scheduled.yml`)

The photon container fetches `photon_data.tar.gz` from `$PHOTON_DATA_URL` on startup, verifies its `.sha256` sidecar, and writes a `photon_data/.ready` sentinel after extraction so in-place container restarts skip the download. CI derives the URL from the image tag in [_deploy-and-test.yml](.github/workflows/_deploy-and-test.yml) and injects it into helm values; `templates/photon-data-validation.yaml` fails the helm render if it's missing.

**Rolling back to a previous build:**

```bash
# See available pointers and recent tags
curl -s https://storage.googleapis.com/ent-geocoder-prd/photon-data/latest-prod.txt

# Re-deploy a known-good image (the data is paired automatically)
gh workflow run photon-deploy.yml -f target='tst → prd' -f image_tag=<previous-tag>
```

**90-day lifecycle rule** (apply once per bucket):

```json
{
  "lifecycle": {
    "rule": [
      {
        "action": {"type": "Delete"},
        "condition": {
          "age": 90,
          "matchesPrefix": ["nominatim-data", "photon-data"],
          "matchesSuffix": [".gz", ".sha256"]
        }
      }
    ]
  }
}
```
The `matchesSuffix` filter spares the `latest*.txt` pointer files.


## Usage

### Running locally

```
# Build geocoder
./gradlew build

# Download a photon jar
cd photon
./import/download-photon-jar.sh

# EITHER download source data, convert to nominatim.ndjson (downloads nominatim-converter binary automatically)
./import/create-nominatim-data.sh import/config/converter-prod.json -z

# OR download the latest nominatim.ndjson build by Github Actions
./download-latest-nominatim-data.sh

# Create the photon index
./import/create-photon-data.sh nominatim.ndjson.gz

# OR just download the latest Photon search index built by Github Actions
rm -rf photon_data
./download-latest-photon-data.sh

# Run Photon
./photon-start.sh

# Switch to a different terminal and start the proxy (or just run `no.entur.geocoder.proxy.AppKt` from your IDE)
cd ../proxy
java -jar build/libs/proxy-all.jar
```

Now try some example requests:
```bash
curl -s 'http://localhost:8080/v3/autocomplete?q=sk%C3%B8yen%20stasjon&limit=20'
curl -s 'http://localhost:8080/v3/reverse?lat=59.92&lon=10.67&radius=1&limit=10&layers=address,locality'

# v2 (Pelias-compatible) takes different parameter names
curl -s 'http://localhost:8080/v2/autocomplete?text=sk%C3%B8yen%20stasjon&size=20'
```
Adding `&debug=true` will also reveal native Photon results with `importance` (input weight) and `score` (calculated weight).

You can also access Photon directly. Category values are case-sensitive, so `layer.stopPlace`
matches while `layer.stopplace` silently returns nothing:
```bash
curl -s 'http://localhost:2322/api?q=Berglyveien&include=layer.stopPlace'
```
Or use the opensearch endpoint. Document ids are the entity id with `:` replaced by `-`
(`NSR-StopPlace-58404`, `KVE-PostalAddress-12191345`), not numeric osm ids:
```bash
curl -s 'http://localhost:9201/photon/_mapping' | jq .   # Available fields
curl -s 'http://localhost:9201/photon/_doc/NSR-StopPlace-58404' | jq .
```
Handy when checking what the converter actually wrote, e.g. the indexed alt names a multimodal
parent inherited from its children:
```bash
curl -s 'http://localhost:9201/photon/_doc/NSR-StopPlace-58404' | jq -c '._source.name'
{"default":"Nationaltheatret","alt":"Nationaltheatret stasjon;Nasjonalteatret;Nationaltheatret"}
```

### Debugging data in k8s / GKE

Accessing the opensearch queries in k8s:
```bash
kubectl --context dev port-forward <geocoder-photon-pod> -n geocoder 9201

# Take extra.id from the API response and swap ':' for '-' to get the document id
ID=$(curl -s 'https://geocoder-photon.dev.entur.io/api?q=ullerud' \
  | jq -r '.features[0].properties.extra.id' | tr ':' '-')
echo $ID
NSR-StopPlace-5496
curl -s "http://localhost:9201/photon/_doc/$ID" | jq -c "[._source.importance, ._source.name.default]"
[0.078586,"Ullerud"]
```
`osm_id` is 0 for NSR documents, so it is not a usable key. Use `extra.id`.

### Verifying score and importance

We set the `importance` field in the Nominatim data, while `score` is calculated by Photon.

```
$ curl -s 'http://localhost:8080/v2/autocomplete?text=Oslo&debug=true&size=1' \
  | jq -c '.geocoding.debug.raw_data[] | [.localeTags.name.default, .infos.importance, .score]'
["Oslo",0.92,2.8717440524466067]
["Oslo S",0.538821,2.323909030548797]
["Oslo",0.27596,2.27596]
["Oslo lufthavn",0.550358,2.006965529061908]
```
<sub><sup>(Debug shows three more results than we ask for, see PhotonAutocompleteRequest.RESULT_PRUNING_HEADROOM. Both the importance and the score change with every index build, so treat the numbers as illustrative. The two "Oslo" rows are the group of stop places and the locality.)</sup></sub>
### Using a patched Photon version

#### Build and release patched Photon

* Fetch Photon from source (https://github.com/komoot/photon) and make your changes
* Build with `./gradlew build`
* Create a tag and push that (`git push --tags entur`) to EnTur's fork (https://github.com/entur/photon)
* Draft a new release at https://github.com/entur/photon/releases/new
* Click "Select tag" --> and select the tag name
* Fill in release title and description
* Add `photon-<tag>.jar` from Photon's `target/` folder as a binary asset
* Check "Set as a pre-release"
* Publish the release
* On the release page, right-click the `photon-<tag>.jar` asset and copy the link address

#### Update geocoder to use the patched Photon

* Go to [photon/import/download-photon-jar.sh](photon/import/download-photon-jar.sh) and
  update the `PHOTON_JAR` variable with the new link
* Push your `geocoder` changes
* Go to [photon.yml](https://github.com/entur/geocoder/actions/workflows/photon.yml) and trigger the workflow with target `dev only`.

## Links

### Grafana dashboards

* [Photon metrics](https://grafana.entur.org/d/VpZ62_2Wk/jvm-overview-prometheus?orgId=1&var-datasource=000000002&var-label=app&var-name=geocoder-photon&var-prometheus_group=kub-ent-dev-001&from=now-6h&to=now)
* [Proxy metrics](https://grafana.entur.org/d/VpZ62_2Wk/jvm-overview-prometheus?orgId=1&var-datasource=000000002&var-label=app&var-name=geocoder-proxy&var-prometheus_group=kub-ent-dev-001&from=now-6h&to=now)

### Internal references

* [nominatim-converter](https://github.com/entur/nominatim-converter): builds the index this proxy queries. Importance lives in `src/common/importance.rs`, categories in `src/common/category.rs`
* [entur/photon](https://github.com/entur/photon): the patched Photon fork that does the actual ranking. [PhotonDocSerializer](https://github.com/entur/photon/blob/master/src/main/java/de/komoot/photon/opensearch/PhotonDocSerializer.java) decides which name fields get indexed and at what priority
* [geocoder acceptance tests](https://github.com/entur/geocoder-acceptance-tests)
* [v2 vs v3 comparison tool (bau)](https://ent-bau-dev.web.app/) and the [bau repo](https://github.com/entur/bau)

### External references

* [photon](https://photon.komoot.io/)
* [photon pelias adapter](https://github.com/stadtulm/photon-pelias-adapter)
* [OSM dumps for photon](https://download1.graphhopper.com/public/experimental/extracts/by-country-code/no/) from graphhopper
* [Nominatim DB fields](https://nominatim.org/release-docs/latest/develop/Database-Layout/) (database layout)
* [Nominatim search tool](https://github.com/osm-search/Nominatim)
