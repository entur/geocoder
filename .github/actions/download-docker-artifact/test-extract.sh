#!/usr/bin/env bash

# Tests for extract.sh that simulate Docker and OCI image formats
# without requiring a running Docker daemon.
#
# Usage: bash test-extract.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEST_DIR=$(mktemp -d)
PASS=0
FAIL=0

cleanup() { rm -rf "$TEST_DIR"; }
trap cleanup EXIT

fail() { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }
pass() { echo "PASS: $1"; PASS=$((PASS + 1)); }

# Build a fake image tarball in Docker format and write it to $1.
# The image contains a single layer whose tar holds the file "hello.txt".
build_docker_format() {
  local out="$1"
  local dir="$TEST_DIR/build-docker"
  mkdir -p "$dir/layer-dir"

  echo "hello from docker format" > "$dir/layer-dir/hello.txt"
  tar -cf "$dir/layer.tar" -C "$dir/layer-dir" hello.txt

  # manifest.json as docker save produces it
  cat > "$dir/manifest.json" <<'JSON'
[{"Config":"blobs/sha256/config","RepoTags":["reg/repo:tag"],"Layers":["layer.tar"]}]
JSON

  tar -cf "$out" -C "$dir" manifest.json layer.tar
  rm -rf "$dir"
}

# Build a fake image tarball in OCI format and write it to $1.
build_oci_format() {
  local out="$1"
  local dir="$TEST_DIR/build-oci"
  mkdir -p "$dir/blobs/sha256"

  # Create the artifact inside a layer tar
  local layer_dir="$dir/layer-content"
  mkdir -p "$layer_dir"
  echo "hello from oci format" > "$layer_dir/data.ndjson"
  tar -cf "$dir/blobs/sha256/layerdigest" -C "$layer_dir" data.ndjson

  # Manifest blob referencing the layer
  cat > "$dir/blobs/sha256/manifestdigest" <<'JSON'
{"schemaVersion":2,"layers":[{"digest":"sha256:layerdigest","size":1}]}
JSON

  # index.json pointing to the manifest
  cat > "$dir/index.json" <<'JSON'
{"schemaVersion":2,"manifests":[{"digest":"sha256:manifestdigest","size":1}]}
JSON

  tar -cf "$out" -C "$dir" index.json blobs
  rm -rf "$dir"
}

# Wrap extract.sh so that "docker pull" and "docker save" are replaced by
# shell functions that return our pre-built tarball.
run_extract() {
  local tarball="$1"
  local dest="$2"
  local single_file="${3:-true}"

  # Create a shim that overrides the docker command
  local shim_dir="$TEST_DIR/shim"
  mkdir -p "$shim_dir"
  cat > "$shim_dir/docker" <<SHIM
#!/usr/bin/env bash
if [ "\$1" = "pull" ]; then
  exit 0
elif [ "\$1" = "save" ]; then
  cat "$tarball"
else
  echo "unexpected docker command: \$*" >&2
  exit 1
fi
SHIM
  chmod +x "$shim_dir/docker"

  mkdir -p "$dest"
  PATH="$shim_dir:$PATH" bash "$SCRIPT_DIR/extract.sh" "reg" "repo:tag" "$dest" "$single_file"
}

# --- Test 1: Docker format ---
test_docker_format() {
  local name="docker format extraction"
  local tarball="$TEST_DIR/docker-image.tar"
  local dest="$TEST_DIR/out-docker"
  build_docker_format "$tarball"

  if run_extract "$tarball" "$dest"; then
    if [ -f "$dest/hello.txt" ] && grep -q "hello from docker format" "$dest/hello.txt"; then
      pass "$name"
    else
      fail "$name - file content mismatch"
    fi
  else
    fail "$name - script exited with error"
  fi
}

# --- Test 2: OCI format ---
test_oci_format() {
  local name="OCI format extraction"
  local tarball="$TEST_DIR/oci-image.tar"
  local dest="$TEST_DIR/out-oci"
  build_oci_format "$tarball"

  if run_extract "$tarball" "$dest"; then
    if [ -f "$dest/data.ndjson" ] && grep -q "hello from oci format" "$dest/data.ndjson"; then
      pass "$name"
    else
      fail "$name - file content mismatch"
    fi
  else
    fail "$name - script exited with error"
  fi
}

# --- Test 3: Unsupported format fails gracefully ---
test_unsupported_format() {
  local name="unsupported format fails"
  local tarball="$TEST_DIR/bad-image.tar"
  local dest="$TEST_DIR/out-bad"

  # Tarball with neither manifest.json nor index.json
  local dir="$TEST_DIR/build-bad"
  mkdir -p "$dir"
  echo "junk" > "$dir/junk.txt"
  tar -cf "$tarball" -C "$dir" junk.txt
  rm -rf "$dir"

  if run_extract "$tarball" "$dest" 2>/dev/null; then
    fail "$name - expected non-zero exit"
  else
    pass "$name"
  fi
}

# --- Test 4: GITHUB_OUTPUT is written ---
test_github_output() {
  local name="GITHUB_OUTPUT is written"
  local tarball="$TEST_DIR/docker-image2.tar"
  local dest="$TEST_DIR/out-ghout"
  local gh_output="$TEST_DIR/github_output.txt"
  build_docker_format "$tarball"

  GITHUB_OUTPUT="$gh_output" run_extract "$tarball" "$dest"

  if grep -q "artifact_file=$dest/hello.txt" "$gh_output"; then
    pass "$name"
  else
    fail "$name - expected artifact_file in GITHUB_OUTPUT"
    cat "$gh_output"
  fi
}

# --- Run all tests ---
test_docker_format
test_oci_format
test_unsupported_format
test_github_output

echo ""
echo "Results: $PASS passed, $FAIL failed"
[ "$FAIL" -eq 0 ] || exit 1
