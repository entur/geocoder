FROM bellsoft/liberica-runtime-container:jre-21-musl

ARG PHOTON_JAR_URL

WORKDIR /srv

SHELL ["/bin/ash", "-eo", "pipefail", "-c"]

RUN apk add --no-cache curl

COPY photon.jar ./
RUN unzip -p photon.jar META-INF/MANIFEST.MF \
    | grep -E 'Git-Commit' || true > version

COPY photon-start.sh ./
COPY photon_data ./photon_data

RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "exec ./photon-start.sh"]
