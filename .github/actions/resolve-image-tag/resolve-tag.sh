#!/usr/bin/env bash

set -euo pipefail

# Resolve a Docker image tag alias (e.g. 'latest-prod') to its semantic
# version (e.g. 'main.20260427-102029-SHAd8b8b92'). When the input is
# already a semantic tag, return it unchanged.
#
# Usage: ./resolve-tag.sh <registry> <image_name> <image_tag>

REGISTRY="$1"
IMAGE_NAME="$2"
IMAGE_TAG="$3"

# Semantic tags contain '-SHA' followed by the short SHA. Aliases don't.
if [[ "$IMAGE_TAG" == *-SHA* ]]; then
  echo "$IMAGE_TAG"
  exit 0
fi

IMAGE="${REGISTRY}/${IMAGE_NAME}"
TAGS=$(gcloud container images list-tags "$IMAGE" --filter "tags:$IMAGE_TAG" --format="get(tags)" --limit=1)
RESOLVED_TAG=$(echo "$TAGS" | tr ';' '\n' | grep -- '-SHA' | head -n 1)

if [ -z "$RESOLVED_TAG" ]; then
  echo "Error: could not resolve '$IMAGE_TAG' to a semantic tag" >&2
  exit 1
fi

echo "$RESOLVED_TAG"
