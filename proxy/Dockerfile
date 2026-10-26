FROM gcr.io/distroless/java25-debian13:nonroot@sha256:5d3177252775d4b01603470fe21c015393019089904f68724bd37242e4bc6e6d

WORKDIR /srv

COPY ./build/libs/proxy-all.jar proxy.jar

# No shell in distroless, so the java invocation that used to live in
# proxy-start.sh is inlined here. Keep -Xmx aligned with
# helm/geocoder-proxy/values.yaml (helm request >= 50% above heap).
ENTRYPOINT ["java", \
  "-Xms640m", "-Xmx640m", \
  "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-XX:MaxGCPauseMillis=75", \
  "--enable-native-access=ALL-UNNAMED", \
  "-jar", "proxy.jar"]
