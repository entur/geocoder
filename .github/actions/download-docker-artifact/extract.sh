#!/usr/bin/env bash

set -euo pipefail

REGISTRY="$1"
IMAGE="$2"
DESTINATION="$3"
SINGLE_FILE=${4:-true}

if ! mkdir -p "$DESTINATION"; then
  echo "Error: Couldn't create dir '$DESTINATION"
  exit 1
fi

FULL_IMAGE="${REGISTRY}/${IMAGE}"
echo "Pulling image: $FULL_IMAGE"
docker pull "$FULL_IMAGE"

# Export and extract image
TEMP_DIR=$(mktemp -d)
docker save "$FULL_IMAGE" | tar -xC "$TEMP_DIR"

# Resolve layer path - support both Docker and OCI image formats
if [ -f "$TEMP_DIR/manifest.json" ]; then
  # Docker format: manifest.json is an array with Layers paths
  LAYER_PATH=$(jq -r '.[0].Layers[0]' "$TEMP_DIR/manifest.json")
  [ "$LAYER_PATH" != "null" ] || { echo "ERROR: No layers found in manifest.json"; jq . "$TEMP_DIR/manifest.json"; exit 1; }
elif [ -f "$TEMP_DIR/index.json" ]; then
  # OCI format: index.json -> manifest blob -> layer blobs
  MANIFEST_DIGEST=$(jq -r '.manifests[0].digest' "$TEMP_DIR/index.json" | sed 's/sha256://')
  LAYER_DIGEST=$(jq -r '.layers[0].digest' "$TEMP_DIR/blobs/sha256/$MANIFEST_DIGEST" | sed 's/sha256://')
  LAYER_PATH="blobs/sha256/$LAYER_DIGEST"
else
  echo "ERROR: Unsupported image format - found neither manifest.json nor index.json"
  ls -la "$TEMP_DIR"
  exit 1
fi

echo "Extracting layer: $LAYER_PATH"

# Get the filename from the layer archive before extracting
ARTIFACT_FILENAME=$(tar -tf "$TEMP_DIR/$LAYER_PATH" | head -1)
NUM_ENTRIES=$(tar -tf "$TEMP_DIR/$LAYER_PATH" | wc -l | tr -d ' ')
echo "First entry (of $NUM_ENTRIES) in archive: $ARTIFACT_FILENAME"

tar -xf "$TEMP_DIR/$LAYER_PATH" -C "$DESTINATION" || { echo "ERROR: Failed to extract file"; exit 1; }

if [ "$SINGLE_FILE" = "true" ] && [ "$NUM_ENTRIES" -gt 1 ]; then
  echo "Error: There are more than one file in this archive.  Add 'false' as a 4th argument if want all the files."
  exit 1
fi

if [ "$SINGLE_FILE" = "true" ] && [ ! -f "$DESTINATION/$ARTIFACT_FILENAME" ]; then
  echo "Error: '$DESTINATION/$ARTIFACT_FILENAME' is not a file"
  exit 1
fi
rm -rf "$TEMP_DIR"

# Set output
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "artifact_file=${DESTINATION}/${ARTIFACT_FILENAME}" >> "$GITHUB_OUTPUT"
fi
echo "Extracted to $DESTINATION"