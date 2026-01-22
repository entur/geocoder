#!/usr/bin/env sh

java -Xms256m -Xmx256m -XX:+UseZGC \
     --enable-native-access=ALL-UNNAMED -jar proxy.jar