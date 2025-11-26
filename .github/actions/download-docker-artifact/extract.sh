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

# Get blob hash from repositories file
IMAGE_NAME="${IMAGE}"
REPO_KEY="${REGISTRY}/${IMAGE_NAME%:*}"
TAG="${IMAGE_NAME##*:}"
BLOB_HASH=$(jq -r ".\"$REPO_KEY\".\"$TAG\" // empty" "$TEMP_DIR/repositories")

[ -n "$BLOB_HASH" ] || { echo "ERROR: Blob hash not found for $REPO_KEY:$TAG"; jq . "$TEMP_DIR/repositories"; exit 1; }

# Extract file from blob
echo "Extracting from blob ${BLOB_HASH}"

# Get the filename from the tar archive before extracting
ARTIFACT_FILENAME=$(tar -tf "$TEMP_DIR/blobs/sha256/$BLOB_HASH" | head -n1)
NUM_ENTRIES=$(tar -tf "$TEMP_DIR/blobs/sha256/$BLOB_HASH" | wc -l | sed 's/ //g')
echo "First entry (of $NUM_ENTRIES) in archive: $ARTIFACT_FILENAME"

tar -xf "$TEMP_DIR/blobs/sha256/$BLOB_HASH" -C "$DESTINATION" || { echo "ERROR: Failed to extract file"; exit 1; }

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