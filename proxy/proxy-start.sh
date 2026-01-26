#!/usr/bin/env sh

# Align memory with helm/geocoder-proxy/values.yaml.  Helm values should be at least 25% higher.
java -Xms640m -Xmx640m -XX:+ExitOnOutOfMemoryError -XX:MaxGCPauseMillis=100 --enable-native-access=ALL-UNNAMED \
     -jar proxy.jar