#!/usr/bin/env bash

set -euo pipefail

# Fail unless <registry>/<image_name>:<image_tag> exists and is a build tag.
# Usage: ./verify-tag.sh <registry> <image_name> <image_tag>

REGISTRY="$1"
IMAGE_NAME="$2"
IMAGE_TAG="$3"

if [[ "$IMAGE_TAG" != *-SHA* ]]; then
  echo "Error: '$IMAGE_TAG' is not a build tag (<branch>.<timestamp>-SHA<sha>)" >&2
  exit 1
fi

IMAGE="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
if ! gcloud container images describe "$IMAGE" > /dev/null 2>&1; then
  echo "Error: $IMAGE does not exist" >&2
  exit 1
fi
