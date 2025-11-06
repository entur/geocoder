#!/usr/bin/env sh
set -eu

REGISTRY="$1"
IMAGE="$2"
DESTINATION="$3"

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
mkdir -p "$DESTINATION"

# Get the filename from the tar archive before extracting
ARTIFACT_FILENAME=$(tar -tf "$TEMP_DIR/blobs/sha256/$BLOB_HASH" | head -n1)
echo "Found file in blob: $ARTIFACT_FILENAME"

tar -xf "$TEMP_DIR/blobs/sha256/$BLOB_HASH" -C "$DESTINATION" || { echo "ERROR: Failed to extract file"; exit 1; }

rm -rf "$TEMP_DIR"

# Set output
echo "artifact_file=${DESTINATION}/${ARTIFACT_FILENAME}" >> "$GITHUB_OUTPUT"
ls -lh "${DESTINATION}/${ARTIFACT_FILENAME}"
