

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
      "place_id": -820042863,
      "object_type": "N",
      "object_id": -820042863,
      "categories": [
        "osm.stop_place"
      ],
      "rank_address": 30,
      "importance": 0.000010,
      "name": {
        "name": "Dorvonjárga"
      },
      "address": {
        "city": "KVE:TopographicPlace:5610"
      },
      "postcode": null,
      "country_code": "no",
      "centroid": [
        25.815626,
        69.401136
      ],
      "bbox": [
        25.815626,
        69.401136,
        25.815626,
        69.401136
      ],
      "parent_place_id": null,
      "housenumber": null,
      "extratags": {}
    }
  ]
}
```
