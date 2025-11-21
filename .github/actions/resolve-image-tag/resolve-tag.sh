#!/usr/bin/env bash

set -euo pipefail

# Resolve Docker image tag from 'latest' to semantic version
# Usage: ./resolve-tag.sh <registry> <image_name> <image_tag>
# Example: ./resolve-tag.sh eu.gcr.io/entur-system-1287 geocoder-proxy latest

REGISTRY="${1:-eu.gcr.io/entur-system-1287}"
IMAGE_NAME="${2:-geocoder-proxy}"
IMAGE_TAG="${3:-latest}"

# Get all tags
IMAGE="${REGISTRY}/${IMAGE_NAME}"
TAGS=$(gcloud container images list-tags "$IMAGE" --filter "$IMAGE_TAG" --format="get(tags)" --limit=1)

# Extract the semantic tag
RESOLVED_TAG=$(echo "$TAGS" | tr ';' '\n' | grep -v '^latest$' | head -n 1)

if [ -z "$RESOLVED_TAG" ]; then
  echo "Error: Could not resolve '$IMAGE_TAG' to a semantic tag" >&2
  exit 1
fi

echo "$RESOLVED_TAG"

