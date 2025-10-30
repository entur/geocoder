#!/usr/bin/env sh

SCRIPTDIR=$(cd $(dirname $0); pwd)
JAR=$SCRIPTDIR/build/libs/converter-all.jar
if [ ! -f "$JAR" ]; then
  echo "Could not find $JAR. Run './gradlew assemble' to create it."
  exit 1
fi

java -Xmx6g -jar $JAR $@

