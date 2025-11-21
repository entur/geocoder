#!/usr/bin/env bash

set -euo pipefail

FILE_PATH="$1"

TEMP_DIR=$(mktemp -d)
cp "$FILE_PATH" "$TEMP_DIR/"
FILE_NAME=$(basename "$FILE_PATH")

cat > "$TEMP_DIR/Dockerfile" << EOF
FROM scratch
COPY ${FILE_NAME} /${FILE_NAME}
EOF

echo "$TEMP_DIR"
