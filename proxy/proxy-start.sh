#!/usr/bin/env sh

# Align memory with helm/geocoder-proxy/values.yaml.  Helm values should be at least 50% higher.
java -Xms640m -Xmx640m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError -XX:MaxGCPauseMillis=75 --enable-native-access=ALL-UNNAMED \
     -jar proxy.jar