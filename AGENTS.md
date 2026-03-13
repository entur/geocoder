# AGENTS.md

Guidelines for AI coding assistants working on the Geocoder project.

## Entur Standards

Read and follow the Entur platform standards at:
https://github.com/entur/ai/blob/main/AGENTS.md

When working on a specific task, also read the relevant docs
linked from that file (e.g. java.md, helm.md, docker.md).

## Project Overview

Geocoder is a Norwegian geocoding service with three modules:
- **proxy/** - Ktor HTTP API server providing v2 (Pelias-compatible) and v3 APIs
- **photon/** - Photon runtime: config, import scripts, Docker build, and dev convenience scripts
- **common/** - Shared domain models and utilities

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
- v3 API uses camelCase for all JSON keys and query parameters — no snake_case
- When changing v3 API code, ensure the implementation matches `openapi3.yml` (parameter names, defaults, response schemas)

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
- v2: Centralized in `ErrorHandler.kt`, returns Pelias-style error responses
- v3: Route-level error handling in `App.kt` (`v3problem`), returns RFC 9457 `application/problem+json` with `status`, `title`, `detail`

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
| Photon import scripts | `photon/import/` |
| Import config | `photon/import/config/` |
| OpenAPI spec (v2) | `proxy/src/main/resources/openapi.yml` |
| OpenAPI spec (v3) | `proxy/src/main/resources/openapi3.yml` |
| Dependency versions | `gradle/libs.versions.toml` |