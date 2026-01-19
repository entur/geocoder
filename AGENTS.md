# AGENTS.md

Guidelines for AI coding assistants working on the Geocoder project.

## Project Overview

Geocoder is a Norwegian geocoding service with three modules:
- **proxy/** - Ktor HTTP API server providing v2 (Pelias-compatible) and v3 APIs
- **converter/** - CLI tool that converts OSM, Matrikkel, Stedsnavn, and transit data to Nominatim format
- **common/** - Shared domain models and utilities
- **photon/** - Photon docker build files

The proxy forwards requests to Photon (an OpenStreetMap-based search engine) after transforming request/response formats.

## Build Commands

```bash
./gradlew build          # Build all modules with tests
./gradlew test           # Run tests only
./gradlew ktlintCheck    # Check code style
./gradlew ktlintFormat   # Auto-fix code style
./gradlew :proxy:build   # Build specific module
```

## Code Conventions

### Kotlin Style
- Max line length: 140 characters
- Package structure: `no.entur.geocoder.{module}.{feature}`
- Use data classes for DTOs and value objects
- Use companion objects for factory methods and constants

### Architecture Patterns
- All HTTP handlers are suspend functions (coroutine-based)
- Request transformation pipeline: User Request → Internal Request → Photon → Internal Response → User Response
- Keep request/response models separate for each API version (v2, v3, photon)

### Testing
- Tests use JUnit Jupiter with Kotlin Test assertions
- Use Ktor TestHost for API testing
- Test files mirror source structure

## Important Areas

### Coordinate Handling
- Use BigDecimal for coordinate precision in JSON serialization
- Coordinates are in WGS84 (EPSG:4326) for external APIs

### Category System
Categories use prefixes for filtering:
- `osm.*` - OpenStreetMap categories
- `source.kartverket.*` - Norwegian mapping data
- `tariff_zone_id.*` - Transit zone filtering
- Maintain backward compatibility with legacy prefixes

### Error Handling
Centralized in `proxy/src/main/kotlin/no/entur/geocoder/proxy/ErrorHandler.kt`:
- Client errors → 400
- Backend parse errors → 502
- Connection failures → 503

## Things to Avoid

- Don't break Pelias API compatibility in v2 endpoints
- Don't load entire PBF files into memory (use streaming/Sequence)
- Don't remove legacy category prefixes without migration plan
- Don't change boost/popularity weights without understanding impact on search ranking

## Key Files

| Purpose | Location |
|---------|----------|
| Server entry & routing | `proxy/src/main/kotlin/no/entur/geocoder/proxy/App.kt` |
| Pelias API implementation | `proxy/src/main/kotlin/no/entur/geocoder/proxy/pelias/PeliasApi.kt` |
| Photon client | `proxy/src/main/kotlin/no/entur/geocoder/proxy/photon/PhotonApi.kt` |
| CLI entry point | `converter/src/main/kotlin/no/entur/geocoder/converter/cli/Command.kt` |
| Boost configuration | `converter/src/main/kotlin/no/entur/geocoder/converter/ConverterConfig.kt` |
| OpenAPI spec | `proxy/src/main/resources/openapi.yml` |
| Dependency versions | `gradle/libs.versions.toml` |