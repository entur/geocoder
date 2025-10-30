FROM bellsoft/liberica-runtime-container:jre-21-musl AS build-image

WORKDIR /srv
SHELL ["/bin/ash", "-co", "pipefail"]

COPY ./build/libs/converter-all.jar convert.jar
RUN apk add --no-cache curl

ENV ADRESSE_URL=https://nedlasting.geonorge.no/geonorge/Basisdata/MatrikkelenAdresse/CSV/Basisdata_0000_Norge_25833_MatrikkelenAdresse_CSV.zip \
    STEDSNAVN_URL=https://nedlasting.geonorge.no/geonorge/Basisdata/Stedsnavn/GML/Basisdata_0000_Norge_25833_Stedsnavn_GML.zip \
    TIAMAT_URL=https://storage.googleapis.com/marduk-production/tiamat/Current_latest.zip \
    OSM_URL=https://storage.googleapis.com/ror-osmdata-prd/osm-data/norway-latest.osm.pbf \
    PHOTON_URL=https://github.com/entur/photon/releases/download/with-metrics/photon-0.7.0.jar

RUN curl -sfL --retry 2 $ADRESSE_URL | unzip -p - > adresse.csv && \
    curl -sfL --retry 2 $STEDSNAVN_URL | unzip -p - > stedsnavn.gml && \
    java -jar convert.jar -m adresse.csv -g stedsnavn.gml -o nominatim.ndjson && \
    rm adresse.csv stedsnavn.gml

RUN curl -sfL --retry 2 $TIAMAT_URL -o tiamat.zip && \
    unzip -p tiamat.zip > tiamat.xml && \
    java -jar convert.jar -a -s tiamat.xml -o nominatim.ndjson && \
    rm tiamat.*

RUN curl -sfLO --retry 2 $OSM_URL && \
    java -jar convert.jar -a -p norway-latest.osm.pbf -o nominatim.ndjson && \
    rm norway-latest.osm.pbf

RUN curl -sfL --retry 2 $PHOTON_URL -o photon.jar && \
    java -jar photon.jar \
        -nominatim-import \
        -import-file nominatim.ndjson \
        -languages no,en \
        -extra-tags ALL

FROM scratch

WORKDIR /
COPY --from=build-image /srv/photon.jar photon.jar
COPY --from=build-image /srv/photon_data photon_data