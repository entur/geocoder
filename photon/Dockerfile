FROM gcr.io/distroless/java25-debian13:nonroot

WORKDIR /srv

COPY photon.jar ./

# photon_data is fetched by the `fetch-photon-data` init container (see
# helm/geocoder-photon/values.yaml) and shared in via a volume mounted at
# /srv/photon_data. Nothing is downloaded here, so this image needs no
# shell/curl/tar - hence distroless. For local runs, populate ./photon_data
# and use photon-start.sh instead.
#
# The old entrypoint deleted stale *.lock files on start; that's dropped here.
# Photon's embedded OpenSearch uses NativeFSLockFactory, whose node lock is
# released when the JVM exits (incl. OOMKill), so a leftover lock file is
# harmless when the same pod restarts this container onto the kept volume.
#
# Heap 1536m fits within the 3072Mi container request; keep aligned with
# helm/geocoder-photon/values.yaml (request >= 50% above heap).
ENTRYPOINT ["java", \
  "-Xms1536m", "-Xmx1536m", \
  "-XX:+UseG1GC", "-XX:+UseStringDeduplication", "-XX:MaxGCPauseMillis=75", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "photon.jar", \
  "serve", "-default-language", "no", \
  "-listen-ip", "0.0.0.0", "-listen-port", "2322", \
  "-metrics-enable", "prometheus"]
