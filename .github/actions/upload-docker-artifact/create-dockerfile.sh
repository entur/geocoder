#!/usr/bin/env sh
set -eu

FILE_PATH="$1"

TEMP_DIR=$(mktemp -d)
cp "$FILE_PATH" "$TEMP_DIR/"
FILE_NAME=$(basename "$FILE_PATH")

cat > "$TEMP_DIR/Dockerfile" << EOF
FROM scratch
COPY ${FILE_NAME} /${FILE_NAME}
EOF

# Set output
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  echo "temp_dir=$TEMP_DIR" >> "$GITHUB_OUTPUT"
fi
echo "Created Dockerfile for ${FILE_NAME}"
cat "$TEMP_DIR/Dockerfile"
