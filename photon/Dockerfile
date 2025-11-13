FROM bellsoft/liberica-runtime-container:jre-21-musl

ARG PHOTON_JAR_URL

WORKDIR /srv
RUN apk add --no-cache curl
RUN curl -sfL --retry 2 -o photon.jar ${PHOTON_JAR_URL}
RUN unzip -p photon.jar META-INF/MANIFEST.MF | grep Implementation-Version > version

COPY photon-start.sh ./
COPY photon_data.tar.gz ./
RUN tar xzf photon_data.tar.gz && rm photon_data.tar.gz
RUN chown -R 1000:1000 /srv
USER 1000:1000

CMD ["sh", "-c", "exec ./photon-start.sh"]
