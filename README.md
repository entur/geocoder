# Geocoder

Geocoding service consisting of a Photon search engine and a proxy service.

## Deployment Scenarios

### Proxy

Builds and deploys on push to main branch.

- Deploys to **dev**
- Review needed for deploy to **staging**

### Photon

Manual build and deploy via GitHub Actions workflow.

**Workflow options:**
- `build_import`: Controls data import
    - `no` - Build/deploy only (uses latest Photon Data)
    - `yes` - Full import (Nominatim + Photon data)
    - `only-photon-data` - Photon Data only (uses latest Nominatim Data)
- `deploy_env`: Deployment target (`none`, `dev`, `staging`, `both`)
- `photon_data_image_tag`: Tag to use when `build_import=no` (default: `latest`)
- `photon_jar_url`: Custom Photon JAR URL

**Data pipeline:**
1. **Nominatim Data** - Converts OSM/Kartverket/StopPlace/etc data to Nominatim format
2. **Photon Data** - Imports Nominatim data into Photon search index
3. **Photon Image** - Builds Docker image with Photon JAR and Photon Data
4. **Deploy** - Deploys to selected environments. Review required for staging & prod.

Data artifacts are stored as Docker images in GCR for efficient transfer and caching.


## Usage

### Running locally

```
# Build geocoder
./gradlew build

# Import and convert data
curl -sfLo photon.jar https://github.com/entur/photon/releases/download/2025-11-03/photon-0.7.0.jar
converter/create-nominatim-data.sh # creates nominatim.ndjson
converter/create-photon-data.sh    # creates the opensearch data folder for photon

# Run Photon
java -jar photon.jar

# Switch to a different terminal and start the proxy, or just run ProxyKt from your IDE
java -jar proxy/build/libs/proxy-all.jar
```

Now try some example requests:
```bash
curl -s http://localhost:8080/v2/autocomplete?text=sk%C3%B8yen%20stasjon&size=20
curl -s http://localhost:8080/v2/reverse?point.lat=59.92&point.lon=10.67&boundary.circle.radius=1&size=10&layers=address%2Clocality
```
You can also access Photon directly:
```bash
curl -s http://localhost:2322/api?q=Berglyveien&include=layer.stopplace
```
Or use the opensearch endpoint to debug queries:
```bash
curl -s http://localhost:9201/photon/_mapping | jq .       # Available fields
curl -s http://localhost:9201/photon/_doc/719158973 | jq . # Get document by ID
```

### Using a patched Photon version

#### Build and release patched Photon

* Fetch Photon from source (https://github.com/komoot/photon) and make your changes
* Build your branch with `./gradlew build`
* Push it to a branch on EnTur's fork (https://github.com/entur/photon)
* Draft a new release at https://github.com/entur/photon/releases/new
* Click "Select tag" --> "Create new tag" and enter a tag name
* Select Target: `<your branch name>`
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
  If you made changes to the Photon import code you need to check the "Build import image", otherwise
  you can just deploy to e.g. `dev`.

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
