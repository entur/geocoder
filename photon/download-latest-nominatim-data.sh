#!/usr/bin/env bash
# Download the latest Nominatim NDJSON data from the production Docker image.
# Useful for local debugging — extracts the data artifact into the current directory.
# Usage: ./download-latest-nominatim-data.sh [suffix] [tag]
#   suffix: e.g. '-se' for country-specific data (default: none)
#   tag:    Docker image tag (default: latest-prod)

set -euo pipefail

SUFFIX=${1:-} # e.g. '-se'
TAG=${2:-latest-prod}

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
"$SCRIPTDIR/../.github/actions/download-docker-artifact/extract.sh" eu.gcr.io/entur-system-1287 "geocoder-nominatim-data$SUFFIX:$TAG" .
