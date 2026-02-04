#!/usr/bin/env bash

set -euo pipefail

SUFFIX=${1:-} # e.g. '-se'
TAG=${2:-latest-prod}

SCRIPTDIR=$(cd "$(dirname $0)"; pwd)
$SCRIPTDIR/../.github/actions/download-docker-artifact/extract.sh eu.gcr.io/entur-system-1287 "geocoder-nominatim-data$SUFFIX:$TAG" .
