#!/usr/bin/env sh

set -eu

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
BINARY="$SCRIPTDIR/build/nominatim-convert"

VERSION="v0.2.0"
BASE_URL="https://github.com/entur/nominatim-convert/releases/download/$VERSION"

if [ ! -f "$BINARY" ]; then
    mkdir -p "$SCRIPTDIR/build"
    OS=$(uname -s)
    case "$OS" in
        Linux)  ARTIFACT="nominatim-convert-linux-x86_64" ;;
        Darwin) ARTIFACT="nominatim-convert-macos-aarch64" ;;
        *) echo "Unsupported OS: $OS" >&2; exit 1 ;;
    esac
    echo "Downloading nominatim-convert $VERSION..."
    curl -sfL --retry 2 "$BASE_URL/$ARTIFACT" -o "$BINARY"
    chmod +x "$BINARY"
fi

"$BINARY" "$@"
