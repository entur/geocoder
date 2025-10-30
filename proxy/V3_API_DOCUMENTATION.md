# V3 API

Example v3 geocoding API

## V1 → V3 Parameter Migration

| V1                           | V3                      |
|------------------------------|-------------------------|
| `text`                       | `query`                 |
| `point.lat` / `point.lon`    | `latitude` / `longitude`|
| `size`                       | `limit`                 |
| `lang`                       | `language`              |
| `boundary.country`           | `countries`             |
| `boundary.county.ids`        | `countyIds`             |
| `boundary.locality.ids`      | `localityIds`           |
| `tariff_zone_ids`            | `tariffZones`           |
| `layers`                     | `placeTypes`            |
| `transport_mode`             | `transportModes`        |
| `boundary.circle.radius`     | `radius`                |

## Endpoints

### `GET /v3/autocomplete`

**Parameters:**
- `query` (required) - Search text
- `limit` (default: 10) - Max results
- `language` (default: "no")
- `placeTypes` - Comma-separated: ADDRESS, STREET, LOCALITY, STATION, POI, etc.
- `sources` - Filter by data source
- `countries`, `countyIds`, `localityIds`, `tariffZones`, `transportModes`

**Example:**
```
GET /v3/autocomplete?query=oslo&limit=5&placeTypes=STATION
```

### `GET /v3/reverse`

**Parameters:**
- `latitude` (required)
- `longitude` (required)
- `radius` - Search radius in meters
- `limit` (default: 10)
- `language` (default: "no")

**Example:**
```
GET /v3/reverse?latitude=59.91&longitude=10.75&radius=1000
```

## Response Format

```json
{
  "results": [
    {
      "id": "NSR:StopPlace:123",
      "name": "Oslo S",
      "displayName": "Oslo S, Jernbanetorget 1, 0154 Oslo",
      "placeType": "STATION",
      "location": {
        "latitude": 59.910919,
        "longitude": 10.750882
      },
      "address": {
        "streetName": "Jernbanetorget",
        "houseNumber": "1",
        "postalCode": "0154",
        "locality": "Oslo",
        "county": "Oslo",
        "countryCode": "NO"
      },
      "transportModes": ["rail", "metro", "bus"],
      "tariffZones": ["1"],
      "source": {
        "provider": "National Stop Register",
        "sourceId": "NSR:123",
        "accuracy": "EXACT"
      }
    }
  ],
  "metadata": {
    "query": { "text": "oslo s", "limit": 10, "language": "no" },
    "resultCount": 1,
    "timestamp": 1728561234567,
    "boundingBox": {
      "southwest": { "latitude": 59.91, "longitude": 10.75 },
      "northeast": { "latitude": 59.911, "longitude": 10.751 }
    }
  }
}
```

## Place Types

- `ADDRESS` - Street addresses
- `STREET` - Streets
- `LOCALITY` - Cities, towns
- `BOROUGH` - Districts
- `COUNTY` - Administrative counties
- `VENUE` - Buildings
- `STOP_PLACE` - Transit stops
- `STATION` - Train/metro stations
- `POI` - Points of interest

## Accuracy

- `EXACT` - Precise coordinate
- `INTERPOLATED` - Calculated position
- `APPROXIMATE` - Estimated position

## Data Sources

- **Kartverket** - Norwegian addresses
- **NSR** - Public transport
- **OpenStreetMap** - POIs

## Error Responses

```json
{
  "error": "Invalid parameters",
  "message": "Parameter 'latitude' is required",
  "statusCode": 400
}
```

**Status Codes:**
- `400` - Invalid parameters
- `502` - Backend error
- `503` - Connection failed
- `500` - Unexpected error


