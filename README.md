## Usage

```
./gradlew clean build
java -jar build/libs/netex-photon-all.jar input-netex.xml output-photon.nbjson
```

## Some references

* [Boost calculation in kakka](https://github.com/entur/kakka/blob/f8dbc8225e0cd84c013f6f4695a60e9f0b82c280/src/main/java/no/entur/kakka/geocoder/routes/pelias/mapper/netex/StopPlaceToPeliasMapper.java#L120)
* [Boost config in kakka](https://github.com/entur/kakka/blob/master/helm/kakka/env/values-kub-ent-prd.yaml#L38)
* [photon](https://photon.komoot.de/)
* [photon pelias adapter](https://github.com/stadtulm/photon-pelias-adapter)
* [list of photon alt names](https://github.com/komoot/photon/blob/master/app/opensearch/src/main/java/de/komoot/photon/opensearch/PhotonDocSerializer.java#L99)
* [OSM dumps for photon](https://download1.graphhopper.com/public/experimental/extracts/by-country-code/no/)
* [Nominatim DB fields](https://nominatim.org/release-docs/latest/develop/Database-Layout/)
* [Photon JSON import PR](https://github.com/komoot/photon/pull/885)
* [Pelias](https://github.com/entur/pelias-api)
* [bau - geocoder comparison tool](https://github.com/entur/bau)

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

### photon
```json5
{
      "type": "Feature", //OK
      "properties": {
        "osm_type": "W", //ny (object_type)
        "osm_id": 800000001, //ny (object_id)
        "osm_key": "railway", //ny
        "osm_value": "station", //ny
        "type": "street", //ny
        "postcode": "0154", //ny (postcode)
        "countrycode": "NO", //ny (country_code)
        "name": "Oslo Central Station", //OK (name.name)
        "country": "Norway", //ny (rel. country_a) 
        "city": "Oslo", //ny (address.city)
        "locality": "Sentrum", //OK (rel. locality_gid)
        "street": "Jernbanetorget 1", //OK (address.street)
        "county": "Oslo", //OK (address.county)
        "extent": [10.748, 59.912, 10.755, 59.909] //ny
      },
      "geometry": { //OK
        "type": "Point",
        "coordinates": [
          10.752011,
          59.910004
        ]
      }
}
```

### nominatim import (ndjson)
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

### Exporter WIP (ndjson)
```json5
{
  "type": "Place",
  "content": [
    {
      "place_id": 854487540,
      "object_type": "N",
      "object_id": 854487540,
      "categories": [],
      "rank_address": 30,
      "importance": 0.000010,
      "parent_place_id": 0,
      "name": {
        "name": "Skøyen stasjon"
      },
      "address": {
        "street": "NOT_AN_ADDRESS-NSR:StopPlace:152",
        "county": "Oslo"
      },
      "postcode": "unknown",
      "country_code": "no",
      "centroid": [
        10.678831,
        59.922353
      ],
      "bbox": [
        59.922353,
        10.678831,
        59.922353,
        10.678831
      ],
      "extratags": {
        "id": "NSR:StopPlace:152",
        "gid": "openstreetmap:venue:NSR:StopPlace:152",
        "layer": "venue",
        "source": "openstreetmap",
        "source_id": "NSR:StopPlace:152",
        "accuracy": "point",
        "country_a": "NOR",
        "county_gid": "whosonfirst:county:KVE:TopographicPlace:03",
        "locality": "Oslo",
        "locality_gid": "whosonfirst:locality:KVE:TopographicPlace:0301",
        "label": "Skøyen stasjon,Oslo",
        "category": "railStation",
        "tariff_zones": "BRA:TariffZone:311,RUT:FareZone:4,RUT:TariffZone:1"
      }
    }
  ]
}
```
