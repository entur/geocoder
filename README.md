# Geocoder

Geocoding service consisting of a Photon search engine and a proxy service.

## Deployment

### Proxy

**Automatic Deployment:**
- **Push to main** → Builds → Deploys to **dev** → Runs acceptance tests

**Manual Deployment via Workflow Dispatch:**
- `🚀 Deploy → Staging` - Deploy to staging (uses existing image)
- `🚀 Deploy → Prod` - Deploy to production (uses existing image)

**Workflow Inputs:**
- `image_tag` - Specify image tag (default: `latest`)

### Photon

**Scheduled Build:**
- **Daily at 07:32 UTC** → Full data import → Build → Deploy to **all environments**

**Manual Build/Deploy via Workflow Dispatch:**
- `🔨 Download and convert data → build Photon → Dev` - Full data pipeline + deploy to dev
- `⚡ Use latest data → build Photon → Dev` - Build using latest Nominatim data + deploy dev
- `🚀 Deploy → Staging` - Deploy pre-built image to staging
- `🚀 Deploy → Prod` - Deploy pre-built image to production

**Workflow Inputs:**
- `photon_image_tag` - Image tag to deploy (default: `latest`)
- `photon_jar_url` - Custom Photon JAR URL (optional)

**Data Pipeline:**
1. **Nominatim Data** - Converts OSM/Kartverket/StopPlace/etc data → `nominatim.ndjson.gz`
2. **Photon Data** - Imports Nominatim data into Photon search index → `photon_data.tar.gz`
3. **Photon Image** - Builds Docker image using Photon JAR + search index
4. **Deploy** - Deploys to selected environments (no review required)

💾 Data artifacts are stored in GCR Docker images (e.g., `geocoder-nominatim-data:latest`, `geocoder-photon-data:latest`).

### Acceptance Tests

- Runs automatically after every deployment
- Uses [geocoder-acceptance-tests](https://github.com/entur/geocoder-acceptance-tests) repository


## Usage

### Running locally

```
# Build geocoder
./gradlew build

# Download a photon jar
curl -sfLo photon.jar https://github.com/entur/photon/releases/download/os-metrics-and-version/photon-0.7.0.jar

# EITHER import and convert data
converter/create-nominatim-data.sh # creates nominatim.ndjson
converter/create-photon-data.sh    # creates the photon_data search index for Photon

# OR just download the latest Photon search index built by Github Actions
rm -rf photon_data
./geocoder/converter/download-latest-photon-data.sh
tar xzvf photon_data.tar.gz

# Run Photon
java -jar photon.jar

# Switch to a different terminal and start the proxy (or just run ProxyKt from your IDE)
java -jar proxy/build/libs/proxy-all.jar
```

Now try some example requests:
```bash
curl -s http://localhost:8080/v2/autocomplete?text=sk%C3%B8yen%20stasjon&size=20
curl -s http://localhost:8080/v2/reverse?point.lat=59.92&point.lon=10.67&boundary.circle.radius=1&size=10&layers=address%2Clocality
```
Adding `&debug=true` will also reveal native Photon results with `importance` (input weight) and `score` (calculated weight).

You can also access Photon directly:
```bash
curl -s http://localhost:2322/api?q=Berglyveien&include=layer.stopplace
```
Or use the opensearch endpoint to debug queries:
```bash
curl -s http://localhost:9201/photon/_mapping | jq .       # Available fields
curl -s http://localhost:9201/photon/_doc/719158973 | jq . # Get document by ID
```

### Debugging data in k8s / GKE

Accessing the opensearch queries in k8s:
```bash
kubectl --context dev port-forward geocoder-photon-85994c94dd-6lqhv -n geocoder 9201
curl -s 'https://geocoder-photon.dev.entur.io/api?q=ullerud' |jq  '.features[].properties.osm_id' |head -1
200127208213
curl -s 'http://localhost:9201/photon/_doc/200127208213' |jq -c "[._source.importance, ._source.name.default]"
[0.23010299956639815,"Ullerud terrasse"]
```

### Verifying score and importance

We set the `importance` field in the Nominatim data, while `score` is calculated by Photon.

```
$ curl -s 'http://localhost:8080/v2/autocomplete?text=Oslo&debug=true&size=1' \
  | jq -c '.geocoding.debug.raw_data[] | [.localeTags.name.default, .infos.importance, .score]'
["Oslo",1.0,51.235104]
["Oslo lufthavn",0.347712,26.492702]
["Oslo S",0.330103,25.840235]
["Oslo bussterminal",0.330103,24.307642]
```
<small>(Debug shows three more results than we ask for, see PhotonAutocompleteRequest.RESULT_PRUNING_HEADROOM)</small>
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

* Go to [build-photon.yml](.github/workflows/build-photon.yml) in `geocoder` and
  update `on.workflow_dispatch.inputs.photon_jar_url.default` variable with the new link
* Push your `geocoder` changes
* Go to https://github.com/entur/geocoder/actions/workflows/build-photon.yml and trigger the workflow.
  You should set "Import and convert data" --> `only-photon-data` (or `all`) to avoid OpenSearch data
  inconsistencies, and deploy to e.g. `dev`.

## Links

### Grafana dashboards

* [Photon metrics](https://grafana.entur.org/d/VpZ62_2Wk/jvm-overview-prometheus?orgId=1&var-datasource=000000002&var-label=app&var-name=geocoder-photon&var-prometheus_group=kub-ent-dev-001&from=now-6h&to=now)
* [Proxy metrics](https://grafana.entur.org/d/VpZ62_2Wk/jvm-overview-prometheus?orgId=1&var-datasource=000000002&var-label=app&var-name=geocoder-proxy&var-prometheus_group=kub-ent-dev-001&from=now-6h&to=now)
* [v1 vs v2 metrics](https://grafana.entur.org/d/bf2mxeovemi9sc/geocoder-v1-vs-v2?orgId=1&var-cluster_environment=dev&from=now-30m&to=now)
* [pelias parameter usage](https://grafana.entur.org/d/bez56ipo02t4we/geocoder-v1-endpoint-parameter-usage?orgId=1&refresh=30s)

### Internal references

* [v1 vs v2 comparison tool (bau)](https://ent-bau-dev.web.app/)
* [bau repo](https://github.com/entur/bau)
* [Boost calculation in kakka](https://github.com/entur/kakka/blob/f8dbc8225e0cd84c013f6f4695a60e9f0b82c280/src/main/java/no/entur/kakka/geocoder/routes/pelias/mapper/netex/StopPlaceToPeliasMapper.java#L120)
* [Boost config in kakka](https://github.com/entur/kakka/blob/master/helm/kakka/env/values-kub-ent-prd.yaml#L38)
* [geocoder acceptance tests](https://github.com/entur/geocoder-acceptance-tests)
* [pelias-api @ entur](https://github.com/entur/pelias-api)

### External references

* [photon](https://photon.komoot.de/)
* [photon pelias adapter](https://github.com/stadtulm/photon-pelias-adapter)
* [list of photon alt names](https://github.com/komoot/photon/blob/master/app/opensearch/src/main/java/de/komoot/photon/opensearch/PhotonDocSerializer.java#L99)
* [OSM dumps for photon](https://download1.graphhopper.com/public/experimental/extracts/by-country-code/no/) from graphhopper
* [Nominatim DB fields](https://nominatim.org/release-docs/latest/develop/Database-Layout/) (database layout)
* [Photon JSON import PR](https://github.com/komoot/photon/pull/885) (outdated)
* [Nominatim search tool](https://github.com/osm-search/Nominatim)
