## Usage

#### Building everything, converting and importing NeTEx data, and running photon + the proxy

```bash
docker compose up --build
```
Now try some example requests, e.g.

* http://localhost:8080/v1/autocomplete?text=sk%C3%B8yen%20stasjon&size=20
* http://localhost:8080/v1/reverse?point.lat=59.92&point.lon=10.67&boundary.circle.radius=1&size=10&layers=address%2Clocality


## Manual usage

If you don't like docker, you can also run the converter and photon manually.

#### Manually building the converter and converting data
```bash
./gradlew build
curl -sfL https://nedlasting.geonorge.no/geonorge/Basisdata/MatrikkelenAdresse/CSV/Basisdata_03_Oslo_25833_MatrikkelenAdresse_CSV.zip | jar -xv
curl -sfL https://storage.googleapis.com/marduk-production/tiamat/03_Oslo_latest.zip | jar -xv
java -jar converter/build/libs/converter-all.jar \
          -s ./tiamat-*.xml \
          -m Basis*/*.csv \
          -p converter/src/test/resources/oslo-center.osm.pbf \
          -o /tmp/output-photon.nbjson
```

#### Manually importing the converted data to photon

```bash
cd ..
git clone https://github.com/komoot/photon.git
cd photon
./gradlew build
java -jar target/photon-0.7.0.jar -nominatim-import -import-file /tmp/output-photon.nbjson -languages no \
     -extra-tags id,gid,layer,source,source_id,accuracy,country_a,county_gid,locality,locality_gid,label,category,tariff_zones
```
Start the server with `java -jar target/photon-0.7.0.jar`, and visit e.g. http://localhost:2322/api?q=jernbanetorget&limit=20 to see the imported data.

#### Manually running the pelias-impersonating proxy
```bash
java -jar proxy/build/libs/proxy-all.jar
```
Go to http://localhost:8080/ for an overview of the available endpoints.

Example query:
```bash
http://localhost:8080/v1/autocomplete?text=oslo&tariff_zone_ids=INN
```

## Some references

* [Boost calculation in kakka](https://github.com/entur/kakka/blob/f8dbc8225e0cd84c013f6f4695a60e9f0b82c280/src/main/java/no/entur/kakka/geocoder/routes/pelias/mapper/netex/StopPlaceToPeliasMapper.java#L120)
* [Boost config in kakka](https://github.com/entur/kakka/blob/master/helm/kakka/env/values-kub-ent-prd.yaml#L38)
* [photon](https://photon.komoot.de/)
* [photon pelias adapter](https://github.com/stadtulm/photon-pelias-adapter)
* [list of photon alt names](https://github.com/komoot/photon/blob/master/app/opensearch/src/main/java/de/komoot/photon/opensearch/PhotonDocSerializer.java#L99)
* [OSM dumps for photon](https://download1.graphhopper.com/public/experimental/extracts/by-country-code/no/) from graphhopper
* [Nominatim DB fields](https://nominatim.org/release-docs/latest/develop/Database-Layout/) (database layout)
* [Photon JSON import PR](https://github.com/komoot/photon/pull/885) (outdated)
* [pelias-api @ entur](https://github.com/entur/pelias-api)
* [bau - geocoder comparison tool](https://github.com/entur/bau) hosted at https://ent-bau-dev.web.app/
* [Nominatim search tool](https://github.com/osm-search/Nominatim)


## Photon debugging

Query photon directly:

```bash
curl -s 'http://localhost:2322/api?q=Oslo&include=tariff_zone_id.rut' | jq .
```

Or use the opensearch endpoint to debug queries, e.g.

```bash
curl -s http://localhost:9201/photon/_mapping | jq .     # Available fields
curl -s http://localhost:9201/photon/_doc/719158973 | jq # Get document by ID
```

## Relevant example data formats

### pelias
```json5
{
      "type": "Feature", //OK
      "geometry": { //OK
        "type": "Point",
        "coordinates": [ 10.725322, 59.514887 ]
      },
      "properties": {
        "id": "NSR:StopPlace:59639",  //mangler
        "gid": "openstreetmap:venue:NSR:StopPlace:59639", //mangler
        "layer": "venue", //mangler
        "source": "openstreetmap", //mangler
        "source_id": "NSR:StopPlace:59639", //mangler
        "name": "Sonsveien stasjon", //OK
        "street": "NOT_AN_ADDRESS-NSR:StopPlace:59639", //OK
        "accuracy": "point", //mangler
        "country_a": "NOR", //mangler
        "county": "Akershus", //OK
        "county_gid": "whosonfirst:county:KVE:TopographicPlace:32", //mangler
        "locality": "Vestby", //OK
        "locality_gid": "whosonfirst:locality:KVE:TopographicPlace:3216", //mangler
        "label": "Sonsveien stasjon, Vestby", //mangler
        "category": [ //mangler
          "onstreetBus",
          "railStation"
        ],
        "tariff_zones": [ //mangler
          "RUT:TariffZone:3S",
          "RUT:FareZone:8"
        ],
      }
}
```

### nominatim ndjson import example
```json5
{
  "type": "Place",
  "content": [
    {
      "place_id": 108402,
      "object_type": "N",
      "object_id": 567528174,
      "categories": [
        "osm.highway.bus_stop"
      ],
      "rank_address": 30,
      "importance": 9.99999999995449E-6,
      "parent_place_id": 107700,
      "name": {
        "name": "Triesen Schule"
      },
      "address": {
        "city:de": "Triesen",
        "neighbourhood:de": "Oberdorf",
        "city": "Triesen",
        "street": "Landstrasse",
        "neighbourhood": "Oberdorf",
        "county": "Oberland"
      },
      "postcode": "9495",
      "country_code": "li",
      "centroid": [
        9.524946,
        47.1057983
      ],
      "bbox": [
        9.524946,
        47.1057983,
        9.524946,
        47.1057983
      ]
    }
  ]
}
```

### Converter ndjson output (WIP)
```json5
{
  "type": "Place",
  "content": [
    {
      "place_id": 719158973,
      "object_type": "N",
      "object_id": 719158973,
      "categories": [
        "osm.public_transport.stop_place",
        "tariff_zone_id.RUT",
        "tariff_zone_id.RUT"
      ],
      "rank_address": 30,
      "importance": 0.30000000000000004,
      "parent_place_id": 0,
      "name": {
        "name": "Berglyveien"
      },
      "address": {
        "county": "Oslo"
      },
      "postcode": "unknown",
      "country_code": "no",
      "centroid": [
        10.806430,
        59.828811
      ],
      "bbox": [
        59.828811,
        10.806430,
        59.828811,
        10.806430
      ],
      "extra": {
        "locality_gid": "KVE:TopographicPlace:0301",
        "country_a": "NOR",
        "locality": "Oslo",
        "accuracy": "point",
        "source": "nsr",
        "label": "Berglyveien,Oslo",
        "tariff_zones": "RUT:TariffZone:1,RUT:FareZone:4",
        "layer": "stopplace",
        "id": "NSR:StopPlace:6744",
        "source_id": "NSR:StopPlace:6744",
        "county_gid": "KVE:TopographicPlace:03",
        "transport_modes": "onstreetBus"
      }
    }
  ]
}
```

### photon output
http://localhost:2322/api?q=Berglyveien&include=layer.stopplace
```json5
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "osm_type": "N",
        "osm_id": 719158973,
        "osm_key": "public_transport",
        "osm_value": "stop_place",
        "type": "house",
        "countrycode": "NO",
        "name": "Berglyveien",
        "county": "Oslo",
        "extra": {
          "locality_gid": "KVE:TopographicPlace:0301",
          "country_a": "NOR",
          "transport_modes": "onstreetBus",
          "locality": "Oslo",
          "accuracy": "point",
          "source": "nsr",
          "label": "Berglyveien,Oslo",
          "tariff_zones": "RUT:TariffZone:1,RUT:FareZone:4",
          "id": "NSR:StopPlace:6744",
          "source_id": "NSR:StopPlace:6744",
          "county_gid": "KVE:TopographicPlace:03",
          "layer": "stopplace"
        }
      },
      "geometry": {
        "type": "Point",
        "coordinates": [
          10.80643,
          59.828811
        ]
      }
    }
  ]
}
```

### Proxy output (WIP)
http://localhost:8080/v1/autocomplete?text=berglyveien&layers=stopplace
```json5
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [
          10.80643,
          59.828811
        ]
      },
      "properties": {
        "id": "NSR:StopPlace:6744",
        "layer": "stopplace",
        "source": "nsr",
        "source_id": "NSR:StopPlace:6744",
        "name": "Berglyveien",
        "accuracy": "point",
        "country_a": "NOR",
        "county": "Oslo",
        "county_gid": "KVE:TopographicPlace:03",
        "locality": "Oslo",
        "locality_gid": "KVE:TopographicPlace:0301",
        "label": "Berglyveien, Oslo",
        "transport_modes": [
          "onstreetBus"
        ],
        "tariff_zones": [
          "RUT:TariffZone:1",
          "RUT:FareZone:4"
        ]
      }
    }
  ],
  "bbox": [
    10.80643,
    59.828811,
    10.80643,
    59.828811
  ]
}
```
