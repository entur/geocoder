FROM bellsoft/liberica-runtime-container:jre-21-musl

ARG PHOTON_JAR_URL

WORKDIR /srv

SHELL ["/bin/sh", "-euo", "pipefail", "-c"]

RUN apk add --no-cache curl
RUN curl -sfL --retry 2 -o photon.jar "${PHOTON_JAR_URL}"
RUN unzip -p photon.jar META-INF/MANIFEST.MF \
    | grep -E 'Implementation-Version' || true > version

COPY photon-start.sh ./
COPY photon_data.tar.gz ./
RUN tar xzf photon_data.tar.gz && rm photon_data.tar.gz
RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "exec ./photon-start.sh"]
