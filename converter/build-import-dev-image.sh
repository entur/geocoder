#!/bin/bash
set -e

echo "Building import image with buildx --load..."
docker --context=default buildx build --load -t geocoder-import:local -f Dockerfile.dev .
