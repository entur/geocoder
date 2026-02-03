#!/usr/bin/env sh

set -eu

SCRIPTDIR=$(cd "$(dirname "$0")"; pwd)
"$SCRIPTDIR/../gradlew" shadowJar >/dev/null
JAR=$SCRIPTDIR/build/libs/converter-all.jar

java -Xmx6g -jar "$JAR" "$@"
